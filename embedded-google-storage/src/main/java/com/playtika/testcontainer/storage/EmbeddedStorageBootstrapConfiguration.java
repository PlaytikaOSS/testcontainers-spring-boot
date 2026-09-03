package com.playtika.testcontainer.storage;

import com.playtika.testcontainer.common.spring.ContainerStartupCoordinator;
import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import io.aiven.testcontainers.fakegcsserver.FakeGcsServerContainer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommons;
import static com.playtika.testcontainer.storage.StorageProperties.BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER;

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
                                                      @Qualifier(BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER) FakeGcsServerContainer storageServer,
                                                      ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                storageServer,
                StorageProperties.PORT,
                "storage");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.google.storage", "embeddedGoogleStorageToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER, destroyMethod = "stop")
    FakeGcsServerContainer storageServer(ConfigurableEnvironment environment,
                                         StorageProperties properties,
                                         Optional<Network> network,
                                         ContainerStartupCoordinator startupCoordinator) {
        FakeGcsServerContainer storageContainer =
                new FakeGcsServerContainer(ContainerUtils.getDockerImageName(properties))
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

        FakeGcsServerContainer configuredStorageContainer = (FakeGcsServerContainer) configureCommons(storageContainer, properties, log);
        startupCoordinator.schedule(() -> {
            ContainerUtils.startAndLogTime(configuredStorageContainer, log);
            registerStorageEnvironment(configuredStorageContainer, environment, properties);
        });
        return configuredStorageContainer;
    }

    private void registerStorageEnvironment(
            FakeGcsServerContainer container,
            ConfigurableEnvironment environment,
            StorageProperties properties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.google.storage.host", container.getHost());
        map.put("embedded.google.storage.port", container.getMappedPort(StorageProperties.PORT));
        map.put("embedded.google.storage.endpoint", container.url());
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
            ConfigurableEnvironment environment,
            StorageProperties storageProperties,
            ContainerStartupCoordinator startupCoordinator) {
        // the endpoint property is only registered once the storage container has started, so the
        // scheduled start must be flushed before it can be read - a @Value parameter would resolve
        // too early (at argument-binding time, before this method body runs)
        startupCoordinator.flush();
        String endpoint = environment.getProperty("embedded.google.storage.endpoint");
        return new StorageResourcesGenerator(endpoint, storageProperties);
    }
}
