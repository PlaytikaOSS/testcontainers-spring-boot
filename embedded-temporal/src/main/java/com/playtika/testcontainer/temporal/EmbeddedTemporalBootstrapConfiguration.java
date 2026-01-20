package com.playtika.testcontainer.temporal;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.temporal.TemporalProperties.BEAN_NAME_EMBEDDED_TEMPORAL;
import static com.playtika.testcontainer.temporal.TemporalProperties.INTERNAL_PORT;
import static com.playtika.testcontainer.temporal.TemporalProperties.INTERNAL_UI_PORT;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(
        name = "embedded.temporal.enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(TemporalProperties.class)
public class EmbeddedTemporalBootstrapConfiguration {

    private static final String TEMPORAL_NETWORK_ALIAS = "temporal.testcontainer.docker";

    @Bean(value = BEAN_NAME_EMBEDDED_TEMPORAL, destroyMethod = "stop")
    public GenericContainer<?> temporal(ConfigurableEnvironment environment,
                                        TemporalProperties properties,
                                        Optional<Network> network) {
        GenericContainer<?> container =
                new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                        .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint(
                                "temporal",
                                "server",
                                "start-dev"
                        ))
                        .withCommand(
                                "--ip", "0.0.0.0",
                                "--headless")
                        .withExposedPorts(INTERNAL_PORT)
                        .withNetworkAliases(TEMPORAL_NETWORK_ALIAS)
                        .waitingFor(new HostPortWaitStrategy());

        if (properties.isUiEnabled()) {
            container
                    .withCommand("--ip", "0.0.0.0")
                    .addExposedPort(INTERNAL_UI_PORT);
        }

        network.ifPresent(container::withNetwork);

        configureCommonsAndStart(container, properties, log);
        registerTemporalEnvironment(container, environment, properties);
        return container;
    }

    private void registerTemporalEnvironment(GenericContainer<?> container,
                                             ConfigurableEnvironment environment,
                                             TemporalProperties properties) {
        String host = container.getHost();
        Integer port = container.getMappedPort(INTERNAL_PORT);

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.temporal.host", host);
        map.put("embedded.temporal.port", port);
        map.put("embedded.temporal.networkAlias", TEMPORAL_NETWORK_ALIAS);
        map.put("embedded.temporal.internalPort", INTERNAL_PORT);
        if (properties.isUiEnabled()) {
            Integer uiPort = container.getMappedPort(INTERNAL_UI_PORT);
            map.put("embedded.temporal.uiPort", uiPort);
        }

        MapPropertySource propertySource = new MapPropertySource("embeddedTemporalInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
