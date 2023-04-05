package com.playtika.test.cockroach;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import com.playtika.test.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
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
import org.testcontainers.containers.CockroachContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.test.cockroach.CockroachDBProperties.BEAN_NAME_EMBEDDED_COCKROACHDB;
import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.cockroach.enabled", matchIfMissing = true)
@EnableConfigurationProperties(CockroachDBProperties.class)
public class EmbeddedCockroachDBBootstrapConfiguration {

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "cockroach")
    ToxiproxyContainer.ContainerProxy cockroachContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                                @Qualifier(BEAN_NAME_EMBEDDED_COCKROACHDB) CockroachContainer cockroachContainer,
                                                                CockroachDBProperties properties,
                                                                ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(cockroachContainer, properties.getPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.cockroach.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.cockroach.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.cockroach.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedСockroachdbToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started СockroachDB ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_COCKROACHDB, destroyMethod = "stop")
    public CockroachContainer cockroach(ConfigurableEnvironment environment,
                                          CockroachDBProperties properties,
                                          Optional<Network> network) throws Exception {

        CockroachContainer cockroachContainer = new CockroachContainer(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getPort())
                .withInitScript(properties.getInitScriptPath());

        network.ifPresent(cockroachContainer::withNetwork);

        cockroachContainer = (CockroachContainer) configureCommonsAndStart(cockroachContainer, properties, log);
        registerCockroachDBEnvironment(cockroachContainer, environment, properties);
        return cockroachContainer;
    }

    private void registerCockroachDBEnvironment(CockroachContainer cockroach,
                                                ConfigurableEnvironment environment,
                                                CockroachDBProperties properties) {
        Integer mappedPort = cockroach.getMappedPort(properties.getPort());
        String host = cockroach.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.cockroach.port", mappedPort);
        map.put("embedded.cockroach.host", host);
        map.put("embedded.cockroach.schema", cockroach.getDatabaseName());
        map.put("embedded.cockroach.user", cockroach.getUsername());
        map.put("embedded.cockroach.password", cockroach.getPassword());

        String jdbcURL = "jdbc:postgresql://{}:{}/{}";
        log.info("Started CockroachDB server. Connection details: {}, " +
                "JDBC connection url: " + jdbcURL, map, host, mappedPort, cockroach.getDatabaseName());

        MapPropertySource propertySource = new MapPropertySource("embeddedCockroachDBInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
