/*
* The MIT License (MIT)
*
* Copyright (c) 2018 Playtika
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
import org.testcontainers.containers.GenericContainer;

import java.util.Map;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.redis.EnvUtils.registerRedisEnvironment;
import static com.playtika.test.redis.RedisProperties.BEAN_NAME_EMBEDDED_REDIS;

@Slf4j
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Conditional(EmbeddedRedisBootstrapConfiguration.EmbeddedRedisCondition.class)
@EnableConfigurationProperties(RedisProperties.class)
public class EmbeddedRedisBootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RedisStatusCheck redisStartupCheckStrategy() {
        return new RedisStatusCheck();
    }

    @Bean(name = BEAN_NAME_EMBEDDED_REDIS, destroyMethod = "stop")
    public GenericContainer redis(ConfigurableEnvironment environment,
                                  RedisProperties properties,
                                  RedisStatusCheck redisStatusCheck) {

        log.info("Starting Redis server. Docker image: {}", properties.dockerImage);

        GenericContainer redis =
                new GenericContainer(properties.dockerImage)
                        .withLogConsumer(containerLogsConsumer(log))
                        .withExposedPorts(properties.port)
                        .withEnv("REDIS_USER", properties.getUser())
                        .withEnv("REDIS_PASSWORD", properties.getPassword())
                        .withCommand("redis-server", "--requirepass", properties.getPassword())
                        .waitingFor(redisStatusCheck)
                        .withStartupTimeout(properties.getTimeoutDuration());
        redis.start();
        Map<String, Object> redisEnv = registerRedisEnvironment(environment, redis, properties, redis.getMappedPort(properties.port));
        log.info("Started Redis server. Connection details: {}", redisEnv);
        return redis;
    }

    static class EmbeddedRedisCondition extends AllNestedConditions {

        public EmbeddedRedisCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(name = "embedded.redis.enabled", matchIfMissing = true)
        static class EmbeddedRedisEnabled {
        }

        @ConditionalOnProperty(name = "embedded.redis.clustered", havingValue = "false", matchIfMissing = true)
        static class EmbeddedRedisNotClustered {
        }
    }
}
