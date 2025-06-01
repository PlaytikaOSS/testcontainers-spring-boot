package com.playtika.testcontainer.k3s;

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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.k3s.K3sContainer;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.k3s.K3sProperties.EMBEDDED_K3S;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.k3s.enabled", matchIfMissing = true)
@EnableConfigurationProperties(K3sProperties.class)
public class EmbeddedK3sBootstrapConfiguration {

    private static final String K3S_NETWORK_ALIAS = "k3s.testcontainer.docker";

    @Bean(name = EMBEDDED_K3S, destroyMethod = "stop")
    public K3sContainer k3s(K3sProperties properties,
                            Optional<Network> network) {
        K3sContainer k3sContainer = new K3sContainer(ContainerUtils.getDockerImageName(properties));
        k3sContainer
                .withCommand(new String[]{"server", "--tls-san=" + k3sContainer.getHost()})
                .withExposedPorts(properties.getPort())
                .waitingFor(new LogMessageWaitStrategy().withRegEx(".*Node controller sync successful.*"))
                .withNetworkAliases(K3S_NETWORK_ALIAS);

        network.ifPresent(k3sContainer::withNetwork);

        k3sContainer = (K3sContainer) configureCommonsAndStart(k3sContainer, properties, log);
        log.info("Started K3s");

        return k3sContainer;
    }

    @Bean
    public DynamicPropertyRegistrar k3sDynamicPropertyRegistrar(@Qualifier(EMBEDDED_K3S) K3sContainer k3s, K3sProperties properties) {
        return registry -> {
            registry.add("embedded.k3s.kubeconfig", k3s::getKubeConfigYaml);
            registry.add("embedded.k3s.networkAlias", () -> K3S_NETWORK_ALIAS);
            registry.add("embedded.k3s.internalPort", properties::getPort);
        };
    }

}
