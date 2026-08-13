package com.playtika.testcontainer.kafka.checks;

import com.playtika.testcontainer.common.checks.AbstractCommandWaitStrategy;
import com.playtika.testcontainer.kafka.properties.KafkaConfigurationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class KafkaStatusCheck extends AbstractCommandWaitStrategy {

    private final KafkaConfigurationProperties properties;

    @Override
    public String[] getCheckCommand() {
        // cp-kafka:8.x dropped the `cub` (Confluent Utility Belt) tool that older images shipped;
        // kafka-broker-api-versions is the CLI still present that fails fast while the broker isn't reachable yet.
        return new String[] {
                "kafka-broker-api-versions",
                "--bootstrap-server",
                String.format("localhost:%d", 9092)
        };
    }

}