package com.playtika.test.azurite;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Order(1)
class DisableAzuriteTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EmbeddedAzuriteBootstrapConfiguration.class));

    @Test
    @DisplayName("ensures the azurite container is not being created when azurite is disabled")
    void contextLoads() {
        contextRunner
                .withPropertyValues(
                        "embedded.azurite.enabled=false"
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(AzuriteProperties.AZURITE_BEAN_NAME));
    }
}
