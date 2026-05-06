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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.azure.AzuriteContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
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
    ToxiproxyClientProxy azuriteBlobContainerProxy(ToxiproxyClient toxiproxyClient,
                                                    ToxiproxyContainer toxiproxyContainer,
                                                    @Qualifier(AZURITE_BEAN_NAME) AzuriteContainer azurite,
                                                    AzuriteProperties properties,
                                                    ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                azurite,
                properties.getBlobStoragePort(),
                "azurite");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.azurite", "embeddedAzuriteBlobToxiproxyInfo", environment, "blobStoragePort");

        return proxy;
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "azurite")
    ToxiproxyClientProxy azuriteQueueContainerProxy(ToxiproxyClient toxiproxyClient,
                                                     ToxiproxyContainer toxiproxyContainer,
                                                     @Qualifier(AZURITE_BEAN_NAME) AzuriteContainer azurite,
                                                     AzuriteProperties properties,
                                                     ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                azurite,
                properties.getQueueStoragePort(),
                "azurite");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.azurite", "embeddedAzuriteQueueToxiproxyInfo", environment, "queueStoragePor");

        return proxy;
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "azurite")
    ToxiproxyClientProxy azuriteTableContainerProxy(ToxiproxyClient toxiproxyClient,
                                                     ToxiproxyContainer toxiproxyContainer,
                                                     @Qualifier(AZURITE_BEAN_NAME) AzuriteContainer azurite,
                                                     AzuriteProperties properties,
                                                     ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                azurite,
                properties.getTableStoragePort(),
                "azurite");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.azurite", "embeddedAzuriteTableToxiproxyInfo", environment, "tableStoragePort");

        return proxy;
    }

    @Bean(name = AZURITE_BEAN_NAME, destroyMethod = "stop")
    public AzuriteContainer azurite(ConfigurableEnvironment environment,
                                    AzuriteProperties properties,
                                    Optional<Network> network) {
        AzuriteContainer azuriteContainer = new AzuriteContainer(ContainerUtils.getDockerImageName(properties))
                .withNetworkAliases(AZURITE_BLOB_NETWORK_ALIAS)
                .withCreateContainerCmdModifier(cmd -> {
                    List<String> args = new ArrayList<>(Arrays.asList(cmd.getCmd()));
                    args.add("--skipApiVersionCheck");
                    cmd.withCmd(args);
                });

        network.ifPresent(azuriteContainer::withNetwork);

        azuriteContainer = (AzuriteContainer) configureCommonsAndStart(azuriteContainer, properties, log);
        registerEnvironment(azuriteContainer, environment, properties);
        return azuriteContainer;
    }

    private void registerEnvironment(AzuriteContainer azurite,
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
