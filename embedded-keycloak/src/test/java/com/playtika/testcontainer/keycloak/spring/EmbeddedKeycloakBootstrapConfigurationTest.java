package com.playtika.testcontainer.keycloak.spring;

import com.playtika.testcontainer.keycloak.util.KeyCloakToken;
import com.playtika.testcontainer.keycloak.util.KeycloakClient;
import com.playtika.testcontainer.keycloak.util.KeycloakClientTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import static com.playtika.testcontainer.keycloak.KeycloakProperties.DEFAULT_REALM;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;

@SpringBootTest(
        classes = {SpringTestApplication.class, KeycloakClientTestConfiguration.class},
        webEnvironment = RANDOM_PORT)
@ActiveProfiles({"enabled", "realm", "test"})
public class EmbeddedKeycloakBootstrapConfigurationTest {

    @Autowired
    private KeycloakClient keycloakClient;

    @LocalServerPort
    private int httpPort;

    @Test
    public void shouldRunThroughSpringSecurity() {
        assertThat(callSecuredPingEndpoint()).isEqualTo("pong");
    }

    @Test
    public void shouldGetMasterRealmInfoFromKeycloak() {
        String realmInfo = keycloakClient.getRealmInfo(DEFAULT_REALM).getRealm();
        assertThat(realmInfo).isEqualTo(DEFAULT_REALM);
    }

    private String callSecuredPingEndpoint() {
        KeyCloakToken keyCloakToken = keycloakClient.keycloakToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTHORIZATION, format("Bearer %s", keyCloakToken.getAccessToken()));

        RestOperations restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:" + httpPort + "/api/echo",
                GET,
                new HttpEntity<>(headers),
                String.class
        );

        return response.getBody();
    }
}
