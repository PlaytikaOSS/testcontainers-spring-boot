package com.playtika.test.kafka;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Order(6)
@TestPropertySource(properties = {
        "embedded.zookeeper.fileSystemBind.enabled=true",
        "embedded.kafka.fileSystemBind.enabled=true",
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Test that embedded-kafka with filesystem binding")
public class EmbeddedKafkaWithBindingTest extends EmbeddedKafkaTest {

    @AfterAll
    public void afterAll() throws Exception {
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
