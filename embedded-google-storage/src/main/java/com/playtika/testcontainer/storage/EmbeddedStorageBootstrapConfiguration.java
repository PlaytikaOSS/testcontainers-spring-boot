package com.playtika.testcontainer.storage;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.io.IOException;
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
    ToxiproxyClientProxy googleStorageContainerProxy(ToxiproxyClient toxiproxyClient,
                                                      ToxiproxyContainer toxiproxyContainer,
                                                      @Qualifier(BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER) GenericContainer<?> storageServer) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                storageServer,
                StorageProperties.PORT,
                "storage");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "google.storage")
    public DynamicPropertyRegistrar googleStorageToxiProxyDynamicPropertyRegistrar(@Qualifier("googleStorageContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.google.storage");
    }

    @Bean(name = BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER, destroyMethod = "stop")
    public GenericContainer<?> storageServer(StorageProperties properties, Optional<Network> network) throws IOException {
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
        return storageContainer;
    }

    @Bean
    public DynamicPropertyRegistrar googleStorageDynamicPropertyRegistrar(@Qualifier(BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER) GenericContainer<?> container, StorageProperties properties) {
        return registry -> {
            registry.add("embedded.google.storage.host", container::getHost);
            registry.add("embedded.google.storage.port", () -> container.getMappedPort(StorageProperties.PORT));
            registry.add("embedded.google.storage.endpoint", () -> buildContainerEndpoint(container));
            registry.add("embedded.google.storage.project-id", properties::getProjectId);
            registry.add("embedded.google.storage.bucket-location", properties::getBucketLocation);
            registry.add("embedded.google.storage.networkAlias", () -> GOOGLE_STORAGE_NETWORK_ALIAS);
            registry.add("embedded.google.storage.internalPort", () -> StorageProperties.PORT);
        };
    }

    private void prepareContainerConfiguration(GenericContainer<?> container) throws IOException {
        String containerEndpoint = buildContainerEndpoint(container);

        log.info("Google Cloud Fake Storage Server with externalUrl={}", containerEndpoint);
        new GoogleCloudStorageHttpClient()
                .sendUpdateConfigRequest(containerEndpoint);
    }

    @Bean
    StorageResourcesGenerator storageResourcesGenerator(
            @Qualifier(BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER) GenericContainer<?> container,
            StorageProperties storageProperties) {
        String endpoint = buildContainerEndpoint(container);
        return new StorageResourcesGenerator(endpoint, storageProperties);
    }

    private String buildContainerEndpoint(GenericContainer<?> container) {
        return format(
                "http://%s:%d",
                container.getHost(),
                container.getMappedPort(StorageProperties.PORT));
    }
}
