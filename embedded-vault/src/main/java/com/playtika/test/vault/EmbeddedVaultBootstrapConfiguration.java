package com.playtika.test.vault;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.vault.VaultContainer;

import java.util.Arrays;
import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.vault.VaultProperties.BEAN_NAME_EMBEDDED_VAULT;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Slf4j
@Order(HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.vault.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(VaultProperties.class)
public class EmbeddedVaultBootstrapConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_VAULT, destroyMethod = "stop")
    public VaultContainer vault(ConfigurableEnvironment environment, VaultProperties properties) {

        VaultContainer vault = new VaultContainer<>(ContainerUtils.getDockerImageName(properties))
                .withVaultToken(properties.getToken())
                .withExposedPorts(properties.getPort());

        String[] secrets = properties.getSecrets().entrySet().stream()
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                .toArray(String[]::new);

        if (secrets.length > 0) {
            vault.withSecretInVault(properties.getPath(), secrets[0], Arrays.copyOfRange(secrets, 1, secrets.length));
        }

        vault = (VaultContainer) configureCommonsAndStart(vault, properties, log);
        registerVaultEnvironment(vault, environment, properties);
        return vault;
    }

    private void registerVaultEnvironment(VaultContainer vault, ConfigurableEnvironment environment, VaultProperties properties) {
        Integer mappedPort = vault.getMappedPort(properties.getPort());
        String host = vault.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.vault.host", host);
        map.put("embedded.vault.port", mappedPort);
        map.put("embedded.vault.token", properties.getToken());

        log.info("Started vault. Connection Details: {}, Connection URI: http://{}:{}", map, host, mappedPort);

        MapPropertySource propertySource = new MapPropertySource("embeddedVaultInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
