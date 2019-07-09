package com.testcontainers.redis.wait;

import com.testcontainers.redis.RedisProperties;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

public class DefaultRedisClusterWaitStrategy extends WaitAllStrategy implements RedisClusterWaitStrategy {
    public DefaultRedisClusterWaitStrategy(RedisProperties properties) {
        withStrategy(new RedisStatusCheck())
                .withStrategy(new RedisClusterStatusCheck(properties));
    }
}
