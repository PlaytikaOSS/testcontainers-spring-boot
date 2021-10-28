package com.playtika.test.mariadb;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.MariaDBContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.mariadb.MariaDBProperties.BEAN_NAME_EMBEDDED_MARIADB;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.mariadb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(MariaDBProperties.class)
public class EmbeddedMariaDBBootstrapConfiguration {
    @Bean(name = BEAN_NAME_EMBEDDED_MARIADB, destroyMethod = "stop")
    public MariaDBContainer mariadb(ConfigurableEnvironment environment,
                                    MariaDBProperties properties) throws Exception {
        log.info("Starting mariadb server. Docker image: {}", properties.dockerImage);

        MariaDBContainer mariadb =
                new MariaDBContainer<>(properties.dockerImage)
                        .withEnv("MYSQL_ALLOW_EMPTY_PASSWORD", "yes")
                        .withUsername(properties.getUser())
                        .withPassword(properties.getPassword())
                        .withDatabaseName(properties.getDatabase())
                        .withCommand(
                                "--character-set-server=" + properties.getEncoding(),
                                "--collation-server=" + properties.getCollation(),
                                "--max_allowed_packet=" + properties.getMaxAllowedPacket())
                        .withExposedPorts(properties.port)
                        .withInitScript(properties.initScriptPath);
        mariadb = (MariaDBContainer) configureCommonsAndStart(mariadb, properties, log);
        registerMariadbEnvironment(mariadb, environment, properties);
        return mariadb;
    }

    private void registerMariadbEnvironment(MariaDBContainer mariadb,
                                            ConfigurableEnvironment environment,
                                            MariaDBProperties properties) {
        Integer mappedPort = mariadb.getMappedPort(properties.port);
        String host = mariadb.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mariadb.port", mappedPort);
        map.put("embedded.mariadb.host", host);
        map.put("embedded.mariadb.schema", properties.getDatabase());
        map.put("embedded.mariadb.user", properties.getUser());
        map.put("embedded.mariadb.password", properties.getPassword());

        String jdbcURL = "jdbc:mysql://{}:{}/{}";
        log.info("Started mariadb server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort, properties.getDatabase());

        MapPropertySource propertySource = new MapPropertySource("embeddedMariaInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
