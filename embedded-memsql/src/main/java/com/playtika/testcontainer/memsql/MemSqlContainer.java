package com.playtika.testcontainer.memsql;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

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
}
