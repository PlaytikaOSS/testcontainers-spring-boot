package com.playtika.test.kafka;

import com.playtika.test.kafka.configuration.EmbeddedKafkaBootstrapConfiguration;
import com.playtika.test.kafka.configuration.EmbeddedKafkaTestOperationsAutoConfiguration;
import com.playtika.test.kafka.configuration.camel.EmbeddedKafkaCamelAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@Order(1)
@DisplayName("Test that application")
public class DisableKafkaTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    EmbeddedKafkaBootstrapConfiguration.class,
                    EmbeddedKafkaTestOperationsAutoConfiguration.class,
                    EmbeddedKafkaCamelAutoConfiguration.class));

    @Test
    @DisplayName("runs with zookeeper & kafka & schema registry disabled")
    public void contextLoads() {
        contextRunner
                .withPropertyValues(
                        "embedded.kafka.enabled=false"
                )
                .run((context) -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean("kafka")
                        .doesNotHaveBean("schema-registry"));
    }
}
