package com.playtika.test.keycloak;

import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ResourceLoader;

@Slf4j
public class KeycloakContainerFactory {

    private final ConfigurableEnvironment environment;
    private final KeycloakProperties properties;
    private final ResourceLoader resourceLoader;

    @Autowired
    KeycloakContainerFactory(ConfigurableEnvironment environment, KeycloakProperties properties,
        ResourceLoader resourceLoader) {
        this.environment = environment;
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    public KeycloakContainer newKeycloakContainer() {
        log.info("Starting Keycloak server. Docker image: {}", properties.getDockerImage());

        KeycloakContainer keycloak = new KeycloakContainer(properties, resourceLoader)
                .withReuse(properties.isReuseContainer());
        keycloak.start();

        registerKeycloakEnvironment(keycloak);

        return keycloak;
    }

    private void registerKeycloakEnvironment(KeycloakContainer keycloak) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.keycloak.host", keycloak.getIp());
        map.put("embedded.keycloak.http-port", keycloak.getHttpPort());
        map.put("embedded.keycloak.auth-server-url", keycloak.getAuthServerUrl());

        log.info("Started Keycloak server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedKeycloakInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
