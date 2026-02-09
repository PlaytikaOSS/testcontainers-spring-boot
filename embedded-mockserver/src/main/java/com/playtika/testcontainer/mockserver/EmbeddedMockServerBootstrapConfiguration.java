package com.playtika.testcontainer.mockserver;

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
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.Network;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.mockserver.MockServerProperties.EMBEDDED_MOCK_SERVER;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.mockserver.enabled", matchIfMissing = true)
@EnableConfigurationProperties(MockServerProperties.class)
public class EmbeddedMockServerBootstrapConfiguration {

    private static final String MOCKSERVER_NETWORK_ALIAS = "mockserver.testcontainer.docker";

    @Bean(name = EMBEDDED_MOCK_SERVER, destroyMethod = "stop")
    public MockServerContainer mockServerContainer(ConfigurableEnvironment environment,
                                                   MockServerProperties properties,
                                                   Optional<Network> network) {
        MockServerContainer mockServerContainer = new MockServerContainer(ContainerUtils.getDockerImageName(properties));
        mockServerContainer
                .withExposedPorts(properties.getPort())
                .withNetworkAliases(MOCKSERVER_NETWORK_ALIAS);

        network.ifPresent(mockServerContainer::withNetwork);

        mockServerContainer = (MockServerContainer) configureCommonsAndStart(mockServerContainer, properties, log);
        registerMockServerEnvironment(mockServerContainer, properties, environment);
        return mockServerContainer;
    }

    private void registerMockServerEnvironment(MockServerContainer mockServerContainer,
                                               MockServerProperties properties,
                                               ConfigurableEnvironment environment) {
        int mappedPort = mockServerContainer.getMappedPort(properties.getPort());
        String host = mockServerContainer.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mockserver.host", host);
        map.put("embedded.mockserver.port", mappedPort);
        map.put("embedded.mockserver.networkAlias", MOCKSERVER_NETWORK_ALIAS);
        map.put("embedded.mockserver.internalPort", properties.getPort());

        log.info("Started MockServer server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedMockServerInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
