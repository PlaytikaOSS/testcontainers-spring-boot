package com.playtika.testcontainer.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.playtika.testcontainer.common.spring.DependsOnPostProcessor;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import static com.playtika.testcontainer.elasticsearch.ElasticSearchProperties.BEAN_NAME_EMBEDDED_ELASTIC_SEARCH;

@AutoConfiguration(afterName = "org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration")
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnClass(ElasticsearchClient.class)
@ConditionalOnProperty(name = "embedded.elasticsearch.enabled", matchIfMissing = true)
public class EmbeddedElasticSearchRestClientDependenciesAutoConfiguration {

    @Bean
    public static BeanFactoryPostProcessor elasticRestClientDependencyPostProcessor() {
        return new DependsOnPostProcessor(ElasticsearchClient.class, new String[]{BEAN_NAME_EMBEDDED_ELASTIC_SEARCH});
    }
}
