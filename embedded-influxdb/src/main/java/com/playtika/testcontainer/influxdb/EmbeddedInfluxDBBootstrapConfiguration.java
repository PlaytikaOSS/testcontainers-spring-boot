package com.playtika.testcontainer.influxdb;

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
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.influxdb.InfluxDBProperties.EMBEDDED_INFLUX_DB;

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
                                                 @Qualifier(EMBEDDED_INFLUX_DB) ConcreteInfluxDbContainer influxdb,
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

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "influxdb")
    public DynamicPropertyRegistrar influxdbToxiProxyDynamicPropertyRegistrar(
        @Qualifier("influxdbContainerProxy") ToxiproxyContainer.ContainerProxy proxy) {
        return registry -> {
            registry.add("embedded.influxdb.toxiproxy.host", proxy::getContainerIpAddress);
            registry.add("embedded.influxdb.toxiproxy.port", proxy::getProxyPort);
            registry.add("embedded.influxdb.toxiproxy.proxyName", proxy::getName);
        };
    }

    @Bean(name = EMBEDDED_INFLUX_DB, destroyMethod = "stop")
    public ConcreteInfluxDbContainer influxdb(InfluxDBProperties properties, Optional<Network> network) {
        ConcreteInfluxDbContainer influxDBContainer = new ConcreteInfluxDbContainer(ContainerUtils.getDockerImageName(properties));
        influxDBContainer
                .withAdmin(properties.getAdminUser())
                .withAdminPassword(properties.getAdminPassword())
                .withAuthEnabled(properties.isEnableHttpAuth())
                .withUsername(properties.getUser())
                .withPassword(properties.getPassword())
                .withDatabase(properties.getDatabase())
                .withExposedPorts(properties.getPort())
                .withNetworkAliases(INFLUXDB_NETWORK_ALIAS);

        network.ifPresent(influxDBContainer::withNetwork);

        influxDBContainer.waitingFor(getInfluxWaitStrategy(properties.getUser(), properties.getPassword()));

        influxDBContainer = (ConcreteInfluxDbContainer) configureCommonsAndStart(influxDBContainer, properties, log);
        return influxDBContainer;
    }

    @Bean
    public DynamicPropertyRegistrar influxdbDynamicPropertyRegistrar(@Qualifier(EMBEDDED_INFLUX_DB) ConcreteInfluxDbContainer influx, InfluxDBProperties properties) {
        return registry -> {
            Integer mappedPort = influx.getMappedPort(properties.getPort());
            String host = influx.getHost();
            registry.add("embedded.influxdb.port", () -> mappedPort);
            registry.add("embedded.influxdb.host", () -> host);
            registry.add("embedded.influxdb.database", properties::getDatabase);
            registry.add("embedded.influxdb.user", properties::getUser);
            registry.add("embedded.influxdb.password", properties::getPassword);
            registry.add("embedded.influxdb.networkAlias", () -> INFLUXDB_NETWORK_ALIAS);
            registry.add("embedded.influxdb.internalPort", properties::getPort);
        };
    }

    private WaitAllStrategy getInfluxWaitStrategy(String user, String password) {
        return new WaitAllStrategy()
                .withStrategy(Wait.forHttp("/ping")
                        .withBasicCredentials(user, password)
                        .forStatusCode(204))
                .withStrategy(Wait.forListeningPort());
    }

    private static class ConcreteInfluxDbContainer extends InfluxDBContainer<ConcreteInfluxDbContainer> {
        ConcreteInfluxDbContainer(final DockerImageName dockerImageName) {
            super(dockerImageName);
            addExposedPort(INFLUXDB_PORT);
        }
    }
}
