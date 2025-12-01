package com.playtika.testcontainer.victoriametrics;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@EnableConfigurationProperties(VictoriaMetricsProperties.class)
@AutoConfigureAfter({DockerPresenceBootstrapConfiguration.class, EmbeddedToxiProxyBootstrapConfiguration.class})
@ConditionalOnProperty(name = "embedded.victoriametrics.enabled", matchIfMissing = true)
public class EmbeddedVictoriaMetricsBootstrapConfiguration {

    private static final String VICTORIAMETRICS_NETWORK_ALIAS = "victoriametrics.testcontainer.docker";
    private static final String BEAN_NAME_EMBEDDED_VICTORIA_METRICS = "victoriaMetrics";

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
    public ToxiproxyClientProxy victoriaMetricsContainerProxy(ToxiproxyClient toxiproxyClient,
                                                               ToxiproxyContainer toxiproxyContainer,
                                                               @Qualifier(BEAN_NAME_EMBEDDED_VICTORIA_METRICS) GenericContainer<?> victoriametrics,
                                                               VictoriaMetricsProperties properties) {

        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                victoriametrics,
                properties.getPort(),
                "victoriametrics"
        );
    }

    @Bean(name = BEAN_NAME_EMBEDDED_VICTORIA_METRICS, destroyMethod = "stop")
    public GenericContainer<?> victoriaMetrics(VictoriaMetricsProperties properties,
                                              Optional<Network> network,
                                              WaitStrategy victoriaMetricsWaitStrategy) {
        GenericContainer<?> victoriaMetrics = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getPort())
                .withNetworkAliases(VICTORIAMETRICS_NETWORK_ALIAS)
                .waitingFor(victoriaMetricsWaitStrategy);
        network.ifPresent(victoriaMetrics::withNetwork);
        configureCommonsAndStart(victoriaMetrics, properties, log);
        return victoriaMetrics;
    }

    @Bean
    public DynamicPropertyRegistrar victoriaMetricsDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_VICTORIA_METRICS) GenericContainer<?> victoriaMetrics,
            VictoriaMetricsProperties properties) {
        return registry -> {
            registry.add("embedded.victoriametrics.host", victoriaMetrics::getHost);
            registry.add("embedded.victoriametrics.port", () -> victoriaMetrics.getMappedPort(properties.getPort()));
            registry.add("embedded.victoriametrics.networkAlias", () -> VICTORIAMETRICS_NETWORK_ALIAS);
            registry.add("embedded.victoriametrics.internalPort", () -> properties.getPort());
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "victoriametrics")
    public DynamicPropertyRegistrar victoriaMetricsToxiProxyDynamicPropertyRegistrar(
            @Qualifier("victoriaMetricsContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.victoriametrics");
    }
}
