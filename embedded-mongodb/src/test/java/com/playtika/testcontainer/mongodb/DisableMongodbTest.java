package com.playtika.testcontainer.mongodb;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.Container;

public class DisableMongodbTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EmbeddedMongodbBootstrapConfiguration.class,
                    EmbeddedMongodbTestOperationsAutoConfiguration.class,
                    EmbeddedMongodbDependenciesAutoConfiguration.class));

    @Test
    public void contextLoads() {
        contextRunner
                .withPropertyValues(
                        "embedded.mongodb.enabled=false"
                )
                .run((context) -> Assertions.assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(Container.class)
                        .doesNotHaveBean("mongoClientDependencyPostProcessor"));
    }

}
