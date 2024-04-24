package com.playtika.testcontainer.vault;

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
        "embedded.vault.cas-enabled-for-sub-paths=sub-path1, sub-path2"
}, classes = EmbeddedVaultWithCasEnabledForSubPathsTest.TestConfiguration.class)
public class EmbeddedVaultWithCasEnabledForSubPathsTest {
    private static final String SECRET_ENTRY_KEY = "cas_key";
    private static final String SECRET_ENTRY_VALUE = "cas_val";

    private static final String CHECK_AND_SET_REQUIRED_ERROR = "check-and-set parameter required for this call";
    private static final String SECRET_MOUNT_NAME = "secret";
    private static final String SECRET_MOUNT_PATH = SECRET_MOUNT_NAME + "/data/";
    private static final String SUB_PATH1 = "sub-path1";
    private static final String SUB_PATH2 = "sub-path2";

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
        assertThat(environment.getProperty("embedded.vault.cas-enabled-for-sub-paths")).isNotEmpty();
    }

    @Test
    public void shouldFailOnWriteWhenCasEnabledButNotPassed() {
        Map<String, Object> body = new HashMap<>();
        body.put("data",  buildCredentialsEntity());

        assertThatThrownBy(() -> vaultTemplate.write(SECRET_MOUNT_PATH + SUB_PATH1, body))
                .isExactlyInstanceOf(VaultException.class)
                .hasMessageContaining(CHECK_AND_SET_REQUIRED_ERROR);

        assertThatThrownBy(() -> vaultTemplate.write(SECRET_MOUNT_PATH + SUB_PATH2, body))
                .isExactlyInstanceOf(VaultException.class)
                .hasMessageContaining(CHECK_AND_SET_REQUIRED_ERROR);

        Map<String, Object> options = new HashMap<>();
        options.put("cas", "0");

        Map<String, Object> versionedBody = new HashMap<>();
        versionedBody.put("data", buildCredentialsEntity());
        versionedBody.put("options", options);

        vaultTemplate.write(SECRET_MOUNT_PATH + SUB_PATH1, versionedBody);
        vaultTemplate.write(SECRET_MOUNT_PATH + SUB_PATH2, versionedBody);
    }

    @Test
    public void shouldSuccessfullyUpdateWhenVersionPassedCorrectly() {
        String pathKey = SUB_PATH1 + "/success_update_path_key";
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
