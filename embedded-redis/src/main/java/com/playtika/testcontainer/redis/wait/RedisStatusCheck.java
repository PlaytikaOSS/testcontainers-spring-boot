package com.playtika.testcontainer.redis.wait;

import com.playtika.testcontainer.common.checks.AbstractCommandWaitStrategy;
import com.playtika.testcontainer.redis.RedisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RedisStatusCheck extends AbstractCommandWaitStrategy {

    private final RedisProperties properties;

    @Override
    public String[] getCheckCommand() {
        if (properties.isRequirepass()) {
            return new String[]{
                    "redis-cli", "-a", properties.getPassword(), "-p", String.valueOf(properties.getPort()), "ping"
            };
        } else {
            return new String[]{
                    "redis-cli", "-p", String.valueOf(properties.getPort()), "ping"
            };
        }
    }
}
