package com.playtika.testcontainer.toxiproxy;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.toxiproxy")
public class ToxiProxyProperties extends CommonContainerProperties {

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "ghcr.io/shopify/toxiproxy:2.6.0";
    }
}
