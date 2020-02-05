/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Playtika
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
package com.playtika.test.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.containerLogsConsumer;
import static com.playtika.test.rabbitmq.RabbitMQProperties.BEAN_NAME_EMBEDDED_RABBITMQ;
import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Slf4j
@Configuration
@Order(HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "embedded.rabbitmq.enabled", matchIfMissing = true)
@EnableConfigurationProperties(RabbitMQProperties.class)
public class EmbeddedRabbitMQBootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RabbitMQStatusCheck rabbitMQStatusCheck() {
        return new RabbitMQStatusCheck();
    }


    @Bean(name = BEAN_NAME_EMBEDDED_RABBITMQ, destroyMethod = "stop")
    public GenericContainer rabbitmq(
        ConfigurableEnvironment environment,
        RabbitMQProperties properties,
        RabbitMQStatusCheck rabbitMQStatusCheck) {
        log.info("Starting RabbitMQ server. Docker image: {}", properties.getDockerImage());

        GenericContainer rabbitMQ =
            new GenericContainer(properties.getDockerImage())
                .withEnv("RABBITMQ_DEFAULT_VHOST", properties.getVhost())
                .withEnv("RABBITMQ_DEFAULT_USER", properties.getUser())
                .withEnv("RABBITMQ_DEFAULT_PASS", properties.getPassword())
                .waitingFor(rabbitMQStatusCheck)
                .withLogConsumer(containerLogsConsumer(log))
                .withExposedPorts(properties.getPort())
                .withStartupTimeout(properties.getTimeoutDuration());
        rabbitMQ.start();
        registerRabbitMQEnvironment(rabbitMQ, environment, properties);
        return rabbitMQ;
    }


    private void registerRabbitMQEnvironment(
        GenericContainer rabbitMQ,
        ConfigurableEnvironment environment,
        RabbitMQProperties properties) {
        Integer mappedPort = rabbitMQ.getMappedPort(properties.getPort());
        String host = rabbitMQ.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.rabbitmq.port", mappedPort);
        map.put("embedded.rabbitmq.host", host);
        map.put("embedded.rabbitmq.vhost", properties.getVhost());
        map.put("embedded.rabbitmq.user", properties.getUser());
        map.put("embedded.rabbitmq.password", properties.getPassword());

        log.info("Started RabbitMQ server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedRabbitMqInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
