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

import static com.playtika.test.rabbitmq.RabbitMQProperties.BEAN_NAME_EMBEDDED_RABBITMQ;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@SpringBootTest(
        classes = EmbeddedRabbitMQBootstrapConfigurationTest.TestConfiguration.class
)
@ActiveProfiles("enabled")
public class EmbeddedRabbitMQBootstrapConfigurationTest {
    private static final String QUEUE_NAME = "QUEUE";

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }

    @Test
    public void testRabbitTemplate() {
        rabbitAdmin.declareQueue(new Queue(QUEUE_NAME));

        assertThat(rabbitAdmin.getQueueProperties(QUEUE_NAME)).isNotNull();
        assertThat(rabbitAdmin.getQueueProperties("bar")).isNull();

        String expectedMessageBody = "Hello RabbitMQ!";
        rabbitTemplate.convertAndSend(QUEUE_NAME, expectedMessageBody);

        Message message = rabbitTemplate.receive(QUEUE_NAME);

        assertThat(expectedMessageBody).isEqualTo(new String(message.getBody()));
    }

    @Test
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.rabbitmq.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.rabbitmq.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.rabbitmq.vhost")).isNotEmpty();
        assertThat(environment.getProperty("embedded.rabbitmq.user")).isNotEmpty();
        assertThat(environment.getProperty("embedded.rabbitmq.password")).isNotEmpty();
        assertThat(environment.getProperty("embedded.rabbitmq.httpPort")).isNotEmpty();
    }

    @Test
    public void shouldSetupDependsOnForRabbitTemplate() {
        String[] beanNamesForType = beanFactory.getBeanNamesForType(RabbitTemplate.class);
        assertThat(beanNamesForType)
                .as("rabbitTemplate should be present")
                .hasSize(1)
                .contains("rabbitTemplate");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(BEAN_NAME_EMBEDDED_RABBITMQ);
    }

}
