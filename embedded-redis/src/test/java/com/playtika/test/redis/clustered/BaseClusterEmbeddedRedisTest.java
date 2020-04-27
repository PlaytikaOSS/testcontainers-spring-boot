/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Playtika
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
package com.playtika.test.redis.clustered;

import com.playtika.test.redis.BaseEmbeddedRedisTest;
import com.playtika.test.redis.RedisProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.util.Collections;

import static java.lang.String.format;

@Slf4j
@SpringBootTest(
        classes = BaseClusterEmbeddedRedisTest.TestConfiguration.class,
        properties = {
                "embedded.redis.clustered=true"
        }
)
public abstract class BaseClusterEmbeddedRedisTest extends BaseEmbeddedRedisTest {

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

        @Bean
        public RedisConnectionFactory connectionFactory(RedisProperties redisProperties) {
            log.info(format("Connecting to Redis Cluster node: %s:%s", redisProperties.getHost(), redisProperties.getPort()));
            RedisClusterConfiguration redisConfiguration = new RedisClusterConfiguration(
                    Collections.singletonList(format("%s:%s", redisProperties.getHost(), redisProperties.getPort())));
            if(redisProperties.isRequirepass()) {
                redisConfiguration.setPassword(RedisPassword.of(redisProperties.getPassword()));
            }
            return new JedisConnectionFactory(redisConfiguration);
        }
    }
}
