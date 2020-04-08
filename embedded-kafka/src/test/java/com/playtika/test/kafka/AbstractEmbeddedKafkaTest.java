package com.playtika.test.kafka;

import com.playtika.test.common.utils.ThrowingRunnable;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.assertj.core.util.Lists;

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
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BATCH_SIZE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractEmbeddedKafkaTest {
    protected AdminClient adminClient;
    protected List<String> kafkaBrokerList;

    public void setAdminClient(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    public void setKafkaBrokerList(List<String> kafkaBrokerList) {
        this.kafkaBrokerList = kafkaBrokerList;
    }

    protected void assertThatTopicExists(String topicName) throws Exception {
        ListTopicsResult result = adminClient.listTopics();
        Set<String> topics = result.names().get(10, TimeUnit.SECONDS);
        assertThat(topics).contains(topicName);
    }

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

    protected String consumeMessage(String topic) {
        return consumeMessages(topic)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no message received"));
    }

    protected String consumeTransactionalMessage(String topic) {
        return consumeTransactionalMessages(topic)
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

    protected List<String> consumeTransactionalMessages(String topic) {
        try (KafkaConsumer<String, String> consumer = createConsumer(topic, true)) {
            return pollForRecords(consumer)
                    .stream()
                    .map(ConsumerRecord::value)
                    .collect(Collectors.toList());
        }
    }

    protected KafkaProducer<String, String> createProducer() {
        Map<String, Object> producerConfiguration = getKafkaProducerConfiguration();
        return new KafkaProducer<>(producerConfiguration);
    }

    protected KafkaProducer<String, String> createTransactionalProducer() {
        Map<String, Object> producerConfiguration = getKafkaProducerConfiguration(true);
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(producerConfiguration);
        kafkaProducer.initTransactions();
        return kafkaProducer;
    }

    protected KafkaConsumer<String, String> createConsumer(String topic) {
        Map<String, Object> consumerConfiguration = getKafkaConsumerConfiguration();
        Properties properties = new Properties();
        properties.putAll(consumerConfiguration);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(singleton(topic));
        return consumer;
    }

    protected KafkaConsumer<String, String> createConsumer(String topic, boolean transactional) {
        Map<String, Object> consumerConfiguration = getKafkaConsumerConfiguration(transactional);
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
        return getKafkaProducerConfiguration(false);
    }

    protected Map<String, Object> getKafkaProducerConfiguration(boolean transactional) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerList);
        configs.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        configs.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        configs.put(RETRIES_CONFIG, 0);
        configs.put(BATCH_SIZE_CONFIG, 0);
        if (transactional) {
            configs.put("transactional.id", "tx-0");
            configs.put("max.in.flight.requests.per.connection", 1);
            configs.put("enable.idempotence", true);
            configs.put("acks", "all");
            configs.put("retries", 10);
            configs.put("delivery.timeout.ms", 300000);
            configs.put("retry.backoff.ms", 1000);
        }
        return configs;
    }

    protected Map<String, Object> getKafkaConsumerConfiguration() {
        return getKafkaConsumerConfiguration(false);
    }

    protected Map<String, Object> getKafkaConsumerConfiguration(boolean transactional) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerList);
        configs.put(GROUP_ID_CONFIG, "testGroup");
        configs.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        configs.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        configs.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        if (transactional) {
            configs.put("isolation.level", "read_committed");
            configs.put("enable.auto.commit", false);

        }
        return configs;
    }

    protected static long durationOf(ThrowingRunnable operation) throws Exception {
        long startTimestamp = System.currentTimeMillis();
        operation.run();
        return System.currentTimeMillis() - startTimestamp;
    }

    protected static Path projectDir() {
        String classesPath = AbstractEmbeddedKafkaTest.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath();
        return Paths.get(classesPath).getParent().getParent();
    }
}
