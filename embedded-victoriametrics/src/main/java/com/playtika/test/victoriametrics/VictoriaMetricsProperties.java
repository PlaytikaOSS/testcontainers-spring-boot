package com.playtika.test.victoriametrics;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.victoriametrics")
public class VictoriaMetricsProperties extends CommonContainerProperties {

    static final String VICTORIA_METRICS_BEAN_NAME = "victoriametrics";

    boolean enabled = true;
    String networkAlias = "victoriametrics";
    int port = 8428;

    @Override
    public String getDefaultDockerImage() {
        return "victoriametrics/victoria-metrics:v1.89.1";
    }
}
