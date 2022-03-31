package com.playtika.test.toxiproxy;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.toxiproxy")
public class ToxiProxyProperties extends CommonContainerProperties {

    public ToxiProxyProperties() {
        //we override here the default one that is used by org.testcontainers:toxiproxy module
        this.setDockerImage("ghcr.io/shopify/toxiproxy:2.4.0");
    }

    @Override
    public String getDefaultDockerImage() {
        return "shopify/toxiproxy";
    }
}
