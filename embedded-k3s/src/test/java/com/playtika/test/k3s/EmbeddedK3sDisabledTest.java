package com.playtika.test.k3s;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.Container;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedK3sDisabledTest {

    @Test
    void contextLoads() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(EmbeddedK3sBootstrapConfiguration.class))
                .withPropertyValues("embedded.k3s.enabled=false")
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(Container.class));
    }

}
