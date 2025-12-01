package com.playtika.testcontainer.consul;

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
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.consul.ConsulProperties.BEAN_NAME_EMBEDDED_CONSUL;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@EnableConfigurationProperties(ConsulProperties.class)
@ConditionalOnProperty(name = "embedded.consul.enabled", matchIfMissing = true)
public class EmbeddedConsulBootstrapConfiguration {

    private static final String CONSUL_NETWORK_ALIAS = "consul.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "consul")
    ToxiproxyClientProxy consulContainerProxy(ToxiproxyClient toxiproxyClient,
                                               ToxiproxyContainer toxiproxyContainer,
                                               @Qualifier(BEAN_NAME_EMBEDDED_CONSUL) GenericContainer<?> consulContainer,
                                               ConsulProperties properties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                consulContainer,
                properties.getPort(),
                "consul");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "consul")
    public DynamicPropertyRegistrar consulToxiProxyDynamicPropertyRegistrar(@Qualifier("consulContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.consul");
    }

    @Bean(name = BEAN_NAME_EMBEDDED_CONSUL, destroyMethod = "stop")
    public GenericContainer<?> consulContainer(ConsulProperties properties, Optional<Network> network) {
        GenericContainer<?> consul = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getPort())
                .waitingFor(
                        Wait.forHttp("/v1/status/leader")
                                .forStatusCode(200)
                ).withStartupTimeout(properties.getTimeoutDuration())
                .withNetworkAliases(CONSUL_NETWORK_ALIAS);

        network.ifPresent(consul::withNetwork);

        if (properties.getConfigurationFile() != null) {
            consul = consul.withClasspathResourceMapping(
                    properties.getConfigurationFile(), "/consul/config/test.hcl",
                    BindMode.READ_ONLY);
        }

        consul = configureCommonsAndStart(consul, properties, log);
        return consul;
    }

    @Bean
    public DynamicPropertyRegistrar consulDynamicPropertyRegistrar(@Qualifier(BEAN_NAME_EMBEDDED_CONSUL) GenericContainer<?> consul, ConsulProperties properties) {
        return registry -> {
            registry.add("embedded.consul.port", () -> consul.getMappedPort(properties.getPort()));
            registry.add("embedded.consul.host", consul::getHost);
            registry.add("embedded.consul.networkAlias", () -> CONSUL_NETWORK_ALIAS);
            registry.add("embedded.consul.internalPort", properties::getPort);
        };
    }
}
