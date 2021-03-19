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
package com.playtika.test.kafka.camel.samples;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleProductionRouteContext {

    public static final String PRODUCTION_ROUTE = "productionRoute";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    SampleRouteConfiguration configuration;

    @Value("${embedded.kafka.brokerList}")
    String kafkaBrokerList;

    @Bean
    public RouteBuilder productionRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(configuration.camelTopicEndpoint())
                        .process(exchange -> {
                            logger.info("------------------ BEGIN OF RECEIVED MESSAGE --------------");
                            logger.info("Hello " + exchange.getIn().getBody().toString() + "!");
                            logger.info("Message ID " + exchange.getIn().getHeader(KafkaConstants.KEY));
                            logger.info("------------------- END OF RECEIVED MESSAGE ---------------");
                        })
                        .end()
                        .setId(PRODUCTION_ROUTE);
            }
        };
    }

    @Bean
    public SampleRouteConfiguration sampleRouteConfiguration() {
        return () -> "kafka:camelTopic" +
                "?" +
                "brokers=" + kafkaBrokerList +
                "&" +
                "groupId=testConsumer" +
                "&" +
                "autoOffsetReset=earliest" +
                "&" +
                "autoCommitIntervalMs=100" +
                "&" +
                "serializerClass=org.apache.kafka.common.serialization.StringSerializer" +
                "&" +
                "keySerializerClass=org.apache.kafka.common.serialization.StringSerializer" +
                "&" +
                "autoCommitEnable=true";
    }
}
