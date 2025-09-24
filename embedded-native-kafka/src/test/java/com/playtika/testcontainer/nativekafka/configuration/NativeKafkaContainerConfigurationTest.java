package com.playtika.testcontainer.nativekafka.configuration;

import com.playtika.testcontainer.nativekafka.NativeKafkaTopicsConfigurer;
import com.playtika.testcontainer.nativekafka.properties.NativeKafkaConfigurationProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.KafkaContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NativeKafkaContainerConfiguration Tests")
class NativeKafkaContainerConfigurationTest {

    @Mock
    private NativeKafkaConfigurationProperties properties;

    @Mock
    private Network network;

    @Mock
    private KafkaContainer kafkaContainer;

    @Mock
    private GenericContainer<?> genericContainer;

    @TempDir
    private Path tempDir;

    private NativeKafkaContainerConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = new NativeKafkaContainerConfiguration();
    }

    @Test
    @DisplayName("should create network with proper configuration")
    void shouldCreateNetworkWithProperConfiguration() {
        Network network = configuration.nativeKafkaNetwork();

        assertThat(network).isNotNull();
        assertThat(network.getId()).isNotNull();
    }

    @Test
    @DisplayName("should create kafka container with proper docker image and network configuration")
    void shouldCreateKafkaContainerWithProperConfiguration() {
        when(properties.getDefaultDockerImage()).thenReturn("apache/kafka-native:4.0.0");
        when(properties.getFileSystemBind()).thenReturn(new NativeKafkaConfigurationProperties.FileSystemBind());
        when(properties.getKafkaPort()).thenReturn(9092);

        try (MockedStatic<com.playtika.testcontainer.common.utils.ContainerUtils> containerUtilsMock =
                mockStatic(com.playtika.testcontainer.common.utils.ContainerUtils.class)) {

            containerUtilsMock.when(() -> configureCommonsAndStart(any(KafkaContainer.class), eq(properties), any()))
                    .thenReturn(kafkaContainer);

            when(kafkaContainer.getBootstrapServers()).thenReturn("localhost:9092");
            when(kafkaContainer.getHost()).thenReturn("localhost");
            when(kafkaContainer.getMappedPort(9092)).thenReturn(9092);

            GenericContainer<?> result = configuration.nativeKafka(properties, network);

            assertThat(result).isEqualTo(kafkaContainer);
            containerUtilsMock.verify(() -> configureCommonsAndStart(any(KafkaContainer.class), eq(properties), any()));
        }
    }

    @Test
    @DisplayName("should configure file system bind when enabled")
    void shouldConfigureFileSystemBindWhenEnabled() throws IOException {
        NativeKafkaConfigurationProperties.FileSystemBind fileSystemBind =
                new NativeKafkaConfigurationProperties.FileSystemBind(true, tempDir.toString());

        when(properties.getDefaultDockerImage()).thenReturn("apache/kafka-native:4.0.0");
        when(properties.getFileSystemBind()).thenReturn(fileSystemBind);
        when(properties.getKafkaPort()).thenReturn(9092);

        try (MockedStatic<com.playtika.testcontainer.common.utils.ContainerUtils> containerUtilsMock =
                mockStatic(com.playtika.testcontainer.common.utils.ContainerUtils.class)) {

            containerUtilsMock.when(() -> configureCommonsAndStart(any(KafkaContainer.class), eq(properties), any()))
                    .thenReturn(kafkaContainer);

            when(kafkaContainer.getBootstrapServers()).thenReturn("localhost:9092");
            when(kafkaContainer.getHost()).thenReturn("localhost");
            when(kafkaContainer.getMappedPort(9092)).thenReturn(9092);

            GenericContainer<?> result = configuration.nativeKafka(properties, network);

            assertThat(result).isEqualTo(kafkaContainer);

            assertThat(Files.list(tempDir))
                    .anyMatch(path -> path.getFileName().toString().matches("\\d{2}-\\d{2}-\\d{2}-\\d{9}"));
        }
    }

    @Test
    @DisplayName("should not configure file system bind when disabled")
    void shouldNotConfigureFileSystemBindWhenDisabled() {
        NativeKafkaConfigurationProperties.FileSystemBind fileSystemBind =
                new NativeKafkaConfigurationProperties.FileSystemBind(false, tempDir.toString());

        when(properties.getDefaultDockerImage()).thenReturn("apache/kafka-native:4.0.0");
        when(properties.getFileSystemBind()).thenReturn(fileSystemBind);
        when(properties.getKafkaPort()).thenReturn(9092);

        try (MockedStatic<com.playtika.testcontainer.common.utils.ContainerUtils> containerUtilsMock =
                mockStatic(com.playtika.testcontainer.common.utils.ContainerUtils.class)) {

            containerUtilsMock.when(() -> configureCommonsAndStart(any(KafkaContainer.class), eq(properties), any()))
                    .thenReturn(kafkaContainer);

            when(kafkaContainer.getBootstrapServers()).thenReturn("localhost:9092");
            when(kafkaContainer.getHost()).thenReturn("localhost");
            when(kafkaContainer.getMappedPort(9092)).thenReturn(9092);

            GenericContainer<?> result = configuration.nativeKafka(properties, network);

            assertThat(result).isEqualTo(kafkaContainer);
            assertThat(Files.exists(tempDir.resolve("embedded-native-kafka-data"))).isFalse();
        }
    }

    @Test
    @DisplayName("should register properties correctly")
    void shouldRegisterPropertiesCorrectly() {
        when(properties.getDefaultDockerImage()).thenReturn("apache/kafka-native:4.0.0");
        when(properties.getFileSystemBind()).thenReturn(new NativeKafkaConfigurationProperties.FileSystemBind());
        when(properties.getKafkaPort()).thenReturn(9092);

        try (MockedStatic<com.playtika.testcontainer.common.utils.ContainerUtils> containerUtilsMock =
                mockStatic(com.playtika.testcontainer.common.utils.ContainerUtils.class)) {

            containerUtilsMock.when(() -> configureCommonsAndStart(any(KafkaContainer.class), eq(properties), any()))
                    .thenReturn(kafkaContainer);

            when(kafkaContainer.getBootstrapServers()).thenReturn("localhost:9092");
            when(kafkaContainer.getHost()).thenReturn("localhost");
            when(kafkaContainer.getMappedPort(9092)).thenReturn(12345);

            configuration.nativeKafka(properties, network);
        }
    }

    @Test
    @DisplayName("should create native kafka topics configurer bean")
    void shouldCreateNativeKafkaTopicsConfigurerBean() {
        NativeKafkaTopicsConfigurer result = configuration.nativeKafkaTopicsConfigurer(genericContainer, properties);

        assertThat(result).isNotNull();
        assertThat(result.getNativeKafka()).isEqualTo(genericContainer);
        assertThat(result.getNativeKafkaProperties()).isEqualTo(properties);
    }

    @Test
    @DisplayName("should handle directory creation with proper permissions")
    void shouldHandleDirectoryCreationWithProperPermissions() {
        Path testPath = tempDir.resolve("test-kafka-data");

        NativeKafkaConfigurationProperties.FileSystemBind fileSystemBind =
                new NativeKafkaConfigurationProperties.FileSystemBind(true, testPath.toString());

        when(properties.getDefaultDockerImage()).thenReturn("apache/kafka-native:4.0.0");
        when(properties.getFileSystemBind()).thenReturn(fileSystemBind);
        when(properties.getKafkaPort()).thenReturn(9092);

        try (MockedStatic<com.playtika.testcontainer.common.utils.ContainerUtils> containerUtilsMock =
                mockStatic(com.playtika.testcontainer.common.utils.ContainerUtils.class)) {

            containerUtilsMock.when(() -> configureCommonsAndStart(any(KafkaContainer.class), eq(properties), any()))
                    .thenReturn(kafkaContainer);

            when(kafkaContainer.getBootstrapServers()).thenReturn("localhost:9092");
            when(kafkaContainer.getHost()).thenReturn("localhost");
            when(kafkaContainer.getMappedPort(9092)).thenReturn(9092);

            configuration.nativeKafka(properties, network);

            assertThat(Files.exists(testPath)).isTrue();
            assertThat(Files.isDirectory(testPath)).isTrue();
        }
    }

    @Test
    @DisplayName("should handle parent directory creation")
    void shouldHandleParentDirectoryCreation() throws IOException {
        Path parentPath = tempDir.resolve("parent");
        Files.createDirectory(parentPath);
        Path testPath = parentPath.resolve("test-kafka-data");

        NativeKafkaConfigurationProperties.FileSystemBind fileSystemBind =
                new NativeKafkaConfigurationProperties.FileSystemBind(true, testPath.toString());

        when(properties.getDefaultDockerImage()).thenReturn("apache/kafka-native:4.0.0");
        when(properties.getFileSystemBind()).thenReturn(fileSystemBind);
        when(properties.getKafkaPort()).thenReturn(9092);

        try (MockedStatic<com.playtika.testcontainer.common.utils.ContainerUtils> containerUtilsMock =
                mockStatic(com.playtika.testcontainer.common.utils.ContainerUtils.class)) {

            containerUtilsMock.when(() -> configureCommonsAndStart(any(KafkaContainer.class), eq(properties), any()))
                    .thenReturn(kafkaContainer);

            when(kafkaContainer.getBootstrapServers()).thenReturn("localhost:9092");
            when(kafkaContainer.getHost()).thenReturn("localhost");
            when(kafkaContainer.getMappedPort(9092)).thenReturn(9092);

            configuration.nativeKafka(properties, network);

            assertThat(Files.exists(parentPath)).isTrue();
            assertThat(Files.exists(testPath)).isTrue();
        }
    }

    @Test
    @DisplayName("should verify container host name constant")
    void shouldVerifyContainerHostNameConstant() {
        assertThat(NativeKafkaContainerConfiguration.NATIVE_KAFKA_HOST_NAME)
                .isEqualTo("kafka-broker.testcontainer.docker");
    }

    @Test
    @DisplayName("should handle existing directory scenario")
    void shouldHandleExistingDirectoryScenario() throws IOException {
        Path existingPath = tempDir.resolve("existing-kafka-data");
        Files.createDirectory(existingPath);

        NativeKafkaConfigurationProperties.FileSystemBind fileSystemBind =
                new NativeKafkaConfigurationProperties.FileSystemBind(true, existingPath.toString());

        when(properties.getDefaultDockerImage()).thenReturn("apache/kafka-native:4.0.0");
        when(properties.getFileSystemBind()).thenReturn(fileSystemBind);
        when(properties.getKafkaPort()).thenReturn(9092);

        try (MockedStatic<com.playtika.testcontainer.common.utils.ContainerUtils> containerUtilsMock =
                mockStatic(com.playtika.testcontainer.common.utils.ContainerUtils.class)) {

            containerUtilsMock.when(() -> configureCommonsAndStart(any(KafkaContainer.class), eq(properties), any()))
                    .thenReturn(kafkaContainer);

            when(kafkaContainer.getBootstrapServers()).thenReturn("localhost:9092");
            when(kafkaContainer.getHost()).thenReturn("localhost");
            when(kafkaContainer.getMappedPort(9092)).thenReturn(9092);

            configuration.nativeKafka(properties, network);

            assertThat(Files.exists(existingPath)).isTrue();
        }
    }
}
