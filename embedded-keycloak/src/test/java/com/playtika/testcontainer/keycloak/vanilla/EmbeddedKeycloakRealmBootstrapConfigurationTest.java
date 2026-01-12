package com.playtika.testcontainer.keycloak.vanilla;

import com.playtika.testcontainer.keycloak.util.KeycloakClient;
import com.playtika.testcontainer.keycloak.util.KeycloakClientTestConfiguration;
import com.playtika.testcontainer.keycloak.util.RealmInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {VanillaTestApplication.class, KeycloakClientTestConfiguration.class})
@ActiveProfiles({"active", "realm"})
public class EmbeddedKeycloakRealmBootstrapConfigurationTest {

    @Autowired
    private KeycloakClient keycloakClient;

    @Test
    public void shouldGetTestRealmInfoFromKeycloak() {
        String realm = keycloakClient.realm();
        RealmInfo realmInfo = keycloakClient.getRealmInfo(realm);

        assertThat(realmInfo.getRealm()).isEqualTo(realm);
    }

    @Test
    public void shouldGetAccessTokenFromKeycloak() {
        String token = keycloakClient.keycloakToken().getAccessToken();
        assertThat(token).isNotEmpty();
    }
}
