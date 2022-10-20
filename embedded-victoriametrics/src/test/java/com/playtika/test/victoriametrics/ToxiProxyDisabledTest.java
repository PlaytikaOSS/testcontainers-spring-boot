package com.playtika.test.victoriametrics;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class ToxiProxyDisabledTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EmbeddedVictoriaMetricsBootstrapConfiguration.class));

    @Test
    public void shouldDisableToxiProxy() {
        contextRunner
                .withPropertyValues(
                        "embedded.toxiproxy.proxies.victoriametrics.enabled=false"
                )
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean("victoriaMetricsContainerProxy"));
    }
}
