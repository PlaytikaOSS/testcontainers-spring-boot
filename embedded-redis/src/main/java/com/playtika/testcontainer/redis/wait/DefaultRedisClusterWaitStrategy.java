package com.playtika.testcontainer.redis.wait;

import com.playtika.testcontainer.redis.RedisProperties;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

public class DefaultRedisClusterWaitStrategy extends WaitAllStrategy implements RedisClusterWaitStrategy {
    public DefaultRedisClusterWaitStrategy(RedisProperties properties) {
        withStrategy(new RedisStatusCheck(properties))
                .withStrategy(new RedisClusterStatusCheck(properties));
    }
}
