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
package com.playtika.test.kafka.properties;

import com.playtika.test.common.properties.CommonContainerProperties;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import javax.validation.constraints.AssertTrue;
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
    protected String dockerImageVersion = "5.5.1";
    protected Collection<String> topicsToCreate = Collections.emptyList();
    Collection<String> secureTopics = Collections.emptyList();
    protected transient final int replicationFactor = 1;
    //https://github.com/kafka-dev/kafka/blob/0.6.1/core/src/test/scala/unit/kafka/utils/TestUtils.scala#L114
    protected transient final int logFlushIntervalMs = 1;
    //https://github.com/spring-projects/spring-kafka/blob/v1.3.5.RELEASE/spring-kafka-test/src/main/java/org/springframework/kafka/test/rule/KafkaEmbedded.java#L193
    protected transient final int replicaSocketTimeoutMs = 1000;
    protected transient final int controllerSocketTimeoutMs = 1000;

    protected FileSystemBind fileSystemBind = new FileSystemBind();

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

    @Data
    public static final class FileSystemBind {
        private boolean enabled = true;
        private String dataFolder = "target/embedded-kafka-data";
    }

    @SuppressWarnings("unused")
    @AssertTrue(message = "embedded.kafka.secureTopics must be listed in embedded.kafka.topicsToCreate")
    private boolean isSecureTopicsConfigurationValid() {
        return topicsToCreate.containsAll(secureTopics);
    }
}