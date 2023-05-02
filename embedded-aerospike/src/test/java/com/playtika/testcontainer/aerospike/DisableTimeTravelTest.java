package com.playtika.testcontainer.aerospike;

import com.aerospike.client.AerospikeClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

public class DisableTimeTravelTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(UserConfigurations.of(TestConfiguration.class))
            .withConfiguration(AutoConfigurations.of(
                    EmbeddedAerospikeBootstrapConfiguration.class,
                    EmbeddedAerospikeTestOperationsAutoConfiguration.class));


    @Test
    public void contextLoadsWithDisabledTimeTravelProperty() {
        contextRunner
                .withPropertyValues(
                "embedded.aerospike.time-travel.enabled=false"
            )
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(ExpiredDocumentsCleaner.class)
                        .hasBean("disabledExpiredDocumentsCleaner"));
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        public AerospikeClient mockAerospikeClient() {
            return Mockito.mock(AerospikeClient.class);
        }
    }
}
