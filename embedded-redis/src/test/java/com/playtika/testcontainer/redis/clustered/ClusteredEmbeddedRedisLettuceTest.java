package com.playtika.testcontainer.redis.clustered;

import com.playtika.testcontainer.redis.BaseEmbeddedRedisTest;
import com.playtika.testcontainer.redis.RedisProperties;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.util.Collections;

import static com.playtika.testcontainer.redis.RedisProperties.BEAN_NAME_EMBEDDED_REDIS;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        classes = ClusteredEmbeddedRedisLettuceTest.TestConfiguration.class,
        properties = {
                "embedded.redis.clustered=true"
        }
)
public class ClusteredEmbeddedRedisLettuceTest extends BaseEmbeddedRedisTest {

    @Test
    public void shouldSetupDependsOnForLettuceConnectionFactory() throws Exception {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, RedisConnectionFactory.class);
        assertThat(beanNamesForType)
                .as("RedisConnectionFactory should be present")
                .hasSize(1)
                .contains("connectionFactory");
        asList(beanNamesForType).forEach(this::hasDependsOn);

        beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, org.springframework.data.redis.core.RedisTemplate.class);
        assertThat(beanNamesForType)
                .as("redisTemplates should be present")
                .hasSize(2)
                .contains("redisTemplate", "stringRedisTemplate");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    @Test
    public void shouldUseLettuceConnectionFactory() {
        RedisConnectionFactory connectionFactory = beanFactory.getBean(RedisConnectionFactory.class);
        assertThat(connectionFactory)
                .as("Should use LettuceConnectionFactory")
                .isInstanceOf(LettuceConnectionFactory.class);
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(BEAN_NAME_EMBEDDED_REDIS);
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

        @Bean
        public RedisConnectionFactory connectionFactory(RedisProperties redisProperties) {
            log.info(format("Connecting to Redis Cluster node with Lettuce: %s:%s", redisProperties.getHost(), redisProperties.getPort()));
            RedisClusterConfiguration redisConfiguration = new RedisClusterConfiguration(
                    Collections.singletonList(format("%s:%s", redisProperties.getHost(), redisProperties.getPort())));
            if (redisProperties.isRequirepass()) {
                redisConfiguration.setPassword(RedisPassword.of(redisProperties.getPassword()));
            }
            return new LettuceConnectionFactory(redisConfiguration);
        }
    }
}
