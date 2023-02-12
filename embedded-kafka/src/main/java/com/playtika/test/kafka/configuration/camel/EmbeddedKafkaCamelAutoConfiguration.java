package com.playtika.test.kafka.configuration.camel;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelAutoConfiguration;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import static com.playtika.test.kafka.properties.KafkaConfigurationProperties.KAFKA_BEAN_NAME;

@AutoConfiguration(afterName = "org.apache.camel.spring.boot.CamelAutoConfiguration")
@AutoConfigureOrder
@ImportAutoConfiguration(classes = { CamelAutoConfiguration.class })
@ConditionalOnClass(CamelContext.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(value = {"embedded.kafka.enabled"}, havingValue = "true", matchIfMissing = true)
public class EmbeddedKafkaCamelAutoConfiguration {

    @Bean
    public static BeanFactoryPostProcessor kafkaCamelDependencyPostProcessor() {
        return new DependsOnPostProcessor(CamelContext.class, new String[]{KAFKA_BEAN_NAME});
    }
}
