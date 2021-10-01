package com.playtika.test.keycloak;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ResourceLoader;

import static com.playtika.test.keycloak.KeycloakProperties.BEAN_NAME_EMBEDDED_KEYCLOAK;
import static java.util.Objects.requireNonNull;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@EnableConfigurationProperties(KeycloakProperties.class)
@ConditionalOnProperty(name = "embedded.keycloak.enabled", matchIfMissing = true)
public class EmbeddedKeycloakBootstrapConfiguration {

    @Bean
    public KeycloakContainerFactory keycloakContainerFactory(
            ConfigurableEnvironment environment,
            KeycloakProperties properties,
            ResourceLoader resourceLoader) {
        return new KeycloakContainerFactory(environment, properties, resourceLoader);
    }

    /**
     * Creates and starts a {@link KeycloakContainer} if property {@code embedded.keycloak.enabled}
     * evaluates to {@code true}. The configuration makes no difference if just vanilla Keycloak is
     * on the classpath or any Spring adapter. The container will always be needed. Also registers a
     * shutdown hook to stop the container on context shutdown.
     *
     * @param factory The {@link KeycloakContainerFactory} to use, injected by Spring, must not be
     *                null
     * @return The created {@link KeycloakContainer} instance to be registered as bean
     */
    @Bean(name = BEAN_NAME_EMBEDDED_KEYCLOAK, destroyMethod = "stop")
    public KeycloakContainer keycloakContainer(KeycloakContainerFactory factory) {
        log.info("Detected keycloak-spring-boot-adapter, ");
        return requireNonNull(factory).newKeycloakContainer();
    }
}
