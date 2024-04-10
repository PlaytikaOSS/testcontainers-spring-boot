package com.playtika.testcontainer.pulsar;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.pulsar")
public class PulsarProperties extends CommonContainerProperties {
    public static final String EMBEDDED_PULSAR = "embeddedPulsar";

    int brokerPort = 6650;

    // https://hub.docker.com/r/apachepulsar/pulsar
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "apachepulsar/pulsar:3.2.2";
    }
}
