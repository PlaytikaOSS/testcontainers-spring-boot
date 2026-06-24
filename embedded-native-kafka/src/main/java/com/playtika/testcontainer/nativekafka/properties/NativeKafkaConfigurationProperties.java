package com.playtika.testcontainer.nativekafka.properties;

import com.github.dockerjava.api.model.Capability;
import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.With;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.kafka")
public class NativeKafkaConfigurationProperties extends CommonContainerProperties {

    public static final String NATIVE_KAFKA_BEAN_NAME = "nativeKafka";
    public static final String NATIVE_KAFKA_PACKAGE_PROPERTIES_BEAN_NAME = "nativeKafkaPackageProperties";

    protected String bootstrapServers;
    protected int kafkaPort = 9092;
    protected int socketTimeoutMs = 5_000;
    protected int bufferSize = 64 * 1024;

    protected Collection<String> topicsToCreate = Collections.emptyList();
    protected Collection<TopicConfiguration> topicsConfiguration = Collections.emptyList();

    protected FileSystemBind fileSystemBind = new FileSystemBind();

    public NativeKafkaConfigurationProperties() {
        this.setCapabilities(List.of(Capability.NET_ADMIN));
    }

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "apache/kafka-native:4.3.1";
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @With
    public static final class FileSystemBind {
        private boolean enabled = false;
        private String dataFolder = "target/embedded-native-kafka-data";
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    public static final class TopicConfiguration {
        private String topic;
        private int partitions;
    }
}