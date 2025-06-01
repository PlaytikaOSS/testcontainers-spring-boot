package com.playtika.testcontainer.nats;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.EmbeddedToxiProxyBootstrapConfiguration;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.nats.NatsProperties.BEAN_NAME_EMBEDDED_NATS;
import static com.playtika.testcontainer.nats.NatsProperties.BEAN_NAME_EMBEDDED_NATS_TOXI_PROXY;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter({DockerPresenceBootstrapConfiguration.class, EmbeddedToxiProxyBootstrapConfiguration.class})
@ConditionalOnProperty(name = "embedded.nats.enabled", matchIfMissing = true)
@EnableConfigurationProperties(NatsProperties.class)
public class EmbeddedNatsBootstrapConfiguration {

    private static final String NATS_NETWORK_ALIAS = "nats.testcontainer.docker";

    @Bean(name = BEAN_NAME_EMBEDDED_NATS_TOXI_PROXY)
    @ConditionalOnToxiProxyEnabled(module = "nats")
    ToxiproxyClientProxy natsContainerProxy(ToxiproxyClient toxiproxyClient,
                                             ToxiproxyContainer toxiproxyContainer,
                                             @Qualifier(BEAN_NAME_EMBEDDED_NATS) GenericContainer<?> natsContainer,
                                             NatsProperties properties,
                                             ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                natsContainer,
                properties.getClientPort(),
                "nats");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.nats", "embeddedNatsToxiproxyInfo", environment);

        return proxy;
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "nats")
    public DynamicPropertyRegistrar natsToxiProxyDynamicPropertyRegistrar(@Qualifier(BEAN_NAME_EMBEDDED_NATS_TOXI_PROXY) ToxiproxyContainer.ContainerProxy proxy) {
        return registry -> {
            registry.add("embedded.nats.toxiproxy.host", proxy::getContainerIpAddress);
            registry.add("embedded.nats.toxiproxy.port", proxy::getProxyPort);
            registry.add("embedded.nats.toxiproxy.proxyName", proxy::getName);
        };
    }

    @Bean(name = BEAN_NAME_EMBEDDED_NATS, destroyMethod = "stop")
    public GenericContainer<?> natsContainer(NatsProperties properties,
                                             Optional<Network> network) {
        WaitStrategy waitStrategy = new WaitAllStrategy()
                .withStrategy(new HostPortWaitStrategy())
                .withStartupTimeout(properties.getTimeoutDuration());

        GenericContainer<?> natsContainer = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getClientPort(), properties.getHttpMonitorPort(), properties.getRouteConnectionsPort())
                .withCopyFileToContainer(MountableFile.forClasspathResource("nats-server.conf"), "/nats-server.conf")
                .waitingFor(waitStrategy)
                .withNetworkAliases(NATS_NETWORK_ALIAS);

        network.ifPresent(natsContainer::withNetwork);

        natsContainer = configureCommonsAndStart(natsContainer, properties, log);

        Integer clientMappedPort = natsContainer.getMappedPort(properties.getClientPort());
        Integer httpMonitorMappedPort = natsContainer.getMappedPort(properties.getHttpMonitorPort());
        Integer routeConnectionsMappedPort = natsContainer.getMappedPort(properties.getRouteConnectionsPort());
        String host = natsContainer.getHost();
        log.info("Started NATS server. Connection details host={}, port={}, httpMonitorPort={}, routeConnectionsPort={}, networkAlias={}, internalClientPort={}, internalHttpMonitorPort={}, internalRouteConnectionsPort={}",
                host, clientMappedPort, httpMonitorMappedPort, routeConnectionsMappedPort, NATS_NETWORK_ALIAS, properties.getClientPort(), properties.getHttpMonitorPort(), properties.getRouteConnectionsPort());
        return natsContainer;
    }

    @Bean
    public DynamicPropertyRegistrar natsDynamicPropertyRegistrar(@Qualifier(BEAN_NAME_EMBEDDED_NATS) GenericContainer<?> natsContainer, NatsProperties properties) {
        return registry -> {
            registry.add("embedded.nats.host", natsContainer::getHost);
            registry.add("embedded.nats.port", () -> natsContainer.getMappedPort(properties.getClientPort()));
            registry.add("embedded.nats.httpMonitorPort", () -> natsContainer.getMappedPort(properties.getHttpMonitorPort()));
            registry.add("embedded.nats.routeConnectionsPort", () -> natsContainer.getMappedPort(properties.getRouteConnectionsPort()));
            registry.add("embedded.nats.networkAlias", () -> NATS_NETWORK_ALIAS);
            registry.add("embedded.nats.internalClientPort", properties::getClientPort);
            registry.add("embedded.nats.internalHttpMonitorPort", properties::getHttpMonitorPort);
            registry.add("embedded.nats.internalRouteConnectionsPort", properties::getRouteConnectionsPort);
        };
    }
}
