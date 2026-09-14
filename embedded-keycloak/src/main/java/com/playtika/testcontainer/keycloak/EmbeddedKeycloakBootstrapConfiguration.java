package com.playtika.testcontainer.keycloak;

import com.playtika.testcontainer.common.spring.ContainerStartupCoordinator;
import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import eu.rekawek.toxiproxy.ToxiproxyClient;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.containers.Network;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommons;
import static com.playtika.testcontainer.keycloak.KeycloakProperties.BEAN_NAME_EMBEDDED_KEYCLOAK;
import static java.lang.String.format;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@EnableConfigurationProperties(KeycloakProperties.class)
@ConditionalOnProperty(name = "embedded.keycloak.enabled", matchIfMissing = true)
public class EmbeddedKeycloakBootstrapConfiguration {

    private static final String KEYCLOAK_NETWORK_ALIAS = "keycloak.testcontainer.docker";
    private static final int KEYCLOAK_INTERNAL_HTTP_PORT = 8080;

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "keycloak")
    ToxiproxyClientProxy keycloakContainerProxy(ToxiproxyClient toxiproxyClient,
                                                ToxiproxyContainer toxiproxyContainer,
                                                @Qualifier(BEAN_NAME_EMBEDDED_KEYCLOAK) KeycloakContainer keycloakContainer,
                                                KeycloakProperties properties,
                                                ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                keycloakContainer,
                KEYCLOAK_INTERNAL_HTTP_PORT,
                "keycloak");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.keycloak", "embeddedKeycloakToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_KEYCLOAK, destroyMethod = "stop")
    public KeycloakContainer keycloakContainer(ConfigurableEnvironment environment,
                                               KeycloakProperties properties,
                                               ResourceLoader resourceLoader,
                                               Optional<Network> network,
                                               ContainerStartupCoordinator startupCoordinator) {
        KeycloakContainer keycloak = new KeycloakContainer(ContainerUtils.getDockerImageName(properties).toString())
                .withNetworkAliases(KEYCLOAK_NETWORK_ALIAS)
                .withAdminUsername(properties.getAdminUser())
                .withAdminPassword(properties.getAdminPassword());

        applyDbConfig(keycloak, properties);
        applyImportFile(keycloak, properties, resourceLoader);

        network.ifPresent(keycloak::withNetwork);

        KeycloakContainer configuredKeycloak = (KeycloakContainer) configureCommons(keycloak, properties, log);
        startupCoordinator.schedule(() -> {
            ContainerUtils.startAndLogTime(configuredKeycloak, log);
            registerEnvironment(configuredKeycloak, environment, properties);
        });
        return configuredKeycloak;
    }

    private void applyDbConfig(KeycloakContainer keycloak, KeycloakProperties properties) {
        if (properties.getDbVendor() != null) {
            keycloak.withEnv("DB", properties.getDbVendor());
        }
        if (properties.getDbAddr() != null) {
            keycloak.withEnv("DB_URL_HOST", properties.getDbAddr());
        }
        if (properties.getDbPort() != null) {
            keycloak.withEnv("DB_URL_PORT", properties.getDbPort());
        }
        if (properties.getDbDatabase() != null) {
            keycloak.withEnv("DB_URL_DATABASE", properties.getDbDatabase());
        }
        if (properties.getDbSchema() != null) {
            keycloak.withEnv("DB_SCHEMA", properties.getDbSchema());
        }
        if (properties.getDbUser() != null) {
            keycloak.withEnv("DB_USERNAME", properties.getDbUser());
        }
        if (properties.getDbUserFile() != null) {
            keycloak.withEnv("DB_USER_FILE", properties.getDbUserFile());
        }
        if (properties.getDbPassword() != null) {
            keycloak.withEnv("DB_PASSWORD", properties.getDbPassword());
        }
        if (properties.getDbPasswordFile() != null) {
            keycloak.withEnv("DB_PASSWORD_FILE", properties.getDbPasswordFile());
        }
    }

    private void applyImportFile(KeycloakContainer keycloak, KeycloakProperties properties, ResourceLoader resourceLoader) {
        String importFile = properties.getImportFile();
        if (importFile == null) {
            return;
        }
        checkImportFileExists(resourceLoader, importFile);
        keycloak.withRealmImportFile(importFile);
    }

    private void checkImportFileExists(ResourceLoader resourceLoader, String importFile) {
        Resource resource = resourceLoader.getResource("classpath:" + importFile);
        if (!resource.exists()) {
            throw new ImportFileNotFoundException(importFile);
        }
        log.debug("Using import file: {}", resource.getFilename());
    }

    private void registerEnvironment(KeycloakContainer keycloak,
                                     ConfigurableEnvironment environment,
                                     KeycloakProperties properties) {
        String host = keycloak.getHost();
        Integer httpPort = keycloak.getHttpPort();
        String authServerUrl = buildAuthServerUrl(keycloak, properties);

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.keycloak.host", host);
        map.put("embedded.keycloak.http-port", httpPort);
        map.put("embedded.keycloak.auth-server-url", authServerUrl);
        map.put("embedded.keycloak.networkAlias", KEYCLOAK_NETWORK_ALIAS);
        map.put("embedded.keycloak.internalPort", KEYCLOAK_INTERNAL_HTTP_PORT);

        log.info("Started Keycloak server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedKeycloakInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    private String buildAuthServerUrl(KeycloakContainer keycloak, KeycloakProperties properties) {
        return format("http://%s:%d%s", keycloak.getHost(), keycloak.getHttpPort(), properties.getAuthBasePath());
    }

    public static final class ImportFileNotFoundException extends IllegalArgumentException {

        private static final long serialVersionUID = 6350884396691857560L;

        ImportFileNotFoundException(String importFile) {
            super(format(
                    "Classpath resource '%s' defined through 'embedded.keycloak.import-file' does not exist.",
                    importFile));
        }
    }
}
