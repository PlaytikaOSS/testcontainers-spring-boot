package com.playtika.test.redis;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(SpringExtension.class)
@ActiveProfiles("enabled")
public abstract class BaseEmbeddedRedisTest {

    @Autowired
    protected ConfigurableListableBeanFactory beanFactory;

    @Autowired
    protected StringRedisTemplate template;

    @Autowired
    protected ConfigurableEnvironment environment;

    @Test
    public void springDataRedisShouldWork() {
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
}
