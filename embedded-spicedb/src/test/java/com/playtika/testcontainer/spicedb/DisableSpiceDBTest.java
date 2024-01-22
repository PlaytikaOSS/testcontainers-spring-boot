package com.playtika.testcontainer.spicedb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.Container;

import static org.assertj.core.api.Assertions.assertThat;

 class DisableSpiceDBTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EmbeddedSpiceDBBootstrapConfiguration.class));

    @Test
     void contextLoads() {
        contextRunner
                .withPropertyValues(
                        "embedded.spicedb.enabled=false"
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(Container.class)
                        .doesNotHaveBean("spiceDbDependencyPostProcessor"));
    }

}
