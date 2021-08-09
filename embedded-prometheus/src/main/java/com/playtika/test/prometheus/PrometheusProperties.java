package com.playtika.test.prometheus;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Accessors(chain = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.prometheus")
public class PrometheusProperties extends CommonContainerProperties {

    static final String PROMETHEUS_BEAN_NAME = "prometheus";

    boolean enabled = true;
    // https://hub.docker.com/r/prom/prometheus
    String dockerImage = "prom/prometheus:v2.28.1";
    String networkAlias = "prometheus";
    int port = 9090;
}
