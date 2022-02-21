package com.playtika.test.azurite;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.azurite.enabled", matchIfMissing = true)
@EnableConfigurationProperties(AzuriteProperties.class)
public class EmbeddedAzuriteBootstrapConfiguration {

    @Bean(name = AzuriteProperties.AZURITE_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer azurite(ConfigurableEnvironment environment,
                                    AzuriteProperties properties) {
        GenericContainer container = new GenericContainer(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getPort());

        configureCommonsAndStart(container, properties, log);

        registerEnvironment(container, environment, properties);

        return container;
    }

    private void registerEnvironment(GenericContainer azurite,
                                     ConfigurableEnvironment environment,
                                     AzuriteProperties properties) {

        Integer mappedPort = azurite.getMappedPort(properties.getPort());
        String host = azurite.getContainerIpAddress();

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
