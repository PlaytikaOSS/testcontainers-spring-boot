package com.playtika.testcontainer.vertica;

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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import java.time.Duration;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.vertica.VerticaProperties.BEAN_NAME_EMBEDDED_VERTICA;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.vertica.enabled", matchIfMissing = true)
@EnableConfigurationProperties(VerticaProperties.class)
public class EmbeddedVerticaBootstrapConfiguration {

    private static final String VERTICA_NETWORK_ALIAS = "vertica.testcontainer.docker";
    private static final int VERTICA_STARTUP_TIMEOUT_IN_SECONDS = 120;

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "vertica")
    ToxiproxyClientProxy verticaContainerProxy(ToxiproxyClient toxiproxyClient,
                                                ToxiproxyContainer toxiproxyContainer,
                                                @Qualifier(BEAN_NAME_EMBEDDED_VERTICA) GenericContainer<?> embeddedVertica,
                                               VerticaProperties verticaProperties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                embeddedVertica,
                verticaProperties.getPort(),
                "vertica");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "vertica")
    public DynamicPropertyRegistrar verticaToxiProxyDynamicPropertyRegistrar(@Qualifier("verticaContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.vertica");
    }

    @Bean(name = BEAN_NAME_EMBEDDED_VERTICA, destroyMethod = "stop")
    public GenericContainer<?> embeddedVertica(VerticaProperties properties,
                                               Optional<Network> network) {
        GenericContainer<?> verticaContainer = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
            .withExposedPorts(properties.getPort())
            .withEnv("DATABASE_NAME", properties.getDatabase())
            .withEnv("DATABASE_PASSWORD", properties.getPassword())
            .withStartupTimeout(Duration.ofSeconds(VERTICA_STARTUP_TIMEOUT_IN_SECONDS))
            .waitingFor(new HostPortWaitStrategy())
            .withNetwork(Network.SHARED)
            .withNetworkAliases(VERTICA_NETWORK_ALIAS);

        network.ifPresent(verticaContainer::withNetwork);

        verticaContainer = configureCommonsAndStart(verticaContainer, properties, log);

        Integer mappedPort = verticaContainer.getMappedPort(properties.getPort());
        String host = verticaContainer.getHost();
        log.info("Started Vertica server. Connection details: port={}, host={}, database={}, user={}, password={}, networkAlias={}, internalPort={}",
                mappedPort, host, properties.getDatabase(), properties.getUser(), properties.getPassword(), VERTICA_NETWORK_ALIAS, properties.getPort());
        return verticaContainer;
    }

    @Bean
    public DynamicPropertyRegistrar verticaDynamicPropertyRegistrar(@Qualifier(BEAN_NAME_EMBEDDED_VERTICA) GenericContainer<?> verticaContainer, VerticaProperties properties) {
        return registry -> {
            registry.add("embedded.vertica.port", () -> verticaContainer.getMappedPort(properties.getPort()));
            registry.add("embedded.vertica.host", verticaContainer::getHost);
            registry.add("embedded.vertica.database", properties::getDatabase);
            registry.add("embedded.vertica.user", properties::getUser);
            registry.add("embedded.vertica.password", properties::getPassword);
            registry.add("embedded.vertica.networkAlias", () -> VERTICA_NETWORK_ALIAS);
            registry.add("embedded.vertica.internalPort", properties::getPort);
        };
    }

}
