package com.playtika.test.kafka;

import com.playtika.test.common.operations.NetworkTestOperations;
import com.playtika.test.kafka.properties.KafkaConfigurationProperties;
import com.playtika.test.kafka.properties.ZookeeperConfigurationProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.Collections;

import static java.time.Duration.ofMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
@DisplayName("Default embedded-kafka setup test")
public class EmbeddedKafkaTest extends AbstractEmbeddedKafkaTest {
    private static final String TOPIC = "topic1";
    private static final String MESSAGE = "test message";

    @Autowired
    protected KafkaTopicsConfigurer kafkaTopicsConfigurer;
    @Autowired
    protected NetworkTestOperations kafkaNetworkTestOperations;
    @Autowired
    private ZookeeperConfigurationProperties zookeeperProperties;
    @Autowired
    private KafkaConfigurationProperties kafkaProperties;

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

    @AfterAll
    public void shouldBindToFileSystem() throws Exception {
        Path projectDir = projectDir();
        Path zookeeperDataFolder = projectDir.resolve(zookeeperProperties.getFileSystemBind().getDataFolder());
        Path zookeeperTxnLogsFolder = projectDir.resolve(zookeeperProperties.getFileSystemBind().getTxnLogsFolder());
        Path kafkaDataFolder = projectDir.resolve(kafkaProperties.getFileSystemBind().getDataFolder());

        assertThat(zookeeperDataFolder.toFile())
                .isDirectory()
                .isNotEmptyDirectory();
        assertThat(zookeeperTxnLogsFolder.toFile())
                .isDirectory()
                .isNotEmptyDirectory();
        assertThat(kafkaDataFolder.toFile())
                .isDirectory()
                .isNotEmptyDirectory();
    }
}
