package com.playtika.test.nats;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import com.playtika.test.toxiproxy.EmbeddedToxiProxyBootstrapConfiguration;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.nats.NatsProperties.BEAN_NAME_EMBEDDED_NATS;
import static com.playtika.test.nats.NatsProperties.BEAN_NAME_EMBEDDED_NATS_TOXI_PROXY;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter({DockerPresenceBootstrapConfiguration.class, EmbeddedToxiProxyBootstrapConfiguration.class})
@ConditionalOnProperty(name = "embedded.nats.enabled", matchIfMissing = true)
@EnableConfigurationProperties(NatsProperties.class)
public class EmbeddedNatsBootstrapConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_NATS_TOXI_PROXY)
    @ConditionalOnToxiProxyEnabled(module = "nats")
    ToxiproxyContainer.ContainerProxy natsContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                              @Qualifier(BEAN_NAME_EMBEDDED_NATS) GenericContainer natsContainer,
                                                              NatsProperties properties,
                                                              ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(natsContainer, properties.getClientPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.nats.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.nats.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.nats.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedNatsToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started NATS ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_NATS, destroyMethod = "stop")
    public GenericContainer natsContainer(ConfigurableEnvironment environment,
                                          NatsProperties properties,
                                          Optional<Network> network) {

        WaitStrategy waitStrategy = new WaitAllStrategy()
                .withStrategy(new HostPortWaitStrategy())
                .withStartupTimeout(properties.getTimeoutDuration());

        GenericContainer natsContainer = new GenericContainer(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getClientPort(), properties.getHttpMonitorPort(), properties.getRouteConnectionsPort())
                .waitingFor(waitStrategy);

        if (network.isPresent()) {
            natsContainer.withNetwork(network.get());
        }

        natsContainer = configureCommonsAndStart(natsContainer, properties, log);

        registerNatsEnvironment(natsContainer, environment, properties);
        return natsContainer;
    }

    private void registerNatsEnvironment(GenericContainer natsContainer,
                                           ConfigurableEnvironment environment,
                                           NatsProperties properties) {
        Integer clientMappedPort = natsContainer.getMappedPort(properties.getClientPort());
        Integer httpMonitorMappedPort = natsContainer.getMappedPort(properties.getHttpMonitorPort());
        Integer routeConnectionsMappedPort = natsContainer.getMappedPort(properties.getRouteConnectionsPort());
        String host = natsContainer.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        map.put("embedded.nats.host", host);
        map.put("embedded.nats.port", clientMappedPort);
        map.put("embedded.nats.httpMonitorPort", httpMonitorMappedPort);
        map.put("embedded.nats.routeConnectionsPort", routeConnectionsMappedPort);

        log.info("Started NATS server. Connection details {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedNatsInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
