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
package com.playtika.test.kafka.configuration;

import com.github.dockerjava.api.model.Link;
import com.playtika.test.kafka.KafkaTopicsConfigurer;
import com.playtika.test.kafka.checks.KafkaStartupCheckStrategy;
import com.playtika.test.kafka.properties.KafkaConfigurationProperties;
import com.playtika.test.kafka.properties.ZookeeperConfigurationProperties;
import com.playtika.test.kafka.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

@Configuration
@ConditionalOnProperty(value = "embedded.kafka.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(ZookeeperContainerConfiguration.class)
@EnableConfigurationProperties(KafkaConfigurationProperties.class)
@Slf4j
public class KafkaContainerConfiguration {

    private static final String SINGLE_NODE_KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR = "1";

    @Bean
    public String kafkaZookeeperConnect(GenericContainer zookeeper, ZookeeperConfigurationProperties zookeeperProperties) {
        String zookeeperHostname = ContainerUtils.getContainerHostname(zookeeper);
        return String.format("%s:%d", zookeeperHostname, zookeeperProperties.getMappingPort());
    }

    @Bean
    public KafkaStartupCheckStrategy kafkaStartupCheckStrategy(KafkaConfigurationProperties kafkaProperties) {
        return new KafkaStartupCheckStrategy(kafkaProperties);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public GenericContainer kafka(GenericContainer zookeeper,
                                  KafkaStartupCheckStrategy kafkaStartupCheckStrategy,
                                  String kafkaZookeeperConnect,
                                  KafkaConfigurationProperties kafkaProperties) {
        String zookeeperHostname = ContainerUtils.getContainerHostname(zookeeper);
        int kafkaMappingPort = kafkaProperties.getMappingPort();
        String kafkaAdvertisedListeners = String.format("PLAINTEXT://localhost:%d", kafkaMappingPort);

        String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss-nnnnnnnnn"));
        String kafkaData = Paths.get(kafkaProperties.getDataFileSystemBind(), currentTimestamp).toAbsolutePath().toString();
        log.info("Writing kafka data to: {}", kafkaData);

        return new FixedHostPortGenericContainer<>(kafkaProperties.getDockerImage())
                .withStartupCheckStrategy(kafkaStartupCheckStrategy)
                .withEnv("KAFKA_ZOOKEEPER_CONNECT", kafkaZookeeperConnect)
                .withEnv("KAFKA_BROKER_ID", "-1")
                .withEnv("KAFKA_ADVERTISED_LISTENERS", kafkaAdvertisedListeners)
                .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", SINGLE_NODE_KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR)
                .withFileSystemBind(kafkaData, "/var/lib/kafka/data", BindMode.READ_WRITE)
                .withCreateContainerCmdModifier(cmd -> cmd.withLinks(new Link(zookeeperHostname, zookeeperHostname)))
                .withExposedPorts(kafkaMappingPort)
                .withFixedExposedPort(kafkaMappingPort, kafkaMappingPort);
    }

    @Bean
    public KafkaTopicsConfigurer kafkaConfigurer(GenericContainer kafka, KafkaConfigurationProperties kafkaProperties, String kafkaZookeeperConnect) {
        return new KafkaTopicsConfigurer(kafka, kafkaZookeeperConnect, kafkaProperties);
    }

    @Bean
    public String kafkaBrokerList(ConfigurableEnvironment environment, KafkaConfigurationProperties kafkaProperties) {
        String kafkaBrokerList = String.format("localhost:%d", kafkaProperties.getMappingPort());

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.kafka.brokerList", kafkaBrokerList);
        MapPropertySource propertySource = new MapPropertySource("embeddedKafkaInfo", map);
        environment.getPropertySources().addFirst(propertySource);

        log.trace("embedded.kafka.brokerList: {}", kafkaBrokerList);
        return kafkaBrokerList;
    }
}