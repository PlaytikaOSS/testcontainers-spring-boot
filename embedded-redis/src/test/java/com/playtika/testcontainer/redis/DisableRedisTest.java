package com.playtika.testcontainer.redis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.Container;

import static org.assertj.core.api.Assertions.assertThat;

public class DisableRedisTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EmbeddedRedisBootstrapConfiguration.class,
                    EmbeddedRedisTestOperationsAutoConfiguration.class,
                    EmbeddedRedisDependenciesAutoConfiguration.class));

    @Test
    public void contextLoads() {
        contextRunner
                .withPropertyValues(
                        "embedded.redis.enabled=false"
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(Container.class)
                        .doesNotHaveBean("redisConnectionFactoryDependencyPostProcessor")
                        .doesNotHaveBean("redisTemplateDependencyPostProcessor")
                        .doesNotHaveBean("jedisDependencyPostProcessor")
                );
    }

}
