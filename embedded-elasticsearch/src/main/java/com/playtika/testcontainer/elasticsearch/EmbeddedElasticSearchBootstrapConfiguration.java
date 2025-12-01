package com.playtika.testcontainer.elasticsearch;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.Network;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

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

    private static final String ELASTICSEARCH_NETWORK_ALIAS = "elasticsearch.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "elasticsearch")
    ToxiproxyClientProxy elasticsearchContainerProxy(ToxiproxyClient toxiproxyClient,
                                                      ToxiproxyContainer toxiproxyContainer,
                                                      @Qualifier(BEAN_NAME_EMBEDDED_ELASTIC_SEARCH) ElasticsearchContainer elasticSearch,
                                                      ElasticSearchProperties properties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                elasticSearch,
                properties.getHttpPort(),
                "elasticsearch");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "elasticsearch")
    public DynamicPropertyRegistrar elasticsearchToxiProxyDynamicPropertyRegistrar(@Qualifier("elasticsearchContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.elasticsearch");
    }

    @ConditionalOnMissingBean(name = BEAN_NAME_EMBEDDED_ELASTIC_SEARCH)
    @Bean(name = BEAN_NAME_EMBEDDED_ELASTIC_SEARCH, destroyMethod = "stop")
    public ElasticsearchContainer elasticSearch(ElasticSearchProperties properties,
                                                Optional<Network> network) {
        ElasticsearchContainer elasticSearch = ElasticSearchContainerFactory.create(properties)
                .withNetworkAliases(ELASTICSEARCH_NETWORK_ALIAS);
        network.ifPresent(elasticSearch::withNetwork);
        elasticSearch = (ElasticsearchContainer) configureCommonsAndStart(elasticSearch, properties, log);
        Integer httpPort = elasticSearch.getMappedPort(properties.getHttpPort());
        Integer transportPort = elasticSearch.getMappedPort(properties.getTransportPort());
        String host = elasticSearch.getHost();
        log.info("Started ElasticSearch server. Connection details: clusterName={}, host={}, httpPort={}, transportPort={}, networkAlias={}, internalHttpPort={}, internalTransportPort={}",
                properties.getClusterName(), host, httpPort, transportPort, ELASTICSEARCH_NETWORK_ALIAS, properties.getHttpPort(), properties.getTransportPort());
        return elasticSearch;
    }

    @Bean
    public DynamicPropertyRegistrar elasticsearchDynamicPropertyRegistrar(@Qualifier(BEAN_NAME_EMBEDDED_ELASTIC_SEARCH) ElasticsearchContainer elasticSearch, ElasticSearchProperties properties) {
        return registry -> {
            registry.add("embedded.elasticsearch.clusterName", properties::getClusterName);
            registry.add("embedded.elasticsearch.host", elasticSearch::getHost);
            registry.add("embedded.elasticsearch.httpPort", () -> elasticSearch.getMappedPort(properties.getHttpPort()));
            registry.add("embedded.elasticsearch.transportPort", () -> elasticSearch.getMappedPort(properties.getTransportPort()));
            registry.add("embedded.elasticsearch.networkAlias", () -> ELASTICSEARCH_NETWORK_ALIAS);
            registry.add("embedded.elasticsearch.internalHttpPort", properties::getHttpPort);
            registry.add("embedded.elasticsearch.internalTransportPort", properties::getTransportPort);
        };
    }
}
