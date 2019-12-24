/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.playtika.test.kafka;

import com.playtika.test.common.operations.NetworkTestOperations;
import com.playtika.test.common.utils.ThrowingRunnable;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.time.Duration.ofMillis;
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

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = EmbeddedKafkaTests.TestConfiguration.class,
        properties = {
                "embedded.kafka.topicsToCreate=autoCreatedTopic",
                "embedded.kafka.install.enabled=true"
        }
)
public class EmbeddedKafkaTests {

    private static final String TOPIC = "topic1";
    private static final String MESSAGE = "test message";

    @Value("${embedded.kafka.brokerList}")
    String kafkaBrokerList;
    @Autowired
    private AdminClient adminClient;
    @Autowired
    private KafkaTopicsConfigurer kafkaTopicsConfigurer;
    @Autowired
    private NetworkTestOperations kafkaNetworkTestOperations;

    @Test
    public void shouldAutoCreateTopic() throws Exception {
        assertThatTopicExists("autoCreatedTopic");
    }

    @Test
    public void shouldCreateTopic() throws Exception {
        String topicToCreate = "topicToCreate";
        this.kafkaTopicsConfigurer.createTopics(Collections.singletonList(topicToCreate));

        assertThatTopicExists(topicToCreate);
    }

    @Test
    public void should_sendAndConsumeMessage() throws Exception {
        sendMessage(TOPIC, MESSAGE);

        String consumedMessage = consumeMessage(TOPIC);

        assertThat(consumedMessage).isEqualTo(MESSAGE);
    }

    @Test
    public void shouldEmulateLatencyOnSend() throws Exception {
        kafkaNetworkTestOperations.withNetworkLatency(ofMillis(1000),
                () -> assertThat(durationOf(() -> sendMessage(TOPIC, "abc0")))
                        .isGreaterThan(1000L)
        );

        assertThat(durationOf(() -> sendMessage(TOPIC, "abc1")))
                .isLessThan(100L);

        assertThat(consumeMessages(TOPIC)).containsExactly("abc0", "abc1");
    }

    private void assertThatTopicExists(String topicName) throws Exception {
        ListTopicsResult result = adminClient.listTopics();
        Set<String> topics = result.names().get(10, TimeUnit.SECONDS);
        assertThat(topics).contains(topicName);
    }

    private void sendMessage(String topic, String message) throws Exception {
        try (KafkaProducer<String, String> kafkaProducer = createProducer()) {

            kafkaProducer.send(new ProducerRecord<>(topic, message)).get();
        }
    }

    private String consumeMessage(String topic) {
        return consumeMessages(topic).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no message received"));
    }

    private List<String> consumeMessages(String topic) {
        try (KafkaConsumer<String, String> consumer = createConsumer(topic)) {
            return pollForRecords(consumer).stream()
                    .map(ConsumerRecord::value)
                    .collect(Collectors.toList());
        }
    }

    private KafkaProducer<String, String> createProducer() {
        Map<String, Object> producerConfiguration = getKafkaProducerConfiguration();
        return new KafkaProducer<>(producerConfiguration);
    }

    private KafkaConsumer<String, String> createConsumer(String topic) {
        Map<String, Object> consumerConfiguration = getKafkaConsumerConfiguration();
        Properties properties = new Properties();
        properties.putAll(consumerConfiguration);
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(singleton(topic));
        return consumer;
    }

    private static <K, V> List<ConsumerRecord<K, V>> pollForRecords(KafkaConsumer<K, V> consumer) {
        ConsumerRecords<K, V> received = consumer.poll(Duration.ofSeconds(10));
        return received == null ? emptyList() : Lists.newArrayList(received);
    }

    private Map<String, Object> getKafkaProducerConfiguration() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerList);
        configs.put(KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        configs.put(VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        configs.put(RETRIES_CONFIG, 0);
        configs.put(BATCH_SIZE_CONFIG, 0);

        return configs;
    }

    private Map<String, Object> getKafkaConsumerConfiguration() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("bootstrap.servers", kafkaBrokerList);
        configs.put(GROUP_ID_CONFIG, "testGroup");
        configs.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        configs.put(KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        configs.put(VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        return configs;
    }

    private static long durationOf(ThrowingRunnable op) throws Exception {
        long start = System.currentTimeMillis();
        op.run();
        return System.currentTimeMillis() - start;
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestConfiguration {

        @Bean
        AdminClient adminClient(@Value("${embedded.kafka.brokerList}") String brokers) {
            Properties config = new Properties();
            config.put("bootstrap.servers", brokers);
            return AdminClient.create(config);
        }
    }

}