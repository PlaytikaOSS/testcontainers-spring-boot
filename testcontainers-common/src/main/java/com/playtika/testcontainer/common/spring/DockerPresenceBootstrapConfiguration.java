package com.playtika.testcontainer.common.spring;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

@Configuration
@AutoConfigureOrder(value = Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "embedded.containers", name = "enabled", matchIfMissing = true)
public class DockerPresenceBootstrapConfiguration {

    public static final String DOCKER_IS_AVAILABLE = "dockerPresenceMarker";

    @Bean(name = DOCKER_IS_AVAILABLE)
    public DockerPresenceMarker dockerPresenceMarker() {
        return new DockerPresenceMarker(DockerClientFactory.instance().isDockerAvailable());
    }

    @Bean
    public static DependsOnDockerPostProcessor containerDependsOnDockerPostProcessor() {
        return new DependsOnDockerPostProcessor(GenericContainer.class);
    }

    @Bean
    public static DependsOnDockerPostProcessor networkDependsOnDockerPostProcessor() {
        return new DependsOnDockerPostProcessor(Network.class);
    }
}
