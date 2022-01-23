package com.playtika.test.storage;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

import java.io.IOException;
import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static java.lang.String.format;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.google.storage.enabled", matchIfMissing = true)
@EnableConfigurationProperties(StorageProperties.class)
public class EmbeddedStorageBootstrapConfiguration {

    @Bean(name = StorageProperties.BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER, destroyMethod = "stop")
    GenericContainer<?> storageServer(
        ConfigurableEnvironment environment,
        StorageProperties properties) throws IOException {
        log.info("Starting Google Cloud Fake Storage Server. Docker image: {}", properties.getDockerImage());

        GenericContainer<?> container = new GenericContainer<>(properties.getDockerImage())
            .withExposedPorts(properties.getPort())
            .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint(
                "/bin/fake-gcs-server",
                "-scheme", "http",
                "-host", properties.getHost(),
                "-port", String.valueOf(properties.getPort()),
                "-event.pubsub-project-id", properties.getEventPubsubProjectId(),
                "-event.pubsub-topic", properties.getEventPubsubTopic(),
                "-event.object-prefix", properties.getEventPrefix(),
                "-event.list", properties.getEventList()
            ));

        container = configureCommonsAndStart(container, properties, log);
        prepareContainerConfiguration(container, properties);
        registerStorageEnvironment(container, environment, properties);
        return container;
    }

    private void prepareContainerConfiguration(GenericContainer<?> container, StorageProperties properties) throws IOException {
        try {
            String containerEndpoint = buildContainerEndpoint(container, properties);
            String modifyExternalUrlRequestUri = format("%s%s", containerEndpoint, "/internal/config");
            log.info("Google Cloud Fake Storage Server with externalUrl={}", containerEndpoint);

            String updateExternalUrlJson = "{"
                + "\"externalUrl\": \"" + containerEndpoint + "\""
                + "}";

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
        GenericContainer<?> container,
        ConfigurableEnvironment environment,
        StorageProperties properties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.google.storage.host", container.getContainerIpAddress());
        map.put("embedded.google.storage.port", container.getMappedPort(properties.getPort()));
        map.put("embedded.google.storage.endpoint", buildContainerEndpoint(container, properties));
        map.put("embedded.google.storage.project-id", properties.getProjectId());
        map.put("embedded.google.storage.bucket-location", properties.getBucketLocation());

        log.info("Started Google Cloud Fake Storage Server. Connection details: {}, ", map);
        log.info("Consult with the doc https://github.com/fsouza/fake-gcs-server for more details");

        MapPropertySource propertySource = new MapPropertySource("embeddedGoogleStorageInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    @Bean
    StorageResourcesGenerator storageResourcesGenerator(
        @Value("${embedded.google.storage.endpoint}") String endpoint,
        StorageProperties storageProperties) {
        return new StorageResourcesGenerator(endpoint, storageProperties);
    }

    private String buildContainerEndpoint(GenericContainer<?> container, StorageProperties properties) {
        return format(
            "http://%s:%d",
            container.getContainerIpAddress(),
            container.getMappedPort(properties.getPort()));
    }
}
