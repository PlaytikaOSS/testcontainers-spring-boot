package com.playtika.testcontainer.k3s;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.k3s")
public class K3sProperties extends CommonContainerProperties {

    static final String EMBEDDED_K3S = "embeddedK3s";

    String host = "localhost";
    int port = 6443;


    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "rancher/k3s:v1.31.0-k3s1";
    }
}
