package com.playtika.testcontainer.azurite;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.azurite.AzuriteProperties.AZURITE_BEAN_NAME;
import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.azurite.enabled", matchIfMissing = true)
@EnableConfigurationProperties(AzuriteProperties.class)
public class EmbeddedAzuriteBootstrapConfiguration {

    private static final String AZURITE_BLOB_NETWORK_ALIAS = "azurite-blob.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "azurite")
    ToxiproxyContainer.ContainerProxy azuriteBlobContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                                @Qualifier(AZURITE_BEAN_NAME) GenericContainer<?> azurite,
                                                                AzuriteProperties properties,
                                                                ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(azurite, properties.getBlobStoragePort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.azurite.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.azurite.toxiproxy.blobStoragePort", proxy.getProxyPort());
        map.put("embedded.azurite.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedAzuriteBlobToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Azurite ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "azurite")
    ToxiproxyContainer.ContainerProxy azuriteQueueContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                                 @Qualifier(AZURITE_BEAN_NAME) GenericContainer<?> azurite,
                                                                 AzuriteProperties properties,
                                                                 ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(azurite, properties.getQueueStoragePort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.azurite.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.azurite.toxiproxy.queueStoragePor", proxy.getProxyPort());
        map.put("embedded.azurite.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedAzuriteQueueToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Azurite ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "azurite")
    ToxiproxyContainer.ContainerProxy azuriteTableContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                                 @Qualifier(AZURITE_BEAN_NAME) GenericContainer<?> azurite,
                                                                 AzuriteProperties properties,
                                                                 ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(azurite, properties.getTableStoragePort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.azurite.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.azurite.toxiproxy.tableStoragePort", proxy.getProxyPort());
        map.put("embedded.azurite.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedAzuriteTableToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Azurite ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = AZURITE_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer<?> azurite(ConfigurableEnvironment environment,
                                       AzuriteProperties properties,
                                       Optional<Network> network) {
        GenericContainer<?> azuriteContainer = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getBlobStoragePort(), properties.getQueueStoragePort(), properties.getTableStoragePort())
                .withNetworkAliases(AZURITE_BLOB_NETWORK_ALIAS)
                .withCommand("azurite",
                        "-l", "/data",
                        "--blobHost", "0.0.0.0",
                        "--blobPort", String.valueOf(properties.getBlobStoragePort()),
                        "--queueHost", "0.0.0.0",
                        "--queuePort", String.valueOf(properties.getQueueStoragePort()),
                        "--tableHost", "0.0.0.0",
                        "--tablePort", String.valueOf(properties.getTableStoragePort()),
                        "--skipApiVersionCheck");

        network.ifPresent(azuriteContainer::withNetwork);

        configureCommonsAndStart(azuriteContainer, properties, log);
        registerEnvironment(azuriteContainer, environment, properties);
        return azuriteContainer;
    }

    private void registerEnvironment(GenericContainer<?> azurite,
                                     ConfigurableEnvironment environment,
                                     AzuriteProperties properties) {

        Integer mappedBlobStoragePort = azurite.getMappedPort(properties.getBlobStoragePort());
        Integer mappedQueueStoragePort = azurite.getMappedPort(properties.getQueueStoragePort());
        Integer mappedTableStoragePort = azurite.getMappedPort(properties.getTableStoragePort());
        String host = azurite.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.azurite.host", host);
        map.put("embedded.azurite.blobStoragePort", mappedBlobStoragePort);
        map.put("embedded.azurite.queueStoragePor", mappedQueueStoragePort);
        map.put("embedded.azurite.tableStoragePort", mappedTableStoragePort);
        map.put("embedded.azurite.account-name", AzuriteProperties.ACCOUNT_NAME);
        map.put("embedded.azurite.account-key", AzuriteProperties.ACCOUNT_KEY);
        map.put("embedded.azurite.blob-endpoint", "http://" + host + ":" + mappedBlobStoragePort + "/" + AzuriteProperties.ACCOUNT_NAME);
        map.put("embedded.azurite.queue-endpoint", "http://" + host + ":" + mappedQueueStoragePort + "/" + AzuriteProperties.ACCOUNT_NAME);
        map.put("embedded.azurite.table-endpoint", "http://" + host + ":" + mappedTableStoragePort + "/" + AzuriteProperties.ACCOUNT_NAME);
        map.put("embedded.azurite.networkAlias", AZURITE_BLOB_NETWORK_ALIAS);

        log.info("Started Azurite. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedAzuriteInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
