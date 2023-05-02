package com.playtika.testcontainer.toxiproxy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.Container;

import static org.assertj.core.api.Assertions.assertThat;

public class DisableToxiProxyTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EmbeddedToxiProxyBootstrapConfiguration.class
            ));

    @Test
    public void isNotEnabledByDefault() {
        contextRunner
                .withPropertyValues(
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(Container.class)
                        .doesNotHaveBean("toxiproxyNetwork"));
    }

    @Test
    public void isDisabled() {
        contextRunner
                .withPropertyValues(
                        "embedded.toxiproxy.enabled=false"
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(Container.class)
                        .doesNotHaveBean("toxiproxyNetwork"));
    }

    @Test
    void isNotEnabledIfContainersDisabled() {
        contextRunner
                .withPropertyValues(
                        "embedded.containers.enabled=false",
                        "embedded.toxiproxy.enabled=true"
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(Container.class)
                        .doesNotHaveBean("toxiproxyNetwork"));
    }
}
