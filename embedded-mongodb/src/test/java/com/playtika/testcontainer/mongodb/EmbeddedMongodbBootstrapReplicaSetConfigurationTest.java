package com.playtika.testcontainer.mongodb;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Autowired
    TransactionalFooService transactionalFooService;


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

    // Regression test for https://github.com/PlaytikaOSS/testcontainers-spring-boot/issues/1182 :
    // on a standalone mongod, this whole flow throws
    // "Transaction numbers are only allowed on a replica set member or mongos" before commit/rollback even applies.
    @Test
    public void shouldCommitTransactionAcrossMultipleDocuments() {
        Foo foo1 = new Foo(UUID.randomUUID().toString(), "foo1", Instant.parse("2019-09-26T07:57:12.801Z"), 1L);
        Foo foo2 = new Foo(UUID.randomUUID().toString(), "foo2", Instant.parse("2019-09-26T07:57:12.801Z"), 2L);

        transactionalFooService.saveTwo(foo1, foo2);

        assertThat(mongoTemplate.findById(foo1.someId(), Foo.class)).isEqualTo(foo1);
        assertThat(mongoTemplate.findById(foo2.someId(), Foo.class)).isEqualTo(foo2);
    }

    @Test
    public void shouldRollbackTransactionOnException() {
        Foo foo1 = new Foo(UUID.randomUUID().toString(), "foo1", Instant.parse("2019-09-26T07:57:12.801Z"), 1L);
        Foo foo2 = new Foo(UUID.randomUUID().toString(), "foo2", Instant.parse("2019-09-26T07:57:12.801Z"), 2L);

        assertThatThrownBy(() -> transactionalFooService.saveTwoThenFail(foo1, foo2))
                .isInstanceOf(IllegalStateException.class);

        assertThat(mongoTemplate.findById(foo1.someId(), Foo.class)).isNull();
        assertThat(mongoTemplate.findById(foo2.someId(), Foo.class)).isNull();
    }

    @Test
    public void shouldReceiveChangeStreamEvents() throws Exception {
        String collectionName = "changeStream-" + UUID.randomUUID();
        mongoTemplate.createCollection(collectionName);
        MongoCollection<Document> collection = mongoTemplate.getCollection(collectionName);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (MongoCursor<ChangeStreamDocument<Document>> cursor = collection.watch().iterator()) {
            Future<ChangeStreamDocument<Document>> future = executor.submit(cursor::next);

            String insertedId = UUID.randomUUID().toString();
            collection.insertOne(new Document("_id", insertedId).append("value", "bar"));

            ChangeStreamDocument<Document> event = future.get(10, TimeUnit.SECONDS);

            assertThat(event.getOperationType()).isEqualTo(OperationType.INSERT);
            assertThat(event.getFullDocument()).isNotNull();
            assertThat(event.getFullDocument().getString("_id")).isEqualTo(insertedId);
            assertThat(event.getFullDocument().getString("value")).isEqualTo("bar");
        } finally {
            executor.shutdownNow();
            mongoTemplate.dropCollection(collectionName);
        }
    }

    record Foo(@Id String someId, String someString, Instant someTimestamp, Long someNumber) {
    }

    @RequiredArgsConstructor
    static class TransactionalFooService {

        private final MongoTemplate mongoTemplate;

        @Transactional
        public void saveTwo(Foo a, Foo b) {
            mongoTemplate.save(a);
            mongoTemplate.save(b);
        }

        @Transactional
        public void saveTwoThenFail(Foo a, Foo b) {
            mongoTemplate.save(a);
            mongoTemplate.save(b);
            throw new IllegalStateException("boom");
        }
    }

    @EnableAutoConfiguration
    @EnableTransactionManagement
    @Configuration
    static class TestConfiguration {

        @Bean
        MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory dbFactory) {
            return new MongoTransactionManager(dbFactory);
        }

        @Bean
        TransactionalFooService transactionalFooService(MongoTemplate mongoTemplate) {
            return new TransactionalFooService(mongoTemplate);
        }
    }
}
