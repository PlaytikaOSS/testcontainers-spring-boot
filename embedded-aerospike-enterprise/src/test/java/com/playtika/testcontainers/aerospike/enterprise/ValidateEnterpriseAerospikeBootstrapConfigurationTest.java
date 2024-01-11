package com.playtika.testcontainers.aerospike.enterprise;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidateEnterpriseAerospikeBootstrapConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    SetupEnterpriseAerospikeBootstrapConfiguration.class));

    @Test
    void failOnNonEnterpriseImage() {
        contextRunner.withPropertyValues("embedded.aerospike.dockerImage=aerospike-server-community:6.3.1")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failOnUnsuitableEnterpriseImageVersion() {
        contextRunner.withPropertyValues("embedded.aerospike.dockerImage=aerospike/aerospike-server-enterprise:6.1.0.16_1")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void skipValidation() {
        contextRunner.withPropertyValues("embedded.aerospike.dockerImage=aerospike-server:6.1.0.16_1",
                "embedded.aerospike.enabled=false")
                .run(context -> assertThat(context).hasNotFailed());

    }
}
