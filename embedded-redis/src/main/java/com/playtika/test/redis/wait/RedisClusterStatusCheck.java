package com.playtika.test.redis.wait;

import com.playtika.test.common.checks.AbstractRetryingWaitStrategy;
import com.playtika.test.redis.RedisProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ContainerLaunchException;
import redis.clients.jedis.Jedis;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class RedisClusterStatusCheck extends AbstractRetryingWaitStrategy {

    private final RedisProperties properties;

    @Override
    protected void waitUntilReady() {
        try {
            super.waitUntilReady();
        } catch (ContainerLaunchException e) {
            logClusterInfo();
            throw e;
        }
    }

    @Override
    protected boolean isReady() {
        try (Jedis jedis = createJedis()) {
            String clusterInfo = jedis.clusterInfo();
            return clusterInfo.contains("cluster_state:ok");
        }
    }

    private void logClusterInfo() {
        try (Jedis jedis = createJedis()) {
            String clusterInfo = jedis.clusterInfo();
            String info = jedis.info();
            List<String> config = jedis.configGet("*");
            String clusterNodes = jedis.clusterNodes();
            log.error("Cluster in failed state:\n" +
                            "-- cluster info:\n{}\n" +
                            "-- nodes:\n{}\n" +
                            "-- info:\n{}\n" +
                            "-- config:\n{}",
                            clusterInfo, clusterNodes, info, String.join("\n", config));
        }
    }

    private Jedis createJedis() {
        Jedis jedis = new Jedis(properties.host, properties.port);
        if (properties.requirepass) {
            jedis.auth(properties.password);
        }
        return jedis;
    }
}
