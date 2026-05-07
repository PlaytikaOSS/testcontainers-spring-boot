package com.playtika.testcontainers.wiremock;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.Network;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;

@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.wiremock.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(WiremockProperties.class)
public class EmbeddedWiremockBootstrapConfiguration {

    private static final Logger log = LoggerFactory.getLogger(EmbeddedWiremockBootstrapConfiguration.class);

    static final String BEAN_NAME_EMBEDDED_WIREMOCK = "embeddedWiremock";
    private static final String WIREMOCK_NETWORK_ALIAS = "wiremock.testcontainer.docker";

    @Bean(value = BEAN_NAME_EMBEDDED_WIREMOCK, destroyMethod = "stop")
    public WireMockContainer wiremockContainer(ConfigurableEnvironment environment,
                                               WiremockProperties properties,
                                               Optional<Network> network) {
        WireMockContainer wiremock =
                new WireMockContainer(ContainerUtils.getDockerImageName(properties))
                        .withNetworkAliases(WIREMOCK_NETWORK_ALIAS);

        network.ifPresent(wiremock::withNetwork);

        wiremock = (WireMockContainer) configureCommonsAndStart(wiremock, properties, log);
        registerWiremockEnvironment(wiremock, environment);
        return wiremock;
    }

    private void registerWiremockEnvironment(WireMockContainer container, ConfigurableEnvironment environment) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.wiremock.port", container.getPort());
        map.put("embedded.wiremock.host", container.getHost());
        map.put("embedded.wiremock.networkAlias", WIREMOCK_NETWORK_ALIAS);
        map.put("embedded.wiremock.internalPort", 8080);

        log.info("Started wiremock. Connection Details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedWiremockInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
