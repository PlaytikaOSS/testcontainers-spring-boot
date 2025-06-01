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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

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

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "mssqlserver")
    ToxiproxyClientProxy mssqlserverContainerProxy(ToxiproxyClient toxiproxyClient,
                                                    ToxiproxyContainer toxiproxyContainer,
                                                    @Qualifier(BEAN_NAME_EMBEDDED_MSSQLSERVER) EmbeddedMSSQLServerContainer mssqlserver,
                                                    ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                mssqlserver,
                MSSQLServerContainer.MS_SQL_SERVER_PORT,
                "mssqlserver");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.mssqlserver", "embeddedMSSQLServerToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_MSSQLSERVER, destroyMethod = "stop")
    public EmbeddedMSSQLServerContainer mssqlServer(MSSQLServerProperties properties,
                                                    Optional<Network> network) {
        EmbeddedMSSQLServerContainer mssqlServerContainer = new EmbeddedMSSQLServerContainer(ContainerUtils.getDockerImageName(properties))
                .withNetworkAliases("mssqlserver.testcontainer.docker");
        network.ifPresent(mssqlServerContainer::withNetwork);
        configureCommonsAndStart(mssqlServerContainer, properties, log);
        return mssqlServerContainer;
    }

    @Bean
    public DynamicPropertyRegistrar mssqlServerDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_MSSQLSERVER) EmbeddedMSSQLServerContainer mssqlServer,
            MSSQLServerProperties properties) {
        return registry -> {
            registry.add("embedded.mssqlserver.host", mssqlServer::getHost);
            registry.add("embedded.mssqlserver.port", () -> mssqlServer.getMappedPort(1433));
            registry.add("embedded.mssqlserver.networkAlias", () -> "mssqlserver.testcontainer.docker");
            registry.add("embedded.mssqlserver.internalPort", () -> 1433);
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "mssqlserver")
    public DynamicPropertyRegistrar mssqlServerToxiProxyDynamicPropertyRegistrar(
            @Qualifier("mssqlServerContainerProxy") ToxiproxyContainer.ContainerProxy proxy) {
        return registry -> {
            registry.add("embedded.mssqlserver.toxiproxy.host", proxy::getContainerIpAddress);
            registry.add("embedded.mssqlserver.toxiproxy.port", proxy::getProxyPort);
            registry.add("embedded.mssqlserver.toxiproxy.proxyName", proxy::getName);
        };
    }

}
