package com.playtika.test.mongodb;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;

import static com.playtika.test.mongodb.MongodbProperties.BEAN_NAME_EMBEDDED_MONGODB;

@AutoConfiguration(afterName = "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration")
@AutoConfigureOrder
@ConditionalOnClass(MongoTemplate.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.mongodb.enabled", havingValue = "true", matchIfMissing = true)
public class EmbeddedMongodbDependenciesAutoConfiguration {

    @Bean
    public static BeanFactoryPostProcessor mongoClientDependencyPostProcessor() {
        return new DependsOnPostProcessor(MongoTemplate.class, new String[]{BEAN_NAME_EMBEDDED_MONGODB});
    }
}
