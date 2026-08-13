package com.playtika.testcontainer.solr;

import com.playtika.testcontainer.common.spring.ContainerStartupCoordinator;
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
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.solr.SolrContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommons;
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
                                             SolrProperties properties,
                                             ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                solrContainer,
                properties.getPort(),
                "solr");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.solr", "embeddedSolrToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_SOLR, destroyMethod = "stop")
    public GenericContainer<?> solrContainer(ConfigurableEnvironment environment,
                                             SolrProperties properties,
                                             Optional<Network> network,
                                             ContainerStartupCoordinator startupCoordinator) {

        SolrContainer solrContainer = new SolrContainer(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getPort())
                .withNetworkAliases(SOLR_NETWORK_ALIAS);

        network.ifPresent(solrContainer::withNetwork);

        SolrContainer configuredSolrContainer = (SolrContainer) configureCommons(solrContainer, properties, log);
        startupCoordinator.schedule(() -> {
            ContainerUtils.startAndLogTime(configuredSolrContainer, log);
            registerNatsEnvironment(configuredSolrContainer, environment, properties);
        });
        return configuredSolrContainer;
    }

    private void registerNatsEnvironment(GenericContainer<?> natsContainer,
                                         ConfigurableEnvironment environment,
                                         SolrProperties properties) {
        Integer port = natsContainer.getMappedPort(properties.getPort());
        String host = natsContainer.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        map.put("embedded.solr.host", host);
        map.put("embedded.solr.port", port);
        map.put("embedded.solr.networkAlias", SOLR_NETWORK_ALIAS);
        map.put("embedded.solr.internalPort", properties.getPort());

        log.info("Started Solr server. Connection details {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedSolrInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
