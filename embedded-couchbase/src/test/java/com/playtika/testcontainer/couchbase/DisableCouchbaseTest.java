package com.playtika.testcontainer.couchbase;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.Container;

import static org.assertj.core.api.Assertions.assertThat;

public class DisableCouchbaseTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EmbeddedCouchbaseBootstrapConfiguration.class,
                    EmbeddedCouchbaseDependenciesAutoConfiguration.class,
                    EmbeddedCouchbaseTestOperationsAutoConfiguration.class));

    @Test
    public void contextLoads() {
        contextRunner
                .withPropertyValues(
                        "embedded.couchbase.enabled=false"
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(Container.class)
                        .doesNotHaveBean("bucketDependencyPostProcessor")
                        .doesNotHaveBean("asyncBucketDependencyPostProcessor")
                        .doesNotHaveBean("couchbaseClientDependencyPostProcessor")
                );
    }

}
