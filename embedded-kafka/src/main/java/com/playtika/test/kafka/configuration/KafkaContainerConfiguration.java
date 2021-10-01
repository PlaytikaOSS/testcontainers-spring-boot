package com.playtika.test.kafka.configuration;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.kafka.KafkaTopicsConfigurer;
import com.playtika.test.kafka.checks.KafkaStatusCheck;
import com.playtika.test.kafka.properties.KafkaConfigurationProperties;
import com.playtika.test.kafka.properties.ZookeeperConfigurationProperties;
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
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.MountableFile;

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

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.kafka.properties.KafkaConfigurationProperties.KAFKA_BEAN_NAME;
import static java.lang.String.format;
import static org.testcontainers.containers.KafkaContainer.KAFKA_PORT;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "embedded.kafka.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(value = {KafkaConfigurationProperties.class, ZookeeperConfigurationProperties.class})
public class KafkaContainerConfiguration {

    public static final String KAFKA_HOST_NAME = "kafka-broker.testcontainer.docker";

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(Network.class)
    public Network kafkaNetwork() {
        Network network = Network.newNetwork();
        log.info("Created docker Network id={}", network.getId());
        return network;
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaStatusCheck kafkaStartupCheckStrategy(KafkaConfigurationProperties kafkaProperties) {
        return new KafkaStatusCheck(kafkaProperties);
    }

    @Bean(name = KAFKA_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer kafka(
            KafkaStatusCheck kafkaStatusCheck,
            KafkaConfigurationProperties kafkaProperties,
            ZookeeperConfigurationProperties zookeeperProperties,
            ConfigurableEnvironment environment,
            Network network) {

        int kafkaInternalPort = kafkaProperties.getContainerBrokerPort(); // for access from other containers
        int kafkaExternalPort = kafkaProperties.getBrokerPort();  // for access from host
        int saslPlaintextKafkaExternalPort = kafkaProperties.getSaslPlaintextBrokerPort();

        // Map properties to env variables: https://docs.confluent.io/platform/current/installation/docker/config-reference.html#confluent-ak-configuration
        // All properties: https://docs.confluent.io/platform/current/installation/configuration/
        // Kafka Broker properties: https://docs.confluent.io/platform/current/installation/configuration/broker-configs.html

        String dockerImageVersion = kafkaProperties.getDockerImageVersion();
        log.info("Starting kafka broker. Docker image version: {}", dockerImageVersion);

        KafkaContainer kafka = new KafkaContainer(dockerImageVersion) {
            @Override
            public String getBootstrapServers() {
                super.getBootstrapServers();
                return "EXTERNAL_PLAINTEXT://" + getHost() + ":" + getMappedPort(kafkaExternalPort) + "," +
                        "EXTERNAL_SASL_PLAINTEXT://" + getHost() + ":" + getMappedPort(saslPlaintextKafkaExternalPort) + "," +
                        "INTERNAL_PLAINTEXT://" + KAFKA_HOST_NAME + ":" + kafkaInternalPort;
            }
        }
                .withCreateContainerCmdModifier(cmd -> cmd.withUser(kafkaProperties.getDockerUser()))
                .withCreateContainerCmdModifier(cmd -> cmd.withHostName(KAFKA_HOST_NAME))
                .withEmbeddedZookeeper()
                .withEnv("KAFKA_BROKER_ID", "-1")
                //see: https://stackoverflow.com/questions/41868161/kafka-in-kubernetes-cluster-how-to-publish-consume-messages-from-outside-of-kub
                //see: https://github.com/wurstmeister/kafka-docker/blob/master/README.md
                // order matters: external then internal since kafka.client.ClientUtils.getPlaintextBrokerEndPoints take first for simple consumers
                .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP",
                        "EXTERNAL_PLAINTEXT:PLAINTEXT," +
                                "EXTERNAL_SASL_PLAINTEXT:SASL_PLAINTEXT," +
                                "INTERNAL_PLAINTEXT:PLAINTEXT," +
                                "BROKER:PLAINTEXT"
                )
                .withEnv("KAFKA_LISTENERS",
                        "EXTERNAL_PLAINTEXT://0.0.0.0:" + kafkaExternalPort + "," +
                                "EXTERNAL_SASL_PLAINTEXT://0.0.0.0:" + saslPlaintextKafkaExternalPort + "," +
                                "INTERNAL_PLAINTEXT://0.0.0.0:" + kafkaInternalPort + "," +
                                "BROKER://0.0.0.0:9092"
                )
                .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "INTERNAL_PLAINTEXT")
                .withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1")
                .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", String.valueOf(kafkaProperties.getOffsetsTopicReplicationFactor()))
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
                .withEnv("KAFKA_CONFLUENT_SUPPORT_METRICS_ENABLE", "false")
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_LOG_FLUSH_INTERVAL_MS", String.valueOf(kafkaProperties.getLogFlushIntervalMs()))
                .withEnv("KAFKA_REPLICA_SOCKET_TIMEOUT_MS", String.valueOf(kafkaProperties.getReplicaSocketTimeoutMs()))
                .withEnv("KAFKA_CONTROLLER_SOCKET_TIMEOUT_MS", String.valueOf(kafkaProperties.getControllerSocketTimeoutMs()))
                .withEnv("KAFKA_SASL_ENABLED_MECHANISMS", "PLAIN")
                .withEnv("ZOOKEEPER_SASL_ENABLED", "false")
                .withCopyFileToContainer(MountableFile.forClasspathResource("kafka_server_jaas.conf"), "/etc/kafka/kafka_server_jaas.conf")
                .withEnv("KAFKA_OPTS", "-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf")
                .withEnv("KAFKA_GC_LOG_OPTS", "-Dnogclog")
                .withExposedPorts(kafkaInternalPort, kafkaExternalPort, saslPlaintextKafkaExternalPort, KAFKA_PORT)
                .withNetwork(network)
                .withNetworkAliases(KAFKA_HOST_NAME)
                .withExtraHost(KAFKA_HOST_NAME, "127.0.0.1")
                .waitingFor(kafkaStatusCheck);

        kafkaFileSystemBind(kafkaProperties, kafka);
        zookeperFileSystemBind(zookeeperProperties, kafka);

        kafka = (KafkaContainer) configureCommonsAndStart(kafka, kafkaProperties, log);
        registerKafkaEnvironment(kafka, environment, kafkaProperties);
        return kafka;
    }

