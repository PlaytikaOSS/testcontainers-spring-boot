package com.playtika.testcontainer.postgresql;

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
import org.springframework.util.StringUtils;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.postgresql.PostgreSQLProperties.BEAN_NAME_EMBEDDED_POSTGRESQL;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.postgresql.enabled", matchIfMissing = true)
@EnableConfigurationProperties(PostgreSQLProperties.class)
public class EmbeddedPostgreSQLBootstrapConfiguration {

    private static final String POSTGRESQL_NETWORK_ALIAS = "postgresql.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "postgresql")
    ToxiproxyClientProxy postgresqlContainerProxy(ToxiproxyClient toxiproxyClient,
                                                   ToxiproxyContainer toxiproxyContainer,
                                                   @Qualifier(BEAN_NAME_EMBEDDED_POSTGRESQL) PostgreSQLContainer postgresql,
                                                   ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                postgresql,
                PostgreSQLContainer.POSTGRESQL_PORT,
                "postgresql");
        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.postgresql", "embeddedPostgreSQLToxiProxyInfo", environment);
        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_POSTGRESQL, destroyMethod = "stop")
    public PostgreSQLContainer postgresql(ConfigurableEnvironment environment,
                                          PostgreSQLProperties properties,
                                          Optional<Network> network) {
        PostgreSQLContainer postgresql =
                new PostgreSQLContainer<>(ContainerUtils.getDockerImageName(properties))
                        .withUsername(properties.getUser())
                        .withPassword(properties.getPassword())
                        .withDatabaseName(properties.getDatabase())
                        .withInitScript(properties.initScriptPath)
                        .withNetworkAliases(POSTGRESQL_NETWORK_ALIAS);
        network.ifPresent(postgresql::withNetwork);
        String startupLogCheckRegex = properties.getStartupLogCheckRegex();
        if (StringUtils.hasLength(startupLogCheckRegex)) {
            WaitStrategy waitStrategy = new LogMessageWaitStrategy()
                    .withRegEx(startupLogCheckRegex);
            postgresql.waitingFor(waitStrategy);
        }
        postgresql = (PostgreSQLContainer) configureCommonsAndStart(postgresql, properties, log);
        registerPostgreSQLEnvironment(postgresql, environment, properties);
        return postgresql;
    }

    private void registerPostgreSQLEnvironment(PostgreSQLContainer postgresql,
                                               ConfigurableEnvironment environment,
                                               PostgreSQLProperties properties) {
        Integer mappedPort = postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
        String host = postgresql.getHost();
        String jdbcURL = "jdbc:postgresql://" + host + ":" + mappedPort + "/" + properties.getDatabase();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.postgresql.port", mappedPort);
        map.put("embedded.postgresql.host", host);
        map.put("embedded.postgresql.schema", properties.getDatabase());
        map.put("embedded.postgresql.user", properties.getUser());
        map.put("embedded.postgresql.password", properties.getPassword());
        map.put("embedded.postgresql.networkAlias", POSTGRESQL_NETWORK_ALIAS);
        map.put("embedded.postgresql.internalPort", PostgreSQLContainer.POSTGRESQL_PORT);

        log.info("Started postgresql server. Connection details: host={}, port={}, schema={}, user={}, password={}, networkAlias={}, internalPort={}, JDBC connection url: {}",
                host, mappedPort, properties.getDatabase(), properties.getUser(), properties.getPassword(), POSTGRESQL_NETWORK_ALIAS, PostgreSQLContainer.POSTGRESQL_PORT, jdbcURL);

        MapPropertySource propertySource = new MapPropertySource("embeddedPostgreSQLInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
