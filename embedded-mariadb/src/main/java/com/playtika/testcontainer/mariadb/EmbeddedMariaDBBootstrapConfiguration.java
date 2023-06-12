package com.playtika.testcontainer.mariadb;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
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
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Map;
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
    ToxiproxyContainer.ContainerProxy mariadbContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                            @Qualifier(BEAN_NAME_EMBEDDED_MARIADB) MariaDBContainer mariadbContainer,
                                                            MariaDBProperties properties,
                                                            ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(mariadbContainer, properties.getPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mariadb.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.mariadb.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.mariadb.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedMariadbToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started MariaDB ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_MARIADB, destroyMethod = "stop")
    public MariaDBContainer mariadb(ConfigurableEnvironment environment,
                                    MariaDBProperties properties,
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
        registerMariadbEnvironment(mariadb, environment, properties);
        return mariadb;
    }

    private void registerMariadbEnvironment(MariaDBContainer mariadb,
                                            ConfigurableEnvironment environment,
                                            MariaDBProperties properties) {
        Integer mappedPort = mariadb.getMappedPort(properties.getPort());
        String host = mariadb.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mariadb.port", mappedPort);
        map.put("embedded.mariadb.host", host);
        map.put("embedded.mariadb.schema", properties.getDatabase());
        map.put("embedded.mariadb.user", properties.getUser());
        map.put("embedded.mariadb.password", properties.getPassword());
        map.put("embedded.mariadb.networkAlias", MARIADB_NETWORK_ALIAS);
        map.put("embedded.mariadb.internalPort", properties.getPort());

        String jdbcURL = "jdbc:mysql://{}:{}/{}";
        log.info("Started mariadb server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort, properties.getDatabase());

        MapPropertySource propertySource = new MapPropertySource("embeddedMariaInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
