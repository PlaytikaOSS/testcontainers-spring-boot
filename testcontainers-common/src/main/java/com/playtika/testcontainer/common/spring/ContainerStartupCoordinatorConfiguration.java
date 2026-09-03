package com.playtika.testcontainer.common.spring;

import com.playtika.testcontainer.common.properties.TestcontainersProperties;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Kept separate from {@link DockerPresenceBootstrapConfiguration} on purpose: that class's
 * {@code dockerPresenceMarker} bean throws if Docker isn't available, which would make
 * {@link ContainerStartupCoordinator} unusable in tests that intentionally avoid touching Docker
 * (e.g. {@code ApplicationContextRunner} tests that only load one module's bootstrap configuration
 * to check validation/wiring). This class has no such dependency.
 */
@Configuration
@AutoConfigureOrder(value = Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "embedded.containers", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TestcontainersProperties.class)
public class ContainerStartupCoordinatorConfiguration {

    @Bean
    public ContainerStartupCoordinator containerStartupCoordinator(TestcontainersProperties properties) {
        return new ContainerStartupCoordinator(properties);
    }
}
