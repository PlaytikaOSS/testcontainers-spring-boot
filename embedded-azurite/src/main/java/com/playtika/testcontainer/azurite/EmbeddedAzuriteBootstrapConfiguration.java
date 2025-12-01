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
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

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
                                                    AzuriteProperties properties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                azurite,
                properties.getBlobStoragePort(),
                "azurite");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "azurite")
    public DynamicPropertyRegistrar azuriteBlobToxiProxyDynamicPropertyRegistrar(ToxiproxyClientProxy azuriteBlobContainerProxy) {
        return registry -> {
            registry.add("embedded.azurite.toxiproxy.host", azuriteBlobContainerProxy::getContainerIpAddress);
            registry.add("embedded.azurite.toxiproxy.blobStoragePort", azuriteBlobContainerProxy::getProxyPort);
            registry.add("embedded.azurite.toxiproxy.proxyName", azuriteBlobContainerProxy::getName);
            log.info("Started Azurite ToxiProxy connection details {}", Map.of(
                "embedded.azurite.toxiproxy.host", azuriteBlobContainerProxy.getContainerIpAddress(),
                "embedded.azurite.toxiproxy.blobStoragePort", azuriteBlobContainerProxy.getProxyPort(),
                "embedded.azurite.toxiproxy.proxyName", azuriteBlobContainerProxy.getName()
            ));
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "azurite")
    ToxiproxyClientProxy azuriteQueueContainerProxy(ToxiproxyClient toxiproxyClient,
                                                     ToxiproxyContainer toxiproxyContainer,
                                                     @Qualifier(AZURITE_BEAN_NAME) GenericContainer<?> azurite,
                                                     AzuriteProperties properties) {
        return ToxiproxyHelper.createProxy(
            toxiproxyClient,
            toxiproxyContainer,
            azurite,
            properties.getQueueStoragePort(),
            "azurite");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "azurite")
    public DynamicPropertyRegistrar azuriteQueueToxiProxyDynamicPropertyRegistrar(ToxiproxyClientProxy azuriteQueueContainerProxy) {
        return registry -> {
            registry.add("embedded.azurite.toxiproxy.host", azuriteQueueContainerProxy::getContainerIpAddress);
            registry.add("embedded.azurite.toxiproxy.queueStoragePort", azuriteQueueContainerProxy::getProxyPort);
            registry.add("embedded.azurite.toxiproxy.proxyName", azuriteQueueContainerProxy::getName);
            log.info("Started Azurite ToxiProxy connection details {}", Map.of(
                "embedded.azurite.toxiproxy.host", azuriteQueueContainerProxy.getContainerIpAddress(),
                "embedded.azurite.toxiproxy.queueStoragePort", azuriteQueueContainerProxy.getProxyPort(),
                "embedded.azurite.toxiproxy.proxyName", azuriteQueueContainerProxy.getName()
            ));
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "azurite")
    ToxiproxyClientProxy azuriteTableContainerProxy(ToxiproxyClient toxiproxyClient,
                                                     ToxiproxyContainer toxiproxyContainer,
                                                     @Qualifier(AZURITE_BEAN_NAME) GenericContainer<?> azurite,
                                                     AzuriteProperties properties) {

        return ToxiproxyHelper.createProxy(
            toxiproxyClient,
            toxiproxyContainer,
            azurite,
            properties.getTableStoragePort(),
            "azurite");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "azurite")
    public DynamicPropertyRegistrar azuriteTableToxiProxyDynamicPropertyRegistrar(ToxiproxyClientProxy azuriteTableContainerProxy) {
        return registry -> {
            registry.add("embedded.azurite.toxiproxy.host", azuriteTableContainerProxy::getContainerIpAddress);
            registry.add("embedded.azurite.toxiproxy.tableStoragePort", azuriteTableContainerProxy::getProxyPort);
            registry.add("embedded.azurite.toxiproxy.proxyName", azuriteTableContainerProxy::getName);
            log.info("Started Azurite ToxiProxy connection details {}", Map.of(
                "embedded.azurite.toxiproxy.host", azuriteTableContainerProxy.getContainerIpAddress(),
                "embedded.azurite.toxiproxy.tableStoragePort", azuriteTableContainerProxy.getProxyPort(),
                "embedded.azurite.toxiproxy.proxyName", azuriteTableContainerProxy.getName()
            ));
        };
    }

    @Bean(name = AZURITE_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer<?> azurite(AzuriteProperties properties,
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
        return azuriteContainer;
    }

    @Bean
    public DynamicPropertyRegistrar azuriteDynamicPropertyRegistrar(
            @Qualifier(AZURITE_BEAN_NAME) GenericContainer<?> azurite,
            AzuriteProperties properties) {
        return registry -> {
            Integer mappedBlobStoragePort = azurite.getMappedPort(properties.getBlobStoragePort());
            Integer mappedQueueStoragePort = azurite.getMappedPort(properties.getQueueStoragePort());
            Integer mappedTableStoragePort = azurite.getMappedPort(properties.getTableStoragePort());
            String host = azurite.getHost();

            registry.add("embedded.azurite.host", () -> host);
            registry.add("embedded.azurite.blobStoragePort", () -> mappedBlobStoragePort);
            registry.add("embedded.azurite.queueStoragePort", () -> mappedQueueStoragePort);
            registry.add("embedded.azurite.tableStoragePort", () -> mappedTableStoragePort);
            registry.add("embedded.azurite.account-name", () -> AzuriteProperties.ACCOUNT_NAME);
            registry.add("embedded.azurite.account-key", () -> AzuriteProperties.ACCOUNT_KEY);
            registry.add("embedded.azurite.blob-endpoint", () -> "http://" + host + ":" + mappedBlobStoragePort + "/" + AzuriteProperties.ACCOUNT_NAME);
            registry.add("embedded.azurite.queue-endpoint", () -> "http://" + host + ":" + mappedQueueStoragePort + "/" + AzuriteProperties.ACCOUNT_NAME);
            registry.add("embedded.azurite.table-endpoint", () -> "http://" + host + ":" + mappedTableStoragePort + "/" + AzuriteProperties.ACCOUNT_NAME);
            registry.add("embedded.azurite.networkAlias", () -> AZURITE_BLOB_NETWORK_ALIAS);

            log.info("Started Azurite. Connection details: host={}, blobStoragePort={}, queueStoragePort={}, tableStoragePort={}, " +
                            "blob-endpoint=http://{}:{}/{}, queue-endpoint=http://{}:{}/{}, table-endpoint=http://{}:{}/{}",
                    host, mappedBlobStoragePort, mappedQueueStoragePort, mappedTableStoragePort,
                    host, mappedBlobStoragePort, AzuriteProperties.ACCOUNT_NAME,
                    host, mappedQueueStoragePort, AzuriteProperties.ACCOUNT_NAME,
                    host, mappedTableStoragePort, AzuriteProperties.ACCOUNT_NAME);
        };
    }

}
