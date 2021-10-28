package com.playtika.test.kafka.configuration;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
public class EmbeddedKafkaBootstrapConfiguration {

    @Import(value = {
            KafkaContainerConfiguration.class,
            SchemaRegistryContainerConfiguration.class,
    })
    @Configuration
    public static class AllConfigurations {
    }
}