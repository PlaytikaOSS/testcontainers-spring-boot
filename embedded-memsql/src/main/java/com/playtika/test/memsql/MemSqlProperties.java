package com.playtika.test.memsql;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.memsql")
public class MemSqlProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_MEMSQL = "embeddedMemsql";
    String user = "root";
    String password = "";
    String database = "test_db";
    String schema = "test_db";
    String host = "localhost";
    int port = 3306;

    public MemSqlProperties() {
        this.setCapabilities(Arrays.asList(Capability.NET_ADMIN));
    }

    @Override
    public String getDefaultDockerImage() {
        return "memsql/quickstart:minimal-7.0.5";
    }
}
