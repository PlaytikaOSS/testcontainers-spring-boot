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
package com.playtika.test.redis;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import redis.clients.jedis.Jedis;

import static com.playtika.test.redis.RedisProperties.BEAN_NAME_EMBEDDED_REDIS;

@Slf4j
@Configuration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.redis.enabled", matchIfMissing = true)
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
public class EmbeddedRedisDependenciesAutoConfiguration {

    @Configuration
    @ConditionalOnClass(RedisConnectionFactory.class)
    public static class RedisConnectionFactoryDependencyContext {
        @Bean
        public static BeanFactoryPostProcessor redisConnectionFactoryDependencyPostProcessor() {
            return new DependsOnPostProcessor(RedisConnectionFactory.class, new String[]{BEAN_NAME_EMBEDDED_REDIS});
        }
    }

    @Configuration
    @ConditionalOnClass(RedisTemplate.class)
    public static class RedisTemplateDependencyContext {
        @Bean
        public static BeanFactoryPostProcessor redisTemplateDependencyPostProcessor() {
            return new DependsOnPostProcessor(RedisTemplate.class, new String[]{BEAN_NAME_EMBEDDED_REDIS});
        }
    }

    @Configuration
    @ConditionalOnClass(Jedis.class)
    public static class JedisDependencyContext {
        @Bean
        public static BeanFactoryPostProcessor jedisDependencyPostProcessor() {
            return new DependsOnPostProcessor(Jedis.class, new String[]{BEAN_NAME_EMBEDDED_REDIS});
        }
    }
}
