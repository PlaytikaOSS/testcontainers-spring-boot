package com.playtika.test.mysql;

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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.mysql.MySQLProperties.BEAN_NAME_EMBEDDED_MYSQL;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.mysql.enabled", matchIfMissing = true)
@EnableConfigurationProperties(MySQLProperties.class)
public class EmbeddedMySQLBootstrapConfiguration {

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "mysql")
    ToxiproxyContainer.ContainerProxy mysqlContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                          @Qualifier(BEAN_NAME_EMBEDDED_MYSQL) MySQLContainer mysql,
                                                          MySQLProperties properties,
                                                          ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(mysql, properties.getPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mysql.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.mysql.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.mysql.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedMysqlToxiProxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Mysql ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_MYSQL, destroyMethod = "stop")
    public MySQLContainer mysql(ConfigurableEnvironment environment,
                                MySQLProperties properties,
                                Optional<Network> network) {

        MySQLContainer mysql = new MySQLContainer<>(ContainerUtils.getDockerImageName(properties))
                .withEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "yes")
                .withUsername(properties.getUser())
                .withDatabaseName(properties.getDatabase())
                .withPassword(properties.getPassword())
                .withCommand(
                        "--character-set-server=" + properties.getEncoding(),
                        "--collation-server=" + properties.getCollation())
                //.withExposedPorts(properties.getPort())
                .withInitScript(properties.getInitScriptPath());

        network.ifPresent(mysql::withNetwork);
        mysql = (MySQLContainer) configureCommonsAndStart(mysql, properties, log);
        registerMySQLEnvironment(mysql, environment, properties);
        return mysql;
    }

    private void registerMySQLEnvironment(MySQLContainer mysql,
                                          ConfigurableEnvironment environment,
                                          MySQLProperties properties) {
        Integer mappedPort = mysql.getMappedPort(properties.getPort());
        String host = mysql.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mysql.port", mappedPort);
        map.put("embedded.mysql.host", host);
        map.put("embedded.mysql.schema", properties.getDatabase());
        map.put("embedded.mysql.user", properties.getUser());
        map.put("embedded.mysql.password", properties.getPassword());

        String jdbcURL = "jdbc:mysql://{}:{}/{}";
        log.info("Started mysql server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort, properties.getDatabase());

        MapPropertySource propertySource = new MapPropertySource("embeddedMySQLInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
