package com.playtika.test.kafka;

import com.playtika.test.common.operations.NetworkTestOperations;
import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static java.time.Duration.ofMillis;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

abstract class BaseEmbeddedKafkaTest extends AbstractEmbeddedKafkaTest {
    private static final String TOPIC = "topic1";
    private static final String MESSAGE = "test message";

    protected KafkaTopicsConfigurer kafkaTopicsConfigurer;
    protected NetworkTestOperations kafkaNetworkTestOperations;

    @Autowired
    @Override
    public void setAdminClient(AdminClient adminClient) {
        super.setAdminClient(adminClient);
    }

    @Autowired
    @Override
    public void setKafkaBrokerList(@Value("${embedded.kafka.brokerList}") List<String> kafkaBrokerList) {
        super.setKafkaBrokerList(kafkaBrokerList);
    }

    @Autowired
    public void setKafkaTopicsConfigurer(KafkaTopicsConfigurer kafkaTopicsConfigurer) {
        this.kafkaTopicsConfigurer = kafkaTopicsConfigurer;
    }

    @Autowired
    public void setKafkaNetworkTestOperations(NetworkTestOperations kafkaNetworkTestOperations) {
        this.kafkaNetworkTestOperations = kafkaNetworkTestOperations;
    }

    @Test
    @DisplayName("creates topics on startup")
    public void shouldAutoCreateTopic() throws Exception {
        assertThatTopicExists("autoCreatedTopic");
    }

    @Test
    @DisplayName("allows to create other topics")
    public void shouldCreateTopic() throws Exception {
        String topicToCreate = "topicToCreate";

        kafkaTopicsConfigurer.createTopics(Collections.singletonList(topicToCreate));

        assertThatTopicExists(topicToCreate);
    }

    @Test
    @DisplayName("allows send and consume messages")
    public void shouldSendAndConsumeMessage() throws Exception {
        sendMessage(TOPIC, MESSAGE);

        String consumedMessage = consumeMessage(TOPIC);

        assertThat(consumedMessage)
                .isEqualTo(MESSAGE);
    }

    @Test
    @DisplayName("allows send and consume transactional messages")
    public void shouldSendAndConsumeTransactionalMessage() throws Exception {
        sendTransactionalMessage(TOPIC, MESSAGE);

        String consumedMessage = consumeTransactionalMessage(TOPIC);

        assertThat(consumedMessage)
                .isEqualTo(MESSAGE);
    }

    @Test
    @DisplayName("allows to emulate latency on send")
    public void shouldEmulateLatencyOnSend() throws Exception {
        kafkaNetworkTestOperations
                .withNetworkLatency(
                        ofMillis(1000),
                        () -> assertThat(durationOf(() -> sendMessage(TOPIC, "abc0")))
                                .isGreaterThan(1000L));

        assertThat(durationOf(() -> sendMessage(TOPIC, "abc1")))
                .isLessThan(200L);

        assertThat(consumeMessages(TOPIC))
                .containsExactly("abc0", "abc1");
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
