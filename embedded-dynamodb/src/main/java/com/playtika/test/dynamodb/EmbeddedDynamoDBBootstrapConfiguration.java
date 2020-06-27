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
package com.playtika.test.dynamodb;


import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.dynamodb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(DynamoDBProperties.class)
public class EmbeddedDynamoDBBootstrapConfiguration {

    @Bean(name = DynamoDBProperties.BEAN_NAME_EMBEDDED_DYNAMODB, destroyMethod = "stop")
    public GenericContainer dynamoDb(ConfigurableEnvironment environment,
                                     DynamoDBProperties properties) throws Exception {
        log.info("Starting DynamoDb server. Docker image: {}", properties.dockerImage);

        GenericContainer container =
                new GenericContainer(properties.dockerImage)
                        .withLogConsumer(containerLogsConsumer(log))
                        .withExposedPorts(properties.port)
                        .waitingFor(new HostPortWaitStrategy())
                        .withReuse(properties.isReuseContainer())
                        .withStartupTimeout(properties.getTimeoutDuration());

        container.start();

        registerDynamodbEnvironment(container, environment, properties);
        return container;
    }

    private void registerDynamodbEnvironment(GenericContainer container,
                                             ConfigurableEnvironment environment,
                                             DynamoDBProperties properties) {
        Integer mappedPort = container.getMappedPort(properties.port);
        String host = container.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.dynamodb.port", mappedPort);
        map.put("embedded.dynamodb.host", host);
        map.put("embedded.dynamodb.accessKey", properties.getAccessKey());
        map.put("embedded.dynamodb.secretKey", properties.getSecretKey());

        log.info("Started DynamoDb server. Connection details: {}, ", map);
        log.info("Consult with the doc " +
                "https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html " +
                "for more details");

        MapPropertySource propertySource = new MapPropertySource("embeddedDynamodbInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
