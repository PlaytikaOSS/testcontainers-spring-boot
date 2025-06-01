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
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.io.IOException;
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

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.google.pubsub", "embeddedGooglePubSubToxiproxyInfo", environment);

        return proxy;
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "google.pubsub")
    public DynamicPropertyRegistrar googlePubSubToxiProxyDynamicPropertyRegistrar(@Qualifier("googlePubSubContainerProxy") ToxiproxyContainer.ContainerProxy proxy) {
        return registry -> {
            registry.add("embedded.google.pubsub.toxiproxy.host", proxy::getContainerIpAddress);
            registry.add("embedded.google.pubsub.toxiproxy.port", proxy::getProxyPort);
            registry.add("embedded.google.pubsub.toxiproxy.proxyName", proxy::getName);
        };
    }

    @Bean(name = BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB, destroyMethod = "stop")
    public GenericContainer<?> pubsub(PubsubProperties properties, Optional<Network> network) {
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
        return pubsubContainer;
    }

    @Bean
    public DynamicPropertyRegistrar googlePubSubDynamicPropertyRegistrar(@Qualifier(BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB) GenericContainer<?> container, PubsubProperties properties) {
        return registry -> {
            registry.add("embedded.google.pubsub.port", () -> container.getMappedPort(properties.getPort()));
            registry.add("embedded.google.pubsub.host", container::getHost);
            registry.add("embedded.google.pubsub.project-id", properties::getProjectId);
            registry.add("embedded.google.pubsub.networkAlias", () -> GOOGLE_PUB_SUB_NETWORK_ALIAS);
            registry.add("embedded.google.pubsub.internalPort", properties::getPort);
        };
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
