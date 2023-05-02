package com.playtika.testcontainer.elasticsearch;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.elasticsearch.ElasticSearchProperties.BEAN_NAME_EMBEDDED_ELASTIC_SEARCH;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.elasticsearch.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ElasticSearchProperties.class)
public class EmbeddedElasticSearchBootstrapConfiguration {

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "elasticsearch")
    ToxiproxyContainer.ContainerProxy elasticsearchContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                                  @Qualifier(BEAN_NAME_EMBEDDED_ELASTIC_SEARCH) ElasticsearchContainer elasticSearch,
                                                                  ElasticSearchProperties properties,
                                                                  ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(elasticSearch, properties.getHttpPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.elasticsearch.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.elasticsearch.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.elasticsearch.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedElasticSearchToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started ElasticSearch ToxiProxy connection details {}", map);

        return proxy;
    }

    @ConditionalOnMissingBean(name = BEAN_NAME_EMBEDDED_ELASTIC_SEARCH)
    @Bean(name = BEAN_NAME_EMBEDDED_ELASTIC_SEARCH, destroyMethod = "stop")
    public ElasticsearchContainer elasticSearch(ConfigurableEnvironment environment,
                                                ElasticSearchProperties properties,
                                                Optional<Network> network) {

        ElasticsearchContainer elasticSearch = ElasticSearchContainerFactory.create(properties);
        network.ifPresent(elasticSearch::withNetwork);
        elasticSearch = (ElasticsearchContainer) configureCommonsAndStart(elasticSearch, properties, log);
        registerElasticSearchEnvironment(elasticSearch, environment, properties);
        return elasticSearch;
    }

    private void registerElasticSearchEnvironment(ElasticsearchContainer elasticSearch,
                                                  ConfigurableEnvironment environment,
                                                  ElasticSearchProperties properties) {
        Integer httpPort = elasticSearch.getMappedPort(properties.getHttpPort());
        Integer transportPort = elasticSearch.getMappedPort(properties.getTransportPort());
        String host = elasticSearch.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.elasticsearch.clusterName", properties.getClusterName());
        map.put("embedded.elasticsearch.host", host);
        map.put("embedded.elasticsearch.httpPort", httpPort);
        map.put("embedded.elasticsearch.transportPort", transportPort);

        log.info("Started ElasticSearch server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedElasticSearchInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
