package com.playtika.testcontainer.victoriametrics;

import com.playtika.testcontainer.common.spring.ContainerStartupCoordinatorConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class ToxiProxyDisabledTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ContainerStartupCoordinatorConfiguration.class,
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
