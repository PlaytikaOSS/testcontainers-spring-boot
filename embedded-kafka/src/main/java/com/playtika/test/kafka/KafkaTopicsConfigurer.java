/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.playtika.test.kafka;

import com.playtika.test.common.utils.ContainerUtils;
import com.playtika.test.kafka.properties.KafkaConfigurationProperties;
import com.playtika.test.kafka.properties.ZookeeperConfigurationProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;

import javax.annotation.PostConstruct;
import java.util.Collection;

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
        ContainerUtils.ExecCmdResult output = ContainerUtils.execCmd(this.kafka.getDockerClient(), this.kafka.getContainerId(), createTopicCmd);
        log.debug("Topic={} creation cmd='{}' exitCode={} : {}",
                topic, createTopicCmd, output.getExitCode(), output.getOutput());
    }

    private void restrictTopics(String username, Collection<String> topics) {
        if (!topics.isEmpty()) {
            log.info("Creating ACLs for Kafka topics: {}", topics);
            for (String topic : topics) {
                String[] topicConsumerACLsCmd = getTopicConsumerACLCmd(username, topic, zookeeperProperties.getZookeeperConnect());
                String[] topicProducerACLsCmd = getTopicProducerACLCmd(username, topic, zookeeperProperties.getZookeeperConnect());
                ContainerUtils.ExecCmdResult topicConsumerACLsOutput = ContainerUtils.execCmd(this.kafka.getDockerClient(), this.kafka.getContainerId(), topicConsumerACLsCmd);
                ContainerUtils.ExecCmdResult topicProducerACLsOutput = ContainerUtils.execCmd(this.kafka.getDockerClient(), this.kafka.getContainerId(), topicProducerACLsCmd);
                log.debug("Topic={} consumer ACLs cmd='{}' exitCode={} : {}, producer ACLs cmd='{}' exitCode={} : {}",
                        topic, topicConsumerACLsCmd, topicConsumerACLsOutput.getExitCode(), topicConsumerACLsOutput.getOutput(),
                        topicProducerACLsCmd, topicProducerACLsOutput.getExitCode(), topicProducerACLsOutput.getOutput());
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
