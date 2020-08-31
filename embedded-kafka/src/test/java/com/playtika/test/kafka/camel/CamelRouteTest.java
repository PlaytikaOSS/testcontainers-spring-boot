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
package com.playtika.test.kafka.camel;

import com.playtika.test.kafka.AbstractEmbeddedKafkaTest;
import com.playtika.test.kafka.camel.samples.SampleRouteConfiguration;
import com.playtika.test.kafka.camel.samples.SampleTestRouteMonitor;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.apache.camel.component.kafka.KafkaConstants;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import static com.playtika.test.kafka.properties.KafkaConfigurationProperties.KAFKA_BEAN_NAME;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CamelRouteTest extends AbstractEmbeddedKafkaTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Autowired
    private SampleRouteConfiguration routeConfiguration;

    private CamelContext camelContext;
    private SampleTestRouteMonitor routeMonitor;

    @Autowired
    public void setCamelContext(CamelContext camelContext) throws Exception {
        this.camelContext = camelContext;
        this.routeMonitor = new SampleTestRouteMonitor(camelContext);
    }

    @Test
    public void should_sendAndReceiveMessage() throws InterruptedException {
        String message = "this is a test!";
        producerTemplate.sendBodyAndHeader(routeConfiguration.helloTopicEndpoint(), message, KafkaConstants.KEY, "12345678");

        routeMonitor.getResultEndpoint().setResultWaitTime(15000);
        routeMonitor.getResultEndpoint().expectedBodiesReceived(message);
        routeMonitor.getResultEndpoint().assertIsSatisfied();
    }

    @Test
    public void shouldSetupDependsOnForCamel() {
        String[] beanNamesForType = beanFactory.getBeanNamesForType(CamelContext.class);
        assertThat(beanNamesForType)
                .as("CamelContext should be present")
                .hasSize(1);
        asList(beanNamesForType).forEach(this::hasDependsOn);
        assertEquals(ServiceStatus.Started, camelContext.getStatus());
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(KAFKA_BEAN_NAME);
    }

}