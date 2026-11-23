package com.playtika.testcontainer.keycloak.util;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

public final class KeycloakClient {

    private final Environment environment;
    private final RestClient restClient;

    private KeycloakClient(Environment environment) {
        this.environment = environment;
        this.restClient = RestClient.builder().build();
    }

    public static KeycloakClient newKeycloakClient(Environment environment) {
        return new KeycloakClient(requireNonNull(environment));
    }

    public KeyCloakToken keycloakToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", fromEnv("testing.keycloak.client"));
        map.add("client_secret", fromEnv("testing.keycloak.client-secret"));
        map.add("grant_type", "password");
        map.add("username", fromEnv("testing.keycloak.username"));
        map.add("password", fromEnv("testing.keycloak.password"));

        String url = format("%s/realms/%s/protocol/openid-connect/token", baseUrl(), realm());
        return restClient.post().uri(url).headers(h -> h.addAll(headers)).body(map).retrieve().body(KeyCloakToken.class);
    }

    public String baseUrl() {
        return fromEnv("embedded.keycloak.auth-server-url");
    }

    public String realm() {
        return fromEnv("testing.keycloak.realm");
    }

    public RealmInfo getRealmInfo(String realm) {
        return restClient.get().uri(format("%s/realms/%s", baseUrl(), requireNonNull(realm))).retrieve().body(
            RealmInfo.class);
    }

    private String fromEnv(String key) {
        return environment.getProperty(requireNonNull(key));
    }
}
