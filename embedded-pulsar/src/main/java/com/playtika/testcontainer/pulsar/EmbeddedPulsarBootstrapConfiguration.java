package com.playtika.testcontainer.pulsar;

import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
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
    ToxiproxyClientProxy pulsarContainerProxy(ToxiproxyClient toxiproxyClient,
                                               ToxiproxyContainer toxiproxyContainer,
                                               @Qualifier(EMBEDDED_PULSAR) PulsarContainer embeddedPulsar,
                                               PulsarProperties pulsarProperties,
                                               ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                embeddedPulsar,
                pulsarProperties.getBrokerPort(),
                "pulsar");
        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.pulsar", "embeddedPulsarToxiProxyInfo", environment);
        return proxy;
    }

    @Bean(name = EMBEDDED_PULSAR)
    public PulsarContainer embeddedPulsar(ConfigurableEnvironment environment,
                                          PulsarProperties pulsarProperties,
                                          @Deprecated @Value("${embedded.pulsar.imageTag:#{null}}") String deprImageTag,
                                          Optional<Network> network) {
        if (deprImageTag != null) {
            throw new IllegalArgumentException("Property `embedded.pulsar.imageTag` is deprecated. Please replace property `embedded.pulsar.imageTag` with `embedded.pulsar.dockerImageVersion`.");
        }
        PulsarContainer pulsarContainer = new PulsarContainer(ContainerUtils.getDockerImageName(pulsarProperties))
                .withNetworkAliases(PULSAR_NETWORK_ALIAS);

        network.ifPresent(pulsarContainer::withNetwork);
        pulsarContainer = (PulsarContainer) ContainerUtils.configureCommonsAndStart(pulsarContainer, pulsarProperties, log);
        registerPulsarEnvironment(pulsarContainer, environment, pulsarProperties);
        return pulsarContainer;
    }

    private void registerPulsarEnvironment(PulsarContainer pulsarContainer,
                                           ConfigurableEnvironment environment,
                                           PulsarProperties properties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.pulsar.brokerUrl", pulsarContainer.getPulsarBrokerUrl());
        map.put("embedded.pulsar.httpServiceUrl", pulsarContainer.getHttpServiceUrl());
        map.put("embedded.pulsar.networkAlias", PULSAR_NETWORK_ALIAS);
        map.put("embedded.pulsar.internalBrokerPort", properties.getBrokerPort());

        log.info("Started Pulsar. brokerUrl={}, httpServiceUrl={}",
                pulsarContainer.getPulsarBrokerUrl(), pulsarContainer.getHttpServiceUrl());

        MapPropertySource propertySource = new MapPropertySource("embeddedPulsarInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
