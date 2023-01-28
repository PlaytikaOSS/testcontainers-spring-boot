package com.playtika.test.kafka;

import com.playtika.test.kafka.properties.KafkaConfigurationProperties;
import com.playtika.test.kafka.properties.KafkaConfigurationProperties.TopicConfiguration;
import com.playtika.test.kafka.properties.ZookeeperConfigurationProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import java.util.Collection;
import java.util.Map;

import static com.playtika.test.common.utils.ContainerUtils.executeInContainer;
import static com.playtika.test.kafka.configuration.KafkaContainerConfiguration.KAFKA_HOST_NAME;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

@Slf4j
@RequiredArgsConstructor
@Getter
public class KafkaTopicsConfigurer implements InitializingBean {
    private static final int DEFAULT_PARTITION_COUNT = 1;

    private final GenericContainer<?> kafka;
    private final ZookeeperConfigurationProperties zookeeperProperties;
    private final KafkaConfigurationProperties kafkaProperties;

    @Override
    public void afterPropertiesSet() {
        createTopics(this.kafkaProperties.getTopicsToCreate(), this.kafkaProperties.getTopicsConfiguration());
        restrictTopics(KafkaConfigurationProperties.KAFKA_USER, this.kafkaProperties.getSecureTopics());
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
            log.info("Creating Kafka topics for configuration: {}", topicsConfigurationToCreate);
            topicsConfigurationToCreate.parallelStream()
                                       .forEach(this::createTopic);
            log.info("Created Kafka topics for configuration: {}", topicsConfigurationToCreate);
        }
    }

    private void createTopic(TopicConfiguration topicConfiguration) {
        String topic = topicConfiguration.getTopic();
        int partitions = topicConfiguration.getPartitions();
        String containerBrokerList = format("%s:%d", KAFKA_HOST_NAME, kafkaProperties.getInternalBrokerPort());
        String[] createTopicCmd = getCreateTopicCmd(topic, partitions, containerBrokerList);
        Container.ExecResult execResult = executeInContainer(this.kafka, createTopicCmd);
        log.debug("Topic={} creation cmd='{}' execResult={}", topic, createTopicCmd, execResult);
    }

    private void restrictTopics(String username, Collection<String> topics) {
        if (!topics.isEmpty()) {
            log.info("Creating ACLs for Kafka topics: {}", topics);
            for (String topic : topics) {
                String[] topicConsumerACLsCmd = getTopicConsumerACLCmd(username, topic, zookeeperProperties.getZookeeperConnect());
                String[] topicProducerACLsCmd = getTopicProducerACLCmd(username, topic, zookeeperProperties.getZookeeperConnect());
                Container.ExecResult topicConsumerACLsOutput = executeInContainer(this.kafka, topicConsumerACLsCmd);
                Container.ExecResult topicProducerACLsOutput = executeInContainer(this.kafka, topicProducerACLsCmd);
                log.debug("Topic={} consumer ACLs cmd='{}' execResult={}, producer ACLs cmd='{}' execResult={}",
                          topic, topicConsumerACLsCmd, topicConsumerACLsOutput,
                          topicProducerACLsCmd, topicProducerACLsOutput.getExitCode());
            }
            log.info("Created ACLs for Kafka topics: {}", topics);
        }
    }

    private String[] getCreateTopicCmd(String topicName, int partitions, String kafkaContainerBrokerList) {
        return new String[]{
                "kafka-topics",
                "--create", "--topic", topicName,
                "--partitions", String.valueOf(partitions),
                "--replication-factor", "1",
                "--if-not-exists",
                "--bootstrap-server", kafkaContainerBrokerList
        };
    }

    private String[] getTopicConsumerACLCmd(String username, String topicName, String kafkaZookeeperConnect) {
        return new String[]{
                "kafka-acls",
                "--authorizer-properties",
                "zookeeper.connect=" + kafkaZookeeperConnect,
                "--add", "--allow-principal", "User:" + username,
                "--consumer", "--topic", topicName,
                "--group", "*"
        };
    }

    private String[] getTopicProducerACLCmd(String username, String topicName, String kafkaZookeeperConnect) {
        return new String[]{
                "kafka-acls",
                "--authorizer-properties",
                "zookeeper.connect=" + kafkaZookeeperConnect,
                "--add", "--allow-principal", "User:" + username,
                "--producer", "--topic", topicName
        };
    }


}
