package com.playtika.testcontainer.spicedb;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.EmbeddedToxiProxyBootstrapConfiguration;
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
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.spicedb.SpiceDBProperties.BEAN_NAME_EMBEDDED_SPICEDB;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter({DockerPresenceBootstrapConfiguration.class, EmbeddedToxiProxyBootstrapConfiguration.class})
@ConditionalOnProperty(name = "embedded.spicedb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(SpiceDBProperties.class)
public class EmbeddedSpiceDBBootstrapConfiguration {

    private static final String NATS_NETWORK_ALIAS = "spicedb.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "spicedb")
    ToxiproxyClientProxy spicedbContainerProxy(ToxiproxyClient toxiproxyClient,
                                                ToxiproxyContainer toxiproxyContainer,
                                                @Qualifier(BEAN_NAME_EMBEDDED_SPICEDB) GenericContainer<?> spicedbContainer,
                                                SpiceDBProperties properties,
                                                ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                spicedbContainer,
                properties.getPort(),
                "spicedb");
        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.spicedb", "embeddedSpicedbToxiProxyInfo", environment);
        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_SPICEDB, destroyMethod = "stop")
    public GenericContainer<?> spicedb(ConfigurableEnvironment environment,
                                       SpiceDBProperties properties,
                                       Optional<Network> network) {
        WaitStrategy waitStrategy = new WaitAllStrategy()
            .withStrategy(new HostPortWaitStrategy())
            .withStartupTimeout(properties.getTimeoutDuration());

        GenericContainer<?> spicedbContainer = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
            .withExposedPorts(properties.getPort())
            .withCommand("serve", "--grpc-preshared-key", properties.getPresharedKey(), "--skip-release-check")
            .waitingFor(waitStrategy)
            .withNetworkAliases(NATS_NETWORK_ALIAS);

        network.ifPresent(spicedbContainer::withNetwork);

        spicedbContainer = configureCommonsAndStart(spicedbContainer, properties, log);
        registerSpiceDBEnvironment(spicedbContainer, environment, properties);
        return spicedbContainer;
    }

    private void registerSpiceDBEnvironment(GenericContainer<?> spicedb,
                                            ConfigurableEnvironment environment,
                                            SpiceDBProperties properties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.spicedb.host", spicedb.getHost());
        map.put("embedded.spicedb.port", spicedb.getMappedPort(properties.getPort()));
        map.put("embedded.spicedb.networkAlias", "spicedb.testcontainer.docker");
        map.put("embedded.spicedb.token", properties.getPresharedKey());
        map.put("embedded.spicedb.internalPort", properties.getPort());

        MapPropertySource propertySource = new MapPropertySource("embeddedSpicedbInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
