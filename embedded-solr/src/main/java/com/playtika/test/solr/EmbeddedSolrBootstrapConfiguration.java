package com.playtika.test.solr;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import com.playtika.test.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
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
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.solr.SolrProperties.BEAN_NAME_EMBEDDED_SOLR;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.solr.enabled", matchIfMissing = true)
@EnableConfigurationProperties(SolrProperties.class)
public class EmbeddedSolrBootstrapConfiguration {

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "solr")
    ToxiproxyContainer.ContainerProxy solrContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                         @Qualifier(BEAN_NAME_EMBEDDED_SOLR) SolrContainer solrContainer,
                                                         SolrProperties properties,
                                                         ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(solrContainer, properties.getPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.solr.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.solr.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.solr.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedSolrToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Solr ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_SOLR, destroyMethod = "stop")
    public GenericContainer<?> solrContainer(ConfigurableEnvironment environment,
                                             SolrProperties properties,
                                             Optional<Network> network) {

        SolrContainer solrContainer = new SolrContainer(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getPort());

        network.ifPresent(solrContainer::withNetwork);

        solrContainer = (SolrContainer) configureCommonsAndStart(solrContainer, properties, log);

        registerNatsEnvironment(solrContainer, environment, properties);
        return solrContainer;
    }

    private void registerNatsEnvironment(GenericContainer<?> natsContainer,
                                         ConfigurableEnvironment environment,
                                         SolrProperties properties) {
        Integer port = natsContainer.getMappedPort(properties.getPort());
        String host = natsContainer.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        map.put("embedded.solr.host", host);
        map.put("embedded.solr.port", port);

        log.info("Started Solr server. Connection details {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedSolrInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
