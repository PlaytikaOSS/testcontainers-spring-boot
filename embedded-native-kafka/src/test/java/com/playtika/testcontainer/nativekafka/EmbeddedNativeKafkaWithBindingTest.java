package com.playtika.testcontainer.nativekafka;

import com.playtika.testcontainer.nativekafka.properties.NativeKafkaConfigurationProperties;
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

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.playtika.testcontainer.nativekafka.properties.NativeKafkaConfigurationProperties.NATIVE_KAFKA_BEAN_NAME;
import static org.assertj.core.api.Assertions.assertThat;

@Order(6)
@TestPropertySource(properties = {
    "embedded.kafka.waitTimeoutInSeconds=120",
    "embedded.kafka.fileSystemBind.enabled=true",
    "embedded.kafka.fileSystemBind.dataFolder=${java.io.tmpdir}/embedded-native-kafka-data" // /tmp is more permissible than target
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Test that embedded-native-kafka with filesystem binding")
public class EmbeddedNativeKafkaWithBindingTest extends EmbeddedNativeKafkaTest {

    @AfterAll
    public static void afterAll(@Autowired NativeKafkaConfigurationProperties nativeKafkaProperties, @Qualifier(NATIVE_KAFKA_BEAN_NAME) GenericContainer<?> nativeKafka) throws Exception {
        Path projectDir = projectDir();
        Path nativeKafkaDataFolder = projectDir.resolve(nativeKafkaProperties.getFileSystemBind().getDataFolder());

        assertThat(nativeKafkaDataFolder.toFile()).isNotEmptyDirectory();

        // Delete created files now
        nativeKafka.execInContainer("rm", "-rf", "/tmp/kafka-logs");

        // Delete mounted directories after test run
        Runtime.getRuntime().addShutdownHook(new Thread(DockerClientFactory.TESTCONTAINERS_THREAD_GROUP, () -> {
            PathUtils.recursiveDeleteDir(nativeKafkaDataFolder);
        }));
    }

    protected static Path projectDir() {
        try {
            URI projectDirUri = EmbeddedNativeKafkaWithBindingTest.class.getClassLoader().getResource("").toURI();
            return Paths.get(projectDirUri).getParent().getParent();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}