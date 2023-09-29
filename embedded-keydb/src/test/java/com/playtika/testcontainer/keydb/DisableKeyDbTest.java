package com.playtika.testcontainer.keydb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.Container;

import static org.assertj.core.api.Assertions.assertThat;

class DisableKeyDbTest {

  private static final ApplicationContextRunner CONTEXT_RUNNER = new ApplicationContextRunner()
    .withConfiguration(AutoConfigurations.of(
      EmbeddedKeyDbBootstrapConfiguration.class,
      EmbeddedKeyDbTestOperationsAutoConfiguration.class,
      EmbeddedKeyDbDependenciesAutoConfiguration.class));

  @Test
  void contextLoads() {
    CONTEXT_RUNNER
      .withPropertyValues(
        "embedded.keydb.enabled=false"
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
