package com.playtika.testcontainer.keycloak;

import com.playtika.testcontainer.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

import static java.lang.String.format;

@Slf4j
public class KeycloakContainer extends GenericContainer<KeycloakContainer> {

    public static final int KEYCLOAK_DEFAULT_HTTP_PORT_INTERNAL = 8080;

    private final KeycloakProperties properties;
    private final ResourceLoader resourceLoader;

    public KeycloakContainer(KeycloakProperties properties,
                             ResourceLoader resourceLoader) {
        super(ContainerUtils.getDockerImageName(properties));
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @Override
    protected void configure() {
        withEnv("HTTP_ENABLED", String.valueOf(true));
        withEnv("HTTP_PORT", String.valueOf(KEYCLOAK_DEFAULT_HTTP_PORT_INTERNAL));
        withExposedPorts(KEYCLOAK_DEFAULT_HTTP_PORT_INTERNAL);
        withEnv("KEYCLOAK_ADMIN", properties.getAdminUser());
        withEnv("KEYCLOAK_ADMIN_PASSWORD", properties.getAdminPassword());
        withDB();
        withCommand(properties.getCommand());
        waitingFor(waitForListeningPort());
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
        if (dbVendor != null) {
            withEnv("DB", dbVendor);
        }
    }

    private void withDBAddr() {
        String dbAddr = properties.getDbAddr();
        if (dbAddr != null) {
            withEnv("DB_URL_HOST", dbAddr);
        }
    }

    private void withDBPort() {
        String dbPort = properties.getDbPort();
        if (dbPort != null) {
            withEnv("DB_URL_PORT", dbPort);
        }
    }

    private void withDBDatabase() {
        String dbDatabase = properties.getDbDatabase();
        if (dbDatabase != null) {
            withEnv("DB_URL_DATABASE", dbDatabase);
        }
    }

    private void withDBSchema() {
        String dbSchema = properties.getDbSchema();
        if (dbSchema != null) {
            withEnv("DB_SCHEMA", dbSchema);
        }
    }

    private void withDBUser() {
        String dbUser = properties.getDbUser();
        if (dbUser != null) {
            withEnv("DB_USERNAME", dbUser);
        }
    }

    private void withDBUserFile() {
        String dbUserFile = properties.getDbUserFile();
        if (dbUserFile != null) {
            withEnv("DB_USER_FILE", dbUserFile);
        }
    }

    private void withDBPassword() {
        String dbPassword = properties.getDbPassword();
        if (dbPassword != null) {
            withEnv("DB_PASSWORD", dbPassword);
        }
    }

    private void withDBPasswordFile() {
        String dbPasswordFile = properties.getDbPasswordFile();
        if (dbPasswordFile != null) {
            withEnv("DB_PASSWORD_FILE", dbPasswordFile);
        }
    }

    private void withImportFile(String importFile) {
        if (importFile == null) {
            return;
        }

        checkExists(importFile);

        String importFileInContainer = "/opt/keycloak/data/import/" + importFile;
        withCopyFileToContainer(
                MountableFile.forClasspathResource(importFile),
                importFileInContainer
        );
    }

    private void checkExists(String importFile) {
        Resource resource = resourceLoader.getResource("classpath:" + importFile);
        if (resource.exists()) {
            log.debug("Using import file: {}", resource.getFilename());
            return;
        }

        throw new ImportFileNotFoundException(importFile);
    }

    private WaitStrategy waitForListeningPort() {
        return Wait
                .forListeningPort()
                .withStartupTimeout(properties.getTimeoutDuration());
    }

    public String getIp() {
        return getContainerIpAddress();
    }

    public Integer getHttpPort() {
        return getMappedPort(KEYCLOAK_DEFAULT_HTTP_PORT_INTERNAL);
    }

    public String getAuthServerUrl() {
        return format("http://%s:%d%s", getIp(), getHttpPort(), properties.getAuthBasePath());
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
