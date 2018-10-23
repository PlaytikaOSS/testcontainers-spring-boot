package com.playtika.test.common.properties;

import lombok.Data;

import java.time.Duration;

@Data
public class CommonContainerProperties {

    private long waitTimeoutInSeconds = 60;
    private boolean enabled = true;

    public Duration getTimeoutDuration() {
        return Duration.ofSeconds(waitTimeoutInSeconds);
    }
}
