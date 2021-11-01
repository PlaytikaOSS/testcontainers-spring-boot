package com.playtika.test.pubsub;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
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
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.io.IOException;
import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static java.lang.String.format;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.google.pubsub.enabled", matchIfMissing = true)
@EnableConfigurationProperties({PubsubProperties.class})
public class EmbeddedPubsubBootstrapConfiguration {

    public static final String BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_RESOURCES_GENERATOR = "embeddedGooglePubsubResourcesGenerator";
    public static final String BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_MANAGED_CHANNEL = "embeddedGooglePubsubManagedChannel";

    @Bean(name = PubsubProperties.BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB, destroyMethod = "stop")
    public GenericContainer pubsub(ConfigurableEnvironment environment,
                                   PubsubProperties properties) {
        log.info("Starting Google Cloud Pubsub emulator. Docker image: {}", properties.getDockerImage());

        GenericContainer container = new GenericContainer(properties.getDockerImage())
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
            ).waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*started.*$"));

        container = configureCommonsAndStart(container, properties, log);
        registerPubsubEnvironment(container, environment, properties);
        return container;
    }

    private void registerPubsubEnvironment(GenericContainer container,
                                           ConfigurableEnvironment environment,
                                           PubsubProperties properties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.google.pubsub.port", container.getMappedPort(properties.getPort()));
        map.put("embedded.google.pubsub.host", container.getContainerIpAddress());
        map.put("embedded.google.pubsub.project-id", properties.getProjectId());

        log.info("Started Google Cloud Pubsub emulator. Connection details: {}, ", map);
        log.info("Consult with the doc https://cloud.google.com/pubsub/docs/emulator for more details");

        MapPropertySource propertySource = new MapPropertySource("embeddedGooglePubsubInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    @Bean(name = BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_MANAGED_CHANNEL)
    public ManagedChannel managedChannel(@Qualifier(PubsubProperties.BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB) GenericContainer pubsub, PubsubProperties properties) {
        return ManagedChannelBuilder
            .forAddress(pubsub.getContainerIpAddress(), pubsub.getMappedPort(properties.getPort())).usePlaintext()
            .build();
    }

    @Bean(name = BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_RESOURCES_GENERATOR)
    public PubSubResourcesGenerator pubSubResourcesGenerator(@Qualifier(BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_MANAGED_CHANNEL) ManagedChannel managedChannel,
                                                             PubsubProperties properties) throws IOException {
        return new PubSubResourcesGenerator(managedChannel, properties.getProjectId(), properties.getTopicsAndSubscriptions());
    }
}
