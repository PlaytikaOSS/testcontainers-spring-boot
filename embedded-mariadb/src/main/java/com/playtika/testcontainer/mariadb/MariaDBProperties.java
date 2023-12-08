package com.playtika.testcontainer.mariadb;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;



@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.mariadb")
public class MariaDBProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_MARIADB = "embeddedMariaDb";
    static final String BEAN_NAME_MARIADB_PACKAGE_PROPERTIES= "mariadbPackageProperties";

    String encoding = "utf8mb4";
    String collation = "utf8mb4_unicode_ci";

    String user = "test";
    String password = "test";
    String database = "test";
    String schema = "test_db";
    String host = "localhost";
    int port = 3306;
    long maxAllowedPacket = 16777216;
    /**
     * The SQL file path to execute after the container starts to initialize the database.
     */
    String initScriptPath;

    // https://hub.docker.com/_/mariadb
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "mariadb:10.8-focal";
    }
}
