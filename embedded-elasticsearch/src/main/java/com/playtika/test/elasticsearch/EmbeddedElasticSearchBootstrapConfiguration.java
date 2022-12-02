package com.playtika.test.elasticsearch;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.elasticsearch.ElasticSearchProperties.BEAN_NAME_EMBEDDED_ELASTIC_SEARCH;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.elasticsearch.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ElasticSearchProperties.class)
public class EmbeddedElasticSearchBootstrapConfiguration {

    @ConditionalOnMissingBean(name = BEAN_NAME_EMBEDDED_ELASTIC_SEARCH)
    @Bean(name = BEAN_NAME_EMBEDDED_ELASTIC_SEARCH, destroyMethod = "stop")
    public ElasticsearchContainer elasticSearch(ConfigurableEnvironment environment,
                                                ElasticSearchProperties properties) {

        ElasticsearchContainer elasticSearch = ElasticSearchContainerFactory.create(properties);
        elasticSearch = (ElasticsearchContainer) configureCommonsAndStart(elasticSearch, properties, log);
        registerElasticSearchEnvironment(elasticSearch, environment, properties);
        return elasticSearch;
    }

    private void registerElasticSearchEnvironment(ElasticsearchContainer elasticSearch,
                                                  ConfigurableEnvironment environment,
                                                  ElasticSearchProperties properties) {
        Integer httpPort = elasticSearch.getMappedPort(properties.httpPort);
        Integer transportPort = elasticSearch.getMappedPort(properties.transportPort);
        String host = elasticSearch.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.elasticsearch.clusterName", properties.clusterName);
        map.put("embedded.elasticsearch.host", host);
        map.put("embedded.elasticsearch.httpPort", httpPort);
        map.put("embedded.elasticsearch.transportPort", transportPort);

        log.info("Started ElasticSearch server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedElasticSearchInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
