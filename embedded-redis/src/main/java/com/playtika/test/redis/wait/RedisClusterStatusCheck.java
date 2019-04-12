/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
        try (Jedis jedis = new Jedis(properties.host, properties.port)) {
            jedis.auth(properties.password);
            String clusterInfo = jedis.clusterInfo();
            return clusterInfo.contains("cluster_state:ok");
        }
    }

    private void logClusterInfo() {
        try (Jedis jedis = new Jedis(properties.host, properties.port)) {
            jedis.auth(properties.password);
            String clusterInfo = jedis.clusterInfo();
            String info = jedis.info();
            List<String> config = jedis.configGet("*");
            String clusterNodes = jedis.clusterNodes();
            log.error("Cluster in failed state:\n" +
                            "-- cluster info:\n{}\n" +
                            "-- nodes:\n{}\n",
                            "-- info:\n{}\n" +
                            "-- config:\n{}" +
                            clusterInfo, clusterNodes, info, String.join("\n", config));
        }
    }
}
