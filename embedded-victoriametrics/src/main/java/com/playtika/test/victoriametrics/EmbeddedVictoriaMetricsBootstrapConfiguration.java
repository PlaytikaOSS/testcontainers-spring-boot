package com.playtika.test.victoriametrics;

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

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.common.utils.ContainerUtils.getDockerImageName;

@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@EnableConfigurationProperties(VictoriaMetricsProperties.class)
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.victoriametrics.enabled", matchIfMissing = true)
public class EmbeddedVictoriaMetricsBootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "victoriaMetricsWaitStrategy")
    public WaitStrategy victoriaMetricsWaitStrategy(VictoriaMetricsProperties properties) {
        return new HttpWaitStrategy()
                .forPath("/")
                .forPort(properties.getPort())
                .forStatusCode(200);
    }

    @Bean(name = VictoriaMetricsProperties.VICTORIA_METRICS_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer victoriaMetrics(ConfigurableEnvironment environment,
                                            VictoriaMetricsProperties properties,
                                            WaitStrategy victoriaMetricsWaitStrategy) {

        GenericContainer container =
                new GenericContainer(getDockerImageName(properties))
                        .withExposedPorts(properties.getPort())
                        .withNetwork(Network.SHARED)
                        .withNetworkAliases(properties.getNetworkAlias())
                        .waitingFor(victoriaMetricsWaitStrategy);

        configureCommonsAndStart(container, properties, log);

        registerEnvironment(container, environment, properties);

        return container;
    }

    private void registerEnvironment(GenericContainer victoriaMetrics,
                                     ConfigurableEnvironment environment,
                                     VictoriaMetricsProperties properties) {

        Integer mappedPort = victoriaMetrics.getMappedPort(properties.port);
        String host = victoriaMetrics.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.victoriametrics.host", host);
        map.put("embedded.victoriametrics.port", mappedPort);

        log.info("Started VictoriaMetrics server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedVictoriaMetricsInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