    private void kafkaFileSystemBind(KafkaConfigurationProperties kafkaProperties, KafkaContainer kafka) {
        KafkaConfigurationProperties.FileSystemBind fileSystemBind = kafkaProperties.getFileSystemBind();
        if (fileSystemBind.isEnabled()) {
            String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss-nnnnnnnnn"));
            String dataFolder = fileSystemBind.getDataFolder();
            Path kafkaData = Paths.get(dataFolder, currentTimestamp).toAbsolutePath();
            log.info("Writing kafka data to: {}", kafkaData);
            createPathAndParentOrMakeWritable(kafkaData);

            kafka.addFileSystemBind(kafkaData.toString(), "/var/lib/kafka/data", BindMode.READ_WRITE);
        }
    }

    private void zookeperFileSystemBind(ZookeeperConfigurationProperties zookeeperProperties, KafkaContainer kafka) {
        ZookeeperConfigurationProperties.FileSystemBind zookeeperFileSystemBind = zookeeperProperties.getFileSystemBind();
        if (zookeeperFileSystemBind.isEnabled()) {
            String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss-nnnnnnnnn"));

            String dataFolder = zookeeperFileSystemBind.getDataFolder();
            Path zkData = Paths.get(dataFolder, currentTimestamp).toAbsolutePath();
            log.info("Writing zookeeper data to: {}", zkData);

            String txnLogsFolder = zookeeperFileSystemBind.getTxnLogsFolder();
            Path zkTransactionLogs = Paths.get(txnLogsFolder, currentTimestamp).toAbsolutePath();
            log.info("Writing zookeeper transaction logs to: {}", zkTransactionLogs);

            createPathAndParentOrMakeWritable(zkData);
            kafka.addFileSystemBind(zkData.toString(), "/var/lib/zookeeper/data", BindMode.READ_WRITE);
            createPathAndParentOrMakeWritable(zkTransactionLogs);
            kafka.addFileSystemBind(zkTransactionLogs.toString(), "/var/lib/zookeeper/log", BindMode.READ_WRITE);
        }
    }

    private void registerKafkaEnvironment(GenericContainer kafka,
                                          ConfigurableEnvironment environment,
                                          KafkaConfigurationProperties kafkaProperties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        String host = kafka.getContainerIpAddress();
        Integer mappedBrokerPort = kafka.getMappedPort(kafkaProperties.getBrokerPort());
        String kafkaBrokerList = format("%s:%d", host, mappedBrokerPort);
        map.put("embedded.kafka.brokerList", kafkaBrokerList);

        Integer mappedSaslBrokerPort = kafka.getMappedPort(kafkaProperties.getSaslPlaintextBrokerPort());
        String saslPlaintextKafkaBrokerList = format("%s:%d", host, mappedSaslBrokerPort);
        map.put("embedded.kafka.saslPlaintext.brokerList", saslPlaintextKafkaBrokerList);
        map.put("embedded.kafka.saslPlaintext.user", KafkaConfigurationProperties.KAFKA_USER);
        map.put("embedded.kafka.saslPlaintext.password", KafkaConfigurationProperties.KAFKA_PASSWORD);

        Integer containerPort = kafkaProperties.getContainerBrokerPort();
        String kafkaBrokerListForContainers = format("%s:%d", KAFKA_HOST_NAME, containerPort);
        map.put("embedded.kafka.containerBrokerList", kafkaBrokerListForContainers);

        MapPropertySource propertySource = new MapPropertySource("embeddedKafkaInfo", map);

        log.info("Started kafka broker. Connection details: {}", map);

        environment.getPropertySources().addFirst(propertySource);
    }

    @Bean
    public KafkaTopicsConfigurer kafkaConfigurer(
            GenericContainer kafka,
            KafkaConfigurationProperties kafkaProperties,
            ZookeeperConfigurationProperties zookeeperProperties
    ) {
        return new KafkaTopicsConfigurer(kafka, zookeeperProperties, kafkaProperties);
    }

    /**
     * Create folder (and parent folder if necessary) with write permissions for current user
     *
     * @param path folder to create
     */
    private void createPathAndParentOrMakeWritable(Path path) {
        Stream.of(path.getParent(), path).forEach(p -> {
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
        });
    }

    /**
     * Make folder writable
     *
     * @param path folder to make writable
     */
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
