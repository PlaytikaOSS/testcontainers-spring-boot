package com.playtika.testcontainer.nativekafka;

import com.playtika.testcontainer.nativekafka.properties.NativeKafkaConfigurationProperties;
import com.playtika.testcontainer.nativekafka.properties.NativeKafkaConfigurationProperties.TopicConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@Order(2)
@TestPropertySource(properties = {
    "embedded.nativekafka.fileSystemBind.dataFolder=${java.io.tmpdir}/embedded-native-kafka-data-unexpected"
})
@TestInstance(PER_CLASS)
@DisplayName("Default embedded-native-kafka setup test")
public class EmbeddedNativeKafkaTest extends AbstractEmbeddedNativeKafkaTest {

    private static final String MESSAGE = "test message";

    @Autowired
    protected NativeKafkaTopicsConfigurer nativeKafkaTopicsConfigurer;

    @Test
    @DisplayName("creates topics on startup")
    public void shouldAutoCreateTopic() throws Exception {
        assertThatTopicExists("topic1", 1);
        assertThatTopicExists("topic3", 2);
    }

    @Test
    @DisplayName("allows to create other topics")
    public void shouldCreateTopic() throws Exception {
        String topicToCreate = "topicToCreate";
        int defaultPartitions = 1;
        String topicToCreate1 = "topicToCreate1";
        int customPartitions = 2;

        nativeKafkaTopicsConfigurer.createTopics(java.util.Collections.singletonList(topicToCreate),
                                               java.util.Collections.singletonList(new TopicConfiguration(topicToCreate1, customPartitions)));

        assertThatTopicExists(topicToCreate, defaultPartitions);
        assertThatTopicExists(topicToCreate1, customPartitions);
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
    @DisplayName("provides bootstrap servers configuration")
    public void shouldProvideBootstrapServers() {
        assertThat(nativeKafkaBrokerList)
                .isNotEmpty()
                .allMatch(broker -> broker.contains(":"));
    }

    @Test
    @DisplayName("allows send and consume transactional messages")
    public void shouldSendAndConsumeTransactionalMessage() throws Exception {
        sendTransactionalMessage("topic2", MESSAGE);

        String consumedMessage = consumeTransactionalMessage("topic2");

        assertThat(consumedMessage)
                .isEqualTo(MESSAGE);
    }

    @AfterAll
    public static void afterAll(@Autowired NativeKafkaConfigurationProperties nativeKafkaProperties, TestInfo testInfo) throws Exception {
        // JUnit invokes static afterAll() even when child class is running and TestInstance.Lifecycle.PER_CLASS is being used
        if (!testInfo.getTestClass().map(EmbeddedNativeKafkaTest.class::equals).orElse(false)) {
            return;
        }

        Path projectDir = projectDir();
        Path nativeKafkaDataFolder = projectDir.resolve(nativeKafkaProperties.getFileSystemBind().getDataFolder());

        assertThat(nativeKafkaDataFolder.toFile()).doesNotExist();
    }

}