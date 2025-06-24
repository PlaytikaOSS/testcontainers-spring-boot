package com.playtika.testcontainer.nativekafka;

import com.playtika.testcontainer.nativekafka.properties.NativeKafkaConfigurationProperties;
import com.playtika.testcontainer.nativekafka.properties.NativeKafkaConfigurationProperties.TopicConfiguration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.InitializingBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

@Slf4j
@RequiredArgsConstructor
@Getter
public class NativeKafkaTopicsConfigurer implements InitializingBean {
    private static final int DEFAULT_PARTITION_COUNT = 1;

    private final GenericContainer<?> nativeKafka;
    private final NativeKafkaConfigurationProperties nativeKafkaProperties;

    @Override
    public void afterPropertiesSet() {
        createTopics(this.nativeKafkaProperties.getTopicsToCreate(), this.nativeKafkaProperties.getTopicsConfiguration());
    }

    public void createTopics(Collection<String> topics, Collection<TopicConfiguration> topicsConfiguration) {
        Map<String, TopicConfiguration> defaultTopicToTopicConfigurationMap =
                topics.stream()
                      .collect(toMap(topic -> topic,
                                     topic -> new TopicConfiguration(topic, DEFAULT_PARTITION_COUNT)));

        Map<String, TopicConfiguration> topicToTopicConfigurationMap =
                topicsConfiguration.stream()
                                   .collect(toMap(TopicConfiguration::getTopic,
                                                  topicConfiguration -> topicConfiguration));

        defaultTopicToTopicConfigurationMap.putAll(topicToTopicConfigurationMap);

        Collection<TopicConfiguration> topicsConfigurationToCreate = defaultTopicToTopicConfigurationMap.values();

        if (!topicsConfigurationToCreate.isEmpty()) {
            log.info("Creating Native Kafka topics for configuration: {}", topicsConfigurationToCreate);
            createTopicsUsingAdminClient(topicsConfigurationToCreate);
            log.info("Created Native Kafka topics for configuration: {}", topicsConfigurationToCreate);
        }
    }

    private void createTopicsUsingAdminClient(Collection<TopicConfiguration> topicsConfigurationToCreate) {
        Properties adminProps = new Properties();
        adminProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ((KafkaContainer) nativeKafka).getBootstrapServers());

        try (AdminClient adminClient = AdminClient.create(adminProps)) {
            List<NewTopic> newTopics = topicsConfigurationToCreate.stream()
                    .map(config -> new NewTopic(config.getTopic(), config.getPartitions(), (short) 1))
                    .collect(Collectors.toList());

            CreateTopicsResult result = adminClient.createTopics(newTopics);

            // Wait for all topics to be created
            result.all().get();
            log.debug("Successfully created {} topics using Admin API", newTopics.size());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to create topics using Admin API", e);
            throw new RuntimeException("Failed to create topics", e);
        }
    }
}