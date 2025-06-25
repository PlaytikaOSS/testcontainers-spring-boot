package com.playtika.testcontainer.nativekafka;

import com.playtika.testcontainer.nativekafka.configuration.EmbeddedNativeKafkaBootstrapConfiguration;
import com.playtika.testcontainer.nativekafka.configuration.EmbeddedNativeKafkaTestOperationsAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Order(1)
@DisplayName("Test that application")
public class DisableNativeKafkaTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EmbeddedNativeKafkaBootstrapConfiguration.class,
                    EmbeddedNativeKafkaTestOperationsAutoConfiguration.class));

    @Test
    @DisplayName("runs with native kafka disabled")
    public void contextLoads() {
        contextRunner
                .withPropertyValues(
                        "embedded.nativekafka.enabled=false"
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean("nativeKafka"));
    }
}