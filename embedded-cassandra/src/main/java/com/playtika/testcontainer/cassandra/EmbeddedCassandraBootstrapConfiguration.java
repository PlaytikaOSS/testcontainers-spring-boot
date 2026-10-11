package com.playtika.testcontainer.cassandra;

import com.playtika.testcontainer.common.spring.ContainerStartupCoordinator;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.common.utils.FileUtils;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.RequiredArgsConstructor;
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
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.cassandra.CassandraDatabaseDelegate;
import org.testcontainers.containers.Network;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import javax.script.ScriptException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.cassandra.CassandraProperties.BEAN_NAME_EMBEDDED_CASSANDRA;
import static com.playtika.testcontainer.cassandra.CassandraProperties.DEFAULT_DATACENTER;
import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommons;

@Slf4j
@Configuration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.cassandra.enabled", matchIfMissing = true)
@EnableConfigurationProperties(CassandraProperties.class)
@RequiredArgsConstructor
public class EmbeddedCassandraBootstrapConfiguration {

    private static final String CASSANDRA_NETWORK_ALIAS = "cassandra.testcontainer.docker";

    private final ResourceLoader resourceLoader;

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

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.cassandra", "embeddedCassandraToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_CASSANDRA, destroyMethod = "stop")
    public CassandraContainer cassandra(ConfigurableEnvironment environment,
                                        CassandraProperties properties,
                                        Optional<Network> network,
                                        ContainerStartupCoordinator startupCoordinator) {

        CassandraContainer cassandra = new CassandraContainer(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getPort())
                .withNetworkAliases(CASSANDRA_NETWORK_ALIAS);

        network.ifPresent(cassandra::withNetwork);
        CassandraContainer configuredCassandra = (CassandraContainer) configureCommons(cassandra, properties, log);
        startupCoordinator.schedule(() -> {
            ContainerUtils.startAndLogTime(configuredCassandra, log);
            try {
                initKeyspace(properties, configuredCassandra);
            } catch (ScriptException e) {
                throw new IllegalStateException("Failed to init Cassandra keyspace", e);
            }
            Map<String, Object> cassandraEnv = registerCassandraEnvironment(environment, configuredCassandra, properties);
            log.info("Started Cassandra. Connection details: {}", cassandraEnv);
        });
        return configuredCassandra;
    }

    static Map<String, Object> registerCassandraEnvironment(ConfigurableEnvironment environment,
                                                            CassandraContainer cassandra,
                                                            CassandraProperties properties) {
        String host = cassandra.getHost();
        Integer mappedPort = cassandra.getMappedPort(properties.getPort());
        LinkedHashMap<String, Object> cassandraEnv = new LinkedHashMap<>();
        cassandraEnv.put("embedded.cassandra.port", mappedPort);
        cassandraEnv.put("embedded.cassandra.host", host);
        cassandraEnv.put("embedded.cassandra.datacenter", DEFAULT_DATACENTER);
        cassandraEnv.put("embedded.cassandra.keyspace-name", properties.keyspaceName);
        cassandraEnv.put("embedded.cassandra.networkAlias", CASSANDRA_NETWORK_ALIAS);
        cassandraEnv.put("embedded.cassandra.internalPort", properties.getPort());
        MapPropertySource propertySource = new MapPropertySource("embeddedCassandraInfo", cassandraEnv);
        environment.getPropertySources().addFirst(propertySource);
        return cassandraEnv;
    }

    private void initKeyspace(CassandraProperties properties, CassandraContainer cassandra) throws ScriptException {
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
