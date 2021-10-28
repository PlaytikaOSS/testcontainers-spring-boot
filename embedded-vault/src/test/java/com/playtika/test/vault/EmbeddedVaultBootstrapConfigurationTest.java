package com.playtika.test.vault;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.support.Versioned;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@SpringBootTest(properties = {
        "embedded.vault.secrets.secret_one=password1"
}
,classes = EmbeddedVaultBootstrapConfigurationTest.TestConfiguration.class)
public class EmbeddedVaultBootstrapConfigurationTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private VaultOperations vaultOperations;

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.vault.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.vault.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.vault.token")).isNotEmpty();
        assertThat(environment.getProperty("secret_one")).isNotEmpty();
    }

    @Test
    public void shouldReadASecret() {
        Versioned<Map<String, Object>> secrets = vaultOperations.opsForVersionedKeyValue("secret").get("application");

        assertThat(secrets.getData())
                .as("check secret")
                .containsExactly(entry("secret_one", "password1"));
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
