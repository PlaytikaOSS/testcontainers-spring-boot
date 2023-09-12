package com.playtika.testcontainer.memsql;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Data
@Validated
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.memsql")
public class MemSqlProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_MEMSQL = "embeddedMemsql";
    String user = "root";
    String password = "pass";
    String database = "test_db";
    String schema = "test_db";
    String host = "localhost";
    @NotEmpty
    String licenseKey;
    int port = 3306;
    String statusCheck = "source /schema.sql; use test_db; SELECT 1;";

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "ghcr.io/singlestore-labs/singlestoredb-dev:0.2.9";
    }
}
