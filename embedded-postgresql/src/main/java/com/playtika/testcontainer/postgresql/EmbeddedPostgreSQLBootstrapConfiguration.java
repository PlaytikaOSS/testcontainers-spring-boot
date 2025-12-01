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
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.util.StringUtils;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

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
                                                   @Qualifier(BEAN_NAME_EMBEDDED_POSTGRESQL) PostgreSQLContainer postgresql) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                postgresql,
                PostgreSQLContainer.POSTGRESQL_PORT,
                "postgresql");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "postgresql")
    public DynamicPropertyRegistrar postgresqlToxiProxyDynamicPropertyRegistrar(@Qualifier("postgresqlContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.postgresql");
    }

    @Bean(name = BEAN_NAME_EMBEDDED_POSTGRESQL, destroyMethod = "stop")
    public PostgreSQLContainer postgresql(PostgreSQLProperties properties,
                                          Optional<Network> network) {
        PostgreSQLContainer postgresql =
                new PostgreSQLContainer(ContainerUtils.getDockerImageName(properties))
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
        Integer mappedPort = postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
        String host = postgresql.getHost();
        String jdbcURL = "jdbc:postgresql://" + host + ":" + mappedPort + "/" + properties.getDatabase();
        log.info("Started postgresql server. Connection details: host={}, port={}, schema={}, user={}, password={}, networkAlias={}, internalPort={}, JDBC connection url: {}",
                host, mappedPort, properties.getDatabase(), properties.getUser(), properties.getPassword(), POSTGRESQL_NETWORK_ALIAS, PostgreSQLContainer.POSTGRESQL_PORT, jdbcURL);
        return postgresql;
    }

    @Bean
    public DynamicPropertyRegistrar postgresqlDynamicPropertyRegistrar(@Qualifier(BEAN_NAME_EMBEDDED_POSTGRESQL) PostgreSQLContainer postgresql, PostgreSQLProperties properties) {
        return registry -> {
            registry.add("embedded.postgresql.port", () -> postgresql.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
            registry.add("embedded.postgresql.host", postgresql::getHost);
            registry.add("embedded.postgresql.schema", properties::getDatabase);
            registry.add("embedded.postgresql.user", properties::getUser);
            registry.add("embedded.postgresql.password", properties::getPassword);
            registry.add("embedded.postgresql.networkAlias", () -> POSTGRESQL_NETWORK_ALIAS);
            registry.add("embedded.postgresql.internalPort", () -> PostgreSQLContainer.POSTGRESQL_PORT);
        };
    }

}
