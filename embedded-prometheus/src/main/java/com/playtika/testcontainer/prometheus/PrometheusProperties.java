package com.playtika.testcontainer.prometheus;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.prometheus")
public class PrometheusProperties extends CommonContainerProperties {

    static final String PROMETHEUS_BEAN_NAME = "prometheus";

    boolean enabled = true;
    String networkAlias = "prometheus";
    int port = 9090;

    // https://hub.docker.com/r/prom/prometheus
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "prom/prometheus:v2.48.0";
    }
}
