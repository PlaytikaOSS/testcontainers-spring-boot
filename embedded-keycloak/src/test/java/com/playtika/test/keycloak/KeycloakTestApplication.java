package com.playtika.test.keycloak;

import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(KeycloakConfiguration.class)
@KeycloakConfiguration
@SpringBootApplication
public class KeycloakTestApplication {

}
