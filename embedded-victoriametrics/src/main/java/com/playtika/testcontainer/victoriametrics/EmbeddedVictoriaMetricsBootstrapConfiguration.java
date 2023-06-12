package com.playtika.testcontainer.victoriametrics;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.toxiproxy.EmbeddedToxiProxyBootstrapConfiguration;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.common.utils.ContainerUtils.getDockerImageName;

@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@EnableConfigurationProperties(VictoriaMetricsProperties.class)
@AutoConfigureAfter({DockerPresenceBootstrapConfiguration.class, EmbeddedToxiProxyBootstrapConfiguration.class})
@ConditionalOnProperty(name = "embedded.victoriametrics.enabled", matchIfMissing = true)
public class EmbeddedVictoriaMetricsBootstrapConfiguration {

    private static final String VICTORIAMETRICS_NETWORK_ALIAS = "victoriametrics.testcontainer.docker";

    @Bean
    @ConditionalOnMissingBean(name = "victoriaMetricsWaitStrategy")
    public WaitStrategy victoriaMetricsWaitStrategy(VictoriaMetricsProperties properties) {
        return new HttpWaitStrategy()
                .forPath("/")
                .forPort(properties.getPort())
                .forStatusCode(200);
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "victoriametrics")
    public ToxiproxyContainer.ContainerProxy victoriaMetricsContainerProxy(ToxiproxyContainer toxiproxy,
                                                                           GenericContainer<?> victoriametrics,
                                                                           VictoriaMetricsProperties properties,
                                                                           ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxy.getProxy(victoriametrics, properties.getPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.victoriametrics.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.victoriametrics.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.victoriametrics.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedVictoriaMetricsToxiProxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started VictoriaMetrics ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = VictoriaMetricsProperties.VICTORIA_METRICS_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer<?> victoriaMetrics(ConfigurableEnvironment environment,
                                            VictoriaMetricsProperties properties,
                                            WaitStrategy victoriaMetricsWaitStrategy,
                                            Optional<Network> network) {

        GenericContainer<?> victoriaMetrics =
                new GenericContainer<>(getDockerImageName(properties))
                        .withExposedPorts(properties.getPort())
                        .withNetwork(Network.SHARED)
                        .withNetworkAliases(properties.getNetworkAlias(), VICTORIAMETRICS_NETWORK_ALIAS)
                        .waitingFor(victoriaMetricsWaitStrategy);

        network.ifPresent(victoriaMetrics::withNetwork);

        configureCommonsAndStart(victoriaMetrics, properties, log);

        registerEnvironment(victoriaMetrics, environment, properties);

        return victoriaMetrics;
    }

    private void registerEnvironment(GenericContainer<?> victoriaMetrics,
                                     ConfigurableEnvironment environment,
                                     VictoriaMetricsProperties properties) {

        Integer mappedPort = victoriaMetrics.getMappedPort(properties.port);
        String host = victoriaMetrics.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.victoriametrics.host", host);
        map.put("embedded.victoriametrics.port", mappedPort);
        map.put("embedded.victoriametrics.staticNetworkAlias", VICTORIAMETRICS_NETWORK_ALIAS);
        map.put("embedded.victoriametrics.internalPort", properties.getPort());

        log.info("Started VictoriaMetrics server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedVictoriaMetricsInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
