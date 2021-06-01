package com.playtika.test.prometheus;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.prometheus")
public class PrometheusProperties extends CommonContainerProperties {

    static final String PROMETHEUS_BEAN_NAME = "prometheus";

    boolean enabled = true;
    String dockerImage = "prom/prometheus:v2.10.0";
    String networkAlias = "prometheus";
    int port = 9090;
}
