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
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

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
                                                SpiceDBProperties properties) {

        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                spicedbContainer,
                properties.getPort(),
                "spicedb");
    }

    @Bean(name = BEAN_NAME_EMBEDDED_SPICEDB, destroyMethod = "stop")
    public GenericContainer<?> spicedb(SpiceDBProperties properties,
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

        return spicedbContainer;
    }


    @Bean
    public DynamicPropertyRegistrar spicedbDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_SPICEDB) GenericContainer<?> spicedb,
            SpiceDBProperties properties) {
        return registry -> {
            registry.add("embedded.spicedb.host", spicedb::getHost);
            registry.add("embedded.spicedb.port", () -> spicedb.getMappedPort(properties.getPort()));
            registry.add("embedded.spicedb.networkAlias", () -> "spicedb.testcontainer.docker");
            registry.add("embedded.spicedb.token", properties::getPresharedKey);
            registry.add("embedded.spicedb.internalPort", properties::getPort);
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "spicedb")
    public DynamicPropertyRegistrar spicedbToxiProxyDynamicPropertyRegistrar(
            @Qualifier("spicedbContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.spicedb");
    }
}
