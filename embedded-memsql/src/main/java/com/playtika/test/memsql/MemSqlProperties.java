package com.playtika.test.memsql;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotEmpty;

import java.util.Arrays;

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

    public MemSqlProperties() {
        this.setCapabilities(Arrays.asList(Capability.NET_ADMIN));
    }

    @Override
    public String getDefaultDockerImage() {
        return "singlestore/cluster-in-a-box:alma-7.8.7-b1ce5aefd0-4.0.7-1.13.8";
    }
}
