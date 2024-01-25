package com.playtika.testcontainer.mssqlserver;

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
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.mssqlserver.MSSQLServerProperties.BEAN_NAME_EMBEDDED_MSSQLSERVER;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.mssqlserver.enabled", matchIfMissing = true)
@EnableConfigurationProperties(MSSQLServerProperties.class)
public class EmbeddedMSSQLServerBootstrapConfiguration {

    private static final String MSSQLSERVER_NETWORK_ALIAS = "mssqlserver.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "mssqlserver")
    ToxiproxyContainer.ContainerProxy mssqlserverContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                                @Qualifier(BEAN_NAME_EMBEDDED_MSSQLSERVER) EmbeddedMSSQLServerContainer mssqlserver,
                                                                ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(mssqlserver, MSSQLServerContainer.MS_SQL_SERVER_PORT);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mssqlserver.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.mssqlserver.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.mssqlserver.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedMSSQLServerToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started MSSQLServer ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_MSSQLSERVER, destroyMethod = "stop")
    public EmbeddedMSSQLServerContainer mssqlserver(ConfigurableEnvironment environment,
                                                    MSSQLServerProperties properties,
                                                    Optional<Network> network) {

        EmbeddedMSSQLServerContainer mssqlServerContainer = new EmbeddedMSSQLServerContainer(ContainerUtils.getDockerImageName(properties))
                .withPassword(properties.getPassword())
                .withInitScript(properties.getInitScriptPath())
                .withNetworkAliases(MSSQLSERVER_NETWORK_ALIAS);

        network.ifPresent(mssqlServerContainer::withNetwork);

        String startupLogCheckRegex = properties.getStartupLogCheckRegex();
        if (StringUtils.hasLength(startupLogCheckRegex)) {
            WaitStrategy waitStrategy = new LogMessageWaitStrategy()
                    .withRegEx(startupLogCheckRegex);
            mssqlServerContainer.waitingFor(waitStrategy);
        }

        if (properties.isAcceptLicence()) {
            mssqlServerContainer.acceptLicense();
        }

        mssqlServerContainer = (EmbeddedMSSQLServerContainer) configureCommonsAndStart(mssqlServerContainer, properties, log);
        registerMSSQLServerEnvironment(mssqlServerContainer, environment, properties);

        return mssqlServerContainer;
    }

    private void registerMSSQLServerEnvironment(MSSQLServerContainer<?> mssqlServerContainer,
                                                ConfigurableEnvironment environment,
                                                MSSQLServerProperties properties) {
        Integer mappedPort = mssqlServerContainer.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT);
        String host = mssqlServerContainer.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mssqlserver.port", mappedPort);
        map.put("embedded.mssqlserver.host", host);
        // Database and user cannot be chosen when starting the MSSQL image
        map.put("embedded.mssqlserver.database", "master");
        map.put("embedded.mssqlserver.user", "sa");
        map.put("embedded.mssqlserver.password", properties.getPassword());
        map.put("embedded.mssqlserver.networkAlias", MSSQLSERVER_NETWORK_ALIAS);
        map.put("embedded.mssqlserver.internalPort", MSSQLServerContainer.MS_SQL_SERVER_PORT);

        String jdbcURL = "jdbc:sqlserver://{}:{};databaseName={};trustServerCertificate=true";
        log.info("Started mssql server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort, "master");

        MapPropertySource propertySource = new MapPropertySource("embeddedMSSQLServerInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
