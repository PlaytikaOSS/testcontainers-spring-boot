package com.playtika.testcontainer.oracle;

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
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.oracle.OracleProperties.BEAN_NAME_EMBEDDED_ORACLE;
import static com.playtika.testcontainer.oracle.OracleProperties.ORACLE_PORT;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.oracle.enabled", matchIfMissing = true)
@EnableConfigurationProperties(OracleProperties.class)
public class EmbeddedOracleBootstrapConfiguration {

    private static final String ORACLE_NETWORK_ALIAS = "oracle.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "oracle")
    ToxiproxyClientProxy oracleContainerProxy(ToxiproxyClient toxiproxyClient,
                                               ToxiproxyContainer toxiproxyContainer,
                                               @Qualifier(BEAN_NAME_EMBEDDED_ORACLE) OracleContainer oracle) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                oracle,
                ORACLE_PORT,
                "oracle");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "oracle")
    public DynamicPropertyRegistrar oracleToxiProxyDynamicPropertyRegistrar(@Qualifier("oracleContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.oracle");
    }

    @Bean(name = BEAN_NAME_EMBEDDED_ORACLE, destroyMethod = "stop")
    public OracleContainer oracle(OracleProperties properties, Optional<Network> network) {
        OracleContainer oracle =
                new OracleContainer(ContainerUtils.getDockerImageName(properties))
                        .withUsername(properties.getUser())
                        .withPassword(properties.getPassword())
                        .withInitScript(properties.initScriptPath)
                        .withNetworkAliases(ORACLE_NETWORK_ALIAS);

        network.ifPresent(oracle::withNetwork);
        oracle = (OracleContainer) configureCommonsAndStart(oracle, properties, log);
        return oracle;
    }

    @Bean
    public DynamicPropertyRegistrar oracleDynamicPropertyRegistrar(@Qualifier(BEAN_NAME_EMBEDDED_ORACLE) OracleContainer oracle, OracleProperties properties) {
        return registry -> {
            Integer mappedPort = oracle.getMappedPort(ORACLE_PORT);
            String host = oracle.getHost();
            registry.add("embedded.oracle.port", () -> mappedPort);
            registry.add("embedded.oracle.host", () -> host);
            registry.add("embedded.oracle.database", properties::getDatabase);
            registry.add("embedded.oracle.user", properties::getUser);
            registry.add("embedded.oracle.password", properties::getPassword);
            registry.add("embedded.oracle.networkAlias", () -> ORACLE_NETWORK_ALIAS);
            registry.add("embedded.oracle.internalPort", () -> ORACLE_PORT);
        };
    }
}
