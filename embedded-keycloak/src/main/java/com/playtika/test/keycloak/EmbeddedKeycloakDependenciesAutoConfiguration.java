package com.playtika.test.keycloak;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.client.KeycloakClientRequestFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import static com.playtika.test.keycloak.KeycloakProperties.BEAN_NAME_EMBEDDED_KEYCLOAK;

@AutoConfiguration(after = DockerPresenceBootstrapConfiguration.class, before = KeycloakConfiguration.class)
@AutoConfigureOrder
@ConditionalOnClass(KeycloakConfiguration.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.keycloak.enabled", matchIfMissing = true)
public class EmbeddedKeycloakDependenciesAutoConfiguration {

    @Bean
    @ConditionalOnClass(KeycloakClientRequestFactory.class)
    public static BeanFactoryPostProcessor keycloakClientRequestFactoryDependencyPostProcessor() {
        return new DependsOnPostProcessor(KeycloakClientRequestFactory.class,
                new String[]{BEAN_NAME_EMBEDDED_KEYCLOAK});
    }
}
