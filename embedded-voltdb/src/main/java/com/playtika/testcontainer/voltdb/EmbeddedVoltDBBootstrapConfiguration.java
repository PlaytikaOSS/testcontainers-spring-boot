package com.playtika.testcontainer.voltdb;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.voltdb.VoltDBProperties.BEAN_NAME_EMBEDDED_VOLTDB;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.voltdb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(VoltDBProperties.class)
public class EmbeddedVoltDBBootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean
    VoltDBStatusCheck voltDBStartupCheckStrategy() {
        return new VoltDBStatusCheck();
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "voltdb")
    ToxiproxyContainer.ContainerProxy verticaContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                            @Qualifier(BEAN_NAME_EMBEDDED_VOLTDB) GenericContainer<?> embeddedVertica,
                                                            ConfigurableEnvironment environment,
                                                            VoltDBProperties properties) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(embeddedVertica, properties.getPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.vertica.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.vertica.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.vertica.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedVerticaToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Vertica ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_VOLTDB, destroyMethod = "stop")
    public GenericContainer<?> voltDB(ConfigurableEnvironment environment,
                                      VoltDBProperties properties,
                                      VoltDBStatusCheck voltDbStatusCheck,
                                      Optional<Network> network) {

        GenericContainer<?> voltDB = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withEnv("HOST_COUNT", "1")
                .withExposedPorts(properties.port)
                .waitingFor(voltDbStatusCheck);

        network.ifPresent(voltDB::withNetwork);

        voltDB = configureCommonsAndStart(voltDB, properties, log);

        registerVoltDBEnvironment(voltDB, environment, properties);
        return voltDB;
    }

    private void registerVoltDBEnvironment(GenericContainer<?> voltDB,
                                           ConfigurableEnvironment environment,
                                           VoltDBProperties properties) {
        Integer mappedPort = voltDB.getMappedPort(properties.port);
        String host = voltDB.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        map.put("embedded.voltdb.port", mappedPort);
        map.put("embedded.voltdb.host", host);

        String jdbcURL = "jdbc:voltdb://{}:{}";
        log.info("Started VoltDB server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort);

        MapPropertySource propertySource = new MapPropertySource("embeddedVoltDBInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
