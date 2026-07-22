package com.playtika.testcontainer.artifactory;

import com.playtika.testcontainer.postgresql.PostgreSQLProperties;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedArtifactoryBootstrapConfigurationSystemYamlTest {

    @Test
    void shouldUseConfiguredPostgresqlNetworkAliasInSystemYaml() {
        PostgreSQLProperties postgresqlProperties = new PostgreSQLProperties();
        postgresqlProperties.setDatabase("artifactory");
        postgresqlProperties.setNetworkAlias("postgresql.internal");

        PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:18-alpine")
                .withNetworkAliases("postgresql.internal")
                .withUsername("artifactory")
                .withPassword("secret");

        String systemYaml = EmbeddedArtifactoryBootstrapConfiguration.getSystemYaml(
                postgresqlProperties,
                postgreSQLContainer);

        assertThat(systemYaml)
                .contains("url: \"jdbc:postgresql://postgresql.internal:5432/artifactory\"")
                .contains("username: \"artifactory\"")
                .contains("password: \"secret\"");
    }
}
