package com.playtika.testcontainer.grafana;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.grafana.GrafanaProperties.GRAFANA_BEAN_NAME;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.grafana.enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrafanaProperties.class)
public class EmbeddedGrafanaBootstrapConfiguration {

    private static final String GRAFANA_NETWORK_ALIAS = "grafana.testcontainer.docker";

    @Bean
    @ConditionalOnMissingBean(name = "grafanaWaitStrategy")
    public WaitStrategy grafanaWaitStrategy(GrafanaProperties properties) {
        return new HttpWaitStrategy()
                .forPath("/")
                .forPort(properties.getPort())
                .forStatusCode(200);
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "grafana")
    ToxiproxyClientProxy grafanaContainerProxy(ToxiproxyClient toxiproxyClient,
                                                ToxiproxyContainer toxiproxyContainer,
                                                @Qualifier(GRAFANA_BEAN_NAME) GenericContainer<?> grafana,
                                                GrafanaProperties properties,
                                                ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                grafana,
                properties.getPort(),
                "grafana");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.grafana", "embeddedGrafanaToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = GRAFANA_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer<?> grafana(GrafanaProperties properties,
                                       WaitStrategy grafanaWaitStrategy,
                                       Optional<Network> network) {
        GenericContainer<?> container =
                new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                        .withEnv("GF_SECURITY_ADMIN_USER", properties.getUsername())
                        .withEnv("GF_SECURITY_ADMIN_PASSWORD", properties.getPassword())
                        .withExposedPorts(properties.getPort())
                        .withNetwork(Network.SHARED)
                        .withNetworkAliases(properties.getNetworkAlias(), GRAFANA_NETWORK_ALIAS)
                        .waitingFor(grafanaWaitStrategy);

        network.ifPresent(container::withNetwork);

        configureCommonsAndStart(container, properties, log);
        return container;
    }

    @Bean
    public DynamicPropertyRegistrar grafanaDynamicPropertyRegistrar(
            @Qualifier(GRAFANA_BEAN_NAME) GenericContainer<?> grafana,
            GrafanaProperties properties) {
        return registry -> {
            registry.add("embedded.grafana.host", grafana::getHost);
            registry.add("embedded.grafana.port", () -> grafana.getMappedPort(properties.port));
            registry.add("embedded.grafana.username", properties::getUsername);
            registry.add("embedded.grafana.password", properties::getPassword);
            registry.add("embedded.grafana.networkAlias", () -> GRAFANA_NETWORK_ALIAS);
            registry.add("embedded.grafana.internalPort", properties::getPort);
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "grafana")
    public DynamicPropertyRegistrar grafanaToxiProxyDynamicPropertyRegistrar(
            @Qualifier("grafanaContainerProxy") ToxiproxyContainer.ContainerProxy proxy) {
        return registry -> {
            registry.add("embedded.grafana.toxiproxy.host", proxy::getContainerIpAddress);
            registry.add("embedded.grafana.toxiproxy.port", proxy::getProxyPort);
            registry.add("embedded.grafana.toxiproxy.proxyName", proxy::getName);
        };
    }
}
