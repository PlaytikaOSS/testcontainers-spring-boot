package com.playtika.testcontainer.opensearch;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

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
    ToxiproxyClientProxy opensearchContainerProxy(ToxiproxyClient toxiproxyClient,
                                                   ToxiproxyContainer toxiproxyContainer,
                                                   @Qualifier(BEAN_NAME_EMBEDDED_OPEN_SEARCH) OpenSearchContainer opensearch,
                                                   OpenSearchProperties properties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                opensearch,
                properties.getHttpPort(),
                "opensearch");
    }

    @ConditionalOnMissingBean(name = BEAN_NAME_EMBEDDED_OPEN_SEARCH)
    @Bean(name = BEAN_NAME_EMBEDDED_OPEN_SEARCH, destroyMethod = "stop")
    public GenericContainer openSearch(OpenSearchProperties properties,
                                       Optional<Network> network) {
        GenericContainer openSearch = OpenSearchContainerFactory.create(properties)
                .withNetworkAliases(OPENSEARCH_NETWORK_ALIAS);
        network.ifPresent(openSearch::withNetwork);
        openSearch = configureCommonsAndStart(openSearch, properties, log);
        Integer httpPort = openSearch.getMappedPort(properties.getHttpPort());
        Integer transportPort = openSearch.getMappedPort(properties.getTransportPort());
        String host = openSearch.getHost();
        log.info("Started OpenSearch server. Connection details: clusterName={}, host={}, httpPort={}, transportPort={}, networkAlias={}, internalHttpPort={}, internalTransportPort={}",
                properties.getClusterName(), host, httpPort, transportPort, OPENSEARCH_NETWORK_ALIAS, properties.getHttpPort(), properties.getTransportPort());
        return openSearch;
    }

    @Bean
    public DynamicPropertyRegistrar opensearchDynamicPropertyRegistrar(@Qualifier(BEAN_NAME_EMBEDDED_OPEN_SEARCH) GenericContainer<?> openSearch, OpenSearchProperties properties) {
        return registry -> {
            registry.add("embedded.opensearch.clusterName", properties::getClusterName);
            registry.add("embedded.opensearch.host", openSearch::getHost);
            registry.add("embedded.opensearch.httpPort", () -> openSearch.getMappedPort(properties.getHttpPort()));
            registry.add("embedded.opensearch.transportPort", () -> openSearch.getMappedPort(properties.getTransportPort()));
            registry.add("embedded.opensearch.networkAlias", () -> OPENSEARCH_NETWORK_ALIAS);
            registry.add("embedded.opensearch.internalHttpPort", properties::getHttpPort);
            registry.add("embedded.opensearch.internalTransportPort", properties::getTransportPort);
        };
    }
}
