package com.playtika.testcontainer.mongodb;


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
                "embedded.mongodb.username=root",
                "embedded.mongodb.password=letmein",
                "embedded.mongodb.replica-set-name=rs0",
                "spring.mongodb.uri=mongodb://${embedded.mongodb.username}:${embedded.mongodb.password}@${embedded.mongodb.host}:${embedded.mongodb.port}/${embedded.mongodb.database}?replicaSet=${embedded.mongodb.replica-set-name}&directConnection=true&authSource=admin"
        }
        , classes = EmbeddedMongodbBootstrapReplicaSetConfigurationTest.TestConfiguration.class
)
public class EmbeddedMongodbBootstrapReplicaSetConfigurationTest {

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
        assertThat(environment.getProperty("embedded.mongodb.replica-set-name")).isNotEmpty();
    }

    record Foo(@Id String someId, String someString, Instant someTimestamp, Long someNumber) {
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
