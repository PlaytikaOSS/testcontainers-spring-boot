package com.playtika.test.oracle;

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
import org.testcontainers.containers.OracleContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.oracle.OracleProperties.BEAN_NAME_EMBEDDED_ORACLE;
import static com.playtika.test.oracle.OracleProperties.ORACLE_DB;
import static com.playtika.test.oracle.OracleProperties.ORACLE_PORT;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.oracle.enabled", matchIfMissing = true)
@EnableConfigurationProperties(OracleProperties.class)
public class EmbeddedOracleBootstrapConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_ORACLE, destroyMethod = "stop")
    public OracleContainer oracle(ConfigurableEnvironment environment,
                                  OracleProperties properties) {

        OracleContainer oracle =
                new OracleContainer(ContainerUtils.getDockerImageName(properties))
                        .withUsername(properties.getUser())
                        .withPassword(properties.getPassword())
                        .withInitScript(properties.initScriptPath);
        oracle = (OracleContainer) configureCommonsAndStart(oracle, properties, log);
        registerOracleEnvironment(oracle, environment, properties);
        return oracle;
    }

    private void registerOracleEnvironment(OracleContainer oracle,
                                           ConfigurableEnvironment environment,
                                           OracleProperties properties) {
        Integer mappedPort = oracle.getMappedPort(ORACLE_PORT);
        String host = oracle.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.oracle.port", mappedPort);
        map.put("embedded.oracle.host", host);
        map.put("embedded.oracle.database", properties.getDatabase());
        map.put("embedded.oracle.user", properties.getUser());
        map.put("embedded.oracle.password", properties.getPassword());

        String jdbcURL = "jdbc:oracle://{}:{}/{}";
        log.info("Started oracle server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort, ORACLE_DB);

        MapPropertySource propertySource = new MapPropertySource("embeddedOracleInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
