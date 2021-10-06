package com.playtika.test.keycloak.vanilla;

import com.playtika.test.keycloak.util.KeycloakClient;
import com.playtika.test.keycloak.util.RealmInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static com.playtika.test.keycloak.util.KeycloakClient.newKeycloakClient;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = VanillaTestApplication.class)
@ActiveProfiles({"active", "realm"})
public class EmbeddedKeycloakRealmBootstrapConfigurationTest {

    @Autowired
    private Environment environment;

    @Test
    public void shouldGetTestRealmInfoFromKeycloak() {
        KeycloakClient client = newKeycloakClient(environment);
        String realm = client.realm();
        RealmInfo realmInfo = client.getRealmInfo(realm);

        assertThat(realmInfo.getRealm()).isEqualTo(realm);
    }

    @Test
    public void shouldGetAccessTokenFromKeycloak() {
        String token = newKeycloakClient(environment).keycloakToken().getAccessToken();
        assertThat(token).isNotEmpty();
    }
}
