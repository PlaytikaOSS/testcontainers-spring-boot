package com.playtika.testcontainer.prometheus;

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
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.prometheus.PrometheusProperties.PROMETHEUS_BEAN_NAME;


@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.prometheus.enabled", matchIfMissing = true)
@EnableConfigurationProperties(PrometheusProperties.class)
public class EmbeddedPrometheusBootstrapConfiguration {

    private static final String PROMETHEUS_NETWORK_ALIAS = "prometheus.testcontainer.docker";

    @Bean
    @ConditionalOnMissingBean(name = "prometheusWaitStrategy")
    public WaitStrategy prometheusWaitStrategy(PrometheusProperties properties) {
        return new HttpWaitStrategy()
                .forPath("/status")
                .forPort(properties.getPort())
                .forStatusCode(200);
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "prometheus")
    ToxiproxyClientProxy prometheusContainerProxy(ToxiproxyClient toxiproxyClient,
                                                   ToxiproxyContainer toxiproxyContainer,
                                                   @Qualifier(PROMETHEUS_BEAN_NAME) GenericContainer<?> prometheus,
                                                   ConfigurableEnvironment environment,
                                                   PrometheusProperties properties) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                prometheus,
                properties.getPort(),
                "prometheus");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.prometheus", "embeddedPrometheusToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = PROMETHEUS_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer<?> prometheus(ConfigurableEnvironment environment,
                                          PrometheusProperties properties,
                                          WaitStrategy prometheusWaitStrategy,
                                          Optional<Network> network) {

        GenericContainer<?> container =
                new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                        .withExposedPorts(properties.getPort())
                        .withNetwork(Network.SHARED)
                        .withNetworkAliases(properties.getNetworkAlias(), PROMETHEUS_NETWORK_ALIAS)
                        .waitingFor(prometheusWaitStrategy);

        network.ifPresent(container::withNetwork);

        configureCommonsAndStart(container, properties, log);

        registerEnvironment(container, environment, properties);

        return container;
    }

    private void registerEnvironment(GenericContainer<?> prometheus,
                                     ConfigurableEnvironment environment,
                                     PrometheusProperties properties) {

        Integer mappedPort = prometheus.getMappedPort(properties.port);
        String host = prometheus.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.prometheus.host", host);
        map.put("embedded.prometheus.port", mappedPort);
        map.put("embedded.prometheus.staticNetworkAlias", PROMETHEUS_NETWORK_ALIAS);
        map.put("embedded.prometheus.internalPort", properties.getPort());

        log.info("Started Prometheus server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedPrometheusInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
