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
        return () -> "kafka:camelTopic" + // https://camel.apache.org/components/latest/kafka-component.html#_options
                "?" +
                "brokers=" + kafkaBrokerList +
                "&" +
                "groupId=testConsumer" +
                "&" +
                "autoOffsetReset=earliest" +
                "&" +
                "autoCommitIntervalMs=100" +
                "&" +
                "valueSerializer=org.apache.kafka.common.serialization.StringSerializer" + // StringSerializer is default
                "&" +
                "keySerializer=org.apache.kafka.common.serialization.StringSerializer" + // StringSerializer is default
                "&" +
                "autoCommitEnable=true";
    }
}
