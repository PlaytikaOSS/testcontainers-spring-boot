package com.playtika.testcontainer.keydb.clustered;

import com.playtika.testcontainer.keydb.BaseEmbeddedKeyDbTest;
import com.playtika.testcontainer.keydb.KeyDbProperties;
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
  classes = BaseClusterEmbeddedKeyDbTest.TestConfiguration.class,
  properties = {
    "embedded.keydb.clustered=true",
    "embedded.toxiproxy.proxies.keydb.enabled=true"
  }
)
abstract class BaseClusterEmbeddedKeyDbTest extends BaseEmbeddedKeyDbTest {

  @EnableAutoConfiguration
  @Configuration
  static class TestConfiguration {

    @Bean
    public RedisConnectionFactory connectionFactory(KeyDbProperties keyDbProperties) {
      log.info(format("Connecting to KeyDB Cluster node: %s:%s", keyDbProperties.getHost(), keyDbProperties.getPort()));
      RedisClusterConfiguration redisConfiguration = new RedisClusterConfiguration(
        Collections.singletonList(format("%s:%s", keyDbProperties.getHost(), keyDbProperties.getPort())));
      if(keyDbProperties.isRequirepass()) {
        redisConfiguration.setPassword(RedisPassword.of(keyDbProperties.getPassword()));
      }
      return new JedisConnectionFactory(redisConfiguration);
    }
  }

}
