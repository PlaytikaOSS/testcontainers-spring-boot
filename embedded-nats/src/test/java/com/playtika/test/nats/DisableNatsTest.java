package com.playtika.test.nats;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.Container;

import static org.assertj.core.api.Assertions.assertThat;

 class DisableNatsTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EmbeddedNatsBootstrapConfiguration.class));

    @Test
     void contextLoads() {
        contextRunner
                .withPropertyValues(
                        "embedded.nats.enabled=false"
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(Container.class)
                        .doesNotHaveBean("natsDependencyPostProcessor"));
    }

}
