package com.playtika.test.storage;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static java.lang.String.format;

@Slf4j
@Configuration
@ConditionalOnClass(com.google.cloud.storage.Storage.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.google.storage.enabled", matchIfMissing = true)
@EnableConfigurationProperties(StorageProperties.class)
public class EmbeddedStorageBootstrapConfiguration {

    @Bean(name = StorageProperties.BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER, destroyMethod = "stop")
    GenericContainer<?> storageServer(
        ConfigurableEnvironment environment,
        StorageProperties properties) throws IOException {
        GenericContainer<?> container = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
            .withExposedPorts(StorageProperties.PORT)
            .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint(
                "/bin/fake-gcs-server",
                "-backend", "memory",
                "-scheme", "http",
                "-host", "0.0.0.0",
                "-port", String.valueOf(StorageProperties.PORT),
                "-location", properties.getBucketLocation()
            ));

        container = configureCommonsAndStart(container, properties, log);
        prepareContainerConfiguration(container);
        registerStorageEnvironment(container, environment, properties);
        return container;
    }

    private void prepareContainerConfiguration(GenericContainer<?> container) throws IOException {
        String containerEndpoint = buildContainerEndpoint(container);

        log.info("Google Cloud Fake Storage Server with externalUrl={}", containerEndpoint);
        new GoogleCloudStorageHttpClient()
                .sendUpdateConfigRequest(containerEndpoint);
    }

    private void registerStorageEnvironment(
        GenericContainer<?> container,
        ConfigurableEnvironment environment,
        StorageProperties properties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.google.storage.host", container.getContainerIpAddress());
        map.put("embedded.google.storage.port", container.getMappedPort(StorageProperties.PORT));
        map.put("embedded.google.storage.endpoint", buildContainerEndpoint(container));
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

    private String buildContainerEndpoint(GenericContainer<?> container) {
        return format(
            "http://%s:%d",
            container.getContainerIpAddress(),
            container.getMappedPort(StorageProperties.PORT));
    }
}
