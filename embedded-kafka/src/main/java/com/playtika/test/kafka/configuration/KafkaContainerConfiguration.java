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
import com.playtika.test.common.utils.ContainerUtils;
import com.playtika.test.kafka.KafkaTopicsConfigurer;
import com.playtika.test.kafka.checks.KafkaStatusCheck;
import com.playtika.test.kafka.properties.KafkaConfigurationProperties;
import com.playtika.test.kafka.properties.ZookeeperConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.kafka.properties.KafkaConfigurationProperties.KAFKA_BEAN_NAME;
import static java.lang.String.format;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "embedded.kafka.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(ZookeeperContainerConfiguration.class)
@EnableConfigurationProperties(KafkaConfigurationProperties.class)
public class KafkaContainerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public KafkaStatusCheck kafkaStartupCheckStrategy(KafkaConfigurationProperties kafkaProperties) {
        return new KafkaStatusCheck(kafkaProperties);
    }

    @Bean(name = KAFKA_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer kafka(GenericContainer zookeeper,
                                  KafkaStatusCheck kafkaStatusCheck,
                                  KafkaConfigurationProperties kafkaProperties,
                                  ZookeeperConfigurationProperties zookeeperProperties,
                                  ConfigurableEnvironment environment) {

        String zookeeperHostname = ContainerUtils.getContainerHostname(zookeeper);
        int kafkaMappingPort = kafkaProperties.getBrokerPort();
        String kafkaAdvertisedListeners = format("PLAINTEXT://localhost:%d", kafkaMappingPort);

        String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss-nnnnnnnnn"));
        String kafkaData = Paths.get(kafkaProperties.getDataFileSystemBind(), currentTimestamp).toAbsolutePath().toString();
        log.info("Writing kafka data to: {}", kafkaData);

        log.info("Starting kafka broker. Docker image: {}", kafkaProperties.getDockerImage());

        GenericContainer kafka = new FixedHostPortGenericContainer<>(kafkaProperties.getDockerImage())
                .withStartupCheckStrategy(kafkaStatusCheck)
                .withLogConsumer(containerLogsConsumer(log))
                .withEnv("KAFKA_ZOOKEEPER_CONNECT", "zookeeper:" + zookeeperProperties.getZookeeperPort())
                .withEnv("KAFKA_BROKER_ID", "-1")
                .withEnv("KAFKA_ADVERTISED_LISTENERS", kafkaAdvertisedListeners)
                .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", String.valueOf(kafkaProperties.getReplicationFactor()))
                .withFileSystemBind(kafkaData, "/var/lib/kafka/data", BindMode.READ_WRITE)
                .withCreateContainerCmdModifier(cmd -> cmd.withLinks(new Link(zookeeperHostname, "zookeeper")))
                .withExposedPorts(kafkaMappingPort)
                .withFixedExposedPort(kafkaMappingPort, kafkaMappingPort);
        kafka.start();
        registerKafkaEnvironment(kafka, environment, kafkaProperties);
        return kafka;
    }

    private void registerKafkaEnvironment(GenericContainer kafka,
                                          ConfigurableEnvironment environment,
                                          KafkaConfigurationProperties kafkaProperties) {
        Integer mappedPort = kafka.getMappedPort(kafkaProperties.getBrokerPort());
        String host = kafka.getContainerIpAddress();

        String kafkaBrokerList = format("%s:%d", host, mappedPort);

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.kafka.brokerList", kafkaBrokerList);
        MapPropertySource propertySource = new MapPropertySource("embeddedKafkaInfo", map);

        log.info("Started kafka broker. Connection details: {}", map);

        environment.getPropertySources().addFirst(propertySource);
    }

    @Bean
    public KafkaTopicsConfigurer kafkaConfigurer(GenericContainer kafka,
                                                 KafkaConfigurationProperties kafkaProperties,
                                                 ZookeeperConfigurationProperties zookeeperProperties) {
        String zookeeperConnect = format("%s:%d", "zookeeper", zookeeperProperties.getZookeeperPort());
        return new KafkaTopicsConfigurer(kafka, zookeeperConnect, kafkaProperties);
    }
}