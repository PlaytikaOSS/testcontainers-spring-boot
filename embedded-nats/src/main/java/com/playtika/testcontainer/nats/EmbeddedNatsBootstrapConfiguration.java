package com.playtika.testcontainer.nats;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.EmbeddedToxiProxyBootstrapConfiguration;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import io.github.amadeusitgroup.testcontainers.nats.NatsContainer;
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
import org.testcontainers.containers.Network;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.LinkedHashMap;
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
                                             @Qualifier(BEAN_NAME_EMBEDDED_NATS) NatsContainer natsContainer,
                                             ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                natsContainer,
                NatsContainer.DEFAULT_NATS_CLIENT_PORT,
                "nats");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.nats", "embeddedNatsToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_NATS, destroyMethod = "stop")
    public NatsContainer natsContainer(ConfigurableEnvironment environment,
                                       NatsProperties properties,
                                       Optional<Network> network) {
        NatsContainer natsContainer = new NatsContainer(ContainerUtils.getDockerImageName(properties))
                .withNetworkAliases(NATS_NETWORK_ALIAS);

        network.ifPresent(natsContainer::withNetwork);

        natsContainer = (NatsContainer) configureCommonsAndStart(natsContainer, properties, log);

        registerNatsEnvironment(natsContainer, environment);
        return natsContainer;
    }

    private void registerNatsEnvironment(NatsContainer natsContainer,
                                         ConfigurableEnvironment environment) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        map.put("embedded.nats.host", natsContainer.getHost());
        map.put("embedded.nats.port", natsContainer.getClientPort());
        map.put("embedded.nats.httpMonitorPort", natsContainer.getHttpMonitoringPort());
        map.put("embedded.nats.routeConnectionsPort", natsContainer.getRoutingPort());
        map.put("embedded.nats.networkAlias", NATS_NETWORK_ALIAS);
        map.put("embedded.nats.internalClientPort", NatsContainer.DEFAULT_NATS_CLIENT_PORT);
        map.put("embedded.nats.internalHttpMonitorPort", NatsContainer.DEFAULT_NATS_HTTP_MONITORING_PORT);
        map.put("embedded.nats.internalRouteConnectionsPort", NatsContainer.DEFAULT_NATS_ROUTING_PORT);

        log.info("Started NATS server. Connection details {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedNatsInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
