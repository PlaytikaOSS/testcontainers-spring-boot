package com.playtika.test.kafka.properties;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.common.properties.CommonContainerProperties;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.kafka")
public class KafkaConfigurationProperties extends CommonContainerProperties {

    public static final String KAFKA_BEAN_NAME = "kafka";
    public static final String KAFKA_PLAIN_TEXT_TOXI_PROXY_BEAN_NAME = "kafkaPlainTextContainerProxy";
    public static final String KAFKA_SASL_TOXI_PROXY_BEAN_NAME = "kafkaSaslContainerProxy";
    public static final String KAFKA_USER = "alice";
    public static final String KAFKA_PASSWORD = "alice-secret";

    protected String brokerList;
    protected String containerBrokerList;
    protected int internalBrokerPort = 9092;
    protected int brokerPort = 9093;
    protected int containerBrokerPort = 9094;
    protected int saslPlaintextBrokerPort = 9095;
    protected int toxiProxyContainerBrokerPort = 9096;
    protected int toxiProxySaslPlaintextContainerBrokerPort = 9097;
    protected int socketTimeoutMs = 5_000;
    protected int bufferSize = 64 * 1024;

    /**
     * Default Dockerfile USER since 6.0.0 (was root before)
     */
    protected String dockerUser = "appuser";

    protected Collection<String> topicsToCreate = Collections.emptyList();
    protected Collection<TopicConfiguration> topicsConfiguration = Collections.emptyList();
    Collection<String> secureTopics = Collections.emptyList();

    // https://github.com/apache/kafka/blob/trunk/config/server.properties

    // https://docs.confluent.io/platform/current/installation/configuration/broker-configs.html#brokerconfigs_offsets.topic.replication.factor
    protected transient final int offsetsTopicReplicationFactor = 1;

    // https://docs.confluent.io/platform/current/installation/configuration/broker-configs.html#brokerconfigs_log.flush.interval.messages
    protected transient final int logFlushIntervalMs = 1;

    // https://docs.confluent.io/platform/current/installation/configuration/broker-configs.html#brokerconfigs_replica.socket.timeout.ms
    protected transient final int replicaSocketTimeoutMs = 1000;

    // https://docs.confluent.io/platform/current/installation/configuration/broker-configs.html#brokerconfigs_controller.socket.timeout.ms
    protected transient final int controllerSocketTimeoutMs = 1000;

    protected FileSystemBind fileSystemBind = new FileSystemBind();

    public KafkaConfigurationProperties() {
        this.setCapabilities(Arrays.asList(Capability.NET_ADMIN));
    }

    // https://hub.docker.com/r/confluentinc/cp-kafka
    // https://docs.confluent.io/platform/current/installation/versions-interoperability.html
    @Override
    public String getDefaultDockerImage() {
        return "confluentinc/cp-kafka:7.2.1";
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @With
    public static final class FileSystemBind {
        private boolean enabled = false;
        private String dataFolder = "target/embedded-kafka-data";
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static final class TopicConfiguration {
        private String topic;
        private int partitions;
    }

    @SuppressWarnings("unused")
    @AssertTrue(message = "embedded.kafka.secureTopics must be listed in embedded.kafka.topicsToCreate")
    private boolean isSecureTopicsConfigurationValid() {
        return topicsToCreate.containsAll(secureTopics);
    }
}
