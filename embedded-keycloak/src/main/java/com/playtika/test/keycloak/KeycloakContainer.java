package com.playtika.test.keycloak;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

@Slf4j
public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

    public static final String KEYCLOAK_USER = "admin";
    public static final String KEYCLOAK_PASSWORD = "letmein";
    public static final int HTTP = 8080;

    private static final String AUTH_BASE_PATH = "/auth";

    private final KeycloakProperties properties;

    public KeycloakContainer(KeycloakProperties properties) {
        super(properties.getDockerImage());
        this.properties = properties;
    }

    @Override
    protected void configure() {
        withEnv("KEYCLOAK_USER", KEYCLOAK_USER);
        withEnv("KEYCLOAK_PASSWORD", KEYCLOAK_PASSWORD);

        withCommand(
            "-c standalone.xml",
            "-Dkeycloak.profile.feature.upload_scripts=enabled"
        );

        withStartupTimeout(properties.getTimeoutDuration());
        withLogConsumer(containerLogsConsumer(log));

        withExposedPorts(HTTP);

        setWaitStrategy(Wait
            .forHttp(AUTH_BASE_PATH)
            .forPort(HTTP)
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
