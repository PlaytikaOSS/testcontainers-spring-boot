package com.playtika.testcontainer.keydb;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(classes = BaseEmbeddedKeyDbTest.TestConfiguration.class)
@ActiveProfiles("enabled")
public abstract class BaseEmbeddedKeyDbTest {

  @Autowired
  protected ConfigurableListableBeanFactory beanFactory;

  @Autowired
  protected StringRedisTemplate template;

  @Value("${embedded.keydb.port}")
  protected String keydbPort;

  @Value("${embedded.keydb.host}")
  protected String keydbHost;

  @Value("${embedded.keydb.user}")
  protected String keydbUser;

  @Value("${embedded.keydb.password}")
  protected String keydbPassword;

  @Test
  void springDataRedisShouldWork() {
    ValueOperations<String, String> ops = this.template.opsForValue();
    String key = "spring.boot.redis.test";
    if (Boolean.FALSE.equals(this.template.hasKey(key))) {
      ops.set(key, "foo");
    }
    assertThat(ops.get(key)).isEqualTo("foo");
  }

  @Test
  void propertiesAreAvailable() {
    assertThat(keydbPort).isNotEmpty();
    assertThat(keydbHost).isNotEmpty();
    assertThat(keydbUser).isNotEmpty();
    assertThat(keydbPassword).isNotEmpty();
  }

  @EnableAutoConfiguration
  @Configuration
  static class TestConfiguration {
  }

}
