package com.playtika.test.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("embedded.containers")
public class TestcontainersProperties {

    boolean forceShutdown = false;
}
