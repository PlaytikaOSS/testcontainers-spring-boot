package com.playtika.test.keycloak;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

@Slf4j
public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

    private static final int DEFAULT_HTTP_PORT_INTERNAL = 8080;
    private static final String AUTH_BASE_PATH = "/auth";

    private final KeycloakProperties properties;
    private final ResourceLoader resourceLoader;

    public KeycloakContainer(KeycloakProperties properties,
        ResourceLoader resourceLoader) {
        super(properties.getDockerImage());
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @Override
    protected void configure() {
        withEnv("KEYCLOAK_HTTP_PORT", String.valueOf(DEFAULT_HTTP_PORT_INTERNAL));
        withEnv("KEYCLOAK_USER", properties.getAdminUser());
        withEnv("KEYCLOAK_PASSWORD", properties.getAdminPassword());
        withDB();
        withCommand(properties.getCommand());
        withStartupTimeout(properties.getTimeoutDuration());
        withLogConsumer(containerLogsConsumer(log));
        withExposedPorts(DEFAULT_HTTP_PORT_INTERNAL);
        waitingFor(authBasePath());
        withImportFile(properties.getImportFile());
    }

    private void withDB() {
        withDBVendor();
        withDBAddr();
        withDBPort();
        withDBDatabase();
        withDBSchema();
        withDBUser();
        withDBUserFile();
        withDBPassword();
        withDBPasswordFile();
    }

    private void withDBVendor() {
        String dbVendor = properties.getDbVendor();
        if(dbVendor != null) {
            withEnv("DB_VENDOR", dbVendor);
        }
    }

    private void withDBAddr() {
        String dbAddr = properties.getDbAddr();
        if(dbAddr != null) {
            withEnv("DB_ADDR", dbAddr);
        }
    }

    private void withDBPort() {
        String dbPort = properties.getDbPort();
        if(dbPort != null) {
            withEnv("DB_PORT", dbPort);
        }
    }

    private void withDBDatabase() {
        String dbDatabase = properties.getDbDatabase();
        if(dbDatabase != null) {
            withEnv("DB_DATABASE", dbDatabase);
        }
    }

    private void withDBSchema() {
        String dbSchema = properties.getDbSchema();
        if(dbSchema != null) {
            withEnv("DB_SCHEMA", dbSchema);
        }
    }

    private void withDBUser() {
        String dbUser = properties.getDbUser();
        if(dbUser != null) {
            withEnv("DB_USER", dbUser);
        }
    }

    private void withDBUserFile() {
        String dbUserFile = properties.getDbUserFile();
        if(dbUserFile != null) {
            withEnv("DB_USER_FILE", dbUserFile);
        }
    }

    private void withDBPassword() {
        String dbPassword = properties.getDbPassword();
        if(dbPassword != null) {
            withEnv("DB_PASSWORD", dbPassword);
        }
    }

    private void withDBPasswordFile() {
        String dbPasswordFile = properties.getDbPasswordFile();
        if(dbPasswordFile != null) {
            withEnv("DB_PASSWORD_FILE", dbPasswordFile);
        }
    }

    private void withImportFile(String importFile) {
        if (importFile == null) {
            return;
        }

        checkExists(importFile);

        String importFileInContainer = "/tmp/" + importFile;
        withCopyFileToContainer(
            MountableFile.forClasspathResource(importFile),
            importFileInContainer
        );
        withEnv("KEYCLOAK_IMPORT", importFileInContainer);
    }

    private void checkExists(String importFile) {
        Resource resource = resourceLoader.getResource("classpath:" + importFile);
        if (resource.exists()) {
            log.debug("Using import file: {}", resource.getFilename());
            return;
        }

        throw new ImportFileNotFoundException(importFile);
    }

    private WaitStrategy authBasePath() {
        return Wait
            .forHttp(AUTH_BASE_PATH)
            .forPort(DEFAULT_HTTP_PORT_INTERNAL)
            .withStartupTimeout(ofSeconds(properties.getWaitTimeoutInSeconds() * 2));
    }

    public String getIp() {
        return getContainerIpAddress();
    }

    public Integer getHttpPort() {
        return getMappedPort(DEFAULT_HTTP_PORT_INTERNAL);
    }

    public String getAuthServerUrl() {
        return format("http://%s:%d%s", getIp(), getHttpPort(), AUTH_BASE_PATH);
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
