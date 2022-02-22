package com.playtika.test.grafana;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
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

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.grafana.enabled", matchIfMissing = true)
@EnableConfigurationProperties(GrafanaProperties.class)
public class EmbeddedGrafanaBootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "grafanaWaitStrategy")
    public WaitStrategy grafanaWaitStrategy(GrafanaProperties properties) {
        return new HttpWaitStrategy()
                .forPath("/")
                .forPort(properties.getPort())
                .forStatusCode(200);
    }

    @Bean(name = GrafanaProperties.GRAFANA_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer grafana(ConfigurableEnvironment environment,
                                    GrafanaProperties properties,
                                    WaitStrategy grafanaWaitStrategy) {

        GenericContainer container =
                new GenericContainer(ContainerUtils.getDockerImageName(properties))
                        .withEnv("GF_SECURITY_ADMIN_USER", properties.getUsername())
                        .withEnv("GF_SECURITY_ADMIN_PASSWORD", properties.getPassword())
                        .withExposedPorts(properties.getPort())
                        .withNetwork(Network.SHARED)
                        .withNetworkAliases(properties.getNetworkAlias())
                        .waitingFor(grafanaWaitStrategy);

        configureCommonsAndStart(container, properties, log);

        registerEnvironment(container, environment, properties);

        return container;
    }

    private void registerEnvironment(GenericContainer grafana,
                                     ConfigurableEnvironment environment,
                                     GrafanaProperties properties) {

        Integer mappedPort = grafana.getMappedPort(properties.port);
        String host = grafana.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.grafana.host", host);
        map.put("embedded.grafana.port", mappedPort);
        map.put("embedded.grafana.username", properties.getUsername());
        map.put("embedded.grafana.password", properties.getPassword());

        log.info("Started Grafana server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedGrafanaInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
