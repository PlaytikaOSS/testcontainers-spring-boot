package com.playtika.testcontainer.keycloak;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.Network;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.keycloak.KeycloakContainer.KEYCLOAK_DEFAULT_HTTP_PORT_INTERNAL;
import static com.playtika.testcontainer.keycloak.KeycloakProperties.BEAN_NAME_EMBEDDED_KEYCLOAK;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@AutoConfigureBefore(name = "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerJwtConfiguration")
@EnableConfigurationProperties(KeycloakProperties.class)
@ConditionalOnProperty(name = "embedded.keycloak.enabled", matchIfMissing = true)
public class EmbeddedKeycloakBootstrapConfiguration {

    private static final String KEYCLOAK_NETWORK_ALIAS = "keycloak.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "keycloak")
    ToxiproxyClientProxy keycloakContainerProxy(ToxiproxyClient toxiproxyClient,
                                                 ToxiproxyContainer toxiproxyContainer,
                                                 @Qualifier(BEAN_NAME_EMBEDDED_KEYCLOAK) KeycloakContainer keycloakContainer) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                keycloakContainer,
                keycloakContainer.getHttpPort(),
                "keycloak");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "keycloak")
    public DynamicPropertyRegistrar keycloakToxiProxyDynamicPropertyRegistrar(@Qualifier("keycloakContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.keycloak");
    }

    /**
     * Creates and starts a {@link KeycloakContainer} if property {@code embedded.keycloak.enabled}
     * evaluates to {@code true}. The configuration makes no difference if just vanilla Keycloak is
     * on the classpath or any Spring adapter. The container will always be needed. Also registers a
     * shutdown hook to stop the container on context shutdown.
     *
     * @return The created {@link KeycloakContainer} instance to be registered as bean
     */
    @Bean(name = BEAN_NAME_EMBEDDED_KEYCLOAK, destroyMethod = "stop")
    public KeycloakContainer keycloak(KeycloakProperties properties, ResourceLoader resourceLoader, Optional<Network> network) {
        KeycloakContainer keycloak = new KeycloakContainer(properties, resourceLoader)
                .withNetworkAliases(KEYCLOAK_NETWORK_ALIAS);
        network.ifPresent(keycloak::withNetwork);
        return (KeycloakContainer) configureCommonsAndStart(keycloak, properties, log);
    }

    @Bean
    public DynamicPropertyRegistrar keycloakDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_KEYCLOAK) KeycloakContainer keycloak,
            KeycloakProperties properties) {
        return registry -> {
            registry.add("embedded.keycloak.host", keycloak::getHost);
            registry.add("embedded.keycloak.http-port", keycloak::getHttpPort);
            registry.add("embedded.keycloak.auth-server-url", keycloak::getAuthServerUrl);
            registry.add("embedded.keycloak.port", () -> keycloak.getMappedPort(KEYCLOAK_DEFAULT_HTTP_PORT_INTERNAL));
            registry.add("embedded.keycloak.networkAlias", () -> KEYCLOAK_NETWORK_ALIAS);
            registry.add("embedded.keycloak.internalPort", () -> KEYCLOAK_DEFAULT_HTTP_PORT_INTERNAL);
        };
    }
}
