package com.playtika.test.vault;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.vault.VaultException;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.Versioned;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

@SpringBootTest(properties = {
        "embedded.vault.secrets.secret_one=password1",
        "embedded.vault.cas-enabled=true"
}, classes = EmbeddedVaultWithCasEnabledTest.TestConfiguration.class)
public class EmbeddedVaultWithCasEnabledTest {
    private static final String SECRET_ENTRY_KEY = "cas_key";
    private static final String SECRET_ENTRY_VALUE = "cas_val";

    private static final String CHECK_AND_SET_REQUIRED_ERROR = "check-and-set parameter required for this call";
    private static final String SECRET_MOUNT_NAME = "secret";

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private VaultOperations vaultOperations;

    @Autowired
    private VaultTemplate vaultTemplate;

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.vault.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.vault.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.vault.token")).isNotEmpty();
        assertThat(environment.getProperty("secret_one")).isNotEmpty();
        assertThat(environment.getProperty("embedded.vault.cas-enabled")).isNotEmpty();
    }

    @Test
    public void shouldFailOnWriteWhenCasEnabledButNotPassed() {
        assertThatThrownBy(() -> vaultOperations.opsForVersionedKeyValue(SECRET_MOUNT_NAME).put("path_key_cas_not_passed", buildCredentialsEntity()))
                .isExactlyInstanceOf(VaultException.class)
                .hasMessageContaining("check-and-set parameter required for this call");

        HashMap<String, Object> body = new HashMap<>();
        body.put("data", buildCredentialsEntity());

        assertThatThrownBy(() -> vaultTemplate.write("secret/data/cas_application", body))
                .isExactlyInstanceOf(VaultException.class)
                .hasMessageContaining(CHECK_AND_SET_REQUIRED_ERROR);

    }

    @Test
    public void shouldFailOnWriteWhenIncorrectVersionPassedWhenCreate() {
        Versioned<Map<String, String>> body = Versioned.create(buildCredentialsEntity(), Versioned.Version.from(11));

        assertThatThrownBy(() -> vaultOperations.opsForVersionedKeyValue(SECRET_MOUNT_NAME).put("incorrect_cas_version", body))
                .isExactlyInstanceOf(VaultException.class)
                .hasMessageContaining("check-and-set parameter did not match the current version");
    }

    @Test
    public void shouldFailOnWriteWhenVersionAlreadyExistsOnWriteASecret() {
        String pathKey = "when_key_is_already_exists";
        Versioned<Map<String, String>> body = Versioned.create(buildCredentialsEntity(), Versioned.Version.from(0));
        vaultOperations.opsForVersionedKeyValue("secret").put(pathKey, body);

        assertThatThrownBy(() -> vaultOperations.opsForVersionedKeyValue(SECRET_MOUNT_NAME).put(pathKey, body))
                .isExactlyInstanceOf(VaultException.class)
                .hasMessageContaining("check-and-set parameter did not match the current version");
    }

    @Test
    public void shouldSuccessfullyUpdateWhenVersionPassedCorrectly() {
        String pathKey = "success_update_path_key";
        int initialVersion = 0;
        Versioned<Map<String, String>> body = Versioned.create(buildCredentialsEntity(), Versioned.Version.from(initialVersion));
        Versioned.Metadata created = vaultOperations.opsForVersionedKeyValue(SECRET_MOUNT_NAME).put(pathKey, body);
        Versioned.Version createdVersion = created.getVersion();

        assertThat(createdVersion).isEqualTo(Versioned.Version.from(initialVersion + 1));

        Versioned<Map<String, String>> toUpdate = Versioned.create(buildCredentialsEntity(), createdVersion);
        vaultOperations.opsForVersionedKeyValue(SECRET_MOUNT_NAME).put(pathKey, toUpdate);

        Versioned<Map<String, Object>> secrets = vaultOperations.opsForVersionedKeyValue(SECRET_MOUNT_NAME).get(pathKey);

        assertThat(secrets.getData())
                .as("check secret")
                .containsExactly(entry(SECRET_ENTRY_KEY, SECRET_ENTRY_VALUE));

        assertThat(secrets.getVersion())
                .isEqualTo(Versioned.Version.from(createdVersion.getVersion() + 1));
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }

    private Map<String, String> buildCredentialsEntity() {
        HashMap<String, String> entity = new HashMap<>();
        entity.put(SECRET_ENTRY_KEY, SECRET_ENTRY_VALUE);
        return entity;
    }
}
