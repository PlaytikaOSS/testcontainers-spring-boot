package com.playtika.test.pubsub;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.playtika.test.pubsub.PubsubProperties.BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB;

@Configuration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnClass(PubSubTemplate.class)
@ConditionalOnProperty(name = "embedded.google.pubsub.enabled", matchIfMissing = true)
public class EmbeddedPubsubDependenciesAutoConfiguration {
    @Bean
    public static BeanFactoryPostProcessor pubsubTemplateDependencyPostProcessor() {
        return new DependsOnPostProcessor(PubSubTemplate.class, new String[]{BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB});
    }
}