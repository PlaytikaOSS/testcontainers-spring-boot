package com.playtika.testcontainer.redis.clustered;

import com.playtika.testcontainer.redis.BaseEmbeddedRedisTest;
import com.playtika.testcontainer.redis.RedisProperties;
import lombok.extern.slf4j.Slf4j;
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
