package com.playtika.test.keycloak;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.keycloak.KeycloakProperties.DEFAULT_COMMAND;
import static com.playtika.test.keycloak.KeycloakProperties.DEFAULT_HTTP_PORT;
import static com.playtika.test.keycloak.KeycloakProperties.DEFAULT_ADMIN_PASSWORD;
import static com.playtika.test.keycloak.KeycloakProperties.DEFAULT_ADMIN_USER;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

@Slf4j
public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

    private static final String AUTH_BASE_PATH = "/auth";

    private final KeycloakProperties properties;

    public KeycloakContainer(KeycloakProperties properties) {
        super(properties.getDockerImage());
        this.properties = properties;
    }

    @Override
    protected void configure() {
        withEnv("KEYCLOAK_HTTP_PORT", String.valueOf(properties.getHttpPort()));
        withEnv("KEYCLOAK_USER", properties.getAdminUser());
        withEnv("KEYCLOAK_PASSWORD", properties.getAdminPassword());

        withCommand(
            properties.getCommand()
        );

        withStartupTimeout(properties.getTimeoutDuration());
        withLogConsumer(containerLogsConsumer(log));

        withExposedPorts(properties.getHttpPort());

        setWaitStrategy(Wait
            .forHttp(AUTH_BASE_PATH)
            .forPort(DEFAULT_HTTP_PORT)
            .withStartupTimeout(ofSeconds(properties.getWaitTimeoutInSeconds()))
        );

        String importFile = properties.getImportFile();
        if (importFile != null) {
            String importFileInContainer = "/tmp/" + importFile;
            withCopyFileToContainer(MountableFile.forClasspathResource(importFile),
                importFileInContainer);
            withEnv("KEYCLOAK_IMPORT", importFileInContainer);
        }
    }

    String getIp() {
        return getContainerIpAddress();
    }

    Integer getHttpPort() {
        return getMappedPort(8080);
    }

    String getAuthServerUrl() {
        return format("http://%s:%d%s", getIp(), getHttpPort(), AUTH_BASE_PATH);
    }
}
