package com.playtika.testcontainer.vault;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
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

    @Value("${embedded.vault.host}")
    String vaultHost;

    @Value("${embedded.vault.port}")
    String vaultPort;

    @Value("${embedded.vault.token}")
    String vaultToken;

    @Value("${secret_one}")
    String secretOne;

    @Autowired
    private VaultOperations vaultOperations;

    @Test
    public void propertiesAreAvailable() {
        assertThat(vaultHost).isNotEmpty();
        assertThat(vaultPort).isNotEmpty();
        assertThat(vaultToken).isNotEmpty();
        assertThat(secretOne).isNotEmpty();
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
