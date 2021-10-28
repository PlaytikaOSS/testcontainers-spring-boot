package com.playtika.test.kafka.checks;

import com.playtika.test.common.checks.AbstractCommandWaitStrategy;
import com.playtika.test.kafka.properties.ZookeeperConfigurationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ZookeeperStatusCheck extends AbstractCommandWaitStrategy {

    private static final String TIMEOUT_IN_SEC = "30";

    private final ZookeeperConfigurationProperties properties;

    @Override
    public String[] getCheckCommand() {
        return new String[]{
                "cub",
                "zk-ready",
                properties.getZookeeperConnect(),
                TIMEOUT_IN_SEC
        };
    }

}