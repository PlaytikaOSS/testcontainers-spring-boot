package com.playtika.test.kafka.checks;

import com.playtika.test.common.checks.AbstractCommandWaitStrategy;
import com.playtika.test.kafka.properties.KafkaConfigurationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KafkaStatusCheck extends AbstractCommandWaitStrategy {

    private static final String MIN_BROKERS_COUNT = "1";
    private static final String TIMEOUT_IN_SEC = "30";

    private final KafkaConfigurationProperties properties;

    @Override
    public String[] getCheckCommand() {
        return new String[] {
                "cub",
                "kafka-ready",
                MIN_BROKERS_COUNT,
                TIMEOUT_IN_SEC,
                "-b",
                String.format("localhost:%d", this.properties.getContainerBrokerPort())
        };
    }

}