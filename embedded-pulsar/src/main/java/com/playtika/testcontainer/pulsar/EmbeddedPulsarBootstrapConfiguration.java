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
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.containers.ToxiproxyContainer;

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
                                               PulsarProperties pulsarProperties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                embeddedPulsar,
                pulsarProperties.getBrokerPort(),
                "pulsar");
    }

    @Bean(name = EMBEDDED_PULSAR)
    public PulsarContainer embeddedPulsar(PulsarProperties pulsarProperties,
                                          @Deprecated @Value("${embedded.pulsar.imageTag:#{null}}") String deprImageTag,
                                          Optional<Network> network) {
        if (deprImageTag != null) {
            throw new IllegalArgumentException("Property `embedded.pulsar.imageTag` is deprecated. Please replace property `embedded.pulsar.imageTag` with `embedded.pulsar.dockerImageVersion`.");
        }
        PulsarContainer pulsarContainer = new PulsarContainer(ContainerUtils.getDockerImageName(pulsarProperties))
                .withNetworkAliases(PULSAR_NETWORK_ALIAS);

        network.ifPresent(pulsarContainer::withNetwork);
        pulsarContainer = (PulsarContainer) ContainerUtils.configureCommonsAndStart(pulsarContainer, pulsarProperties, log);
        return pulsarContainer;
    }

    @Bean
    public DynamicPropertyRegistrar pulsarDynamicPropertyRegistrar(
            @Qualifier(EMBEDDED_PULSAR) PulsarContainer pulsarContainer,
            PulsarProperties properties) {
        return registry -> {
            registry.add("embedded.pulsar.brokerUrl", pulsarContainer::getPulsarBrokerUrl);
            registry.add("embedded.pulsar.httpServiceUrl", pulsarContainer::getHttpServiceUrl);
            registry.add("embedded.pulsar.networkAlias", () -> PULSAR_NETWORK_ALIAS);
            registry.add("embedded.pulsar.internalBrokerPort", properties::getBrokerPort);

            log.info("Started Pulsar. brokerUrl={}, httpServiceUrl={}",
                    pulsarContainer.getPulsarBrokerUrl(), pulsarContainer.getHttpServiceUrl());
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "pulsar")
    public DynamicPropertyRegistrar pulsarToxiProxyDynamicPropertyRegistrar(
            @Qualifier("pulsarContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.pulsar");
    }
}
