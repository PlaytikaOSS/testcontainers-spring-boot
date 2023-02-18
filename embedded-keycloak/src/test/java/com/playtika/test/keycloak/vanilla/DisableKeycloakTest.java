package com.playtika.test.keycloak.vanilla;

import com.playtika.test.keycloak.EmbeddedKeycloakBootstrapConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.Container;

import static org.assertj.core.api.Assertions.assertThat;

public class DisableKeycloakTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EmbeddedKeycloakBootstrapConfiguration.class));

    @Test
    public void contextLoads() {
        contextRunner
                .withPropertyValues(
                        "embedded.keycloak.enabled=false"
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(Container.class)
                        .doesNotHaveBean("keycloakSpringBootConfigResolverDependencyPostProcessor"));
    }

}
