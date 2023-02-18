package com.playtika.test.keycloak.spring;

import com.playtika.test.keycloak.KeycloakContainer;
import com.playtika.test.keycloak.util.KeyCloakToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import static com.playtika.test.keycloak.KeycloakProperties.DEFAULT_REALM;
import static com.playtika.test.keycloak.util.KeycloakClient.newKeycloakClient;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpMethod.GET;

@SpringBootTest(
        classes = SpringTestApplication.class,
        webEnvironment = RANDOM_PORT)
@ActiveProfiles({"enabled", "realm"})
public class EmbeddedKeycloakBootstrapConfigurationTest {

    @Autowired
    private Environment environment;
    @Autowired
    private ConfigurableListableBeanFactory beanFactory;
    @Autowired
    private KeycloakContainer keycloakContainer;

    @LocalServerPort
    private int httpPort;

    @Test
    public void shouldRunThroughSpringSecurity() {
        assertThat(callSecuredPingEndpoint()).isEqualTo("pong");
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.keycloak.auth-server-url"))
                .isEqualTo(format("http://%s:%d/auth", keycloakContainer.getHost(),
                        keycloakContainer.getHttpPort()));

        assertThat(environment.getProperty("embedded.keycloak.host"))
                .isEqualTo(keycloakContainer.getIp());

        assertThat(environment.getProperty("embedded.keycloak.http-port", Integer.class))
                .isEqualTo(keycloakContainer.getHttpPort());
    }

    @Test
    public void shouldGetMasterRealmInfoFromKeycloak() {
        String realmInfo = newKeycloakClient(environment).getRealmInfo(DEFAULT_REALM).getRealm();
        assertThat(realmInfo).isEqualTo(DEFAULT_REALM);
    }

    private String callSecuredPingEndpoint() {
        KeyCloakToken keyCloakToken = newKeycloakClient(environment).keycloakToken();

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
