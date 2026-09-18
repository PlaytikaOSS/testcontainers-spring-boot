package com.playtika.testcontainer.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("embedded.containers")
public class TestcontainersProperties {

    boolean forceShutdown = false;

    /**
     * When enabled, containers of migrated modules are started concurrently instead of one after
     * another, reducing overall context startup time when several containers are used together.
     */
    boolean parallelStartup = false;
}
