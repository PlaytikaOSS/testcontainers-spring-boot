package com.playtika.testcontainer.mssqlserver;

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
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.mssqlserver.MSSQLServerProperties.BEAN_NAME_EMBEDDED_MSSQLSERVER;
import static org.testcontainers.containers.MSSQLServerContainer.MS_SQL_SERVER_PORT;

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
    ToxiproxyClientProxy mssqlserverContainerProxy(ToxiproxyClient toxiproxyClient,
                                                    ToxiproxyContainer toxiproxyContainer,
                                                    @Qualifier(BEAN_NAME_EMBEDDED_MSSQLSERVER) EmbeddedMSSQLServerContainer mssqlserver) {

        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                mssqlserver,
                MSSQLServerContainer.MS_SQL_SERVER_PORT,
                "mssqlserver");
    }

    @Bean(name = BEAN_NAME_EMBEDDED_MSSQLSERVER, destroyMethod = "stop")
    public EmbeddedMSSQLServerContainer mssqlServer(MSSQLServerProperties properties,
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

        return mssqlServerContainer;
    }

    @Bean
    public DynamicPropertyRegistrar mssqlServerDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_MSSQLSERVER) EmbeddedMSSQLServerContainer mssqlServerContainer,
            MSSQLServerProperties properties) {
        return registry -> {
            Integer mappedPort = mssqlServerContainer.getMappedPort(MS_SQL_SERVER_PORT);
            String host = mssqlServerContainer.getHost();

            registry.add("embedded.mssqlserver.port", () -> mappedPort);
            registry.add("embedded.mssqlserver.host", () -> host);
            registry.add("embedded.mssqlserver.database", () -> "master");
            registry.add("embedded.mssqlserver.user", () -> "sa");
            registry.add("embedded.mssqlserver.password", properties::getPassword);
            registry.add("embedded.mssqlserver.networkAlias", () -> MSSQLSERVER_NETWORK_ALIAS);
            registry.add("embedded.mssqlserver.internalPort", () -> MS_SQL_SERVER_PORT);

            log.info("""
                Started mssql server. Connection details: embedded.mssqlserver.user=sa, embedded.mssqlserver.password={}, embedded.mssqlserver.database = master,
                JDBC connection url: jdbc:sqlserver://{}:{};databaseName={};trustServerCertificate=true""", properties.getPassword(), host, mappedPort, "master");
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "mssqlserver")
    public DynamicPropertyRegistrar mssqlServerToxiProxyDynamicPropertyRegistrar(
            @Qualifier("mssqlServerContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.mssqlserver");
    }

}
