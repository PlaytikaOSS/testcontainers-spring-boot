package com.playtika.test.vault;

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
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.vault.VaultContainer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "vault")
    ToxiproxyContainer.ContainerProxy vaultContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                               @Qualifier(BEAN_NAME_EMBEDDED_VAULT) VaultContainer vault,
                                                               ConfigurableEnvironment environment,
                                                               VaultProperties properties) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(vault, properties.getPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.vault.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.vault.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.vault.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedVaultToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Vault ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_VAULT, destroyMethod = "stop")
    public VaultContainer vault(ConfigurableEnvironment environment,
                                VaultProperties properties,
                                Optional<Network> network) {

        VaultContainer vault = new VaultContainer<>(ContainerUtils.getDockerImageName(properties))
                .withVaultToken(properties.getToken())
                .withExposedPorts(properties.getPort());

        network.ifPresent(vault::withNetwork);

        String[] secrets = properties.getSecrets().entrySet().stream()
                .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue()))
                .toArray(String[]::new);

        if (secrets.length > 0) {
            vault.withSecretInVault(properties.getPath(), secrets[0], Arrays.copyOfRange(secrets, 1, secrets.length));
        }

        if (properties.isCasEnabled()) {
            log.info("Enabling cas for mount secret");
            vault.withInitCommand("write secret/config cas_required=true");
        }

        if (!properties.getCasEnabledForSubPaths().isEmpty()) {
            enableCasForSubPaths(properties.getCasEnabledForSubPaths(), vault);
        }

        vault = (VaultContainer) configureCommonsAndStart(vault, properties, log);
        registerVaultEnvironment(vault, environment, properties);
        return vault;
    }

    private void registerVaultEnvironment(VaultContainer vault, ConfigurableEnvironment environment, VaultProperties properties) {
        Integer mappedPort = vault.getMappedPort(properties.getPort());
        String host = vault.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.vault.host", host);
        map.put("embedded.vault.port", mappedPort);
        map.put("embedded.vault.token", properties.getToken());

        log.info("Started vault. Connection Details: {}, Connection URI: http://{}:{}", map, host, mappedPort);

        MapPropertySource propertySource = new MapPropertySource("embeddedVaultInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }


    private void enableCasForSubPaths(List<String> subPaths, VaultContainer vault) {
        for (String subPath : subPaths) {
            if (!subPath.isEmpty()) {
                log.info("Vault: Enabling cas for sub path {}", subPath);
                vault.withInitCommand("kv metadata put -cas-required=true secret/" + subPath);
            }
        }
    }
}
