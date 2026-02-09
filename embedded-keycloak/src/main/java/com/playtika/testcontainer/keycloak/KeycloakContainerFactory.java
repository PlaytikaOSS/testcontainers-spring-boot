package com.playtika.testcontainer.keycloak;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.containers.Network;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.keycloak.KeycloakContainer.KEYCLOAK_DEFAULT_HTTP_PORT_INTERNAL;

@Slf4j
@RequiredArgsConstructor
public class KeycloakContainerFactory {

    private static final String KEYCLOAK_NETWORK_ALIAS = "keycloak.testcontainer.docker";

    private final ConfigurableEnvironment environment;
    private final KeycloakProperties properties;
    private final ResourceLoader resourceLoader;
    private final Optional<Network> network;

    public KeycloakContainer newKeycloakContainer() {
        KeycloakContainer keycloak = new KeycloakContainer(properties, resourceLoader)
                .withNetworkAliases(KEYCLOAK_NETWORK_ALIAS);

        network.ifPresent(keycloak::withNetwork);

        keycloak = (KeycloakContainer) configureCommonsAndStart(keycloak, properties, log);
        registerKeycloakEnvironment(keycloak);
        return keycloak;
    }

    private void registerKeycloakEnvironment(KeycloakContainer keycloak) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.keycloak.host", keycloak.getIp());
        map.put("embedded.keycloak.http-port", keycloak.getHttpPort());
        map.put("embedded.keycloak.auth-server-url", keycloak.getAuthServerUrl());
        map.put("embedded.keycloak.networkAlias", KEYCLOAK_NETWORK_ALIAS);
        map.put("embedded.keycloak.internalPort", KEYCLOAK_DEFAULT_HTTP_PORT_INTERNAL);

        log.info("Started Keycloak server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedKeycloakInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
