package com.playtika.test.aerospike;

import com.aerospike.client.AerospikeClient;
import com.playtika.test.common.spring.DependsOnPostProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.playtika.test.aerospike.AerospikeProperties.AEROSPIKE_BEAN_NAME;

@Slf4j
@AutoConfiguration
@AutoConfigureOrder
@ConditionalOnClass(AerospikeClient.class)
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(value = "embedded.aerospike.enabled", matchIfMissing = true)
public class EmbeddedAerospikeDependenciesAutoConfiguration {

    @Configuration
    protected static class AerospikeClientPostProcessorConfiguration {
        @Bean
        public static BeanFactoryPostProcessor aerospikeClientDependencyPostProcessor() {
            return new DependsOnPostProcessor(AerospikeClient.class, new String[]{AEROSPIKE_BEAN_NAME});
        }
    }
}
