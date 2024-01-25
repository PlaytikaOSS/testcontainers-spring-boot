package com.playtika.testcontainer.db2;

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
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.LinkedHashMap;
import java.util.Map;
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
    ToxiproxyContainer.ContainerProxy db2ContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                        @Qualifier(BEAN_NAME_EMBEDDED_DB2) Db2Container db2,
                                                        ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(db2, Db2Container.DB2_PORT);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.db2.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.db2.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.db2.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedDb2ToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started DB2 ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_DB2, destroyMethod = "stop")
    public Db2Container db2(ConfigurableEnvironment environment,
                            Db2Properties properties,
                            Optional<Network> network) {
        Db2Container db2Container = new Db2Container(ContainerUtils.getDockerImageName(properties))
                .withDatabaseName(properties.getDatabase())
                .withUsername(properties.getUser())
                .withPassword(properties.getPassword())
                .withInitScript(properties.getInitScriptPath())
                .withNetworkAliases(DB2_NETWORK_ALIAS);

        network.ifPresent(db2Container::withNetwork);

        String startupLogCheckRegex = properties.getStartupLogCheckRegex();
        if (StringUtils.hasLength(startupLogCheckRegex)) {
            WaitStrategy waitStrategy = new LogMessageWaitStrategy()
                    .withRegEx(startupLogCheckRegex);
            db2Container.waitingFor(waitStrategy);
        }

        if (properties.isAcceptLicence()) {
            db2Container.acceptLicense();
        }

        db2Container = (Db2Container) configureCommonsAndStart(db2Container, properties, log);
        registerDb2Environment(db2Container, environment, properties);

        return db2Container;
    }

    private void registerDb2Environment(Db2Container db2Container,
                                        ConfigurableEnvironment environment,
                                        Db2Properties properties) {
        Integer mappedPort = db2Container.getMappedPort(Db2Container.DB2_PORT);
        String host = db2Container.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.db2.port", mappedPort);
        map.put("embedded.db2.host", host);
        map.put("embedded.db2.database", properties.getDatabase());
        map.put("embedded.db2.user", properties.getUser());
        map.put("embedded.db2.password", properties.getPassword());
        map.put("embedded.db2.networkAlias", DB2_NETWORK_ALIAS);
        map.put("embedded.db2.internalPort", Db2Container.DB2_PORT);

        String jdbcURL = "jdbc:db2://{}:{}/{}";
        log.info("Started db2 server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort, properties.getDatabase());

        MapPropertySource propertySource = new MapPropertySource("embeddedDb2Info", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
