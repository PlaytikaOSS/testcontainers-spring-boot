package com.playtika.testcontainer.postgresql;

import com.playtika.testcontainer.common.spring.ContainerStartupCoordinator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Collections;

/**
 * Mimics an {@code EmbeddedXxxBootstrapConfiguration} that registers a connection property via
 * {@link ContainerStartupCoordinator#schedule(Runnable)} instead of actually starting a real
 * Testcontainers container - used to prove the scheduled property is available before the main
 * application context binds it (e.g. via {@code @Value}), without needing Docker.
 */
@Configuration
public class FakeScheduledContainerBootstrapConfiguration {

    public static final String PROPERTY_NAME = "test.fake.scheduled.container.prop";
    public static final String PROPERTY_VALUE = "hello-from-fake-container";

    @Bean
    public String fakeScheduledContainer(ConfigurableEnvironment environment, ContainerStartupCoordinator startupCoordinator) {
        startupCoordinator.schedule(() -> environment.getPropertySources().addFirst(
                new MapPropertySource("fakeScheduledContainerInfo", Collections.singletonMap(PROPERTY_NAME, PROPERTY_VALUE))));
        return "fakeScheduledContainer";
    }
}
