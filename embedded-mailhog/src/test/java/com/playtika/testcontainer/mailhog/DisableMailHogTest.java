package com.playtika.testcontainer.mailhog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.Container;

import static org.assertj.core.api.Assertions.assertThat;

class DisableMailHogTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(
                    AutoConfigurations.of(EmbeddedMailHogBootstrapConfiguration.class)
            );

    @Test
    void contextLoads() {
        contextRunner
                .withPropertyValues(
                        "embedded.mailhog.enabled=false"
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(Container.class));
    }
}
