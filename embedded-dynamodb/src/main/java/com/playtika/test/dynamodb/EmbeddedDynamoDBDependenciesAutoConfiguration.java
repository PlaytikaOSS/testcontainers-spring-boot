package com.playtika.test.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.playtika.test.common.spring.DependsOnPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.playtika.test.dynamodb.DynamoDBProperties.BEAN_NAME_EMBEDDED_DYNAMODB;

@AutoConfiguration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnClass(AmazonDynamoDB.class)
@ConditionalOnProperty(name = "embedded.dynamodb.enabled", matchIfMissing = true)
public class EmbeddedDynamoDBDependenciesAutoConfiguration {

    @Configuration
    public static class EmbeddedDynamoDbDataSourceDependencyContext {
        @Bean
        public static BeanFactoryPostProcessor dynamodbDependencyPostProcessor() {
            return new DependsOnPostProcessor(AmazonDynamoDB.class, new String[]{BEAN_NAME_EMBEDDED_DYNAMODB});
        }
    }

}
