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
import org.testcontainers.containers.Network;
import org.testcontainers.gcloud.PubSubEmulatorContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.pubsub.PubsubProperties.BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB;

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
                                                     @Qualifier(BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB) PubSubEmulatorContainer pubsub,
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

    @Bean(name = BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB, destroyMethod = "stop")
    public PubSubEmulatorContainer pubsub(ConfigurableEnvironment environment,
                                          PubsubProperties properties,
                                          Optional<Network> network) {
        PubSubEmulatorContainer pubsubContainer =
                new PubSubEmulatorContainer(ContainerUtils.getDockerImageName(properties))
                        .withNetworkAliases(GOOGLE_PUB_SUB_NETWORK_ALIAS);

        network.ifPresent(pubsubContainer::withNetwork);

        pubsubContainer = (PubSubEmulatorContainer) configureCommonsAndStart(pubsubContainer, properties, log);
        registerPubsubEnvironment(pubsubContainer, environment, properties);
        return pubsubContainer;
    }

    private void registerPubsubEnvironment(PubSubEmulatorContainer container,
                                           ConfigurableEnvironment environment,
                                           PubsubProperties properties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.google.pubsub.port", container.getMappedPort(properties.getPort()));
        map.put("embedded.google.pubsub.host", container.getHost());
        map.put("embedded.google.pubsub.project-id", properties.getProjectId());
        map.put("embedded.google.pubsub.networkAlias", GOOGLE_PUB_SUB_NETWORK_ALIAS);
        map.put("embedded.google.pubsub.internalPort", properties.getPort());

        log.info("Started Google Cloud Pubsub emulator. Connection details: {}, ", map);
        log.info("Consult with the doc https://cloud.google.com/pubsub/docs/emulator for more details");

        MapPropertySource propertySource = new MapPropertySource("embeddedGooglePubsubInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    @Bean(name = BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_MANAGED_CHANNEL)
    public ManagedChannel managedChannel(@Qualifier(BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB) PubSubEmulatorContainer pubsub) {
        return ManagedChannelBuilder
                .forTarget(pubsub.getEmulatorEndpoint())
                .usePlaintext()
                .build();
    }

    @Bean(name = BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_RESOURCES_GENERATOR)
    public PubSubResourcesGenerator pubSubResourcesGenerator(@Qualifier(BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_MANAGED_CHANNEL) ManagedChannel managedChannel,
                                                             PubsubProperties properties) throws IOException {
        return new PubSubResourcesGenerator(managedChannel, properties.getProjectId(), properties.getTopicsAndSubscriptions());
    }
}
