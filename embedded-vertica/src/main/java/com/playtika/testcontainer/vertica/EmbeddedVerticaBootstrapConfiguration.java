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
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import java.time.Duration;
import java.util.LinkedHashMap;
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
                                               VerticaProperties verticaProperties,
                                               ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                embeddedVertica,
                verticaProperties.getPort(),
                "vertica");
        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.vertica", "embeddedVerticaToxiProxyInfo", environment);
        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_VERTICA, destroyMethod = "stop")
    public GenericContainer<?> embeddedVertica(ConfigurableEnvironment environment,
                                               VerticaProperties properties,
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
        registerVerticaEnvironment(verticaContainer, environment, properties);
        return verticaContainer;
    }

    private void registerVerticaEnvironment(GenericContainer<?> verticaContainer,
                                            ConfigurableEnvironment environment,
                                            VerticaProperties properties) {
        Integer mappedPort = verticaContainer.getMappedPort(properties.getPort());
        String host = verticaContainer.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.vertica.port", mappedPort);
        map.put("embedded.vertica.host", host);
        map.put("embedded.vertica.database", properties.getDatabase());
        map.put("embedded.vertica.user", properties.getUser());
        map.put("embedded.vertica.password", properties.getPassword());
        map.put("embedded.vertica.networkAlias", VERTICA_NETWORK_ALIAS);
        map.put("embedded.vertica.internalPort", properties.getPort());

        log.info("Started Vertica server. Connection details: port={}, host={}, database={}, user={}, password={}, networkAlias={}, internalPort={}",
                mappedPort, host, properties.getDatabase(), properties.getUser(), properties.getPassword(), VERTICA_NETWORK_ALIAS, properties.getPort());

        MapPropertySource propertySource = new MapPropertySource("embeddedVerticaInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
