package com.playtika.test.oracle;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.oracle")
public class OracleProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_ORACLE = "embeddedOracle";
    static final int ORACLE_PORT = 1521;
    static final String ORACLE_DB = "xepdb1";

    String user = "test";
    String password = "test";
    String database = ORACLE_DB;
    /**
     * The SQL file path to execute after the container starts to initialize the database.
     */
    String initScriptPath;

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "gvenzl/oracle-xe:18.4.0-slim";
    }
}
