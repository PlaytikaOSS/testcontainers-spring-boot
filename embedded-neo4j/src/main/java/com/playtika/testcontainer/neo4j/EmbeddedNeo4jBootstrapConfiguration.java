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
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

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
                                              Neo4jProperties properties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                neo4j,
                properties.getBoltPort(),
                "neo4j");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "neo4j")
    public DynamicPropertyRegistrar neo4jToxiProxyDynamicPropertyRegistrar(@Qualifier("neo4jContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.neo4j");
    }

    @Bean(name = BEAN_NAME_EMBEDDED_NEO4J, destroyMethod = "stop")
    public Neo4jContainer neo4j(Neo4jProperties properties, Optional<Network> network) {
        Neo4jContainer neo4j = new Neo4jContainer<>(ContainerUtils.getDockerImageName(properties))
                .withAdminPassword(properties.password)
                .withNetworkAliases(NEO4J_NETWORK_ALIAS);

        network.ifPresent(neo4j::withNetwork);
        neo4j = (Neo4jContainer) configureCommonsAndStart(neo4j, properties, log);
        return neo4j;
    }

    @Bean
    public DynamicPropertyRegistrar neo4jDynamicPropertyRegistrar(@Qualifier(BEAN_NAME_EMBEDDED_NEO4J) Neo4jContainer neo4j, Neo4jProperties properties) {
        return registry -> {
            Integer httpsPort = neo4j.getMappedPort(properties.httpsPort);
            Integer httpPort = neo4j.getMappedPort(properties.httpPort);
            Integer boltPort = neo4j.getMappedPort(properties.boltPort);
            String host = neo4j.getHost();
            registry.add("embedded.neo4j.httpsPort", () -> httpsPort);
            registry.add("embedded.neo4j.httpPort", () -> httpPort);
            registry.add("embedded.neo4j.boltPort", () -> boltPort);
            registry.add("embedded.neo4j.host", () -> host);
            registry.add("embedded.neo4j.password", properties::getPassword);
            registry.add("embedded.neo4j.user", properties::getUser);
            registry.add("embedded.neo4j.networkAlias", () -> NEO4J_NETWORK_ALIAS);
            registry.add("embedded.neo4j.internalHttpsPort", properties::getHttpsPort);
            registry.add("embedded.neo4j.internalHttpPort", properties::getHttpPort);
            registry.add("embedded.neo4j.internalBoltPort", properties::getBoltPort);
        };
    }
}
