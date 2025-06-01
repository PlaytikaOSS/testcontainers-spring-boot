package com.playtika.testcontainer.mariadb;

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
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.mariadb.MariaDBProperties.BEAN_NAME_EMBEDDED_MARIADB;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.mariadb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(MariaDBProperties.class)
public class EmbeddedMariaDBBootstrapConfiguration {

    private static final String MARIADB_NETWORK_ALIAS = "mariadb.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "mariadb")
    ToxiproxyClientProxy mariadbContainerProxy(ToxiproxyClient toxiproxyClient,
                                                ToxiproxyContainer toxiproxyContainer,
                                                @Qualifier(BEAN_NAME_EMBEDDED_MARIADB) MariaDBContainer mariadbContainer,
                                                MariaDBProperties properties,
                                                ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                mariadbContainer,
                properties.getPort(),
                "mariadb");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.mariadb", "embeddedMariadbToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_MARIADB, destroyMethod = "stop")
    public MariaDBContainer mariadb(MariaDBProperties properties,
                                    Optional<Network> network) throws Exception {
        MariaDBContainer mariadb =
                new MariaDBContainer<>(ContainerUtils.getDockerImageName(properties))
                        .withEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "yes")
                        .withUsername(properties.getUser())
                        .withPassword(properties.getPassword())
                        .withDatabaseName(properties.getDatabase())
                        .withCommand(
                                "--character-set-server=" + properties.getEncoding(),
                                "--collation-server=" + properties.getCollation(),
                                "--max_allowed_packet=" + properties.getMaxAllowedPacket())
                        .withExposedPorts(properties.getPort())
                        .withInitScript(properties.getInitScriptPath())
                        .withNetworkAliases(MARIADB_NETWORK_ALIAS);

        network.ifPresent(mariadb::withNetwork);
        mariadb = (MariaDBContainer) configureCommonsAndStart(mariadb, properties, log);
        return mariadb;
    }

    @Bean
    public DynamicPropertyRegistrar mariadbDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_MARIADB) MariaDBContainer mariadb,
            MariaDBProperties properties) {
        return registry -> {
            registry.add("embedded.mariadb.port", () -> mariadb.getMappedPort(properties.getPort()));
            registry.add("embedded.mariadb.host", mariadb::getHost);
            registry.add("embedded.mariadb.schema", properties::getDatabase);
            registry.add("embedded.mariadb.user", properties::getUser);
            registry.add("embedded.mariadb.password", properties::getPassword);
            registry.add("embedded.mariadb.networkAlias", () -> MARIADB_NETWORK_ALIAS);
            registry.add("embedded.mariadb.internalPort", properties::getPort);
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "mariadb")
    public DynamicPropertyRegistrar mariadbToxiProxyDynamicPropertyRegistrar(
            @Qualifier("mariadbContainerProxy") ToxiproxyContainer.ContainerProxy proxy) {
        return registry -> {
            registry.add("embedded.mariadb.toxiproxy.host", proxy::getContainerIpAddress);
            registry.add("embedded.mariadb.toxiproxy.port", proxy::getProxyPort);
            registry.add("embedded.mariadb.toxiproxy.proxyName", proxy::getName);
        };
    }
}
