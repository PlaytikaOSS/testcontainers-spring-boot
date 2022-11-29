package com.playtika.test.rabbitmq;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import static com.playtika.test.rabbitmq.RabbitMQProperties.BEAN_NAME_EMBEDDED_RABBITMQ;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration")
@AutoConfigureOrder
@ConditionalOnClass(RabbitTemplate.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.rabbitmq.enabled", matchIfMissing = true)
public class EmbeddedRabbitMQDependenciesAutoConfiguration {

    @Bean
    public static BeanFactoryPostProcessor rabbitMessagingTemplateDependencyPostProcessor() {
        return new DependsOnPostProcessor(RabbitTemplate.class, new String[]{BEAN_NAME_EMBEDDED_RABBITMQ});
    }
}
