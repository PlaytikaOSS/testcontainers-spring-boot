package com.playtika.test.prometheus;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import com.playtika.test.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
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
import java.util.Map;
import java.util.Optional;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.prometheus.PrometheusProperties.PROMETHEUS_BEAN_NAME;


@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.prometheus.enabled", matchIfMissing = true)
@EnableConfigurationProperties(PrometheusProperties.class)
public class EmbeddedPrometheusBootstrapConfiguration {

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
    ToxiproxyContainer.ContainerProxy prometheusContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                               @Qualifier(PROMETHEUS_BEAN_NAME) GenericContainer<?> prometheus,
                                                               ConfigurableEnvironment environment,
                                                               PrometheusProperties properties) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(prometheus, properties.getPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.prometheus.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.prometheus.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.prometheus.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedPrometheusToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Prometheus ToxiProxy connection details {}", map);

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
                        .withNetworkAliases(properties.getNetworkAlias())
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

        log.info("Started Prometheus server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedPrometheusInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
