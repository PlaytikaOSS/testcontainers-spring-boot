package com.playtika.testcontainers.wiremock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.Container;

import static org.assertj.core.api.Assertions.assertThat;

public class DisableWiremockTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EmbeddedWiremockBootstrapConfiguration.class));

    @Test
    public void shouldSkipEmbeddedWiremockBootstrapConfiguration() {
        contextRunner
                .withPropertyValues(
                        "embedded.wiremock.enabled=false"
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(Container.class));
    }

    @Test
    public void shouldSkipEmbeddedWiremockBootstrapConfigurationWhenContainersDisabled() {
        contextRunner
                .withPropertyValues(
                        "embedded.containers.enabled=false"
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(Container.class));
    }

}
