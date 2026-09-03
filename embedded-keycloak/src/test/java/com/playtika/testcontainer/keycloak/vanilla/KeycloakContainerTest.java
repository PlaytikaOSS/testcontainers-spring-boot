package com.playtika.testcontainer.keycloak.vanilla;

import com.playtika.testcontainer.common.spring.ContainerStartupCoordinatorConfiguration;
import com.playtika.testcontainer.keycloak.EmbeddedKeycloakBootstrapConfiguration;
import com.playtika.testcontainer.keycloak.EmbeddedKeycloakBootstrapConfiguration.ImportFileNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class KeycloakContainerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ContainerStartupCoordinatorConfiguration.class,
                    EmbeddedKeycloakBootstrapConfiguration.class));

    @Test
    public void shouldThrowImportFileNotFoundExceptionWhenImportFileDoesNotExist() {
        contextRunner
                .withPropertyValues("embedded.keycloak.import-file=non-existent-file.json")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(ImportFileNotFoundException.class);
                });
    }
}
