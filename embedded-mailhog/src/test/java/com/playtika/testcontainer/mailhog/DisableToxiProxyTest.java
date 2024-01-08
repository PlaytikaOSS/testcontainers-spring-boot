package com.playtika.testcontainer.mailhog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.ToxiproxyContainer;

import static org.assertj.core.api.Assertions.assertThat;

class DisableToxiProxyTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EmbeddedMailHogBootstrapConfiguration.class));

    @Test
    void isNotEnabledByDefault() {
        contextRunner
                .withPropertyValues(
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(ToxiproxyContainer.ContainerProxy.class));
    }

    @Test
    void shouldDisableToxiProxy() {
        contextRunner
                .withPropertyValues(
                        "embedded.toxiproxy.proxies.mailhog.enabled=false"
                )
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(ToxiproxyContainer.ContainerProxy.class));
    }
}
