package com.playtika.testcontainer.storage;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.storage.StorageProperties.BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER;
import static java.lang.String.format;

@Slf4j
@Configuration
@ConditionalOnClass(com.google.cloud.storage.Storage.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.google.storage.enabled", matchIfMissing = true)
@EnableConfigurationProperties(StorageProperties.class)
public class EmbeddedStorageBootstrapConfiguration {

    private static final String GOOGLE_STORAGE_NETWORK_ALIAS = "googlestorage.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "google.storage")
    ToxiproxyContainer.ContainerProxy googleStorageContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                            @Qualifier(BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER) GenericContainer<?> storageServer,
                                                            ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(storageServer, StorageProperties.PORT);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.google.storage.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.google.storage.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.google.storage.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedGoogleStorageToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Google Storage ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER, destroyMethod = "stop")
    GenericContainer<?> storageServer(ConfigurableEnvironment environment,
                                      StorageProperties properties,
                                      Optional<Network> network) throws IOException {

        GenericContainer<?> storageContainer = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(StorageProperties.PORT)
                .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint(
                        "/bin/fake-gcs-server",
                        "-backend", "memory",
                        "-scheme", "http",
                        "-host", "0.0.0.0",
                        "-port", String.valueOf(StorageProperties.PORT),
                        "-location", properties.getBucketLocation()
                ))
                .withNetworkAliases(GOOGLE_STORAGE_NETWORK_ALIAS);

        network.ifPresent(storageContainer::withNetwork);

        storageContainer = configureCommonsAndStart(storageContainer, properties, log);
        prepareContainerConfiguration(storageContainer);
        registerStorageEnvironment(storageContainer, environment, properties);
        return storageContainer;
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
        map.put("embedded.google.storage.host", container.getHost());
        map.put("embedded.google.storage.port", container.getMappedPort(StorageProperties.PORT));
        map.put("embedded.google.storage.endpoint", buildContainerEndpoint(container));
        map.put("embedded.google.storage.project-id", properties.getProjectId());
        map.put("embedded.google.storage.bucket-location", properties.getBucketLocation());
        map.put("embedded.google.storage.networkAlias", GOOGLE_STORAGE_NETWORK_ALIAS);
        map.put("embedded.google.storage.internalPort", StorageProperties.PORT);

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
                container.getHost(),
                container.getMappedPort(StorageProperties.PORT));
    }
}
