package com.playtika.testcontainer.pulsar;

import com.playtika.testcontainer.common.spring.ContainerStartupCoordinator;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.pulsar.PulsarContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

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

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.pulsar", "embeddedPulsarToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = EMBEDDED_PULSAR)
    public PulsarContainer embeddedPulsar(PulsarProperties pulsarProperties,
                                          ConfigurableEnvironment environment,
                                          Optional<Network> network,
                                          ContainerStartupCoordinator startupCoordinator) {
        PulsarContainer pulsarContainer = new PulsarContainer(ContainerUtils.getDockerImageName(pulsarProperties))
                .withNetworkAliases(PULSAR_NETWORK_ALIAS);

        network.ifPresent(pulsarContainer::withNetwork);
        PulsarContainer configuredPulsarContainer = (PulsarContainer) ContainerUtils.configureCommons(pulsarContainer, pulsarProperties, log);
        startupCoordinator.schedule(() -> {
            ContainerUtils.startAndLogTime(configuredPulsarContainer, log);
            registerEmbeddedPulsarEnvironment(environment, configuredPulsarContainer, pulsarProperties);
        });
        return configuredPulsarContainer;
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
