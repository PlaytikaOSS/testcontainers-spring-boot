package com.playtika.testcontainer.opensearch;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.testcontainers.OpensearchContainer;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.opensearch.OpenSearchProperties.BEAN_NAME_EMBEDDED_OPEN_SEARCH;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.opensearch.enabled", matchIfMissing = true)
@EnableConfigurationProperties(OpenSearchProperties.class)
public class EmbeddedOpenSearchBootstrapConfiguration {

    private static final String OPENSEARCH_NETWORK_ALIAS = "opensearch.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "opensearch")
    ToxiproxyContainer.ContainerProxy opensearchContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                               @Qualifier(BEAN_NAME_EMBEDDED_OPEN_SEARCH) OpensearchContainer opensearch,
                                                               OpenSearchProperties properties,
                                                               ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(opensearch, properties.getHttpPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.opensearch.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.opensearch.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.opensearch.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedOpenSearchToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started OpenSearch ToxiProxy connection details {}", map);

        return proxy;
    }

    @ConditionalOnMissingBean(name = BEAN_NAME_EMBEDDED_OPEN_SEARCH)
    @Bean(name = BEAN_NAME_EMBEDDED_OPEN_SEARCH, destroyMethod = "stop")
    public OpensearchContainer openSearch(ConfigurableEnvironment environment,
                                          OpenSearchProperties properties,
                                          Optional<Network> network) {

        OpensearchContainer openSearch = OpenSearchContainerFactory.create(properties)
                .withNetworkAliases(OPENSEARCH_NETWORK_ALIAS);
        network.ifPresent(openSearch::withNetwork);
        openSearch = (OpensearchContainer) configureCommonsAndStart(openSearch, properties, log);
        registerOpenSearchEnvironment(openSearch, environment, properties);
        return openSearch;
    }

    private void registerOpenSearchEnvironment(OpensearchContainer OpenSearch,
                                               ConfigurableEnvironment environment,
                                               OpenSearchProperties properties) {
        Integer httpPort = OpenSearch.getMappedPort(properties.getHttpPort());
        Integer transportPort = OpenSearch.getMappedPort(properties.getTransportPort());
        String host = OpenSearch.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.opensearch.clusterName", properties.getClusterName());
        map.put("embedded.opensearch.host", host);
        map.put("embedded.opensearch.httpPort", httpPort);
        map.put("embedded.opensearch.transportPort", transportPort);
        map.put("embedded.opensearch.networkAlias", OPENSEARCH_NETWORK_ALIAS);
        map.put("embedded.opensearch.internalHttpPort", properties.getHttpPort());
        map.put("embedded.opensearch.internalTransportPort", properties.getTransportPort());

        log.info("Started OpenSearch server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedOpenSearchInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
