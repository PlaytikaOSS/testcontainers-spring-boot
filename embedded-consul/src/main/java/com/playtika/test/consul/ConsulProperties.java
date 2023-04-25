package com.playtika.test.consul;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.consul")
public class ConsulProperties extends CommonContainerProperties {
    public static final String BEAN_NAME_EMBEDDED_CONSUL = "embeddedConsul";

    private int port = 8500;
    private String configurationFile = null;

    // https://hub.docker.com/_/consul
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "consul:1.10";
    }
}
