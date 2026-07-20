package com.playtika.testcontainer.memsql;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

@Slf4j
public class MemSqlContainer extends JdbcDatabaseContainer<MemSqlContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME =
            DockerImageName.parse("ghcr.io/singlestore-labs/singlestoredb-dev");

    static final int MEMSQL_PORT = 3306;

    private String database = "test_db";
    private String username = "root";
    private String password = "pass";
    private String licenseKey = "";

    public MemSqlContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
    }

    public MemSqlContainer withDatabaseName(String database) {
        this.database = database;
        return self();
    }

    public MemSqlContainer withUsername(String username) {
        this.username = username;
        return self();
    }

    @Override
    public MemSqlContainer withPassword(String password) {
        this.password = password;
        return self();
    }

    public MemSqlContainer withLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
        return self();
    }

    @Override
    protected void configure() {
        addEnv("IGNORE_MIN_REQUIREMENTS", "1");
        addEnv("SINGLESTORE_SET_GLOBAL_DEFAULT_PARTITIONS_PER_LEAF", "1");
        addEnv("LICENSE_KEY", licenseKey);
        addEnv("SINGLESTORE_LICENSE", licenseKey);
        addEnv("ROOT_PASSWORD", password);
        addEnv("START_AFTER_INIT", "Y");
        addExposedPort(MEMSQL_PORT);
    }

    @Override
    public String getDriverClassName() {
        return "org.mariadb.jdbc.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:mariadb://" + getHost() + ":" + getMappedPort(MEMSQL_PORT) + "/" + database;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getDatabaseName() {
        return database;
    }

    @Override
    protected String getTestQueryString() {
        return "SELECT 1";
    }

    @Override
    protected void waitUntilContainerStarted() {
        // JdbcDatabaseContainer.waitUntilContainerStarted() does not call super, so the
        // waitingFor() strategy is never executed and the JDBC check connects to getJdbcUrl()
        // which includes the database name — failing with "Unknown database" since the DB
        // doesn't exist yet. Connect to the root URL first, create the DB, then hand off.
        String rootUrl = "jdbc:mariadb://" + getHost() + ":" + getMappedPort(MEMSQL_PORT) + "/";
        long startNanos = System.nanoTime();
        long timeoutNanos = TimeUnit.SECONDS.toNanos(getStartupTimeoutSeconds());
        Exception lastException = null;

        while ((System.nanoTime() - startNanos) < timeoutNanos) {
            if (!isRunning()) {
                sleep(500);
                continue;
            }
            try (Connection conn = DriverManager.getConnection(rootUrl, username, password);
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE DATABASE IF NOT EXISTS " + database);
                log.debug("Database '{}' created/verified", database);
                break;
            } catch (Exception e) {
                lastException = e;
                sleep(500);
            }
        }

        if (lastException != null && !databaseExists(rootUrl)) {
            throw new ContainerLaunchException("Could not create database '" + database + "'", lastException);
        }

        super.waitUntilContainerStarted();
    }

    private boolean databaseExists(String rootUrl) {
        try (Connection conn = DriverManager.getConnection(rootUrl, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("USE " + database);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
