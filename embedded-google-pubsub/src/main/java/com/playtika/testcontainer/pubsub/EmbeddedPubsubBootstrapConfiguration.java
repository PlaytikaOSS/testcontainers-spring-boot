package com.playtika.testcontainer.pubsub;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.pubsub.PubsubProperties.BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB;
import static java.lang.String.format;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.google.pubsub.enabled", matchIfMissing = true)
@EnableConfigurationProperties({PubsubProperties.class})
public class EmbeddedPubsubBootstrapConfiguration {

    private static final String GOOGLE_PUB_SUB_NETWORK_ALIAS = "googlepubsub.testcontainer.docker";
    public static final String BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_RESOURCES_GENERATOR = "embeddedGooglePubsubResourcesGenerator";
    public static final String BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_MANAGED_CHANNEL = "embeddedGooglePubsubManagedChannel";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "google.pubsub")
    ToxiproxyClientProxy googlePubSubContainerProxy(ToxiproxyClient toxiproxyClient,
                                                     ToxiproxyContainer toxiproxyContainer,
                                                     @Qualifier(BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB) GenericContainer<?> pubsub,
                                                     PubsubProperties properties,
                                                     ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                pubsub,
                properties.getPort(),
                "pubsub");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.google.pubsub", "embeddedPubsubToxiProxyInfo", environment);

        return proxy;
    }


    @Bean(name = BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB, destroyMethod = "stop")
    public GenericContainer<?> pubsub(PubsubProperties properties, ConfigurableEnvironment environment, Optional<Network> network) {
        GenericContainer<?> pubsubContainer = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
            .withExposedPorts(properties.getPort())
            .withCommand(
                "/bin/sh",
                "-c",
                format(
                    "gcloud beta emulators pubsub start --project %s --host-port=%s:%d",
                    properties.getProjectId(),
                    properties.getHost(),
                    properties.getPort()
                )
            ).waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*started.*$"))
            .withNetworkAliases(GOOGLE_PUB_SUB_NETWORK_ALIAS);

        network.ifPresent(pubsubContainer::withNetwork);

        pubsubContainer = configureCommonsAndStart(pubsubContainer, properties, log);

        // Set PUBSUB_EMULATOR_HOST system property immediately after container starts
        // This is needed for Spring Cloud GCP to detect emulator mode
        String emulatorHost = format("%s:%d", pubsubContainer.getHost(), pubsubContainer.getMappedPort(properties.getPort()));
        System.setProperty("PUBSUB_EMULATOR_HOST", emulatorHost);
        // Also set as Spring Boot property to ensure it's available during auto-configuration
        System.setProperty("spring.cloud.gcp.pubsub.emulatorHost", emulatorHost);
        System.setProperty("spring.cloud.gcp.pubsub.emulator-host", emulatorHost);

        registerPubsubEnvironment(pubsubContainer, environment, properties);

        return pubsubContainer;
    }

    private void registerPubsubEnvironment(GenericContainer<?> container, ConfigurableEnvironment environment, PubsubProperties properties) {
        String host = container.getHost();
        Integer port = container.getMappedPort(properties.getPort());
        String emulatorHost = format("%s:%d", host, port);

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.google.pubsub.port", port);
        map.put("embedded.google.pubsub.host", host);
        map.put("embedded.google.pubsub.project-id", properties.getProjectId());
        map.put("embedded.google.pubsub.networkAlias", GOOGLE_PUB_SUB_NETWORK_ALIAS);
        map.put("embedded.google.pubsub.internalPort", properties.getPort());

        // Register Spring Cloud GCP properties for auto-configuration
        // Support both camelCase and kebab-case property names
        map.put("spring.cloud.gcp.pubsub.emulatorHost", emulatorHost);
        map.put("spring.cloud.gcp.pubsub.emulator-host", emulatorHost);
        map.put("spring.cloud.gcp.project-id", properties.getProjectId());

        // Set PUBSUB_EMULATOR_HOST system property for Google Cloud SDK clients
        System.setProperty("PUBSUB_EMULATOR_HOST", emulatorHost);

        log.info("Started Google Cloud Pubsub emulator. Connection details: host={}, port={}, project-id={}",
                host, port, properties.getProjectId());
        log.info("Consult with the doc https://cloud.google.com/pubsub/docs/emulator for more details");

        MapPropertySource propertySource = new MapPropertySource("embeddedPubsubInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    @Bean(name = BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_MANAGED_CHANNEL)
    public ManagedChannel managedChannel(@Qualifier(BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB) GenericContainer<?> pubsub, PubsubProperties properties) {
        return ManagedChannelBuilder
            .forAddress(pubsub.getHost(), pubsub.getMappedPort(properties.getPort())).usePlaintext()
            .build();
    }

    @Bean(name = BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_RESOURCES_GENERATOR)
    public PubSubResourcesGenerator pubSubResourcesGenerator(@Qualifier(BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_MANAGED_CHANNEL) ManagedChannel managedChannel,
                                                             PubsubProperties properties) throws IOException {
        return new PubSubResourcesGenerator(managedChannel, properties.getProjectId(), properties.getTopicsAndSubscriptions());
    }
}
