/*
* The MIT License (MIT)
*
* Copyright (c) 2017 Playtika
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

import com.playtika.test.kafka.properties.ZookeeperConfigurationProperties;
import kafka.admin.AdminUtils;
import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import kafka.utils.ZKStringSerializer$;
import org.I0Itec.zkclient.ZkClient;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = EmbeddedKafkaTests.TestConfiguration.class,
        properties = "embedded.kafka.topicsToCreate=autoCreatedTopic"
)
public class EmbeddedKafkaTests {

    private static final String TOPIC = "topic1";
    private static final String MESSAGE = "test message";

    @Autowired
    String kafkaBrokerList;
    @Autowired
    String zookeeperConnect;
    @Autowired
    private ZkClient zkClient;
    @Autowired
    KafkaTopicsConfigurer kafkaTopicsConfigurer;

    @Test
    public void should_autoCreateTopic() throws Exception {
        boolean exists = AdminUtils.topicExists(this.zkClient, "autoCreatedTopic");

        assertTrue("Topic should be pre-created", exists);
    }

    @Test
    public void should_createTopic() throws Exception {
        String topicToCreate = "topicToCreate";
        this.kafkaTopicsConfigurer.createTopics(Collections.singletonList(topicToCreate));

        boolean exists = AdminUtils.topicExists(this.zkClient, topicToCreate);

        assertTrue("Topic should be created", exists);
    }

    @Test
    public void should_sendAndConsumeMessage() throws Exception {
        sendMessage(TOPIC, MESSAGE);

        String consumedMessage = consumeMessage(TOPIC);

        assertThat(consumedMessage).isEqualTo(MESSAGE);
    }

    private void sendMessage(String topic, String message) throws Exception {
        Map<String, Object> producerConfiguration = getKafkaProducerConfiguration();
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(producerConfiguration);

        kafkaProducer.send(new ProducerRecord<>(topic, message)).get();
        kafkaProducer.close();

    }

    private String consumeMessage(String topic) {
        Map<String, Object> consumerConfiguration = getKafkaConsumerConfiguration();
        Properties properties = new Properties();
        properties.putAll(consumerConfiguration);
        ConsumerConnector consumer = Consumer.createJavaConsumerConnector(new ConsumerConfig(properties));

        Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
        topicCountMap.put(topic, 1);
        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicCountMap);
        List<KafkaStream<byte[], byte[]>> streams = consumerMap.get(topic);
        KafkaStream<byte[], byte[]> messageAndMetadatas = streams.get(0);
        MessageAndMetadata<byte[], byte[]> next = messageAndMetadatas.iterator().next();
        consumer.shutdown();
        return new String(next.message());
    }

    private Map<String, Object> getKafkaProducerConfiguration() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(BOOTSTRAP_SERVERS_CONFIG, kafkaBrokerList);
        configs.put(KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        configs.put(VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        return configs;
    }

    private Map<String, Object> getKafkaConsumerConfiguration() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("zookeeper.connect", zookeeperConnect);
        configs.put(GROUP_ID_CONFIG, "testGroup");
        configs.put(AUTO_OFFSET_RESET_CONFIG, "smallest");
        configs.put(KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        configs.put(VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        return configs;
    }

    @Configuration
    @EnableAutoConfiguration
    static class TestConfiguration {

        @Bean(destroyMethod = "close")
        public ZkClient zkClient(String zookeeperConnect, ZookeeperConfigurationProperties zookeeperProperties) {
            return new ZkClient(zookeeperConnect, zookeeperProperties.getSessionTimeoutMs(),
                    zookeeperProperties.getSocketTimeoutMs(), ZKStringSerializer$.MODULE$);
        }
    }
}