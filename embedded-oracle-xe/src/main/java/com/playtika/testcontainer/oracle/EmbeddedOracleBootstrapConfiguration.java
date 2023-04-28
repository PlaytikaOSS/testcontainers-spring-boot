package com.playtika.testcontainer.oracle;

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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.oracle.OracleProperties.BEAN_NAME_EMBEDDED_ORACLE;
import static com.playtika.testcontainer.oracle.OracleProperties.ORACLE_DB;
import static com.playtika.testcontainer.oracle.OracleProperties.ORACLE_PORT;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.oracle.enabled", matchIfMissing = true)
@EnableConfigurationProperties(OracleProperties.class)
public class EmbeddedOracleBootstrapConfiguration {

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "oracle")
    ToxiproxyContainer.ContainerProxy oracleContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                           @Qualifier(BEAN_NAME_EMBEDDED_ORACLE) OracleContainer oracle,
                                                           ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(oracle, ORACLE_PORT);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.oracle.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.oracle.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.oracle.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedOracleToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Oracle ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_ORACLE, destroyMethod = "stop")
    public OracleContainer oracle(ConfigurableEnvironment environment,
                                  OracleProperties properties,
                                  Optional<Network> network) {

        OracleContainer oracle =
                new OracleContainer(ContainerUtils.getDockerImageName(properties))
                        .withUsername(properties.getUser())
                        .withPassword(properties.getPassword())
                        .withInitScript(properties.initScriptPath);

        network.ifPresent(oracle::withNetwork);
        oracle = (OracleContainer) configureCommonsAndStart(oracle, properties, log);
        registerOracleEnvironment(oracle, environment, properties);
        return oracle;
    }

    private void registerOracleEnvironment(OracleContainer oracle,
                                           ConfigurableEnvironment environment,
                                           OracleProperties properties) {
        Integer mappedPort = oracle.getMappedPort(ORACLE_PORT);
        String host = oracle.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.oracle.port", mappedPort);
        map.put("embedded.oracle.host", host);
        map.put("embedded.oracle.database", properties.getDatabase());
        map.put("embedded.oracle.user", properties.getUser());
        map.put("embedded.oracle.password", properties.getPassword());

        String jdbcURL = "jdbc:oracle://{}:{}/{}";
        log.info("Started oracle server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort, ORACLE_DB);

        MapPropertySource propertySource = new MapPropertySource("embeddedOracleInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
