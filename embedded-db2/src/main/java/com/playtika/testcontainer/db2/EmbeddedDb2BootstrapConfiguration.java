package com.playtika.testcontainer.db2;

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
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.db2.Db2Properties.BEAN_NAME_EMBEDDED_DB2;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.db2.enabled", matchIfMissing = true)
@EnableConfigurationProperties(Db2Properties.class)
public class EmbeddedDb2BootstrapConfiguration {

    private static final String DB2_NETWORK_ALIAS = "db2.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "db2")
    ToxiproxyClientProxy db2ContainerProxy(ToxiproxyClient toxiproxyClient,
                                            ToxiproxyContainer toxiproxyContainer,
                                            @Qualifier(BEAN_NAME_EMBEDDED_DB2) Db2Container db2) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                db2,
                Db2Container.DB2_PORT,
                "db2");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "db2")
    public DynamicPropertyRegistrar db2ToxiProxyDynamicPropertyRegistrar(@Qualifier("db2ContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.db2");
    }

    @Bean(name = BEAN_NAME_EMBEDDED_DB2, destroyMethod = "stop")
    public Db2Container db2(Db2Properties properties,
                            Optional<Network> network) {
        Db2Container db2Container = new Db2Container(ContainerUtils.getDockerImageName(properties))
                .withDatabaseName(properties.getDatabase())
                .withUsername(properties.getUser())
                .withPassword(properties.getPassword())
                .withInitScript(properties.getInitScriptPath())
                .withNetworkAliases(DB2_NETWORK_ALIAS);
        network.ifPresent(db2Container::withNetwork);
        if (StringUtils.hasLength(properties.getStartupLogCheckRegex())) {
            db2Container = db2Container.waitingFor(new LogMessageWaitStrategy()
                    .withRegEx(properties.getStartupLogCheckRegex()));
        }
        if (properties.isAcceptLicence()) {
            db2Container = db2Container.acceptLicense();
        }
        if ("aarch".equals(System.getProperty("system.arch"))){
            db2Container = db2Container.withCommand("platform", "linux/amd64");
        }
        db2Container = (Db2Container) configureCommonsAndStart(db2Container, properties, log);
        Integer mappedPort = db2Container.getMappedPort(Db2Container.DB2_PORT);
        String host = db2Container.getHost();
        String jdbcURL = "jdbc:db2://" + host + ":" + mappedPort + "/" + properties.getDatabase();
        log.info("Started db2 server. Connection details: host={}, port={}, database={}, user={}, password={}, networkAlias={}, internalPort={}, JDBC connection url: {}",
                host, mappedPort, properties.getDatabase(), properties.getUser(), properties.getPassword(), DB2_NETWORK_ALIAS, Db2Container.DB2_PORT, jdbcURL);
        return db2Container;
    }

    @Bean
    public DynamicPropertyRegistrar db2DynamicPropertyRegistrar(@Qualifier(BEAN_NAME_EMBEDDED_DB2) Db2Container db2Container, Db2Properties properties) {
        return registry -> {
            registry.add("embedded.db2.port", () -> db2Container.getMappedPort(Db2Container.DB2_PORT));
            registry.add("embedded.db2.host", db2Container::getHost);
            registry.add("embedded.db2.database", properties::getDatabase);
            registry.add("embedded.db2.user", properties::getUser);
            registry.add("embedded.db2.password", properties::getPassword);
            registry.add("embedded.db2.networkAlias", () -> DB2_NETWORK_ALIAS);
            registry.add("embedded.db2.internalPort", () -> Db2Container.DB2_PORT);
        };
    }

}
