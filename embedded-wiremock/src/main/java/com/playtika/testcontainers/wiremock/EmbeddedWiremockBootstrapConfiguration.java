package com.playtika.testcontainers.wiremock;

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
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.wiremock.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(WiremockProperties.class)
public class EmbeddedWiremockBootstrapConfiguration {

    static final String BEAN_NAME_EMBEDDED_WIREMOCK = "embeddedWiremock";
    private static final String WIREMOCK_NETWORK_ALIAS = "wiremock.testcontainer.docker";
    private static final WaitStrategy DEFAULT_WAITER = Wait.forHttp("/__admin/mappings")
            .withMethod("GET")
            .forStatusCode(200);

    @Bean(name = BEAN_NAME_EMBEDDED_WIREMOCK, destroyMethod = "stop")
    public GenericContainer<?> wiremock(ConfigurableEnvironment environment,
                                        WiremockProperties properties,
                                        Optional<Network> network) {
        GenericContainer<?> wiremock =
            new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .waitingFor(DEFAULT_WAITER)
                .withCommand("--port " + properties.getPort())
                .withExposedPorts(properties.getPort())
                .withNetworkAliases(WIREMOCK_NETWORK_ALIAS);
        network.ifPresent(wiremock::withNetwork);
        configureCommonsAndStart(wiremock, properties, log);
        registerWiremockEnvironment(wiremock, environment, properties);
        return wiremock;
    }

    private void registerWiremockEnvironment(GenericContainer<?> container,
                                             ConfigurableEnvironment environment,
                                             WiremockProperties properties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.wiremock.host", container.getHost());
        map.put("embedded.wiremock.port", container.getMappedPort(properties.getPort()));
        map.put("embedded.wiremock.networkAlias", WIREMOCK_NETWORK_ALIAS);
        map.put("embedded.wiremock.internalPort", properties.getPort());

        log.info("Started wiremock. Connection Details: embedded.wiremock.host={}, embedded.wiremock.port={}," +
                 "embedded.wiremock.networkAlias={}, embedded.wiremock.internalPort={}",
            container.getHost(), container.getMappedPort(properties.getPort()), WIREMOCK_NETWORK_ALIAS, properties.getPort());

        MapPropertySource propertySource = new MapPropertySource("embeddedWiremockInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
