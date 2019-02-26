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
package com.playtika.test.redis;

import com.playtika.test.redis.wait.DefaultRedisClusterWaitStrategy;
import com.playtika.test.redis.wait.RedisClusterWaitStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

import java.util.Map;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.redis.EnvUtils.registerRedisEnvironment;
import static com.playtika.test.redis.RedisProperties.BEAN_NAME_EMBEDDED_REDIS;

@Slf4j
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Conditional(EmbeddedRedisClusterBootstrapConfiguration.EmbeddedRedisClusterCondition.class)
@EnableConfigurationProperties(RedisProperties.class)
public class EmbeddedRedisClusterBootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RedisClusterWaitStrategy redisClusterWaitStrategy(RedisProperties properties) {
        return new DefaultRedisClusterWaitStrategy(properties);
    }

    @Bean(name = BEAN_NAME_EMBEDDED_REDIS, destroyMethod = "stop")
    public GenericContainer redis(ConfigurableEnvironment environment,
                                  RedisProperties properties,
                                  WaitStrategy redisClusterWaitStrategy) {

        log.info("Starting Redis cluster. Docker image: {}", properties.dockerImage);

        // CLUSTER SLOTS command returns IP:port for each node, so ports outside and inside
        // container must be the same
        GenericContainer redis =
                new FixedHostPortGenericContainer(properties.dockerImage)
                        .withFixedExposedPort(properties.port, properties.port)
                        .withLogConsumer(containerLogsConsumer(log))
                        .withEnv("REDIS_USER", properties.getUser())
                        .withEnv("REDIS_PASSWORD", properties.getPassword())
                        .withCopyFileToContainer(MountableFile.forClasspathResource("redis-cluster.conf"), "/data/redis-cluster.conf")
                        .withCopyFileToContainer(MountableFile.forClasspathResource("nodes.conf"), "/data/nodes.conf")
                        .withCommand("redis-server", "/data/redis-cluster.conf", "--requirepass", properties.getPassword())
                        .waitingFor(redisClusterWaitStrategy)
                        .withStartupTimeout(properties.getTimeoutDuration());
        redis.start();
        Map<String, Object> redisEnv = registerRedisEnvironment(environment, redis, properties, properties.port);
        log.info("Started Redis cluster. Connection details: {}", redisEnv);
        return redis;
    }

    static class EmbeddedRedisClusterCondition extends AllNestedConditions {

        public EmbeddedRedisClusterCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(name = "embedded.redis.enabled", matchIfMissing = true)
        static class EmbeddedRedisEnabled {
        }

        @ConditionalOnProperty(name = "embedded.redis.clustered", havingValue = "true")
        static class EmbeddedRedisClustered {
        }
    }
}
