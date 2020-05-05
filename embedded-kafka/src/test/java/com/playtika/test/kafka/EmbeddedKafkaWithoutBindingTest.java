package com.playtika.test.kafka;

import com.playtika.test.kafka.properties.KafkaConfigurationProperties;
import com.playtika.test.kafka.properties.ZookeeperConfigurationProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = {
        "embedded.zookeeper.fileSystemBind.enabled=false",
        "embedded.zookeeper.fileSystemBind.dataFolder=target/embedded-zk-data-without-binding",
        "embedded.zookeeper.fileSystemBind.txnLogsFolder=target/embedded-zk-txn-logs-without-binding",

        "embedded.kafka.fileSystemBind.enabled=false",
        "embedded.kafka.fileSystemBind.dataFolder=target/embedded-kafka-data-without-binding"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Test that embedded-kafka without filesystem binding")
public class EmbeddedKafkaWithoutBindingTest extends EmbeddedKafkaTest {
    @Autowired
    private ZookeeperConfigurationProperties zookeeperProperties;

    @Autowired
    private KafkaConfigurationProperties kafkaProperties;

    @AfterAll
    public void shouldBindToFileSystem() throws Exception {
        Path projectDir = projectDir();
        Path zookeeperDataFolder = projectDir.resolve(zookeeperProperties.getFileSystemBind().getDataFolder());
        Path zookeeperTxnLogsFolder = projectDir.resolve(zookeeperProperties.getFileSystemBind().getTxnLogsFolder());
        Path kafkaDataFolder = projectDir.resolve(kafkaProperties.getFileSystemBind().getDataFolder());

        assertThat(zookeeperDataFolder.toFile())
                .doesNotExist();
        assertThat(zookeeperTxnLogsFolder.toFile())
                .doesNotExist();
        assertThat(kafkaDataFolder.toFile())
                .doesNotExist();
    }
}
