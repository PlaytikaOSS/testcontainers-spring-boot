package com.playtika.testcontainer.common.spring;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ContainerStartupConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ContainerStartupCoordinatorConfiguration.class,
                    ContainerStartupFlushConfiguration.class)
            .withPropertyValues("embedded.containers.parallelStartup=true");

    @Test
    void shouldFlushLazyContainerStartupBeforeBootstrapContextCompletes() {
        contextRunner.withUserConfiguration(ScheduledContainerConfiguration.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getProperty("test.container.endpoint"))
                    .isEqualTo("localhost:1234");
        });
    }

    @Test
    void shouldCompleteBootstrapWithoutContainers() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ContainerStartupFlushConfiguration.ContainerStartupFlushTrigger.class);
        });
    }

    @Test
    void shouldDisableStartupCoordinationWhenContainersAreDisabled() {
        contextRunner.withPropertyValues("embedded.containers.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ContainerStartupCoordinator.class);
            assertThat(context).doesNotHaveBean(ContainerStartupFlushConfiguration.ContainerStartupFlushTrigger.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class ScheduledContainerConfiguration {

        @Bean
        @Lazy
        GenericContainer<?> scheduledContainer(ContainerStartupCoordinator coordinator,
                                                ConfigurableEnvironment environment) {
            coordinator.schedule(() -> environment.getPropertySources().addFirst(new MapPropertySource(
                    "testContainerInfo", Map.of("test.container.endpoint", "localhost:1234"))));
            return mock(GenericContainer.class);
        }
    }
}
