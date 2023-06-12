package com.playtika.testcontainer.pulsar;

import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.pulsar.PulsarProperties.EMBEDDED_PULSAR;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.pulsar.enabled", matchIfMissing = true)
@EnableConfigurationProperties(PulsarProperties.class)
public class EmbeddedPulsarBootstrapConfiguration {

    private static final String PULSAR_NETWORK_ALIAS = "pulsar.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "pulsar")
    ToxiproxyContainer.ContainerProxy pulsarContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                           @Qualifier(EMBEDDED_PULSAR) PulsarContainer embeddedPulsar,
                                                           PulsarProperties pulsarProperties,
                                                           ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(embeddedPulsar, pulsarProperties.getBrokerPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.pulsar.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.pulsar.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.pulsar.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedPulsarToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Pulsar ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = EMBEDDED_PULSAR)
    public PulsarContainer embeddedPulsar(PulsarProperties pulsarProperties,
                                          ConfigurableEnvironment environment,
                                          @Deprecated @Value("${embedded.pulsar.imageTag:#{null}}") String deprImageTag,
                                          Optional<Network> network) {
        if (deprImageTag != null) {
            throw new IllegalArgumentException("Property `embedded.pulsar.imageTag` is deprecated. Please replace property `embedded.pulsar.imageTag` with `embedded.pulsar.dockerImageVersion`.");
        }
        PulsarContainer pulsarContainer = new PulsarContainer(ContainerUtils.getDockerImageName(pulsarProperties))
                .withNetworkAliases(PULSAR_NETWORK_ALIAS);

        network.ifPresent(pulsarContainer::withNetwork);
        pulsarContainer = (PulsarContainer) ContainerUtils.configureCommonsAndStart(pulsarContainer, pulsarProperties, log);
        registerEmbeddedPulsarEnvironment(environment, pulsarContainer, pulsarProperties);
        return pulsarContainer;
    }

    private static void registerEmbeddedPulsarEnvironment(final ConfigurableEnvironment environment,
                                                          final PulsarContainer pulsarContainer,
                                                          PulsarProperties properties) {
        String pulsarBrokerUrl = pulsarContainer.getPulsarBrokerUrl();
        String pulsarHttpServiceUrl = pulsarContainer.getHttpServiceUrl();

        Map<String, Object> pulsarEnv = new LinkedHashMap<>();
        pulsarEnv.put("embedded.pulsar.brokerUrl", pulsarBrokerUrl);
        pulsarEnv.put("embedded.pulsar.httpServiceUrl", pulsarHttpServiceUrl);
        pulsarEnv.put("embedded.pulsar.networkAlias", PULSAR_NETWORK_ALIAS);
        pulsarEnv.put("embedded.pulsar.internalBrokerPort", properties.getBrokerPort());

        MapPropertySource propertySource = new MapPropertySource("embeddedPulsarInfo", pulsarEnv);
        environment.getPropertySources().addFirst(propertySource);
    }
}
