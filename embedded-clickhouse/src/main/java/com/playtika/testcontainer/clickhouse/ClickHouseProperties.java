package com.playtika.testcontainer.clickhouse;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.clickhouse")
public class ClickHouseProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_CLICK_HOUSE = "embeddedClickHouse";

    String host = "localhost";
    int port = 8123;
    String user = "default";
    String password = "";

    String initScriptPath;

    // https://github.com/ClickHouse/ClickHouse/releases
    // https://hub.docker.com/r/clickhouse/clickhouse-server/tags
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "clickhouse/clickhouse-server:23.7.3";
    }
}
