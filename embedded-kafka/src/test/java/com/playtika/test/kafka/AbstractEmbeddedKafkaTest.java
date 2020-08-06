package com.playtika.test.kafka;

import com.playtika.test.common.utils.ThrowingRunnable;
import com.playtika.test.kafka.camel.samples.SampleProductionRouteContext;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.ISOLATION_LEVEL_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BATCH_SIZE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.TRANSACTIONAL_ID_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        properties = {
                "embedded.kafka.topicsToCreate=autoCreatedTopic,secureTopic,helloTopic",
                "embedded.kafka.secureTopics=secureTopic",
                "embedded.kafka.schema-registry.enabled=true"
        },
        classes = {EmbeddedKafkaTest.TestConfiguration.class, SampleProductionRouteContext.class}
)
public abstract class AbstractEmbeddedKafkaTest {
    @Autowired
    protected AdminClient adminClient;
    @Value("${embedded.kafka.brokerList}")
    protected List<String> kafkaBrokerList;

    protected void sendMessage(String topic, String message) throws Exception {
        try (KafkaProducer<String, String> kafkaProducer = createProducer()) {
            kafkaProducer.send(new ProducerRecord<>(topic, message)).get();
        }
    }

    protected void sendTransactionalMessage(String topic, String message) throws Exception {
        try (KafkaProducer<String, String> kafkaProducer = createTransactionalProducer()) {
            kafkaProducer.beginTransaction();
            kafkaProducer.send(new ProducerRecord<>(topic, message)).get();
            kafkaProducer.commitTransaction();
        }
    }

    protected void sendMessage(String topic, String message, Map<String, Object> producerConfiguration) throws Exception {
        try (KafkaProducer<String, String> kafkaProducer = createProducer(producerConfiguration)) {
            kafkaProducer.send(new ProducerRecord<>(topic, message)).get();
        }
    }

    protected String consumeMessage(String topic) {
        return consumeMessages(topic)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no message received"));
    }

    protected String consumeMessage(String topic, Map<String, Object> consumerConfiguration) {
        return consumeMessages(topic, consumerConfiguration)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no message received"));
    }

    protected String consumeTransactionalMessage(String topic) {
        return consumeMessagesTransactional(topic)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no message received"));
    }

    protected List<String> consumeMessages(String topic) {
        try (KafkaConsumer<String, String> consumer = createConsumer(topic)) {
            return pollForRecords(consumer)
                    .stream()
                    .map(ConsumerRecord::value)
                    .collect(Collectors.toList());
        }
    }

    protected List<String> consumeMessagesTransactional(String topic) {
        try (KafkaConsumer<String, String> consumer = createTransactionalConsumer(topic)) {
            return pollForRecords(consumer)
                    .stream()
                    .map(ConsumerRecord::value)
                    .collect(Collectors.toList());
        }
    }

    protected List<String> consumeMessages(String topic, Map<String, Object> consumerConfiguration) {
        try (KafkaConsumer<String, String> consumer = createConsumer(topic, consumerConfiguration)) {
            return pollForRecords(consumer)
                    .stream()
                    .map(ConsumerRecord::value)
                    .collect(Collectors.toList());
        }
    }

    protected KafkaProducer<String, String> createProducer() {
        return createProducer(getKafkaProducerConfiguration());
    }

    protected KafkaProducer<String, String> createTransactionalProducer() {
        KafkaProducer<String, String> kafkaProducer = createProducer(getKafkaTransactionalProducerConfiguration());
        kafkaProducer.initTransactions();
        return kafkaProducer;
    }

    protected KafkaProducer<String, String> createProducer(Map<String, Object> producerConfiguration) {
        return new KafkaProducer<>(producerConfiguration);
    }

    protected KafkaConsumer<String, String> createConsumer(String topic) {
        return createConsumer(topic, getKafkaConsumerConfiguration());
    }

    protected KafkaConsumer<String, String> createTransactionalConsumer(String topic) {
        return createConsumer(topic, getKafkaTransactionalConsumerConfiguration());
    }

    protected KafkaConsumer<String, String> createConsumer(String topic, Map<String, Object> consumerConfiguration) {
        Properties properties = new Properties();
        properties.putAll(consumerConfiguration);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(singleton(topic));
        return consumer;
    }

    protected static <K, V> List<ConsumerRecord<K, V>> pollForRecords(KafkaConsumer<K, V> consumer) {
        ConsumerRecords<K, V> received = consumer.poll(Duration.ofSeconds(10));
        return received == null ? emptyList() : Lists.newArrayList(received);
    }

    protected Map<String, Object> getKafkaProducerConfiguration() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerList);
        configs.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        configs.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        configs.put(RETRIES_CONFIG, 0);
        configs.put(BATCH_SIZE_CONFIG, 0);
        return configs;
    }

    protected Map<String, Object> getKafkaTransactionalProducerConfiguration() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerList);
        configs.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        configs.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        configs.put(BATCH_SIZE_CONFIG, 0);
        configs.put(TRANSACTIONAL_ID_CONFIG, "tx-0");
        configs.put(MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        configs.put(ENABLE_IDEMPOTENCE_CONFIG, true);
        configs.put(ACKS_CONFIG, "all");
        configs.put(RETRIES_CONFIG, 10);
        configs.put(DELIVERY_TIMEOUT_MS_CONFIG, 300000);
        configs.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        return configs;
    }

    protected Map<String, Object> getKafkaConsumerConfiguration() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerList);
        configs.put(GROUP_ID_CONFIG, "testGroup");
        configs.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        configs.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configs.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return configs;
    }

    protected Map<String, Object> getKafkaTransactionalConsumerConfiguration() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerList);
        configs.put(GROUP_ID_CONFIG, "testGroup");
        configs.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        configs.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configs.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configs.put(ISOLATION_LEVEL_CONFIG, "read_committed");
        configs.put(ENABLE_AUTO_COMMIT_CONFIG, false);
        return configs;
    }

    protected static long durationOf(ThrowingRunnable operation) throws Exception {
        long startTimestamp = System.currentTimeMillis();
        operation.run();
        return System.currentTimeMillis() - startTimestamp;
    }

    protected static Path projectDir() throws Exception {
        URI classesPath = AbstractEmbeddedKafkaTest.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI();
        return Paths.get(classesPath).getParent().getParent();
    }

    protected void assertThatTopicExists(String topicName) throws Exception {
        ListTopicsResult result = adminClient.listTopics();
        Set<String> topics = result.names().get(10, TimeUnit.SECONDS);
        assertThat(topics).contains(topicName);
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestConfiguration {

        @Bean
        AdminClient adminClient(@Value("${embedded.kafka.brokerList}") String brokers) {
            Properties config = new Properties();
            config.put(BOOTSTRAP_SERVERS_CONFIG, brokers);
            return AdminClient.create(config);
        }
    }
}
