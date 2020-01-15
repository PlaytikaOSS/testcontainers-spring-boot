package com.playtika.test.redis.wait;

import com.playtika.test.redis.RedisProperties;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

public class DefaultRedisClusterWaitStrategy extends WaitAllStrategy implements RedisClusterWaitStrategy {
    public DefaultRedisClusterWaitStrategy(RedisProperties properties) {
        withStrategy(new RedisStatusCheck(properties))
                .withStrategy(new RedisClusterStatusCheck(properties));
    }
}
