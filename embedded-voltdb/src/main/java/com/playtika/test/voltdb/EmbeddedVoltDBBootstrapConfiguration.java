package com.playtika.test.voltdb;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.voltdb.VoltDBProperties.BEAN_NAME_EMBEDDED_VOLTDB;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.voltdb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(VoltDBProperties.class)
public class EmbeddedVoltDBBootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean
    VoltDBStatusCheck voltDBStartupCheckStrategy() {
        return new VoltDBStatusCheck();
    }

    @Bean(name = BEAN_NAME_EMBEDDED_VOLTDB, destroyMethod = "stop")
    public GenericContainer voltDB(ConfigurableEnvironment environment,
                                   VoltDBProperties properties,
                                   VoltDBStatusCheck voltDbStatusCheck) {

        GenericContainer voltDB = new GenericContainer(ContainerUtils.getDockerImageName(properties))
                .withEnv("HOST_COUNT", "1")
                .withExposedPorts(properties.port)
                .waitingFor(voltDbStatusCheck);
        voltDB = configureCommonsAndStart(voltDB, properties, log);

        registerVoltDBEnvironment(voltDB, environment, properties);
        return voltDB;
    }

    private void registerVoltDBEnvironment(GenericContainer voltDB,
                                           ConfigurableEnvironment environment,
                                           VoltDBProperties properties) {
        Integer mappedPort = voltDB.getMappedPort(properties.port);
        String host = voltDB.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        map.put("embedded.voltdb.port", mappedPort);
        map.put("embedded.voltdb.host", host);

        String jdbcURL = "jdbc:voltdb://{}:{}";
        log.info("Started VoltDB server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort);

        MapPropertySource propertySource = new MapPropertySource("embeddedVoltDBInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
