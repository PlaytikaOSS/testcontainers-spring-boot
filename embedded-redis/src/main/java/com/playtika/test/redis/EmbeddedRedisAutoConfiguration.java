/*
* The MIT License (MIT)
*
* Copyright (c) 2017 Playtika
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

import com.playtika.test.common.spring.DependsOnPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import redis.clients.jedis.Jedis;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.redis.RedisProperties.BEAN_NAME_EMBEDDED_REDIS;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Slf4j
@Configuration
@Order(HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "embedded.redis.enabled", matchIfMissing = true)
@EnableConfigurationProperties(RedisProperties.class)
public class EmbeddedRedisAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    RedisStatusCheck redisStartupCheckStrategy(RedisProperties properties) {
        return new RedisStatusCheck();
    }

    @Bean(name = BEAN_NAME_EMBEDDED_REDIS, destroyMethod = "stop")
    public GenericContainer redis(ConfigurableEnvironment environment,
                                  RedisProperties properties,
                                  RedisStatusCheck redisStatusCheck) throws Exception {

        log.info("Starting redis server. Docker image: {}", properties.dockerImage);

        GenericContainer redis =
                new GenericContainer(properties.dockerImage)
                        .withStartupCheckStrategy(redisStatusCheck)
                        .withLogConsumer(containerLogsConsumer(log))
                        .withExposedPorts(properties.port)
                        .withEnv("REDIS_USER", properties.getUser())
                        .withEnv("REDIS_PASSWORD", properties.getPassword())
                        .withCommand("redis-server", "--requirepass", properties.getPassword())
                        .withClasspathResourceMapping(
                                "redis-health.sh",
                                "/redis-health.sh",
                                BindMode.READ_ONLY
                        );
        redis.start();
        registerRedisEnvironment(redis, environment, properties);
        return redis;
    }

    private void registerRedisEnvironment(GenericContainer redis,
                                          ConfigurableEnvironment environment,
                                          RedisProperties properties) {
        Integer mappedPort = redis.getMappedPort(properties.port);
        String host = redis.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.redis.port", mappedPort);
        map.put("embedded.redis.host", host);
        map.put("embedded.redis.password", properties.getPassword());
        map.put("embedded.redis.user", properties.getUser());
        MapPropertySource propertySource = new MapPropertySource("embeddedRedisInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    @Configuration
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnBean(RedisConnectionFactory.class)
    public static class EmbeddedRedisConnectionFactoryDependencyContext {
        @Bean
        public BeanFactoryPostProcessor redisConnectionFactoryDependencyPostProcessor() {
            return new DependsOnPostProcessor(RedisConnectionFactory.class, new String[]{BEAN_NAME_EMBEDDED_REDIS});
        }
    }

    @Configuration
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnBean(RedisTemplate.class)
    public static class EmbeddedRedisTemplateDependencyContext {
        @Bean
        public BeanFactoryPostProcessor redisTemplateDependencyPostProcessor() {
            return new DependsOnPostProcessor(RedisTemplate.class, new String[]{BEAN_NAME_EMBEDDED_REDIS});
        }
    }

    @Configuration
    @ConditionalOnClass({Jedis.class})
    @ConditionalOnBean(Jedis.class)
    public static class EmbeddedJedisDependencyContext {
        @Bean
        public BeanFactoryPostProcessor jedisDependencyPostProcessor() {
            return new DependsOnPostProcessor(Jedis.class, new String[]{BEAN_NAME_EMBEDDED_REDIS});
        }
    }
}
