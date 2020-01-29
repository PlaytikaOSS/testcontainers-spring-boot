package com.playtika.test.keycloak;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

@Slf4j
public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

    private static final int DEFAULT_HTTP_PORT_INTERNAL = 8080;
    private static final String AUTH_BASE_PATH = "/auth";

    private final KeycloakProperties properties;

    public KeycloakContainer(KeycloakProperties properties) {
        super(properties.getDockerImage());
        this.properties = properties;
    }

    @Override
    protected void configure() {
        withEnv("KEYCLOAK_HTTP_PORT", String.valueOf(DEFAULT_HTTP_PORT_INTERNAL));
        withEnv("KEYCLOAK_USER", properties.getAdminUser());
        withEnv("KEYCLOAK_PASSWORD", properties.getAdminPassword());
        withCommand(properties.getCommand());
        withStartupTimeout(properties.getTimeoutDuration());
        withLogConsumer(containerLogsConsumer(log));
        withExposedPorts(DEFAULT_HTTP_PORT_INTERNAL);
        waitingFor(authBasePath());

        String importFile = properties.getImportFile();
        if (importFile != null) {
            String importFileInContainer = "/tmp/" + importFile;
            withCopyFileToContainer(
                MountableFile.forClasspathResource(importFile),
                importFileInContainer
            );
            withEnv("KEYCLOAK_IMPORT", importFileInContainer);
        }
    }

    private WaitStrategy authBasePath() {
        return Wait
            .forHttp(AUTH_BASE_PATH)
            .forPort(DEFAULT_HTTP_PORT_INTERNAL)
            .withStartupTimeout(ofSeconds(properties.getWaitTimeoutInSeconds()));
    }

    String getIp() {
        return getContainerIpAddress();
    }

    Integer getHttpPort() {
        return getMappedPort(DEFAULT_HTTP_PORT_INTERNAL);
    }

    String getAuthServerUrl() {
        return format("http://%s:%d%s", getIp(), getHttpPort(), AUTH_BASE_PATH);
    }
}
