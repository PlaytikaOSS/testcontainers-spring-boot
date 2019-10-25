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

import com.playtika.test.common.operations.NetworkTestOperations;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.Callable;

import static com.playtika.test.redis.RedisProperties.BEAN_NAME_EMBEDDED_REDIS;
import static java.time.Duration.ofMillis;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmbeddedRedisBootstrapConfigurationTest.TestConfiguration.class)
@ActiveProfiles("enabled")
public class EmbeddedRedisBootstrapConfigurationTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private StringRedisTemplate template;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }

    @Autowired
    ConfigurableEnvironment environment;

    @Autowired
    NetworkTestOperations redisNetworkTestOperations;

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
    public void shouldEmulateLatency() throws Exception {
        ValueOperations<String, String> ops = template.opsForValue();

        redisNetworkTestOperations.withNetworkLatency(ofMillis(1000),
                () -> assertThat(durationOf(() -> ops.get("any")))
                        .isGreaterThan(1000L)
        );

        assertThat(durationOf(() -> ops.get("any")))
                .isLessThan(100L);
    }

    @Test
    public void shouldSetupDependsOnForAllClients() throws Exception {
        String[] beanNamesForType = beanFactory.getBeanNamesForType(RedisConnectionFactory.class);
        assertThat(beanNamesForType)
                .as("RedisConnectionFactory should be present")
                .hasSize(1)
                .contains("redisConnectionFactory");
        asList(beanNamesForType).forEach(this::hasDependsOn);

        beanNamesForType = beanFactory.getBeanNamesForType(RedisTemplate.class);
        assertThat(beanNamesForType)
                .as("redisTemplates should be present")
                .hasSize(2)
                .contains("redisTemplate", "stringRedisTemplate");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(BEAN_NAME_EMBEDDED_REDIS);
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.redis.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.redis.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.redis.user")).isNotEmpty();
        assertThat(environment.getProperty("embedded.redis.password")).isNotEmpty();
    }

    private static long durationOf(Callable<?> op) throws Exception {
        long start = System.currentTimeMillis();
        op.call();
        return System.currentTimeMillis() - start;
    }
}
