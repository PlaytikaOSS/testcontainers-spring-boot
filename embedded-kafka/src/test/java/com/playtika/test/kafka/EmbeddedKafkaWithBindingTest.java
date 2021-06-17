package com.playtika.test.kafka;

import com.playtika.test.kafka.properties.KafkaConfigurationProperties;
import com.playtika.test.kafka.properties.ZookeeperConfigurationProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@Order(6)
@TestPropertySource(properties = {
        "embedded.kafka.fileSystemBind.dataFolder=target/embedded-kafka-data-expected",
        "embedded.zookeeper.fileSystemBind.dataFolder=target/embedded-zk-data-expected",
        "embedded.zookeeper.fileSystemBind.txnLogsFolder=target/embedded-zk-txn-logs-expected",
        "embedded.zookeeper.fileSystemBind.enabled=true",
        "embedded.kafka.fileSystemBind.enabled=true",
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Test that embedded-kafka with filesystem binding")
public class EmbeddedKafkaWithBindingTest extends EmbeddedKafkaTest {

    @AfterAll
    public static void afterAll(@Autowired KafkaConfigurationProperties kafkaProperties, @Autowired ZookeeperConfigurationProperties zookeeperProperties) throws Exception {
        Path projectDir = projectDir();
        Path zookeeperDataFolder = projectDir.resolve(zookeeperProperties.getFileSystemBind().getDataFolder());
        Path zookeeperTxnLogsFolder = projectDir.resolve(zookeeperProperties.getFileSystemBind().getTxnLogsFolder());
        Path kafkaDataFolder = projectDir.resolve(kafkaProperties.getFileSystemBind().getDataFolder());

        assertThat(zookeeperDataFolder.toFile()).isNotEmptyDirectory();
        assertThat(zookeeperTxnLogsFolder.toFile()).isNotEmptyDirectory();
        assertThat(kafkaDataFolder.toFile()).isNotEmptyDirectory();
    }
}
