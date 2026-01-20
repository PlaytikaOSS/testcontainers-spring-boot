package com.playtika.testcontainer.azurite;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
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
@AutoConfigureBefore(name = {
        "com.azure.spring.cloud.autoconfigure.storage.blob.AzureStorageBlobAutoConfiguration",
        "com.azure.spring.cloud.autoconfigure.storage.queue.AzureStorageQueueAutoConfiguration"
})
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "embedded.azurite.enabled", matchIfMissing = true)
@EnableConfigurationProperties(AzuriteProperties.class)
public class EmbeddedAzuriteBootstrapConfiguration {

    private static final String AZURITE_BLOB_NETWORK_ALIAS = "azurite-blob.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "azurite")
    ToxiproxyClientProxy azuriteBlobContainerProxy(ToxiproxyClient toxiproxyClient,
                                                    ToxiproxyContainer toxiproxyContainer,
                                                    @Qualifier(AZURITE_BEAN_NAME) GenericContainer<?> azurite,
                                                    AzuriteProperties properties,
                                                    ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                azurite,
                properties.getBlobStoragePort(),
                "azurite");

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.azurite.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.azurite.toxiproxy.blobStoragePort", proxy.getProxyPort());
        map.put("embedded.azurite.toxiproxy.proxyName", proxy.getName());
        log.info("Started Azurite ToxiProxy connection details {}", Map.of(
            "embedded.azurite.toxiproxy.host", proxy.getContainerIpAddress(),
            "embedded.azurite.toxiproxy.blobStoragePort", proxy.getProxyPort(),
            "embedded.azurite.toxiproxy.proxyName", proxy.getName()
        ));

        MapPropertySource propertySource = new MapPropertySource("embeddedAzuriteBlobToxiProxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        return proxy;
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "azurite")
    ToxiproxyClientProxy azuriteQueueContainerProxy(ToxiproxyClient toxiproxyClient,
                                                     ToxiproxyContainer toxiproxyContainer,
                                                     @Qualifier(AZURITE_BEAN_NAME) GenericContainer<?> azurite,
                                                     AzuriteProperties properties,
                                                     ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
            toxiproxyClient,
            toxiproxyContainer,
            azurite,
            properties.getQueueStoragePort(),
            "azurite");

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.azurite.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.azurite.toxiproxy.queueStoragePort", proxy.getProxyPort());
        map.put("embedded.azurite.toxiproxy.proxyName", proxy.getName());
        log.info("Started Azurite ToxiProxy connection details {}", Map.of(
            "embedded.azurite.toxiproxy.host", proxy.getContainerIpAddress(),
            "embedded.azurite.toxiproxy.queueStoragePort", proxy.getProxyPort(),
            "embedded.azurite.toxiproxy.proxyName", proxy.getName()
        ));

        MapPropertySource propertySource = new MapPropertySource("embeddedAzuriteQueueToxiProxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        return proxy;
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "azurite")
    ToxiproxyClientProxy azuriteTableContainerProxy(ToxiproxyClient toxiproxyClient,
                                                     ToxiproxyContainer toxiproxyContainer,
                                                     @Qualifier(AZURITE_BEAN_NAME) GenericContainer<?> azurite,
                                                     AzuriteProperties properties,
                                                     ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
            toxiproxyClient,
            toxiproxyContainer,
            azurite,
            properties.getTableStoragePort(),
            "azurite");

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.azurite.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.azurite.toxiproxy.tableStoragePort", proxy.getProxyPort());
        map.put("embedded.azurite.toxiproxy.proxyName", proxy.getName());
        log.info("Started Azurite ToxiProxy connection details {}", Map.of(
            "embedded.azurite.toxiproxy.host", proxy.getContainerIpAddress(),
            "embedded.azurite.toxiproxy.tableStoragePort", proxy.getProxyPort(),
            "embedded.azurite.toxiproxy.proxyName", proxy.getName()
        ));

        MapPropertySource propertySource = new MapPropertySource("embeddedAzuriteTableToxiProxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
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
        registerAzuriteEnvironment(azuriteContainer, environment, properties);
        return azuriteContainer;
    }

    private void registerAzuriteEnvironment(GenericContainer<?> azurite,
                                            ConfigurableEnvironment environment,
                                            AzuriteProperties properties) {
        Integer mappedBlobStoragePort = azurite.getMappedPort(properties.getBlobStoragePort());
        Integer mappedQueueStoragePort = azurite.getMappedPort(properties.getQueueStoragePort());
        Integer mappedTableStoragePort = azurite.getMappedPort(properties.getTableStoragePort());
        String host = azurite.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.azurite.host", host);
        map.put("embedded.azurite.blobStoragePort", mappedBlobStoragePort);
        map.put("embedded.azurite.queueStoragePort", mappedQueueStoragePort);
        map.put("embedded.azurite.tableStoragePort", mappedTableStoragePort);
        map.put("embedded.azurite.account-name", AzuriteProperties.ACCOUNT_NAME);
        map.put("embedded.azurite.account-key", AzuriteProperties.ACCOUNT_KEY);
        map.put("embedded.azurite.blob-endpoint", "http://" + host + ":" + mappedBlobStoragePort + "/" + AzuriteProperties.ACCOUNT_NAME);
        map.put("embedded.azurite.queue-endpoint", "http://" + host + ":" + mappedQueueStoragePort + "/" + AzuriteProperties.ACCOUNT_NAME);
        map.put("embedded.azurite.table-endpoint", "http://" + host + ":" + mappedTableStoragePort + "/" + AzuriteProperties.ACCOUNT_NAME);
        map.put("embedded.azurite.networkAlias", AZURITE_BLOB_NETWORK_ALIAS);

        log.info("Started Azurite. Connection details: host={}, blobStoragePort={}, queueStoragePort={}, tableStoragePort={}, " +
                        "blob-endpoint=http://{}:{}/{}, queue-endpoint=http://{}:{}/{}, table-endpoint=http://{}:{}/{}",
                host, mappedBlobStoragePort, mappedQueueStoragePort, mappedTableStoragePort,
                host, mappedBlobStoragePort, AzuriteProperties.ACCOUNT_NAME,
                host, mappedQueueStoragePort, AzuriteProperties.ACCOUNT_NAME,
                host, mappedTableStoragePort, AzuriteProperties.ACCOUNT_NAME);

        MapPropertySource propertySource = new MapPropertySource("embeddedAzuriteInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
