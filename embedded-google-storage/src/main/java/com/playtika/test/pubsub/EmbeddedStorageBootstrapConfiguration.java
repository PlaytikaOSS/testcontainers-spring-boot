package com.playtika.test.pubsub;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import java.io.IOException;
import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.okhttp3.MediaType;
import org.testcontainers.shaded.okhttp3.OkHttpClient;
import org.testcontainers.shaded.okhttp3.Request;
import org.testcontainers.shaded.okhttp3.RequestBody;
import org.testcontainers.shaded.okhttp3.Response;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static java.lang.String.format;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.google.storage.enabled", matchIfMissing = true)
@EnableConfigurationProperties({StorageProperties.class})
public class EmbeddedStorageBootstrapConfiguration {

    //    public static final String BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_RESOURCES_GENERATOR = "embeddedGoogleStorageResourcesGenerator";
    //    public static final String BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_MANAGED_CHANNEL = "embeddedGoogleStorageManagedChannel";

    @Bean(name = StorageProperties.BEAN_NAME_EMBEDDED_GOOGLE_STORAGE, destroyMethod = "stop")
    public GenericContainer storage(
        ConfigurableEnvironment environment,
        StorageProperties properties) throws IOException {
        log.info("Starting Google Cloud Fake Storage Server. Docker image: {}", properties.getDockerImage());

        GenericContainer<?> container = new GenericContainer<>("sergseven/fake-gcs-server:v2")
            .withExposedPorts(properties.getPort())
            .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint(
                "/bin/fake-gcs-server",
                "-scheme", properties.getScheme(),
                "-host", properties.getHost(),
                "-port", String.valueOf(properties.getPort()),
                "-event.pubsub-project-id", properties.getEventPubsubProjectId(),
                "-event.pubsub-topic", properties.getEventPubsubTopic(),
                "-event.object-prefix", properties.getEventPrefix(),
                "-event.list", properties.getEventList()
            ));

        //        GenericContainer<?> container = new GenericContainer(properties.getDockerImage())
        //            .withExposedPorts(properties.getPort())
        //            .withCommand(
        //                "/bin/sh",
        //                "-c",
        //                format(
        //                    "gcloud beta emulators pubsub start --project %s --host-port=%s:%d",
        //                    properties.getProjectId(),
        //                    properties.getHost(),
        //                    properties.getPort()
        //                )
        //            ).waitingFor(new LogMessageWaitStrategy().withRegEx("(?s).*started.*$"));

        container = configureCommonsAndStart(container, properties, log);
        prepareContainerConfiguration(container, properties);
        registerStorageEnvironment(container, environment, properties);
        return container;
    }

    private void prepareContainerConfiguration(GenericContainer<?> container, StorageProperties properties) throws IOException {
        String containerUrl = format(
            "%s://%s:%d",
            properties.getScheme(),
            container.getContainerIpAddress(),
            container.getMappedPort(properties.getPort()));
        String modifyExternalUrlRequestUri = format("%s%s", containerUrl, "/internal/config/url/external");
        log.info("Google Cloud Fake Storage Server modifyExternalUrlRequestUri={}", modifyExternalUrlRequestUri);

        String updateExternalUrlJson = "{"
            + "\"externalUrl\": \"" + containerUrl + "\""
            + "}";

        try {
            Request request = new Request.Builder()
                .url(modifyExternalUrlRequestUri)
                .put(RequestBody.create(MediaType.get("application/json"), updateExternalUrlJson))
                .build();

            OkHttpClient client = new OkHttpClient();
            Response response = client.newCall(request).execute();

            if (response.code() != 200) {
                log.error(
                    "error updating Google Cloud Fake Storage Server with external url, response status code {} != 200 message {}",
                    response.code(),
                    response.message());
            }
        } catch (Exception e) {
            log.error("error updating Google Cloud Fake Storage Server with external host", e);
            throw e;
        }
    }

    private void registerStorageEnvironment(
        GenericContainer container,
        ConfigurableEnvironment environment,
        StorageProperties properties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.google.storage.port", container.getMappedPort(properties.getPort()));
        map.put("embedded.google.storage.host", container.getContainerIpAddress());
        map.put("embedded.google.storage.project-id", properties.getProjectId());

        log.info("Started Google Cloud Fake Storage Server. Connection details: {}, ", map);
        log.info("Consult with the doc https://github.com/fsouza/fake-gcs-server for more details");

        MapPropertySource propertySource = new MapPropertySource("embeddedGoogleStorageInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    //    @Bean(name = BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_MANAGED_CHANNEL)
    //    public ManagedChannel managedChannel(
    //        @Qualifier(PubsubProperties.BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB) GenericContainer pubsub,
    //        PubsubProperties properties) {
    //        return ManagedChannelBuilder
    //            .forAddress(pubsub.getContainerIpAddress(), pubsub.getMappedPort(properties.getPort())).usePlaintext()
    //            .build();
    //    }

    //    @Bean(name = BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_RESOURCES_GENERATOR)
    //    public PubSubResourcesGenerator pubSubResourcesGenerator(
    //        @Qualifier(BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB_MANAGED_CHANNEL) ManagedChannel managedChannel,
    //        PubsubProperties properties) throws IOException {
    //        return new PubSubResourcesGenerator(managedChannel, properties.getProjectId(), properties.getTopicsAndSubscriptions());
    //    }
}
