package com.playtika.testcontainers.wiremock;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

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
    public GenericContainer<?> wiremock(WiremockProperties properties, Optional<Network> network) {
        GenericContainer<?> container = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getPort())
                .withNetworkAliases(WIREMOCK_NETWORK_ALIAS);
        network.ifPresent(container::withNetwork);
        configureCommonsAndStart(container, properties, log);
        return container;
    }

    @Bean
    public DynamicPropertyRegistrar wiremockDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_WIREMOCK) GenericContainer<?> container,
            WiremockProperties properties) {
        return registry -> {
            registry.add("embedded.wiremock.host", container::getHost);
            registry.add("embedded.wiremock.port", () -> container.getMappedPort(properties.getPort()));
            registry.add("embedded.wiremock.networkAlias", () -> WIREMOCK_NETWORK_ALIAS);
            registry.add("embedded.wiremock.internalPort", properties::getPort);
        };
    }
}
