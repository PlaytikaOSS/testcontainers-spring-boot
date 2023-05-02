package com.playtika.testcontainer.k3s;

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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.k3s.K3sContainer;

import java.util.LinkedHashMap;
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

    @Bean(name = EMBEDDED_K3S, destroyMethod = "stop")
    public K3sContainer k3s(ConfigurableEnvironment environment,
                            K3sProperties properties,
                            Optional<Network> network) {
        K3sContainer k3sContainer = new K3sContainer(ContainerUtils.getDockerImageName(properties));
        k3sContainer
                .withCommand(new String[]{"server", "--tls-san=" + k3sContainer.getHost()})
                .withExposedPorts(properties.getPort())
                .waitingFor(new LogMessageWaitStrategy().withRegEx(".*Node controller sync successful.*"));

        network.ifPresent(k3sContainer::withNetwork);

        k3sContainer = (K3sContainer) configureCommonsAndStart(k3sContainer, properties, log);
        registerK3sEnvironment(k3sContainer, environment);
        log.info("Started K3s");

        return k3sContainer;
    }

    private void registerK3sEnvironment(K3sContainer k3s,
                                        ConfigurableEnvironment environment) {
        String kubeConfigYaml = k3s.getKubeConfigYaml();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.k3s.kubeconfig", kubeConfigYaml);

        MapPropertySource propertySource = new MapPropertySource("embeddedK3sInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
