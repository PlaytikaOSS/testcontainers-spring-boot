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
import org.testcontainers.containers.Network;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.common.utils.ContainerUtils.startAndLogTime;
import static com.playtika.test.kafka.properties.ZookeeperConfigurationProperties.ZOOKEEPER_BEAN_NAME;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "embedded.zookeeper.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZookeeperConfigurationProperties.class)
public class ZookeeperContainerConfiguration {

    public static final String ZOOKEEPER_HOST_NAME = "zookeeper.testcontainer.docker";

    @Bean
    @ConditionalOnMissingBean
    public ZookeeperStatusCheck zookeeperStartupCheckStrategy(ZookeeperConfigurationProperties zookeeperProperties) {
        return new ZookeeperStatusCheck(zookeeperProperties);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(Network.class)
    public Network kafkaNetwork() {
        Network network = Network.newNetwork();
        log.info("Created docker Network id={}", network.getId());
        return network;
    }

    @Bean(name = ZOOKEEPER_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer zookeeper(ZookeeperStatusCheck zookeeperStatusCheck,
                                      ZookeeperConfigurationProperties zookeeperProperties,
                                      ConfigurableEnvironment environment,
                                      Network network) {
        log.info("Starting zookeeper server. Docker image: {}", zookeeperProperties.getDockerImage());

        int mappingPort = zookeeperProperties.getZookeeperPort();
        GenericContainer zookeeper = new FixedHostPortGenericContainer<>(zookeeperProperties.getDockerImage())
                .withLogConsumer(containerLogsConsumer(log))
                .withEnv("ZOOKEEPER_CLIENT_PORT", String.valueOf(mappingPort))
                .withExposedPorts(mappingPort)
                .withFixedExposedPort(mappingPort, mappingPort)
                .withNetwork(network)
                .withNetworkAliases(ZOOKEEPER_HOST_NAME)
                .waitingFor(zookeeperStatusCheck)
                .withStartupTimeout(zookeeperProperties.getTimeoutDuration());

        ZookeeperConfigurationProperties.FileSystemBind fileSystemBind = zookeeperProperties.getFileSystemBind();
        if (fileSystemBind.isEnabled()) {
            String currentTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH-mm-ss-nnnnnnnnn"));

            String dataFolder = fileSystemBind.getDataFolder();
            String zkData = Paths.get(dataFolder, currentTimestamp).toAbsolutePath().toString();
            log.info("Writing zookeeper data to: {}", zkData);

            String txnLogsFolder = fileSystemBind.getTxnLogsFolder();
            String zkTransactionLogs = Paths.get(txnLogsFolder, currentTimestamp).toAbsolutePath().toString();
            log.info("Writing zookeeper transaction logs to: {}", zkTransactionLogs);

            zookeeper.withFileSystemBind(zkData, "/var/lib/zookeeper/data", BindMode.READ_WRITE)
                    .withFileSystemBind(zkTransactionLogs, "/var/lib/zookeeper/log", BindMode.READ_WRITE);
        }
        startAndLogTime(zookeeper);
        registerZookeeperEnvironment(zookeeper, environment, zookeeperProperties);
        return zookeeper;
    }

    private void registerZookeeperEnvironment(GenericContainer zookeeper,
                                              ConfigurableEnvironment environment,
                                              ZookeeperConfigurationProperties zookeeperProperties) {

        Integer port = zookeeper.getMappedPort(zookeeperProperties.getZookeeperPort());
        String host = zookeeper.getContainerIpAddress();

        String zookeeperConnect = String.format("%s:%d", host, port);
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.zookeeper.zookeeperConnect", zookeeperConnect);

        String zookeeperConnectForContainers = String.format("%s:%d", ZOOKEEPER_HOST_NAME, port);
        map.put("embedded.zookeeper.containerZookeeperConnect", zookeeperConnectForContainers);

        log.info("Started zookeeper server. Connection details:  {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedZookeeperInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.trace("embedded.zookeeper.zookeeperConnect: {}", zookeeperConnect);
    }

}