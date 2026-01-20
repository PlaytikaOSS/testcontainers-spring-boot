package com.playtika.testcontainer.cassandra;

import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.common.utils.FileUtils;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.delegate.CassandraDatabaseDelegate;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;

import javax.script.ScriptException;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.cassandra.CassandraProperties.BEAN_NAME_EMBEDDED_CASSANDRA;
import static com.playtika.testcontainer.cassandra.CassandraProperties.DEFAULT_DATACENTER;
import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.cassandra.enabled", matchIfMissing = true)
@EnableConfigurationProperties(CassandraProperties.class)
public class EmbeddedCassandraBootstrapConfiguration {

    private static final String CASSANDRA_NETWORK_ALIAS = "cassandra.testcontainer.docker";

    private final ResourceLoader resourceLoader;

    public EmbeddedCassandraBootstrapConfiguration(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "cassandra")
    ToxiproxyClientProxy cassandraContainerProxy(ToxiproxyClient toxiproxyClient,
                                                  ToxiproxyContainer toxiproxyContainer,
                                                  @Qualifier(BEAN_NAME_EMBEDDED_CASSANDRA) CassandraContainer cassandra,
                                                  CassandraProperties properties,
                                                  ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                cassandra,
                properties.getPort(),
                "cassandra");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.cassandra", "embeddedCassandraToxiProxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_CASSANDRA, destroyMethod = "stop")
    public CassandraContainer cassandra(CassandraProperties properties,
                                        ConfigurableEnvironment environment,
                                        Optional<Network> network) throws Exception {
        CassandraContainer cassandra = new CassandraContainer<>(ContainerUtils.getDockerImageName(properties))
            .withExposedPorts(properties.getPort())
            .withNetworkAliases(CASSANDRA_NETWORK_ALIAS);
        network.ifPresent(cassandra::withNetwork);
        cassandra = (CassandraContainer) configureCommonsAndStart(cassandra, properties, log);
        initKeyspace(properties, cassandra);
        registerCassandraEnvironment(cassandra, environment, properties);
        return cassandra;
    }

    private void registerCassandraEnvironment(CassandraContainer cassandra, ConfigurableEnvironment environment, CassandraProperties properties) {
        String host = cassandra.getHost();
        Integer mappedPort = cassandra.getMappedPort(properties.getPort());

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.cassandra.port", mappedPort);
        map.put("embedded.cassandra.host", host);
        map.put("embedded.cassandra.datacenter", DEFAULT_DATACENTER);
        map.put("embedded.cassandra.keyspace-name", properties.keyspaceName);
        map.put("embedded.cassandra.networkAlias", CASSANDRA_NETWORK_ALIAS);
        map.put("embedded.cassandra.internalPort", properties.getPort());

        log.info("Started Cassandra. Connection details: host={}, port={}, datacenter={}, keyspace-name={}, networkAlias={}, internalPort={}",
            host, mappedPort, DEFAULT_DATACENTER, properties.keyspaceName, CASSANDRA_NETWORK_ALIAS, properties.getPort());

        MapPropertySource propertySource = new MapPropertySource("embeddedCassandraInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

    private void initKeyspace(CassandraProperties properties, CassandraContainer<?> cassandra) throws ScriptException {
        String initScriptContent = prepareCassandraInitScript(properties);
        try (DatabaseDelegate databaseDelegate = new CassandraDatabaseDelegate(cassandra)) {
            ScriptUtils.executeDatabaseScript(databaseDelegate, "init.cql", initScriptContent);
        }
    }

    private String prepareCassandraInitScript(CassandraProperties properties) {
        return FileUtils.resolveTemplateAsString(resourceLoader, "cassandra-init.sql", content -> content
                .replace("{{keyspaceName}}", properties.keyspaceName))
            .replace("{{replicationFactor}}", Integer.toString(properties.replicationFactor));
    }
}
