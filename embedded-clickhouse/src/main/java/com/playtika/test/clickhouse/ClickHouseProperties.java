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
    // https://github.com/ClickHouse/ClickHouse/releases
    // https://hub.docker.com/r/yandex/clickhouse-server
    static final String DEFAULT_DOCKER_IMAGE = "yandex/clickhouse-server";
    static final String DEFAULT_DOCKER_IMAGE_TAG = "21.7-alpine";
    String dockerImage = DEFAULT_DOCKER_IMAGE + ":" + DEFAULT_DOCKER_IMAGE_TAG;
    String host = "localhost";
    int port = 8123;
    boolean asCompatibleImage;
    String user = "default";
    String password = "";
}
