package com.playtika.testcontainer.neo4j;

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
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.neo4j.Neo4jProperties.BEAN_NAME_EMBEDDED_NEO4J;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.neo4j.enabled", matchIfMissing = true)
@EnableConfigurationProperties(Neo4jProperties.class)
public class EmbeddedNeo4jBootstrapConfiguration {

    private static final String NEO4J_NETWORK_ALIAS = "neo4j.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "neo4j")
    ToxiproxyClientProxy neo4jContainerProxy(ToxiproxyClient toxiproxyClient,
                                              ToxiproxyContainer toxiproxyContainer,
                                              @Qualifier(BEAN_NAME_EMBEDDED_NEO4J) Neo4jContainer neo4j,
                                              Neo4jProperties properties,
                                              ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                neo4j,
                properties.getBoltPort(),
                "neo4j");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.neo4j", "embeddedNeo4jToxiProxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_NEO4J, destroyMethod = "stop")
    public Neo4jContainer neo4j(ConfigurableEnvironment environment,
                                Neo4jProperties properties,
                                Optional<Network> network) {
        Neo4jContainer neo4j = new Neo4jContainer<>(ContainerUtils.getDockerImageName(properties))
                .withAdminPassword(properties.password)
                .withNetworkAliases(NEO4J_NETWORK_ALIAS);

        network.ifPresent(neo4j::withNetwork);
        neo4j = (Neo4jContainer) configureCommonsAndStart(neo4j, properties, log);
        registerNeo4jEnvironment(neo4j, environment, properties);
        return neo4j;
    }

    private void registerNeo4jEnvironment(Neo4jContainer neo4j,
                                          ConfigurableEnvironment environment,
                                          Neo4jProperties properties) {
        Integer httpsPort = neo4j.getMappedPort(properties.httpsPort);
        Integer httpPort = neo4j.getMappedPort(properties.httpPort);
        Integer boltPort = neo4j.getMappedPort(properties.boltPort);
        String host = neo4j.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.neo4j.httpsPort", httpsPort);
        map.put("embedded.neo4j.httpPort", httpPort);
        map.put("embedded.neo4j.boltPort", boltPort);
        map.put("embedded.neo4j.host", host);
        map.put("embedded.neo4j.password", properties.getPassword());
        map.put("embedded.neo4j.user", properties.getUser());
        map.put("embedded.neo4j.networkAlias", NEO4J_NETWORK_ALIAS);
        map.put("embedded.neo4j.internalHttpsPort", properties.getHttpsPort());
        map.put("embedded.neo4j.internalHttpPort", properties.getHttpPort());
        map.put("embedded.neo4j.internalBoltPort", properties.getBoltPort());

        log.info("Started neo4j server. Connection details {},  " +
                        "Admin UI: http://localhost:{}, user: {}, password: {}",
                map, httpPort, properties.getUser(), properties.getPassword());

        MapPropertySource propertySource = new MapPropertySource("embeddedNeo4jInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
