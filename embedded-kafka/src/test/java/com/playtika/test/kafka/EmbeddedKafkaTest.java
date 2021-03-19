package com.playtika.test.kafka;

import com.playtika.test.common.operations.NetworkTestOperations;
import com.playtika.test.kafka.properties.KafkaConfigurationProperties;
import com.playtika.test.kafka.properties.ZookeeperConfigurationProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.Collections;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@Order(2)
@TestInstance(PER_CLASS)
@DisplayName("Default embedded-kafka setup test")
public class EmbeddedKafkaTest extends AbstractEmbeddedKafkaTest {

    private static final String MESSAGE = "test message";

    @Autowired
    protected KafkaTopicsConfigurer kafkaTopicsConfigurer;
    @Autowired
    protected NetworkTestOperations kafkaNetworkTestOperations;
    @Autowired
    protected ZookeeperConfigurationProperties zookeeperProperties;
    @Autowired
    protected KafkaConfigurationProperties kafkaProperties;

    @Test
    @DisplayName("creates topics on startup")
    public void shouldAutoCreateTopic() throws Exception {
        assertThatTopicExists("topic1");
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
        sendMessage("topic1", MESSAGE);

        String consumedMessage = consumeMessage("topic1");

        assertThat(consumedMessage)
                .isEqualTo(MESSAGE);
    }

    @Test
    @DisplayName("allows send and consume transactional messages")
    public void shouldSendAndConsumeTransactionalMessage() throws Exception {
        sendTransactionalMessage("topic2", MESSAGE);

        String consumedMessage = consumeTransactionalMessage("topic2");

        assertThat(consumedMessage)
                .isEqualTo(MESSAGE);
    }

    @Test
    @DisplayName("allows to emulate latency on send")
    public void shouldEmulateLatencyOnSend() throws Exception {
        kafkaNetworkTestOperations
                .withNetworkLatency(
                        ofMillis(1000),
                        () -> assertThat(durationOf(() -> sendMessage("topic3", "abc0")))
                                .isGreaterThan(1000L));

        assertThat(durationOf(() -> sendMessage("topic3", "abc1")))
                .isLessThan(200L);

        assertThat(consumeMessages("topic3"))
                .containsExactly("abc0", "abc1");
    }

    @AfterAll
    public void afterAll() throws Exception {
        Path projectDir = projectDir();
        Path zookeeperDataFolder = projectDir.resolve(zookeeperProperties.getFileSystemBind().getDataFolder());
        Path zookeeperTxnLogsFolder = projectDir.resolve(zookeeperProperties.getFileSystemBind().getTxnLogsFolder());
        Path kafkaDataFolder = projectDir.resolve(kafkaProperties.getFileSystemBind().getDataFolder());

        assertThat(zookeeperDataFolder.toFile()).doesNotExist();
        assertThat(zookeeperTxnLogsFolder.toFile()).doesNotExist();
        assertThat(kafkaDataFolder.toFile()).doesNotExist();
    }
}
