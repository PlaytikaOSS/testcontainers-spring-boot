package com.playtika.testcontainer.artifactory;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.artifactory.postgresql")
public class PostgreSQLProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_POSTGRESQL = "embeddedPostgreSql";

    String user = "artifactory";
    String password = "artifactory";
    String database = "artifactory";
    String startupLogCheckRegex;
    /**
     * The SQL file path to execute after the container starts to initialize the database.
     */
    String initScriptPath;

    // https://hub.docker.com/_/postgres
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "postgres:17-alpine";
    }
}
