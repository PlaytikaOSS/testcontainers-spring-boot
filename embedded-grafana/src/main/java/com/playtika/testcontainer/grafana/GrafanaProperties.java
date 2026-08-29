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
    int lokiPort = 3100;
    int tempoPort = 3200;
    int otlpGrpcPort = 4317;
    int otlpHttpPort = 4318;
    boolean anonymousAuthEnabled = false;
    String anonymousOrgRole = "Viewer";

    // https://hub.docker.com/r/grafana/otel-lgtm
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "grafana/otel-lgtm:0.32.0";
    }
}
