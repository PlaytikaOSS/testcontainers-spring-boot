package com.playtika.testcontainer.opensearch;

import com.playtika.testcontainer.common.spring.DependsOnPostProcessor;
import org.opensearch.client.RestClient;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import static com.playtika.testcontainer.opensearch.OpenSearchProperties.BEAN_NAME_EMBEDDED_OPEN_SEARCH;

@AutoConfiguration(afterName = "org.opensearch.spring.boot.autoconfigure.OpenSearchRestClientAutoConfiguration")
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnClass(RestClient.class)
@ConditionalOnProperty(name = "embedded.opensearch.enabled", matchIfMissing = true)
public class EmbeddedOpenSearchRestClientDependenciesAutoConfiguration {

    @Bean
    public static BeanFactoryPostProcessor opensearchRestClientDependencyPostProcessor() {
        return new DependsOnPostProcessor(RestClient.class, new String[]{BEAN_NAME_EMBEDDED_OPEN_SEARCH});
    }
}
