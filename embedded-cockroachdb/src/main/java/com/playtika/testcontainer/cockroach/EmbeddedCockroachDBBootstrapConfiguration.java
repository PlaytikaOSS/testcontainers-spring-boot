package com.playtika.testcontainer.cockroach;

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
import org.testcontainers.containers.CockroachContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.Optional;

import static com.playtika.testcontainer.cockroach.CockroachDBProperties.BEAN_NAME_EMBEDDED_COCKROACHDB;
import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.cockroach.enabled", matchIfMissing = true)
@EnableConfigurationProperties(CockroachDBProperties.class)
public class EmbeddedCockroachDBBootstrapConfiguration {

    private static final String COCKROACHDB_NETWORK_ALIAS = "сockroachdb.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "cockroach")
    ToxiproxyClientProxy cockroachContainerProxy(ToxiproxyClient toxiproxyClient,
                                                  ToxiproxyContainer toxiproxyContainer,
                                                  @Qualifier(BEAN_NAME_EMBEDDED_COCKROACHDB) CockroachContainer cockroachContainer,
                                                  ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                cockroachContainer,
                CockroachDBProperties.PORT,
                "cockroach");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.cockroach", "embeddedСockroachdbToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_COCKROACHDB, destroyMethod = "stop")
    public CockroachContainer cockroach(CockroachDBProperties properties,
                                        Optional<Network> network) throws Exception {
        CockroachContainer cockroachContainer = new CockroachContainer(ContainerUtils.getDockerImageName(properties))
                .withInitScript(properties.getInitScriptPath())
                .withNetworkAliases(COCKROACHDB_NETWORK_ALIAS);

        network.ifPresent(cockroachContainer::withNetwork);

        cockroachContainer = (CockroachContainer) configureCommonsAndStart(cockroachContainer, properties, log);
        return cockroachContainer;
    }

    @Bean
    public DynamicPropertyRegistrar cockroachDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_COCKROACHDB) CockroachContainer cockroach) {
        return registry -> {
            registry.add("embedded.cockroach.port", () -> cockroach.getMappedPort(CockroachDBProperties.PORT));
            registry.add("embedded.cockroach.host", cockroach::getHost);
            registry.add("embedded.cockroach.schema", cockroach::getDatabaseName);
            registry.add("embedded.cockroach.user", cockroach::getUsername);
            registry.add("embedded.cockroach.password", cockroach::getPassword);
            registry.add("embedded.cockroach.networkAlias", () -> COCKROACHDB_NETWORK_ALIAS);
            registry.add("embedded.cockroach.internalPort", () -> CockroachDBProperties.PORT);
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "cockroach")
    public DynamicPropertyRegistrar cockroachToxiProxyDynamicPropertyRegistrar(
            @Qualifier("cockroachContainerProxy") ToxiproxyContainer.ContainerProxy proxy) {
        return registry -> {
            registry.add("embedded.cockroach.toxiproxy.host", proxy::getContainerIpAddress);
            registry.add("embedded.cockroach.toxiproxy.port", proxy::getProxyPort);
            registry.add("embedded.cockroach.toxiproxy.proxyName", proxy::getName);
        };
    }
}
