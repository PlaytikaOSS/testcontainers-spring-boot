package com.playtika.test.mongodb;

import eu.rekawek.toxiproxy.model.ToxicDirection;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.ToxiproxyContainer;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        properties = {
                "spring.data.mongodb.uri=mongodb://${embedded.mongodb.host}:${embedded.mongodb.toxiproxy.port}/${embedded.mongodb.database}",
                "embedded.toxiproxy.proxies.mongodb.enabled=true"
        }
        , classes = EmbeddedMongodbBootstrapConfigurationTest.TestConfiguration.class
)
public class EmbeddedMongodbBootstrapConfigurationTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    ConfigurableEnvironment environment;

    @Autowired
    ToxiproxyContainer.ContainerProxy mongodbContainerProxy;

    @Test
    public void shouldSaveAndGet() {
        String someId = UUID.randomUUID().toString();
        Foo foo = new Foo(someId, "foo", Instant.parse("2019-09-26T07:57:12.801Z"), -42L);
        mongoTemplate.save(foo);

        assertThat(mongoTemplate.findById(someId, Foo.class)).isEqualTo(foo);
    }

    @Test
    public void shouldEmulateLatency() throws Exception {
        mongodbContainerProxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, 1000);

        assertThat(durationOf(() -> mongoTemplate.findById("any", Foo.class)))
                        .isCloseTo(1000L, Offset.offset(100L));

        mongodbContainerProxy.toxics().get("latency").remove();

        assertThat(durationOf(() -> mongoTemplate.findById("any", Foo.class)))
                .isLessThan(100L);
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.mongodb.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mongodb.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mongodb.database")).isNotEmpty();
    }

    private static long durationOf(Callable<?> op) throws Exception {
        long start = System.currentTimeMillis();
        op.call();
        return System.currentTimeMillis() - start;
    }

    @Value
    static class Foo {
        @Id
        String someId;
        String someString;
        Instant someTimestamp;
        Long someNumber;
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
