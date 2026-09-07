package com.playtika.testcontainer.common.spring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.testcontainers.containers.GenericContainer;

/**
 * Every {@code EmbeddedXxxBootstrapConfiguration} runs in Spring Cloud's bootstrap phase - a
 * separate context that fully completes, adding its containers' connection properties to the
 * environment, before the main application context (where {@code DataSourceProperties} and
 * friends bind {@code spring.datasource.*} placeholders) is even built. Scheduled container
 * starts must therefore be flushed here too, at the end of the bootstrap phase, not only from
 * {@code EmbeddedContainersShutdownAutoConfiguration.allContainers()} - that one lives in the
 * main context and would flush too late for anything bound from the bootstrap environment.
 */
@Configuration
@AutoConfigureOrder(value = Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "embedded.containers", name = "enabled", matchIfMissing = true)
public class ContainerStartupFlushConfiguration {

    @Bean
    public ContainerStartupFlushTrigger containerStartupFlushTrigger(
            ContainerStartupCoordinator startupCoordinator,
            @Autowired(required = false) GenericContainer[] allContainers) {
        startupCoordinator.flush();
        return new ContainerStartupFlushTrigger();
    }

    public static class ContainerStartupFlushTrigger {
    }
}
