package com.playtika.testcontainer.toxiproxy;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
@ConditionalOnToxiProxyEnabled
@EnableConfigurationProperties(ToxiProxyProperties.class)
public class EmbeddedToxiProxyBootstrapConfiguration {

    private static final String TOXIPROXY_NETWORK_ALIAS = "toxiproxy.testcontainer.docker";
    // keep the old alias for backward compatibility
    private static final String TOXIPROXY_NETWORK_ALIAS_OLD = "toxiproxy";

    @Bean
    @ConditionalOnMissingBean(Network.class)
    Network toxiproxyNetwork() {
        Network network = Network.newNetwork();
        log.info("Created docker Network with id={}", network.getId());
        return network;
    }

    @Bean(name = "toxiproxy", destroyMethod = "stop")
    ToxiproxyContainer toxiproxy(ToxiProxyProperties toxiProxyProperties,
                                 Network network,
                                 ConfigurableEnvironment environment) {
        ToxiproxyContainer toxiproxyContainer = new ToxiproxyContainer(ContainerUtils.getDockerImageName(toxiProxyProperties))
                .withNetwork(network)
                .withNetworkAliases(TOXIPROXY_NETWORK_ALIAS, TOXIPROXY_NETWORK_ALIAS_OLD);

        toxiproxyContainer = (ToxiproxyContainer) ContainerUtils.configureCommonsAndStart(toxiproxyContainer, toxiProxyProperties, log);
        registerEnvironment(toxiproxyContainer, environment);
        return toxiproxyContainer;
    }

    private void registerEnvironment(ToxiproxyContainer container,
                                     ConfigurableEnvironment environment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.toxiproxy.host", container.getHost());
        map.put("embedded.toxiproxy.controlPort", container.getControlPort());
        map.put("embedded.toxiproxy.networkAlias", TOXIPROXY_NETWORK_ALIAS);

        log.info("Started ToxiProxy server. Connection details {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedToxiProxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
