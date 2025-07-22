package com.playtika.testcontainer.keycloak.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

public final class KeycloakClient {

    private final String baseUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final String username;
    private final String password;
    private final RestTemplate restTemplate;

    private KeycloakClient(String baseUrl, String realm, String clientId, String clientSecret, String username, String password) {
        this.baseUrl = requireNonNull(baseUrl);
        this.realm = requireNonNull(realm);
        this.clientId = requireNonNull(clientId);
        this.clientSecret = requireNonNull(clientSecret);
        this.username = requireNonNull(username);
        this.password = requireNonNull(password);
        this.restTemplate = new RestTemplate();
    }

    public static KeycloakClient of(String baseUrl, String realm, String clientId, String clientSecret, String username, String password) {
        return new KeycloakClient(baseUrl, realm, clientId, clientSecret, username, password);
    }

    public KeyCloakToken keycloakToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);
        map.add("grant_type", "password");
        map.add("username", username);
        map.add("password", password);

        String url = format("%s/realms/%s/protocol/openid-connect/token", baseUrl, realm);
        return restTemplate.postForObject(url, new HttpEntity<>(map, headers), KeyCloakToken.class);
    }

    public String realm() {
        return realm;
    }

    public RealmInfo getRealmInfo(String realmName) {
        return restTemplate.getForObject(
            format("%s/realms/%s", baseUrl, realmName),
            RealmInfo.class);
    }
}
