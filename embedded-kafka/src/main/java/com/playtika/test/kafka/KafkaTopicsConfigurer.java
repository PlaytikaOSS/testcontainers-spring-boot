/*
* The MIT License (MIT)
*
* Copyright (c) 2018 Playtika
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
    private final String kafkaZookeeperConnect;
    private final KafkaConfigurationProperties properties;

    @PostConstruct
    void configure() {
        createTopics(this.properties.getTopicsToCreate());
    }

    public void createTopics(Collection<String> topics) {
        if (!topics.isEmpty()) {
            log.info("Creating Kafka topics: {}", topics);
            for (String topic : topics) {
                String[] createTopicCmd = getCreateTopicCmd(topic, kafkaZookeeperConnect);
                ContainerUtils.execCmd(this.kafka.getDockerClient(), this.kafka.getContainerId(), createTopicCmd);
            }
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
}
