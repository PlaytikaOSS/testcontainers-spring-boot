package com.playtika.testcontainer.pulsar;

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
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;

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

    @Bean(name = "embeddedPulsar", destroyMethod = "stop")
    public PulsarContainer pulsar(PulsarProperties properties,
                                  Optional<Network> network) {
        PulsarContainer pulsar = new PulsarContainer(ContainerUtils.getDockerImageName(properties))
                .withNetworkAliases("pulsar.testcontainer.docker");
        network.ifPresent(pulsar::withNetwork);
        configureCommonsAndStart(pulsar, properties, log);
        return pulsar;
    }

    @Bean
    public DynamicPropertyRegistrar pulsarDynamicPropertyRegistrar(
            @Qualifier("embeddedPulsar") PulsarContainer pulsar,
            PulsarProperties properties) {
        return registry -> {
            registry.add("embedded.pulsar.brokerPort", () -> pulsar.getMappedPort(properties.getBrokerPort()));
            registry.add("embedded.pulsar.networkAlias", () -> "pulsar.testcontainer.docker");
            registry.add("embedded.pulsar.internalBrokerPort", properties::getBrokerPort);
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "pulsar")
    public DynamicPropertyRegistrar pulsarToxiProxyDynamicPropertyRegistrar(
            @Qualifier("pulsarContainerProxy") ToxiproxyContainer.ContainerProxy proxy) {
        return registry -> {
            registry.add("embedded.pulsar.toxiproxy.host", proxy::getContainerIpAddress);
            registry.add("embedded.pulsar.toxiproxy.port", proxy::getProxyPort);
            registry.add("embedded.pulsar.toxiproxy.proxyName", proxy::getName);
        };
    }
}
