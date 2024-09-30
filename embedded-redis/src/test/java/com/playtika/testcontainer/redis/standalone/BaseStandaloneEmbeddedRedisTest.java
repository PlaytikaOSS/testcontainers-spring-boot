package com.playtika.testcontainer.redis.standalone;

import com.playtika.testcontainer.redis.BaseEmbeddedRedisTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static com.playtika.testcontainer.redis.RedisProperties.BEAN_NAME_EMBEDDED_REDIS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = BaseStandaloneEmbeddedRedisTest.TestConfiguration.class
)
public abstract class BaseStandaloneEmbeddedRedisTest extends BaseEmbeddedRedisTest {

    @Test
    public void shouldSetupDependsOnForAllClients() throws Exception {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, RedisConnectionFactory.class);
        assertThat(beanNamesForType)
                .as("RedisConnectionFactory should be present")
                .hasSize(1)
                .contains("redisConnectionFactory");
        asList(beanNamesForType).forEach(this::hasDependsOn);

        beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, RedisTemplate.class);
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

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
