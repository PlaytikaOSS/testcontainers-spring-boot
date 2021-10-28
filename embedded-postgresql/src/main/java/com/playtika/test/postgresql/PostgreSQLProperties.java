package com.playtika.test.postgresql;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.postgresql")
public class PostgreSQLProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_POSTGRESQL = "embeddedPostgreSql";
    // https://hub.docker.com/_/postgres
    String dockerImage = "postgres:13-alpine";

    String user = "postgresql";
    String password = "letmein";
    String database = "test_db";
    String startupLogCheckRegex;
    /**
     * The SQL file path to execute after the container starts to initialize the database.
     */
    String initScriptPath;
}
