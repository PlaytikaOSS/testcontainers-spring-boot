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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Collections;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = EmbeddedRedisClusterTest.TestConfiguration.class,
        properties = {
                "embedded.redis.clustered=true"
        }
)
@ActiveProfiles("enabled")
public class EmbeddedRedisClusterTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private StringRedisTemplate template;

    @Autowired
    ConfigurableEnvironment environment;

    @Test
    public void springDataRedisShouldWork() throws Exception {
        ValueOperations<String, String> ops = this.template.opsForValue();
        String key = "spring.boot.redis.test";
        if (!this.template.hasKey(key)) {
            ops.set(key, "foo");
        }
        assertThat(ops.get(key)).isEqualTo("foo");
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.redis.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.redis.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.redis.user")).isNotEmpty();
        assertThat(environment.getProperty("embedded.redis.password")).isNotEmpty();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

        @Bean
        public RedisConnectionFactory connectionFactory(@Value("${embedded.redis.host}") String redisHost,
                                                        @Value("${embedded.redis.port}") int redisPort,
                                                        @Value("${embedded.redis.password}") String redisPassword) {
            log.info(format("Connecting to Redis Cluster node: %s:%s", redisHost, redisPort));
            RedisClusterConfiguration redisConfiguration = new RedisClusterConfiguration(
                    Collections.singletonList(format("%s:%s", redisHost, redisPort)));
            redisConfiguration.setPassword(RedisPassword.of(redisPassword));
            return new JedisConnectionFactory(redisConfiguration);
        }
    }

}
