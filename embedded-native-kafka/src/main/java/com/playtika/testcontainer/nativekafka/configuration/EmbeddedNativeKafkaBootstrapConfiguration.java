package com.playtika.testcontainer.nativekafka.configuration;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
public class EmbeddedNativeKafkaBootstrapConfiguration {

    @Import(value = {
            NativeKafkaContainerConfiguration.class,
    })
    @Configuration
    public static class AllConfigurations {
    }
}