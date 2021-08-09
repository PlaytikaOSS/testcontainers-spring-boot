package com.playtika.test.consul;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Accessors(chain = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.consul")
public class ConsulProperties extends CommonContainerProperties {
    public static final String BEAN_NAME_EMBEDDED_CONSUL = "embeddedConsul";
    // https://hub.docker.com/_/consul
    private String dockerImage = "consul:1.10";
    private int port = 8500;
    private String configurationFile = null;
}
