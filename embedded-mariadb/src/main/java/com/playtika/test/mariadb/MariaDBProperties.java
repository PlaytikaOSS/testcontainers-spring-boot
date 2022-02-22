package com.playtika.test.mariadb;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;


@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.mariadb")
public class MariaDBProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_MARIADB = "embeddedMariaDb";

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

    public MariaDBProperties() {
        this.setCapabilities(Arrays.asList(Capability.NET_ADMIN));
    }

    // https://hub.docker.com/_/mariadb
    @Override
    public String getDefaultDockerImage() {
        return "mariadb:10.6-focal";
    }
}
