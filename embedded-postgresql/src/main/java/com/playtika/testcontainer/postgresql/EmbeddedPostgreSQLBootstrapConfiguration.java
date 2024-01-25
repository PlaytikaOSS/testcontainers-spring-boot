package com.playtika.testcontainer.postgresql;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
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
import org.testcontainers.containers.wait.strategy.ShellStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.postgresql.PostgreSQLProperties.BEAN_NAME_EMBEDDED_POSTGRESQL;
import static java.lang.String.format;

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
    ToxiproxyContainer.ContainerProxy postgresqlContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                               @Qualifier(BEAN_NAME_EMBEDDED_POSTGRESQL) PostgreSQLContainer postgresql,
                                                               ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(postgresql, PostgreSQLContainer.POSTGRESQL_PORT);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.postgresql.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.postgresql.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.postgresql.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedPostgresqlToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Postgresql ToxiProxy connection details {}", map);

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

        postgresql.waitingFor(new ShellStrategy()
                .withCommand(format("pg_isready -U %s -d %s", properties.getUser(), properties.getDatabase()))
                .withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS)));

        String startupLogCheckRegex = properties.getStartupLogCheckRegex();
        if (StringUtils.hasLength(startupLogCheckRegex)) {
            WaitStrategy waitStrategy = new LogMessageWaitStrategy()
                    .withRegEx(startupLogCheckRegex);
            postgresql.waitingFor(waitStrategy);
        }

        postgresql = (PostgreSQLContainer) configureCommonsAndStart(postgresql, properties, log);
        registerPostgresqlEnvironment(postgresql, environment, properties);
        return postgresql;
    }

    private void registerPostgresqlEnvironment(PostgreSQLContainer postgresql,
                                               ConfigurableEnvironment environment,
                                               PostgreSQLProperties properties) {
        Integer mappedPort = postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
        String host = postgresql.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.postgresql.port", mappedPort);
        map.put("embedded.postgresql.host", host);
        map.put("embedded.postgresql.schema", properties.getDatabase());
        map.put("embedded.postgresql.user", properties.getUser());
        map.put("embedded.postgresql.password", properties.getPassword());
        map.put("embedded.postgresql.networkAlias", POSTGRESQL_NETWORK_ALIAS);
        map.put("embedded.postgresql.internalPort", PostgreSQLContainer.POSTGRESQL_PORT);

        String jdbcURL = "jdbc:postgresql://{}:{}/{}";
        log.info("Started postgresql server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort, properties.getDatabase());

        MapPropertySource propertySource = new MapPropertySource("embeddedPostgreInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
