package com.playtika.testcontainer.nativekafka.configuration;

import com.playtika.testcontainer.nativekafka.NativeKafkaTopicsConfigurer;
import com.playtika.testcontainer.nativekafka.properties.NativeKafkaConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Stream;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.nativekafka.properties.NativeKafkaConfigurationProperties.NATIVE_KAFKA_BEAN_NAME;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "embedded.nativekafka.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(value = {NativeKafkaConfigurationProperties.class})
public class NativeKafkaContainerConfiguration {

    public static final String NATIVE_KAFKA_HOST_NAME = "native-kafka-broker.testcontainer.docker";

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(Network.class)
    public Network nativeKafkaNetwork() {
        Network network = Network.newNetwork();
        log.info("Created docker Network id={}", network.getId());
        return network;
    }

    @Bean(name = NATIVE_KAFKA_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer<?> nativeKafka(
            NativeKafkaConfigurationProperties nativeKafkaProperties,
            ConfigurableEnvironment environment,
            Network network) {

        DockerImageName nativeKafkaImageName = DockerImageName.parse(nativeKafkaProperties.getDefaultDockerImage())
                .asCompatibleSubstituteFor("confluentinc/cp-kafka");

        KafkaContainer nativeKafka = new KafkaContainer(nativeKafkaImageName)
                .withNetwork(network)
                .withNetworkAliases(NATIVE_KAFKA_HOST_NAME)
                .withExtraHost(NATIVE_KAFKA_HOST_NAME, "127.0.0.1");

        // Configure file system bind if enabled
        configureFileSystemBind(nativeKafkaProperties, nativeKafka);

        // Configure and start the container using common utilities
        nativeKafka = (KafkaContainer) configureCommonsAndStart(nativeKafka, nativeKafkaProperties, log);

        // Register environment properties
        registerNativeKafkaEnvironment(nativeKafka, environment, nativeKafkaProperties);

        return nativeKafka;
    }

    @Bean
    @ConditionalOnMissingBean
    public NativeKafkaTopicsConfigurer nativeKafkaTopicsConfigurer(
            GenericContainer<?> nativeKafka,
            NativeKafkaConfigurationProperties nativeKafkaProperties) {
        return new NativeKafkaTopicsConfigurer(nativeKafka, nativeKafkaProperties);
    }

    private void configureFileSystemBind(NativeKafkaConfigurationProperties nativeKafkaProperties, KafkaContainer nativeKafka) {
        NativeKafkaConfigurationProperties.FileSystemBind fileSystemBind = nativeKafkaProperties.getFileSystemBind();
        if (fileSystemBind.isEnabled()) {
            String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss-nnnnnnnnn"));
            String dataFolder = fileSystemBind.getDataFolder();
            Path kafkaData = Paths.get(dataFolder, currentTimestamp).toAbsolutePath();
            log.info("Writing native kafka data to: {}", kafkaData);
            createPathAndParentOrMakeWritable(kafkaData);

            nativeKafka.addFileSystemBind(kafkaData.toString(), "/tmp/kafka-logs", BindMode.READ_WRITE);
        }
    }

    private void registerNativeKafkaEnvironment(GenericContainer<?> nativeKafka,
                                              ConfigurableEnvironment environment,
                                              NativeKafkaConfigurationProperties nativeKafkaProperties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        String bootstrapServers = ((KafkaContainer) nativeKafka).getBootstrapServers();
        map.put("embedded.nativekafka.bootstrapServers", bootstrapServers);
        map.put("embedded.nativekafka.brokerList", bootstrapServers);
        map.put("embedded.nativekafka.networkAlias", NATIVE_KAFKA_HOST_NAME);
        map.put("embedded.nativekafka.host", nativeKafka.getHost());
        map.put("embedded.nativekafka.port", nativeKafka.getMappedPort(nativeKafkaProperties.getKafkaPort()));

        MapPropertySource propertySource = new MapPropertySource("embeddedNativeKafkaInfo", map);

        log.info("Started native kafka broker. Connection details: {}", map);

        environment.getPropertySources().addFirst(propertySource);
    }

    private void createPathAndParentOrMakeWritable(Path path) {
        Stream.of(path.getParent(), path).forEach(p -> {
            if (p != null) {
                if (p.toFile().isDirectory()) {
                    makeWritable(p);
                } else {
                    try {
                        log.info("Create writable folder: {}", p);
                        Files.createDirectory(p, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxrwxrwx")));
                    } catch (FileAlreadyExistsException e) {
                        makeWritable(p);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    private void makeWritable(Path path) {
        PosixFileAttributeView fileAttributeView = Files.getFileAttributeView(path, PosixFileAttributeView.class);
        if (fileAttributeView == null) {
            log.warn("Couldn't get file permissions: {}", path);
            return;
        }
        try {
            Set<PosixFilePermission> permissions = fileAttributeView.readAttributes().permissions();
            if (permissions.add(PosixFilePermission.OTHERS_WRITE)) {
                log.info("Make writable to others: {}", path);
                fileAttributeView.setPermissions(permissions);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}