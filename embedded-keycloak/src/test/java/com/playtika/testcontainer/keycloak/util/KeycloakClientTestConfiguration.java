package com.playtika.testcontainer.keycloak.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakClientTestConfiguration {
    @Value("${embedded.keycloak.auth-server-url}")
    private String baseUrl;
    @Value("${testing.keycloak.realm}")
    private String realm;
    @Value("${testing.keycloak.client}")
    private String clientId;
    @Value("${testing.keycloak.client-secret}")
    private String clientSecret;
    @Value("${testing.keycloak.username}")
    private String username;
    @Value("${testing.keycloak.password}")
    private String password;

    @Bean
    public KeycloakClient keycloakClient() {
        return KeycloakClient.of(baseUrl, realm, clientId, clientSecret, username, password);
    }
}