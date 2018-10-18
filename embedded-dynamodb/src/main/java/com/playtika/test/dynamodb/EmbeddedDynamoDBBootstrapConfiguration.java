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
package com.playtika.test.dynamodb;


import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Slf4j
@Configuration
@Order(HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "embedded.dynamodb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(DynamoDBProperties.class)
public class EmbeddedDynamoDBBootstrapConfiguration {

    @Bean(name = DynamoDBProperties.BEAN_NAME_EMBEDDED_DYNAMODB, destroyMethod = "stop")
    public GenericContainer mariadb(ConfigurableEnvironment environment,
                                    DynamoDBProperties properties) throws Exception {
        log.info("Starting mariadb server. Docker image: {}", properties.dockerImage);

        GenericContainer mariadb =
                new GenericContainer(properties.dockerImage)
                        .withLogConsumer(containerLogsConsumer(log))
                        .withExposedPorts(properties.port)
                        .waitingFor(new HostPortWaitStrategy())
                        .withStartupTimeout(properties.getTimeoutDuration());

        mariadb.start();

        registerDynamodbEnvironment(mariadb, environment, properties);
        return mariadb;
    }

    private void registerDynamodbEnvironment(GenericContainer mariadb,
                                             ConfigurableEnvironment environment,
                                             DynamoDBProperties properties) {
        Integer mappedPort = mariadb.getMappedPort(properties.port);
        String host = mariadb.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.dynamodb.port", mappedPort);
        map.put("embedded.dynamodb.host", host);
        map.put("embedded.dynamodb.accessKey", properties.getAccessKey());
        map.put("embedded.dynamodb.secretKey", properties.getSecretKey());

        log.info("Started dynamodb server. Connection details: {}, ", map);
        log.info("Consult with the doc " +
                "https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html " +
                "for more details");

        MapPropertySource propertySource = new MapPropertySource("embeddedDynamodbInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
