package com.playtika.test.influxdb;

import java.util.LinkedHashMap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Slf4j
@Configuration
@Order(HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "embedded.influxdb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(InfluxDBProperties.class)
public class EmbeddedInfluxDBBootstrapConfiguration {
    @Bean(name = InfluxDBProperties.EMBEDDED_INFLUX_DB, destroyMethod = "stop")
    public ConcreteInfluxDbContainer influxdb(ConfigurableEnvironment environment,
                                              InfluxDBProperties properties) {
        log.info("Starting influxDB server. Docker image: {}",
                 properties.dockerImage);

        ConcreteInfluxDbContainer influxDBContainer = new ConcreteInfluxDbContainer(properties.dockerImage);
        influxDBContainer
                .withAdmin(properties.getAdminUser())
                .withAdminPassword(properties.getAdminPassword())
                .withAuthEnabled(properties.isEnableHttpAuth())
                .withUsername(properties.getUser())
                .withPassword(properties.getPassword())
                .withDatabase(properties.getDatabase())
                .withExposedPorts(properties.getPort())
                .withLogConsumer(containerLogsConsumer(log))
                .withStartupTimeout(properties.getTimeoutDuration());

        influxDBContainer.waitingFor(getInfluxWaitStrategy(properties.getUser(), properties.getPassword()));

        influxDBContainer.start();
        registerInfluxEnvironment(influxDBContainer, environment, properties);
        return influxDBContainer;
    }

    private void registerInfluxEnvironment(ConcreteInfluxDbContainer influx,
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

    private static class ConcreteInfluxDbContainer extends InfluxDBContainer<ConcreteInfluxDbContainer> {
        ConcreteInfluxDbContainer(final String dockerImageName) {
            setDockerImageName(dockerImageName);
            addExposedPort(INFLUXDB_PORT);
        }
    }

    private WaitAllStrategy getInfluxWaitStrategy(String user, String password) {
        return new WaitAllStrategy()
                .withStrategy(Wait.forHttp("/ping")
                                  .withBasicCredentials(user, password)
                                  .forStatusCode(204))
                .withStrategy(Wait.forListeningPort());
    }
}
