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

import com.playtika.test.kafka.checks.ZookeeperStatusCheck;
import com.playtika.test.kafka.properties.ZookeeperConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
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

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.kafka.properties.ZookeeperConfigurationProperties.ZOOKEEPER_BEAN_NAME;

@Configuration
@ConditionalOnProperty(value = "embedded.zookeeper.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZookeeperConfigurationProperties.class)
@Slf4j
public class ZookeeperContainerConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ZookeeperStatusCheck zookeeperStartupCheckStrategy(ZookeeperConfigurationProperties zookeeperProperties) {
        return new ZookeeperStatusCheck(zookeeperProperties);
    }

    @Bean(name = ZOOKEEPER_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer zookeeper(ZookeeperStatusCheck zookeeperStatusCheck,
                                      ZookeeperConfigurationProperties zookeeperProperties,
                                      ConfigurableEnvironment environment) throws IOException {
        String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss-nnnnnnnnn"));
        String zkData = Paths.get(zookeeperProperties.getDataFileSystemBind(), currentTimestamp).toAbsolutePath().toString();
        log.info("Writing zookeeper data to: {}", zkData);
        String zkTransactionLogs = Paths.get(zookeeperProperties.getTxnLogsFileSystemBind(), currentTimestamp).toAbsolutePath().toString();
        log.info("Writing zookeeper transaction logs to: {}", zkTransactionLogs);

        log.info("Starting zookeeper server. Docker image: {}", zookeeperProperties.getDockerImage());

        int mappingPort = zookeeperProperties.getZookeeperPort();
        GenericContainer zookeeper = new FixedHostPortGenericContainer<>(zookeeperProperties.getDockerImage())
                .withStartupCheckStrategy(zookeeperStatusCheck)
                .withLogConsumer(containerLogsConsumer(log))
                .withEnv("ZOOKEEPER_CLIENT_PORT", String.valueOf(mappingPort))
                .withFileSystemBind(zkData, "/var/lib/zookeeper/data", BindMode.READ_WRITE)
                .withFileSystemBind(zkTransactionLogs, "/var/lib/zookeeper/log", BindMode.READ_WRITE)
                .withExposedPorts(mappingPort)
                .withFixedExposedPort(mappingPort, mappingPort);
        zookeeper.start();

        registerZookeperEnvironment(zookeeper, environment, zookeeperProperties);
        return zookeeper;
    }

    private void registerZookeperEnvironment(GenericContainer zookeeper,
                                             ConfigurableEnvironment environment,
                                             ZookeeperConfigurationProperties zookeeperProperties) {
        Integer port = zookeeper.getMappedPort(zookeeperProperties.getZookeeperPort());
        String host = zookeeper.getContainerIpAddress();

        String zookeeperConnect = String.format("%s:%d", host, port);
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.zookeeper.zookeeperConnect", zookeeperConnect);
        MapPropertySource propertySource = new MapPropertySource("embeddedZookeeperInfo", map);

        log.info("Started zookeeper server. Connection details:  {}", map);

        environment.getPropertySources().addFirst(propertySource);
        log.trace("embedded.zookeeper.zookeeperConnect: {}", zookeeperConnect);
    }
}