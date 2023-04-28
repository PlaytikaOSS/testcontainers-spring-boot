package com.playtika.testcontainer.memsql;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.Container;

import static org.assertj.core.api.Assertions.assertThat;

public class DisableMemSQLTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EmbeddedMemSqlBootstrapConfiguration.class,
                    EmbeddedMemSqlTestOperationsAutoConfiguration.class,
                    EmbeddedMemSqlDependenciesAutoConfiguration.class));

    @Test
    public void contextLoads() {
        contextRunner
                .withPropertyValues(
                        "embedded.memsql.enabled=false"
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(Container.class)
                        .doesNotHaveBean("datasourceMemsqlDependencyPostProcessor"));
    }

}
