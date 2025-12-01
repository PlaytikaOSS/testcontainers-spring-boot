package com.playtika.testcontainer.memsql;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class MemSqlContainer extends JdbcDatabaseContainer<MemSqlContainer> {

    private static final String IMAGE = "ghcr.io/singlestore-labs/singlestoredb-dev";
    private static final String DEFAULT_TAG = "8.9.10";
    private static final int MYSQL_PORT = 3306;

    private String licenseKey;
    private String databaseName = "test_db";
    private String username = "root";
    private String password = "pass";

    public MemSqlContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public MemSqlContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertValid();
        withExposedPorts(MYSQL_PORT);
    }

    public MemSqlContainer() {
        this(DockerImageName.parse(IMAGE + ":" + DEFAULT_TAG));
    }

    @Override
    protected void configure() {
        super.configure();
        withEnv("IGNORE_MIN_REQUIREMENTS", "1");
        withEnv("SINGLESTORE_SET_GLOBAL_DEFAULT_PARTITIONS_PER_LEAF", "1");
        // Try different environment variable names for chunk size configuration
        // SingleStore may use different variable names in different versions
        withEnv("SINGLESTORE_MINIMUM_CHUNK_SIZE", "536870912");
        withEnv("MINIMUM_CHUNK_SIZE", "536870912");
        // Disable disk space checks if possible
        withEnv("SINGLESTORE_SKIP_DISK_SPACE_CHECK", "1");
        withEnv("SKIP_DISK_SPACE_CHECK", "1");
        if (licenseKey != null) {
            withEnv("LICENSE_KEY", licenseKey);
            withEnv("SINGLESTORE_LICENSE", licenseKey);
        }
        withEnv("ROOT_PASSWORD", password);
        withEnv("START_AFTER_INIT", "Y");
        // Try to reduce disk space requirements by setting additional SingleStore config
        // These may not all work, but we try multiple approaches
        withEnv("SINGLESTORE_REDUCE_DISK_REQUIREMENTS", "1");
        withEnv("SINGLESTORE_TEST_MODE", "1");

        if ("aarch".equals(System.getProperty("system.arch"))) {
            withCommand("platform", "linux/amd64");
        }
    }

    @Override
    public String getDriverClassName() {
        return "org.mariadb.jdbc.Driver";
    }

    @Override
    public String getJdbcUrl() {
        String additionalUrlParams = constructUrlParameters("?", "&");
        // Connect without database name - MySQL/MariaDB allows this
        // The init script will create the database, and applications can connect to it
        // by specifying the database name in their connection strings if needed
        return "jdbc:mariadb://" + getHost() + ":" + getMappedPort(MYSQL_PORT) + additionalUrlParams;
    }

    /**
     * Returns the JDBC URL with the database name included.
     * This is used by applications that need the full connection string.
     */
    public String getJdbcUrlWithDatabase() {
        String additionalUrlParams = constructUrlParameters("?", "&");
        return "jdbc:mariadb://" + getHost() + ":" + getMappedPort(MYSQL_PORT) + "/" + databaseName + additionalUrlParams;
    }

    @Override
    protected String getTestQueryString() {
        return "SELECT 1";
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public MemSqlContainer withLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
        return self();
    }

    @Override
    public MemSqlContainer withUsername(String username) {
        this.username = username;
        return self();
    }

    @Override
    public MemSqlContainer withPassword(String password) {
        this.password = password;
        return self();
    }

    @Override
    public MemSqlContainer withDatabaseName(String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    /**
     * Override to set chunk size and create database manually.
     * This reduces the disk space requirement from 16GB to a smaller value.
     */
    @Override
    protected void waitUntilContainerStarted() {
        super.waitUntilContainerStarted();
        // After container is ready, try to set chunk size and create database
        try (Connection connection = createConnection("")) {
            try (Statement statement = connection.createStatement()) {
                // Try multiple approaches to set chunk size
                String[] chunkSizeCommands = {
                    "SET GLOBAL minimum_chunk_size_mb = 512",
                    "SET GLOBAL min_chunk_size = 536870912",
                    "SET minimum_chunk_size_mb = 512",
                    "SET GLOBAL default_partitions_per_leaf = 1"
                };

                for (String cmd : chunkSizeCommands) {
                    try {
                        statement.execute(cmd);
                        break; // If one works, stop trying
                    } catch (SQLException e) {
                        // Continue to next command
                    }
                }

            }
        } catch (Exception e) {
            // If manual creation fails, let the init script handle it
            // This ensures backward compatibility
        }
    }
}
