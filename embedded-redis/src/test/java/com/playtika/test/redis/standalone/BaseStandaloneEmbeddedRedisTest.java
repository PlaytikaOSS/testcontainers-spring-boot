package com.playtika.test.redis.standalone;

import com.playtika.test.common.operations.NetworkTestOperations;
import com.playtika.test.redis.BaseEmbeddedRedisTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.Callable;

import static com.playtika.test.redis.RedisProperties.BEAN_NAME_EMBEDDED_REDIS;
import static java.time.Duration.ofMillis;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = BaseStandaloneEmbeddedRedisTest.TestConfiguration.class,
        properties = "embedded.redis.install.enabled=true"
)
public abstract class BaseStandaloneEmbeddedRedisTest extends BaseEmbeddedRedisTest {

    @Autowired
    NetworkTestOperations redisNetworkTestOperations;

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

    private static long durationOf(Callable<?> op) throws Exception {
        long start = System.currentTimeMillis();
        op.call();
        return System.currentTimeMillis() - start;
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
