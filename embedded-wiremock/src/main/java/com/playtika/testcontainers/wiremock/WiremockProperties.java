package com.playtika.testcontainers.wiremock;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.wiremock")
public class WiremockProperties extends CommonContainerProperties {

    private String host = "localhost";
    private int port = 8990;

    // https://hub.docker.com/r/wiremock/wiremock
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "wiremock/wiremock:2.32.0";
    }
}
