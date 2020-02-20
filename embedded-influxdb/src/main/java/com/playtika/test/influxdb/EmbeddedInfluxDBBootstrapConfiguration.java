package com.playtika.test.influxdb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Slf4j
@Configuration
@Order(HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "embedded.influxdb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(InfluxDBProperties.class)
public class EmbeddedInfluxDBBootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InfluxDBStatusCheck postgresSQLStartupCheckStrategy(InfluxDBProperties properties) {
        return new InfluxDBStatusCheck(properties);
    }

    @Bean(name = InfluxDBProperties.EMBEDDED_INFLUX_DB, destroyMethod = "stop")
    public GenericContainer influxdb(ConfigurableEnvironment environment,
                                     InfluxDBProperties properties,
                                     InfluxDBStatusCheck influxDBStatusCheck) {
        log.info("Starting influxDB server. Docker image: {}", properties.dockerImage);

        GenericContainer influxdb =
                new GenericContainer(properties.dockerImage)
                        .withEnv("INFLUXDB_ADMIN_USER", properties.getAdminUser())
                        .withEnv("INFLUXDB_ADMIN_PASSWORD", properties.getAdminPassword())
                        .withEnv("INFLUXDB_HTTP_AUTH_ENABLED", String.valueOf(properties.isEnableHttpAuth()))
                        .withEnv("INFLUXDB_USER", properties.getUser())
                        .withEnv("INFLUXDB_USER_PASSWORD", properties.getPassword())
                        .withEnv("INFLUXDB_DB", properties.getDatabase())
                        .withLogConsumer(containerLogsConsumer(log))
                        .withExposedPorts(properties.getPort())
                        .waitingFor(influxDBStatusCheck)
                        .withStartupTimeout(properties.getTimeoutDuration());

        influxdb.setWaitStrategy((new WaitAllStrategy())
                .withStrategy(Wait.forHttp("/ping").withBasicCredentials(properties.getUser(), properties.getPassword()).forStatusCode(204))
                .withStrategy(Wait.forListeningPort()));

        influxdb.start();
        registerInfluxEnvironment(influxdb, environment, properties);
        return influxdb;
    }

    private void registerInfluxEnvironment(GenericContainer influx,
                                           ConfigurableEnvironment environment,
                                           InfluxDBProperties properties) {
        Integer mappedPort = influx.getMappedPort(properties.getPort());
        String host = influx.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.influxdb.port", mappedPort);
        map.put("embedded.influxdb.host", host);
        map.put("embedded.influxdb.database", properties.getDatabase());
        map.put("embedded.influxdb.user", properties.getUser());
        map.put("embedded.influxdb.password", properties.getPassword());

        String influxDBURL = "http://{}:{}";
        log.info("Started InfluxDB server. Connection details: {}, " +
                "HTTP connection url: " + influxDBURL, map, host, mappedPort);

        MapPropertySource propertySource = new MapPropertySource("embeddedInfluxDBInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
