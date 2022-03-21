package com.playtika.test.toxiproxy;

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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(value = "embedded.toxiproxy.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(ToxiProxyProperties.class)
//TODO: enable when at least one container enables toxiproxy
// embedded.toxiproxy.enabled || embedded.{module}.toxiproxy.enabled
//@ConditionalOnProperty(value = "embedded.*.toxiproxy.enabled", havingValue = "true", matchIfMissing = false)
public class EmbeddedToxiProxyBootstrapConfiguration {

    private static final String TOXIPROXY_NETWORK_ALIAS = "toxiproxy";

    @Bean
    @ConditionalOnMissingBean(Network.class)
    Network toxiproxyNetwork() {
        return Network.newNetwork();
    }

    @Bean(name = "toxiproxy", destroyMethod = "stop")
    ToxiproxyContainer toxiproxy(ToxiProxyProperties toxiProxyProperties,
                                 Network network,
                                 ConfigurableEnvironment environment) {
        ToxiproxyContainer toxiproxyContainer = new ToxiproxyContainer(ContainerUtils.getDockerImageName(toxiProxyProperties))
                .withNetwork(network)
                .withNetworkAliases(TOXIPROXY_NETWORK_ALIAS);

        toxiproxyContainer = (ToxiproxyContainer) ContainerUtils.configureCommonsAndStart(toxiproxyContainer, toxiProxyProperties, log);
        registerEnvironment(toxiproxyContainer, environment);
        return toxiproxyContainer;
    }

    private void registerEnvironment(ToxiproxyContainer container,
                                     ConfigurableEnvironment environment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.toxiproxy.host", container.getContainerIpAddress());
        map.put("embedded.toxiproxy.controlPort", container.getControlPort());

        log.info("Started ToxiProxy server. Connection details {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedToxiProxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
