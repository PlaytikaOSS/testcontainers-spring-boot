package com.playtika.testcontainer.influxdb;

import com.playtika.testcontainer.common.spring.ContainerStartupCoordinator;
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
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommons;
import static com.playtika.testcontainer.influxdb.InfluxDBProperties.EMBEDDED_INFLUX_DB;
import static org.testcontainers.containers.InfluxDBContainer.INFLUXDB_PORT;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.influxdb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(InfluxDBProperties.class)
public class EmbeddedInfluxDBBootstrapConfiguration {

    private static final String INFLUXDB_NETWORK_ALIAS = "influxdb.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "influxdb")
    ToxiproxyClientProxy influxdbContainerProxy(ToxiproxyClient toxiproxyClient,
                                                 ToxiproxyContainer toxiproxyContainer,
                                                 @Qualifier(EMBEDDED_INFLUX_DB) InfluxDBContainer influxdb,
                                                 InfluxDBProperties properties,
                                                 ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                influxdb,
                properties.getPort(),
                "influxdb");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.influxdb", "embeddedInfluxDBToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = EMBEDDED_INFLUX_DB, destroyMethod = "stop")
    public InfluxDBContainer influxdb(ConfigurableEnvironment environment,
                                              InfluxDBProperties properties,
                                              Optional<Network> network,
                                              ContainerStartupCoordinator startupCoordinator) {
        InfluxDBContainer influxDBContainer = new InfluxDBContainer(ContainerUtils.getDockerImageName(properties));
        influxDBContainer
                .withAdmin(properties.getAdminUser())
                .withAdminPassword(properties.getAdminPassword())
                .withAuthEnabled(properties.isEnableHttpAuth())
                .withUsername(properties.getUser())
                .withPassword(properties.getPassword())
                .withDatabase(properties.getDatabase())
                .withExposedPorts(properties.getPort())
                .withNetworkAliases(INFLUXDB_NETWORK_ALIAS)
                .withExposedPorts(INFLUXDB_PORT);

        network.ifPresent(influxDBContainer::withNetwork);

        influxDBContainer.waitingFor(getInfluxWaitStrategy(properties.getUser(), properties.getPassword()));

        InfluxDBContainer configuredInfluxDBContainer = (InfluxDBContainer) configureCommons(influxDBContainer, properties, log);
        startupCoordinator.schedule(() -> {
            ContainerUtils.startAndLogTime(configuredInfluxDBContainer, log);
            registerInfluxEnvironment(configuredInfluxDBContainer, environment, properties);
        });
        return configuredInfluxDBContainer;
    }

    private void registerInfluxEnvironment(InfluxDBContainer influx,
                                           ConfigurableEnvironment environment,
                                           InfluxDBProperties properties) {
        Integer mappedPort = influx.getMappedPort(properties.getPort());
        String host = influx.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.influxdb.port", mappedPort);
        map.put("embedded.influxdb.host", host);
        map.put("embedded.influxdb.database", properties.getDatabase());
        map.put("embedded.influxdb.user", properties.getUser());
        map.put("embedded.influxdb.password", properties.getPassword());
        map.put("embedded.influxdb.networkAlias", INFLUXDB_NETWORK_ALIAS);
        map.put("embedded.influxdb.internalPort", properties.getPort());

        String influxDBURL = "http://{}:{}";
        log.info("Started InfluxDB server. Connection details: {}, " +
                "HTTP connection url: " + influxDBURL, map, host, mappedPort);

        MapPropertySource propertySource = new MapPropertySource("embeddedInfluxDBInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    private WaitAllStrategy getInfluxWaitStrategy(String user, String password) {
        return new WaitAllStrategy()
                .withStrategy(Wait.forHttp("/ping")
                        .withBasicCredentials(user, password)
                        .forStatusCode(204))
                .withStrategy(Wait.forListeningPort());
    }
}
