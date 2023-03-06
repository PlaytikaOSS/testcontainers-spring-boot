package com.playtika.test.azurite;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import com.playtika.test.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
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

import static com.playtika.test.azurite.AzuriteProperties.AZURITE_BEAN_NAME;
import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.azurite.enabled", matchIfMissing = true)
@EnableConfigurationProperties(AzuriteProperties.class)
public class EmbeddedAzuriteBootstrapConfiguration {

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "azurite")
    ToxiproxyContainer.ContainerProxy azuriteContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                            @Qualifier(AZURITE_BEAN_NAME) GenericContainer<?> azurite,
                                                            AzuriteProperties properties,
                                                            ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(azurite, properties.getPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.azurite.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.azurite.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.azurite.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedAzuriteToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Azurite ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = AZURITE_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer<?> azurite(ConfigurableEnvironment environment,
                                       AzuriteProperties properties,
                                       Optional<Network> network) {
        GenericContainer<?> azuriteContainer = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getPort());

        network.ifPresent(azuriteContainer::withNetwork);

        configureCommonsAndStart(azuriteContainer, properties, log);
        registerEnvironment(azuriteContainer, environment, properties);
        return azuriteContainer;
    }

    private void registerEnvironment(GenericContainer<?> azurite,
                                     ConfigurableEnvironment environment,
                                     AzuriteProperties properties) {

        Integer mappedPort = azurite.getMappedPort(properties.getPort());
        String host = azurite.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.azurite.host", host);
        map.put("embedded.azurite.port", mappedPort);
        map.put("embedded.azurite.account-name", AzuriteProperties.ACCOUNT_NAME);
        map.put("embedded.azurite.account-key", AzuriteProperties.ACCOUNT_KEY);
        map.put("embedded.azurite.blob-endpoint", "http://" + host + ":" + mappedPort + "/" + AzuriteProperties.ACCOUNT_NAME);

        log.info("Started Azurite. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedAzuriteInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
