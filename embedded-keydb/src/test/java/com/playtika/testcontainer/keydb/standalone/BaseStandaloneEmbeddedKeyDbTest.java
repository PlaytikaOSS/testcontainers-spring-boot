package com.playtika.testcontainer.keydb.standalone;

import com.playtika.testcontainer.keydb.BaseEmbeddedKeyDbTest;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.concurrent.Callable;

import static com.playtika.testcontainer.keydb.KeyDbProperties.BEAN_NAME_EMBEDDED_KEYDB;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
  classes = BaseStandaloneEmbeddedKeyDbTest.TestConfiguration.class,
  properties = {
    "embedded.toxiproxy.proxies.keydb.enabled=true"
  }
)
abstract class BaseStandaloneEmbeddedKeyDbTest extends BaseEmbeddedKeyDbTest {

  @Autowired
  ToxiproxyContainer.ContainerProxy keyDbContainerProxy;

  @Test
  public void shouldEmulateLatency() throws Exception {
    ValueOperations<String, String> ops = template.opsForValue();

    assertThat(durationOf(() -> ops.get("any")))
      .isLessThan(100L);

    keyDbContainerProxy.toxics().latency("latency", ToxicDirection.UPSTREAM, 1000);

    assertThat(durationOf(() -> ops.get("any")))
      .isGreaterThanOrEqualTo(1000L);

    keyDbContainerProxy.toxics()
      .get("latency").remove();

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
      .contains(BEAN_NAME_EMBEDDED_KEYDB);
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
