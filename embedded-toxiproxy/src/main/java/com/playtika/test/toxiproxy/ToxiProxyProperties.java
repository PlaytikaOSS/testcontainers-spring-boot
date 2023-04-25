package com.playtika.test.toxiproxy;

import com.playtika.test.common.properties.CommonContainerProperties;
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
        return "ghcr.io/shopify/toxiproxy:2.5.0";
    }
}
