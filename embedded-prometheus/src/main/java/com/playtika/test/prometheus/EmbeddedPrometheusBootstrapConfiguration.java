package com.playtika.test.prometheus;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
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
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;


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

    @Bean(name = PrometheusProperties.PROMETHEUS_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer prometheus(ConfigurableEnvironment environment,
                                       PrometheusProperties properties,
                                       WaitStrategy prometheusWaitStrategy) {

        GenericContainer container =
                new GenericContainer(DockerImageName.parse(properties.getDockerImage()))
                        .withExposedPorts(properties.getPort())
                        .withNetwork(Network.SHARED)
                        .withNetworkAliases(properties.getNetworkAlias())
                        .waitingFor(prometheusWaitStrategy);

        configureCommonsAndStart(container, properties, log);

        registerEnvironment(container, environment, properties);

        return container;
    }

    private void registerEnvironment(GenericContainer prometheus,
                                     ConfigurableEnvironment environment,
                                     PrometheusProperties properties) {

        Integer mappedPort = prometheus.getMappedPort(properties.port);
        String host = prometheus.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.prometheus.host", host);
        map.put("embedded.prometheus.port", mappedPort);

        log.info("Started Prometheus server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedPrometheusInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
