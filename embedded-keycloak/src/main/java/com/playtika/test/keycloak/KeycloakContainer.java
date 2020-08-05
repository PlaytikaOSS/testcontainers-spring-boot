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
        String db_vendor = properties.getDbVendor();
        if(db_vendor != null) {
          withEnv("DB_VENDOR", db_vendor);
        }
        String db_addr = properties.getDbAddr();
        if(db_addr != null) {
          withEnv("DB_ADDR", db_addr);
        }
        String db_port = properties.getDbPort();
        if(db_port != null) {
          withEnv("DB_PORT", db_port);
        }
        String db_database = properties.getDbDatabase();
        if(db_database != null) {
          withEnv("DB_DATABASE", db_database);
        }
        String db_schema = properties.getDbSchema();
        if(db_schema != null) {
          withEnv("DB_SCHEMA", db_schema);
        }
        String db_user = properties.getDbUser();
        if(db_user != null) {
          withEnv("DB_USER", db_user);
        }
        String db_user_file = properties.getDbUserFile();
        if(db_user_file != null) {
          withEnv("DB_USER_FILE", db_user_file);
        }
        String db_password = properties.getDbPassword();
        if(db_password != null) {
          withEnv("DB_PASSWORD", db_password);
        }
        String db_password_file = properties.getDbPasswordFile();
        if(db_password_file != null) {
          withEnv("DB_PASSWORD_FILE", db_password_file);
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
            .withStartupTimeout(ofSeconds(properties.getWaitTimeoutInSeconds()));
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
