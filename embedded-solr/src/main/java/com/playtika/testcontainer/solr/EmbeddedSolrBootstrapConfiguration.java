package com.playtika.testcontainer.solr;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.solr.SolrContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.solr.SolrProperties.BEAN_NAME_EMBEDDED_SOLR;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.solr.enabled", matchIfMissing = true)
@EnableConfigurationProperties(SolrProperties.class)
public class EmbeddedSolrBootstrapConfiguration {

    private static final String SOLR_NETWORK_ALIAS = "solr.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "solr")
    ToxiproxyClientProxy solrContainerProxy(ToxiproxyClient toxiproxyClient,
                                             ToxiproxyContainer toxiproxyContainer,
                                             @Qualifier(BEAN_NAME_EMBEDDED_SOLR) SolrContainer solrContainer,
                                             SolrProperties properties) {

        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                solrContainer,
                properties.getPort(),
                "solr");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "solr")
    public DynamicPropertyRegistrar solrToxiProxyDynamicPropertyRegistrar(@Qualifier("solrContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.solr");
    }

    @Bean(name = BEAN_NAME_EMBEDDED_SOLR, destroyMethod = "stop")
    public SolrContainer solrContainer(SolrProperties properties, Optional<Network> network) {
        SolrContainer solrContainer = new SolrContainer(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getPort())
                .withNetworkAliases(SOLR_NETWORK_ALIAS);

        network.ifPresent(solrContainer::withNetwork);

        solrContainer = (SolrContainer) configureCommonsAndStart(solrContainer, properties, log);

        return solrContainer;
    }

    @Bean
    public DynamicPropertyRegistrar solrDynamicPropertyRegistrar(@Qualifier(BEAN_NAME_EMBEDDED_SOLR) GenericContainer<?> natsContainer, SolrProperties properties) {
        return registry -> {
            Integer port = natsContainer.getMappedPort(properties.getPort());
            String host = natsContainer.getHost();
            registry.add("embedded.solr.host", () -> host);
            registry.add("embedded.solr.port", () -> port);
            registry.add("embedded.solr.networkAlias", () -> SOLR_NETWORK_ALIAS);
            registry.add("embedded.solr.internalPort", properties::getPort);
        };
    }

}
