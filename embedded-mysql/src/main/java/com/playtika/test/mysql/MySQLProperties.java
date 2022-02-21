package com.playtika.test.mysql;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.mysql")
public class MySQLProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_MYSQL = "embeddedMySQL";

    String encoding = "utf8mb4";
    String collation = "utf8mb4_unicode_ci";

    String user = "test";
    String password = "test";
    String database = "test";
    String schema = "test_db";
    String host = "localhost";
    int port = 3306;
    /**
     * The SQL file path to execute after the container starts to initialize the database.
     */
    String initScriptPath;

    public MySQLProperties() {
        this.setCapabilities(Arrays.asList(Capability.NET_ADMIN));
    }

    // https://hub.docker.com/_/mysql
    @Override
    public String getDefaultDockerImage() {
        return "mysql:8.0";
    }
}
