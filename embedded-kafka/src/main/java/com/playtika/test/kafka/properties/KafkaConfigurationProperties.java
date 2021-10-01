package com.playtika.test.kafka.properties;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.common.properties.CommonContainerProperties;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import javax.validation.constraints.AssertTrue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.kafka")
public class KafkaConfigurationProperties extends CommonContainerProperties {

    public static final String KAFKA_BEAN_NAME = "kafka";
    public static final String KAFKA_USER = "alice";
    public static final String KAFKA_PASSWORD = "alice-secret";

    protected String brokerList;
    protected String containerBrokerList;
    protected int brokerPort = 0;
    protected int containerBrokerPort = 0;
    protected int saslPlaintextBrokerPort = 0;
    protected int socketTimeoutMs = 5_000;
    protected int bufferSize = 64 * 1024;

    // https://hub.docker.com/r/confluentinc/cp-kafka
    // https://docs.confluent.io/platform/current/installation/versions-interoperability.html
    protected String dockerImageVersion = "6.2.0";
    /**
     * Default Dockerfile USER since 6.0.0 (was root before)
     */
    protected String dockerUser = "appuser";

    protected Collection<String> topicsToCreate = Collections.emptyList();
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

    /**
     * Kafka container port will be assigned automatically if free port is available.
     * Override this only if you are sure that specified port is free.
     */
    @PostConstruct
    private void init() {
        if (this.brokerPort == 0) {
            this.brokerPort = ContainerUtils.getAvailableMappingPort();
        }

        if (this.containerBrokerPort == 0) {
            this.containerBrokerPort = ContainerUtils.getAvailableMappingPort();
        }

        if (this.saslPlaintextBrokerPort == 0) {
            this.saslPlaintextBrokerPort = ContainerUtils.getAvailableMappingPort();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @With
    public static final class FileSystemBind {
        private boolean enabled = false;
        private String dataFolder = "target/embedded-kafka-data";
    }

    @SuppressWarnings("unused")
    @AssertTrue(message = "embedded.kafka.secureTopics must be listed in embedded.kafka.topicsToCreate")
    private boolean isSecureTopicsConfigurationValid() {
        return topicsToCreate.containsAll(secureTopics);
    }
}
