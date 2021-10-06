package com.playtika.test.mongodb;


import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(
        properties = {
                "embedded.mongodb.install.enabled=true",
                "embedded.mongodb.username=root",
                "embedded.mongodb.password=letmein",
                "spring.data.mongodb.host=${embedded.mongodb.host}",
                "spring.data.mongodb.port=${embedded.mongodb.port}",
                "spring.data.mongodb.username=${embedded.mongodb.username}",
                "spring.data.mongodb.password=${embedded.mongodb.password}",
                "spring.data.mongodb.database=${embedded.mongodb.database}",
                "spring.data.mongodb.authentication-database=admin"
        }
        ,classes = EmbeddedMongodbBootstrapAuthConfigurationTest.TestConfiguration.class
)
public class EmbeddedMongodbBootstrapAuthConfigurationTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    ConfigurableEnvironment environment;

    @Test
    public void shouldSaveAndGet() {
        String someId = UUID.randomUUID().toString();
        Foo foo = new Foo(someId, "foo", Instant.parse("2019-09-26T07:57:12.801Z"), -42L);
        mongoTemplate.save(foo);

        assertThat(mongoTemplate.findById(someId, Foo.class)).isEqualTo(foo);
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.mongodb.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mongodb.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mongodb.username")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mongodb.password")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mongodb.database")).isNotEmpty();
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
