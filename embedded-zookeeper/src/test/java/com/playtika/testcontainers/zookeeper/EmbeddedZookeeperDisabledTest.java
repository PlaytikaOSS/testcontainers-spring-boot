package com.playtika.testcontainers.zookeeper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.Container;

import static org.assertj.core.api.Assertions.assertThat;

public class EmbeddedZookeeperDisabledTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EmbeddedZookeeperBootstrapConfiguration.class));

    @Test
    void contextLoads() {
        contextRunner
                .withPropertyValues("embedded.zookeeper.enabled=false")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(Container.class)
                );
    }
}
