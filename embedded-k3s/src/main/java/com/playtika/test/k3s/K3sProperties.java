package com.playtika.test.k3s;

import com.playtika.test.common.properties.CommonContainerProperties;
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

    // https://hub.docker.com/r/rancher/k3s
    @Override
    public String getDefaultDockerImage() {
        return "rancher/k3s:v1.24.10-k3s1";
    }
}
