package com.playtika.test.grafana;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.grafana")
public class GrafanaProperties extends CommonContainerProperties {

    static final String GRAFANA_BEAN_NAME = "grafana";

    boolean enabled = true;
    String dockerImage = "grafana/grafana:7.1.3";
    String networkAlias = "grafana";
    String username = "admin";
    String password = "password";
    int port = 3000;
}
