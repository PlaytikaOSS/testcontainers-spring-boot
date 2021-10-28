package com.playtika.test.kafka;

import com.playtika.test.kafka.properties.KafkaConfigurationProperties;
import com.playtika.test.kafka.properties.ZookeeperConfigurationProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.PathUtils;

import java.nio.file.Path;

import static com.playtika.test.kafka.properties.KafkaConfigurationProperties.KAFKA_BEAN_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@Order(6)
@TestPropertySource(properties = {
    "embedded.kafka.waitTimeoutInSeconds=120",
    "embedded.zookeeper.fileSystemBind.enabled=true",
    "embedded.kafka.fileSystemBind.enabled=true",
    "embedded.kafka.fileSystemBind.dataFolder=${java.io.tmpdir}/embedded-kafka-data", // /tmp is more permissible than target
    "embedded.zookeeper.fileSystemBind.dataFolder=${java.io.tmpdir}/embedded-zk-data",
    "embedded.zookeeper.fileSystemBind.txnLogsFolder=${java.io.tmpdir}/embedded-zk-txn-logs",
    "embedded.kafka.dockerUser=root", // Needed to create mounted directories, or Zookeeper fails: "Unable to create data directory /var/lib/zookeeper/log/version-2"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Test that embedded-kafka with filesystem binding")
public class EmbeddedKafkaWithBindingTest extends EmbeddedKafkaTest {

    @AfterAll
    public static void afterAll(@Autowired KafkaConfigurationProperties kafkaProperties, @Autowired ZookeeperConfigurationProperties zookeeperProperties, @Qualifier(KAFKA_BEAN_NAME) GenericContainer kafka) throws Exception {
        Path projectDir = projectDir();
        Path zookeeperDataFolder = projectDir.resolve(zookeeperProperties.getFileSystemBind().getDataFolder());
        Path zookeeperTxnLogsFolder = projectDir.resolve(zookeeperProperties.getFileSystemBind().getTxnLogsFolder());
        Path kafkaDataFolder = projectDir.resolve(kafkaProperties.getFileSystemBind().getDataFolder());

        assertThat(zookeeperDataFolder.toFile()).isNotEmptyDirectory();
        assertThat(zookeeperTxnLogsFolder.toFile()).isNotEmptyDirectory();
        assertThat(kafkaDataFolder.toFile()).isNotEmptyDirectory();

        // Delete created files now
        kafka.execInContainer("rm", "-rf", "/var/lib/zookeeper/log", "/var/lib/zookeeper/data", "/var/lib/kafka/data");

        // Delete mounted directories after test run
        Runtime.getRuntime().addShutdownHook(new Thread(DockerClientFactory.TESTCONTAINERS_THREAD_GROUP, () -> {
            PathUtils.recursiveDeleteDir(kafkaDataFolder);
            PathUtils.recursiveDeleteDir(zookeeperDataFolder);
            PathUtils.recursiveDeleteDir(zookeeperTxnLogsFolder);
        }));
    }
}
