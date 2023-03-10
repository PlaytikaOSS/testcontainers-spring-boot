package com.playtika.test.clickhouse;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.clickhouse")
public class ClickHouseProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_CLICK_HOUSE = "embeddedClickHouse";

    static final String DEFAULT_DOCKER_IMAGE = "yandex/clickhouse-server";
    static final String DEFAULT_DOCKER_IMAGE_TAG = "21.7-alpine";
    String host = "localhost";
    int port = 8123;
    String user = "default";
    String password = "";

    String initScriptPath;

    // https://github.com/ClickHouse/ClickHouse/releases
    // https://hub.docker.com/r/yandex/clickhouse-server
    @Override
    public String getDefaultDockerImage() {
        return DEFAULT_DOCKER_IMAGE + ":" + DEFAULT_DOCKER_IMAGE_TAG;
    }
}
