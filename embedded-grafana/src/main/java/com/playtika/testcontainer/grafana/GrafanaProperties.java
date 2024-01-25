package com.playtika.testcontainer.grafana;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.grafana")
public class GrafanaProperties extends CommonContainerProperties {

    static final String GRAFANA_BEAN_NAME = "grafana";

    boolean enabled = true;
    String networkAlias = "grafana";
    String username = "admin";
    String password = "password";
    int port = 3000;

    // https://hub.docker.com/r/grafana/grafana
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "grafana/grafana:10.3.1";
    }
}
