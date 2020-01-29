/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.playtika.test.keycloak;

import static com.playtika.test.keycloak.KeycloakProperties.BEAN_NAME_EMBEDDED_KEYCLOAK;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import java.util.LinkedHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

@Slf4j
@Configuration
@Order(HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "embedded.keycloak.enabled", matchIfMissing = true)
@EnableConfigurationProperties(KeycloakProperties.class)
public class EmbeddedKeycloakBootstrapConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_KEYCLOAK, destroyMethod = "stop")
    public KeycloakContainer keycloakContainer(
        ConfigurableEnvironment environment,
        KeycloakProperties properties) {
        log.info("Starting Keycloak server. Docker image: {}", properties.getDockerImage());

        KeycloakContainer keycloak = new KeycloakContainer(properties);
        keycloak.start();

        registerKeycloakEnvironment(keycloak, environment);

        return keycloak;
    }

    private void registerKeycloakEnvironment(
        KeycloakContainer keycloak,
        ConfigurableEnvironment environment) {

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.keycloak.host", keycloak.getIp());
        map.put("embedded.keycloak.port.http", keycloak.getHttpPort());
        map.put("embedded.keycloak.auth-server-url", keycloak.getAuthServerUrl());

        log.info("Started Keycloak server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedKeycloakInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
