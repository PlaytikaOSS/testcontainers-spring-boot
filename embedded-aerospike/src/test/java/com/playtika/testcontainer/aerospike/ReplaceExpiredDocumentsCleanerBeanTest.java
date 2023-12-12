package com.playtika.testcontainer.aerospike;

import com.aerospike.client.AerospikeClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

public class ReplaceExpiredDocumentsCleanerBeanTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withConfiguration(AutoConfigurations.of(
                    EmbeddedAerospikeBootstrapConfiguration.class,
                    EmbeddedAerospikeTestOperationsAutoConfiguration.class));

    @Test
    public void contextLoadsWithOtherExpiredDocumentsCleaner() {
        contextRunner.run(context -> assertThat(context)
                .hasNotFailed()
                .doesNotHaveBean("expiredDocumentsCleaner")
                .hasBean("otherExpiredDocumentsCleaner")
        );
    }

    @Configuration
    public static class TestConfiguration {

        @Bean
        public ExpiredDocumentsCleaner otherExpiredDocumentsCleaner() {
            return Mockito.mock(ExpiredDocumentsCleaner.class);
        }

        @Bean
        public AerospikeClient mockAerospikeClient() {
            return Mockito.mock(AerospikeClient.class);
        }

    }

}
