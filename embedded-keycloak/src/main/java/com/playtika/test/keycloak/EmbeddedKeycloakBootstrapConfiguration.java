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
import static java.util.Objects.requireNonNull;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Slf4j
@Configuration
@ComponentScan
@Order(HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(KeycloakProperties.class)
public class EmbeddedKeycloakBootstrapConfiguration {

    /**
     * Creates and starts a {@link KeycloakContainer} if property {@code embedded.keycloak.enabled}
     * evaluates to {@code true}. The configuration makes no difference if just vanilla Keycloak is
     * on the classpath or any Spring adapter. The container will always be needed. Also registers a
     * shutdown hook to stop the container on context shutdown.
     *
     * @param factory The {@link KeycloakContainerFactory} to use, injected by Spring, must not be
     * null
     * @return The created {@link KeycloakContainer} instance to be registered as bean
     */
    @Bean(name = BEAN_NAME_EMBEDDED_KEYCLOAK, destroyMethod = "stop")
    @ConditionalOnProperty(name = "embedded.keycloak.enabled", matchIfMissing = true)
    public KeycloakContainer keycloakContainer(KeycloakContainerFactory factory) {
        log.info("Detected keycloak-spring-boot-adapter, ");
        return requireNonNull(factory).newKeycloakContainer();
    }
}
