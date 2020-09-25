package com.playtika.test.pulsar;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.pulsar")
public class PulsarProperties extends CommonContainerProperties {
    public static final String EMBEDDED_PULSAR = "embeddedPulsar";
    public String imageTag = "2.6.1";
}
