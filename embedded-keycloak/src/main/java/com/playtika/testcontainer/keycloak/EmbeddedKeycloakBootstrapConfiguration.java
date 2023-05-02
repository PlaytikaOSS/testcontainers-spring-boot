package com.playtika.testcontainer.keycloak;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.keycloak.KeycloakProperties.BEAN_NAME_EMBEDDED_KEYCLOAK;
import static java.util.Objects.requireNonNull;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@EnableConfigurationProperties(KeycloakProperties.class)
@ConditionalOnProperty(name = "embedded.keycloak.enabled", matchIfMissing = true)
public class EmbeddedKeycloakBootstrapConfiguration {

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "keycloak")
    ToxiproxyContainer.ContainerProxy keycloakContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                             @Qualifier(BEAN_NAME_EMBEDDED_KEYCLOAK) KeycloakContainer keycloakContainer,
                                                             KeycloakProperties properties,
                                                             ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(keycloakContainer, keycloakContainer.getHttpPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.keycloak.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.keycloak.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.keycloak.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedKeycloakToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Keycloak ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean
    public KeycloakContainerFactory keycloakContainerFactory(ConfigurableEnvironment environment,
                                                             KeycloakProperties properties,
                                                             ResourceLoader resourceLoader,
                                                             Optional<Network> network) {
        return new KeycloakContainerFactory(environment, properties, resourceLoader, network);
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
        return requireNonNull(factory).newKeycloakContainer();
    }
}
