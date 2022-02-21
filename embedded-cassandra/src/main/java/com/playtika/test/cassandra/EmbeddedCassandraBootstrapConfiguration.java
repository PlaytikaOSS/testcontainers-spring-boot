package com.playtika.test.cassandra;

import com.playtika.test.common.utils.ContainerUtils;
import com.playtika.test.common.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.testcontainers.containers.delegate.CassandraDatabaseDelegate;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;

import javax.script.ScriptException;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.playtika.test.cassandra.CassandraProperties.BEAN_NAME_EMBEDDED_CASSANDRA;
import static com.playtika.test.cassandra.CassandraProperties.DEFAULT_DATACENTER;
import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@AutoConfigureOrder
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.cassandra.enabled", matchIfMissing = true)
@EnableConfigurationProperties(CassandraProperties.class)
@RequiredArgsConstructor
public class EmbeddedCassandraBootstrapConfiguration {

    private final ResourceLoader resourceLoader;

    @Bean(name = BEAN_NAME_EMBEDDED_CASSANDRA, destroyMethod = "stop")
    public CassandraContainer cassandra(ConfigurableEnvironment environment,
                                        CassandraProperties properties) throws Exception {

        CassandraContainer cassandra = new CassandraContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getPort());

        cassandra = (CassandraContainer) configureCommonsAndStart(cassandra, properties, log);

        initKeyspace(properties, cassandra);

        Map<String, Object> cassandraEnv = registerCassandraEnvironment(environment, cassandra, properties);

        log.info("Started Cassandra. Connection details: {}", cassandraEnv);
        return cassandra;
    }

    static Map<String, Object> registerCassandraEnvironment(ConfigurableEnvironment environment,
                                                            CassandraContainer cassandra,
                                                            CassandraProperties properties) {
        String host = cassandra.getContainerIpAddress();
        Integer mappedPort = cassandra.getMappedPort(properties.getPort());
        LinkedHashMap<String, Object> cassandraEnv = new LinkedHashMap<>();
        cassandraEnv.put("embedded.cassandra.port", mappedPort);
        cassandraEnv.put("embedded.cassandra.host", host);
        cassandraEnv.put("embedded.cassandra.datacenter", DEFAULT_DATACENTER);
        cassandraEnv.put("embedded.cassandra.keyspace-name", properties.keyspaceName);
        MapPropertySource propertySource = new MapPropertySource("embeddedCassandraInfo", cassandraEnv);
        environment.getPropertySources().addFirst(propertySource);
        return cassandraEnv;
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
