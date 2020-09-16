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

import static org.assertj.core.api.Assertions.assertThat;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.testcontainers.containers.GenericContainer;

@Slf4j
@SpringBootTest(
        classes = DisableRabbitMQTest.TestConfiguration.class,
        properties = "embedded.rabbitmq.enabled=false")
public class DisableRabbitMQTest {
    @Autowired
    private ConfigurableEnvironment environment;
    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Test
    public void propertiesAreNotAvailable() {
        assertThat(environment.getProperty("embedded.rabbitmq.port")).isNullOrEmpty();
        assertThat(environment.getProperty("embedded.rabbitmq.host")).isNullOrEmpty();
        assertThat(environment.getProperty("embedded.rabbitmq.vhost")).isNullOrEmpty();
        assertThat(environment.getProperty("embedded.rabbitmq.user")).isNullOrEmpty();
        assertThat(environment.getProperty("embedded.rabbitmq.password")).isNullOrEmpty();
        assertThat(environment.getProperty("embedded.rabbitmq.httpPort")).isNullOrEmpty();
    }

    @Test
    public void contextLoads() {
        String[] containers = beanFactory.getBeanNamesForType(GenericContainer.class);
        String[] postProcessors = beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class);

        assertThat(containers).isEmpty();
        assertThat(postProcessors).doesNotContain("rabbitMessagingTemplateDependencyPostProcessor");
    }


    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
