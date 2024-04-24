package com.playtika.testcontainer.toxiproxy;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.toxiproxy")
public class ToxiProxyProperties extends CommonContainerProperties {

    public ToxiProxyProperties() {
        //we override here the default one that is used by org.testcontainers:toxiproxy module
        this.setDockerImage("ghcr.io/shopify/toxiproxy:2.7.0");
    }

    @Override
    public String getDefaultDockerImage() {
        return "shopify/toxiproxy";
    }
}
