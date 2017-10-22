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
package com.playtika.test.kafka.camel;

import com.playtika.test.kafka.camel.samples.SampleProductionRouteContext;
import com.playtika.test.kafka.camel.samples.SampleRouteConfiguration;
import com.playtika.test.kafka.camel.samples.SampleTestRouteMonitor;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.test.spring.CamelSpringRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SpringBootTest(
        classes = {
                ProductionRouteTest.TestConfiguration.class,
                SampleProductionRouteContext.class
        },
        properties = "embedded.kafka.topicsToCreate=helloTopic"
)
@RunWith(CamelSpringRunner.class)
public class ProductionRouteTest {

    @Autowired
    private CamelContext camelContext;
    @Autowired
    private ProducerTemplate producerTemplate;
    @Autowired
    private SampleRouteConfiguration routeConfiguration;

    private SampleTestRouteMonitor routeMonitor;

    @Before
    public void setUp() throws Exception {
        if (routeMonitor == null) {
            routeMonitor = new SampleTestRouteMonitor(camelContext);
        }
    }

    @Test
    public void should_sendAndReceiveMessage() throws Exception {
        String message = "this is a test!";
        producerTemplate.sendBodyAndHeader(routeConfiguration.helloTopicEndpoint(), message, KafkaConstants.KEY, "12345678");

        routeMonitor.getResultEndpoint().expectedBodiesReceived(message);
        routeMonitor.getResultEndpoint().assertIsSatisfied(3000);
    }

    @Configuration
    @EnableAutoConfiguration
    public static class TestConfiguration {

        @Bean
        public SampleRouteConfiguration sampleRouteConfiguration(String zookeeperConnect, String kafkaBrokerList) {
            return () -> "kafka:" + kafkaBrokerList +
                    "?" +
                    "zookeeperConnect=" + zookeeperConnect +
                    "&" +
                    "topic=helloTopic" +
                    "&" +
                    "groupId=testConsumer" +
                    "&" +
                    "autoOffsetReset=smallest" +
                    "&" +
                    "autoCommitIntervalMs=100" +
                    "&" +
                    "serializerClass=kafka.serializer.StringEncoder" +
                    "&" +
                    "keySerializerClass=kafka.serializer.StringEncoder" +
                    "&" +
                    "autoCommitEnable=true";
        }
    }
}