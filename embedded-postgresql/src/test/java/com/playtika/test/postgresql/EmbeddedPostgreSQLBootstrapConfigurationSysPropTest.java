package com.playtika.test.postgresql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("enabled")
@SpringBootTest(properties = {"embedded.postgresql.expose-props-as-sys-props=true"})
class EmbeddedPostgreSQLBootstrapConfigurationSysPropTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    void propertiesAreExposedAsSystemProperties() {

        assertThat(environment.getProperty("embedded.postgresql.host")).isNotEmpty();
        assertThat(toSysProp("embedded.postgresql.host")).isEqualTo(environment.getProperty("embedded.postgresql.host"));

        assertThat(environment.getProperty("embedded.postgresql.schema")).isNotEmpty();
        assertThat(toSysProp("embedded.postgresql.schema")).isEqualTo(environment.getProperty("embedded.postgresql.schema"));

        assertThat(environment.getProperty("embedded.postgresql.user")).isNotEmpty();
        assertThat(toSysProp("embedded.postgresql.user")).isEqualTo(environment.getProperty("embedded.postgresql.user"));

        assertThat(environment.getProperty("embedded.postgresql.password")).isNotEmpty();
        assertThat(toSysProp("embedded.postgresql.password")).isEqualTo(environment.getProperty("embedded.postgresql.password"));

        assertThat(environment.getProperty("embedded.postgresql.port")).isNotEmpty();
        assertThat(toSysProp("embedded.postgresql.port")).isEqualTo(environment.getProperty("embedded.postgresql.port"));
    }

    private String toSysProp(String key) {
        return (String) environment.getSystemProperties().get(key.toUpperCase().replace(".", "_"));
    }

}
