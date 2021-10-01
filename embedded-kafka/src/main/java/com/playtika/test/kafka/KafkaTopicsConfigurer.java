package com.playtika.test.kafka;

import com.playtika.test.kafka.properties.KafkaConfigurationProperties;
import com.playtika.test.kafka.properties.ZookeeperConfigurationProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import javax.annotation.PostConstruct;
import java.util.Collection;

import static com.playtika.test.common.utils.ContainerUtils.executeInContainer;

@Slf4j
@RequiredArgsConstructor
@Getter
public class KafkaTopicsConfigurer {

    private final GenericContainer kafka;
    private final ZookeeperConfigurationProperties zookeeperProperties;
    private final KafkaConfigurationProperties kafkaProperties;

    @PostConstruct
    void configure() {
        createTopics(this.kafkaProperties.getTopicsToCreate());
        restrictTopics(KafkaConfigurationProperties.KAFKA_USER, this.kafkaProperties.getSecureTopics());
    }

    public void createTopics(Collection<String> topics) {
        if (!topics.isEmpty()) {
            log.info("Creating Kafka topics: {}", topics);
            topics.parallelStream()
                    .forEach(this::createTopic);
            log.info("Created Kafka topics: {}", topics);
        }
    }

    private void createTopic(String topic) {
        String[] createTopicCmd = getCreateTopicCmd(topic, zookeeperProperties.getZookeeperConnect());
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

    private String[] getCreateTopicCmd(String topicName, String kafkaZookeeperConnect) {
        return new String[]{
                "kafka-topics",
                "--create", "--topic", topicName,
                "--partitions", "1",
                "--replication-factor", "1",
                "--if-not-exists",
                "--zookeeper", kafkaZookeeperConnect
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
