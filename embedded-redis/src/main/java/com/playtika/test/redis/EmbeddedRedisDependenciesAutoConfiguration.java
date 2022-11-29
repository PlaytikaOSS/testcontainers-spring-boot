package com.playtika.test.redis;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
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
@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.redis.enabled", matchIfMissing = true)
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
