package com.playtika.test.elasticsearch;

import com.playtika.test.common.spring.DependsOnPostProcessor;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.playtika.test.elasticsearch.ElasticSearchProperties.BEAN_NAME_EMBEDDED_ELASTIC_SEARCH;

@Configuration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnClass(RestClient.class)
@ConditionalOnProperty(name = "embedded.elasticsearch.enabled", matchIfMissing = true)
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration")
public class EmbeddedElasticSearchRestClientDependenciesAutoConfiguration {

    @Bean
    public static BeanFactoryPostProcessor elasticRestClientDependencyPostProcessor() {
        return new DependsOnPostProcessor(RestClient.class, new String[]{BEAN_NAME_EMBEDDED_ELASTIC_SEARCH});
    }
}
