package com.playtika.testcontainer.vault;

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
import org.springframework.core.annotation.Order;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.vault.VaultContainer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.vault.VaultProperties.BEAN_NAME_EMBEDDED_VAULT;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Slf4j
@Order(HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.vault.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(VaultProperties.class)
public class EmbeddedVaultBootstrapConfiguration {

    private static final String VAULT_NETWORK_ALIAS = "vault.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "vault")
    ToxiproxyClientProxy vaultContainerProxy(ToxiproxyClient toxiproxyClient,
                                              ToxiproxyContainer toxiproxyContainer,
                                              @Qualifier(BEAN_NAME_EMBEDDED_VAULT) VaultContainer vault,
                                              VaultProperties properties) {

        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                vault,
                properties.getPort(),
                "vault");
    }

    @Bean(name = BEAN_NAME_EMBEDDED_VAULT, destroyMethod = "stop")
    public VaultContainer vault(VaultProperties properties,
                                Optional<Network> network) {

        VaultContainer vault = new VaultContainer<>(ContainerUtils.getDockerImageName(properties))
                .withVaultToken(properties.getToken())
                .withExposedPorts(properties.getPort())
                .withNetworkAliases(VAULT_NETWORK_ALIAS);

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

        // Set Spring Cloud Vault properties as system properties for bootstrap phase
        // These are needed for Spring Cloud Vault to initialize during bootstrap
        // The actual configuration structure is in application-test.yml
        Integer mappedPort = vault.getMappedPort(properties.getPort());
        String host = vault.getHost();
        System.setProperty("spring.cloud.vault.host", host);
        System.setProperty("spring.cloud.vault.port", String.valueOf(mappedPort));
        System.setProperty("spring.cloud.vault.token", properties.getToken());
        System.setProperty("spring.cloud.vault.scheme", "http");
        System.setProperty("spring.cloud.vault.kv.enabled", "true");

        return vault;
    }

    @Bean
    public DynamicPropertyRegistrar vaultDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_VAULT) VaultContainer vault,
            VaultProperties properties) {
        return registry -> {
            Integer mappedPort = vault.getMappedPort(properties.getPort());
            String host = vault.getHost();
            String token = properties.getToken();

            registry.add("embedded.vault.host", () -> host);
            registry.add("embedded.vault.port", () -> mappedPort);
            registry.add("embedded.vault.token", () -> token);
            registry.add("embedded.vault.networkAlias", () -> VAULT_NETWORK_ALIAS);
            registry.add("embedded.vault.internalPort", properties::getPort);

            // Register Spring Cloud Vault properties for test context (application-test.yml references these)
            registry.add("spring.cloud.vault.host", () -> host);
            registry.add("spring.cloud.vault.port", () -> mappedPort);
            registry.add("spring.cloud.vault.token", () -> token);
            registry.add("spring.cloud.vault.scheme", () -> "http");
            registry.add("spring.cloud.vault.kv.enabled", () -> "true");

            log.info("Started vault. Connection Details: host={}, port={}, Connection URI: http://{}:{}",
                    host, mappedPort, host, mappedPort);
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "vault")
    public DynamicPropertyRegistrar vaultToxiProxyDynamicPropertyRegistrar(
            @Qualifier("vaultContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.vault");
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
