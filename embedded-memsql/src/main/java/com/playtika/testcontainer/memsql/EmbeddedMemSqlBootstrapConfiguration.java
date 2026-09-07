package com.playtika.testcontainer.memsql;

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.Network;
import org.testcontainers.toxiproxy.ToxiproxyContainer;
import org.testcontainers.utility.MountableFile;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommons;
import static com.playtika.testcontainer.memsql.MemSqlProperties.BEAN_NAME_EMBEDDED_MEMSQL;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.memsql.enabled", matchIfMissing = true)
@EnableConfigurationProperties(MemSqlProperties.class)
public class EmbeddedMemSqlBootstrapConfiguration {

    private static final String MEMSQL_NETWORK_ALIAS = "memsql.testcontainer.docker";

    @Bean
    @ConditionalOnMissingBean
    MemSqlStatusCheck memSqlStartupCheckStrategy(MemSqlProperties properties) {
        return new MemSqlStatusCheck(properties);
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "memsql")
    ToxiproxyClientProxy memsqlContainerProxy(ToxiproxyClient toxiproxyClient,
                                               ToxiproxyContainer toxiproxyContainer,
                                               @Qualifier(BEAN_NAME_EMBEDDED_MEMSQL) MemSqlContainer memsql,
                                               MemSqlProperties properties,
                                               ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                memsql,
                properties.getPort(),
                "memsql");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.memsql", "embeddedMemsqlToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_MEMSQL, destroyMethod = "stop")
    public MemSqlContainer memsql(ConfigurableEnvironment environment,
                                   MemSqlProperties properties,
                                   MemSqlStatusCheck memSqlStatusCheck,
                                   Optional<Network> network,
                                   ContainerStartupCoordinator startupCoordinator) {
        MemSqlContainer memsql = new MemSqlContainer(ContainerUtils.getDockerImageName(properties))
                .withDatabaseName(properties.getDatabase())
                .withUsername(properties.getUser())
                .withPassword(properties.getPassword())
                .withLicenseKey(properties.getLicenseKey())
                .withCopyFileToContainer(MountableFile.forClasspathResource("mem.sql"), "/schema.sql")
                .waitingFor(memSqlStatusCheck)
                .withNetworkAliases(MEMSQL_NETWORK_ALIAS);

        if ("aarch".equals(System.getProperty("system.arch"))) {
            memsql = memsql.withCommand("platform", "linux/amd64");
        }

        network.ifPresent(memsql::withNetwork);
        MemSqlContainer configuredMemsql = (MemSqlContainer) configureCommons(memsql, properties, log);
        startupCoordinator.schedule(() -> {
            ContainerUtils.startAndLogTime(configuredMemsql, log);
            registerMemSqlEnvironment(configuredMemsql, environment, properties);
        });
        return configuredMemsql;
    }

    private void registerMemSqlEnvironment(MemSqlContainer memsql,
                                            ConfigurableEnvironment environment,
                                            MemSqlProperties properties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.memsql.port", memsql.getMappedPort(MemSqlContainer.MEMSQL_PORT));
        map.put("embedded.memsql.host", memsql.getHost());
        map.put("embedded.memsql.schema", memsql.getDatabaseName());
        map.put("embedded.memsql.user", memsql.getUsername());
        map.put("embedded.memsql.password", memsql.getPassword());
        map.put("embedded.memsql.jdbcUrl", memsql.getJdbcUrl());
        map.put("embedded.memsql.networkAlias", MEMSQL_NETWORK_ALIAS);
        map.put("embedded.memsql.internalPort", MemSqlContainer.MEMSQL_PORT);

        log.info("Started memsql server. Connection details {} ", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedMemSqlInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
