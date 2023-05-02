package com.playtika.testcontainer.kafka.camel.samples;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SampleProductionRouteContext {

    public static final String PRODUCTION_ROUTE = "productionRoute";

    @Bean
    public RouteBuilder productionRouteBuilder(SampleRouteConfiguration configuration) {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(configuration.camelTopicEndpoint())
                        .process(exchange -> {
                            log.info("------------------ BEGIN OF RECEIVED MESSAGE --------------");
                            log.info("Hello " + exchange.getIn().getBody().toString() + "!");
                            log.info("Message ID " + exchange.getIn().getHeader(KafkaConstants.KEY));
                            log.info("------------------- END OF RECEIVED MESSAGE ---------------");
                        })
                        .end()
                        .setId(PRODUCTION_ROUTE);
            }
        };
    }

    @Bean
    public SampleRouteConfiguration sampleRouteConfiguration(@Value("${embedded.kafka.brokerList}") String kafkaBrokerList) {
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
