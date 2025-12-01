package com.playtika.testcontainer.rabbitmq;

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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.rabbitmq.RabbitMQProperties.BEAN_NAME_EMBEDDED_RABBITMQ;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.rabbitmq.enabled", matchIfMissing = true)
@EnableConfigurationProperties(RabbitMQProperties.class)
public class EmbeddedRabbitMQBootstrapConfiguration {

    private static final String RABBITMQ_NETWORK_ALIAS = "rabbitmq.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "rabbitmq")
    ToxiproxyClientProxy rabbitmqContainerProxy(ToxiproxyClient toxiproxyClient,
                                                 ToxiproxyContainer toxiproxyContainer,
                                                 @Qualifier(BEAN_NAME_EMBEDDED_RABBITMQ) RabbitMQContainer rabbitmq,
                                                 RabbitMQProperties properties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                rabbitmq,
                properties.getPort(),
                "rabbitmq");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "rabbitmq")
    public DynamicPropertyRegistrar rabbitmqToxiProxyDynamicPropertyRegistrar(@Qualifier("rabbitmqContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.rabbitmq");
    }

    @Bean(name = BEAN_NAME_EMBEDDED_RABBITMQ, destroyMethod = "stop")
    public RabbitMQContainer rabbitmq(RabbitMQProperties properties,
                                      Optional<Network> network) {
        Integer[] exposedPorts = Stream.concat(properties.getAdditionalPorts().stream(), Stream.of(properties.getPort(), properties.getHttpPort()))
                .distinct()
                .toArray(Integer[]::new);

        RabbitMQContainer rabbitMQ =
                new RabbitMQContainer(ContainerUtils.getDockerImageName(properties))
                        .withAdminPassword(properties.getPassword())
                        .withEnv("RABBITMQ_DEFAULT_VHOST", properties.getVhost())
                        .withExposedPorts(exposedPorts)
                        .withNetworkAliases(RABBITMQ_NETWORK_ALIAS);

        if (properties.getEnabledPlugins() != null && properties.getEnabledPlugins().size() != 0) {
            rabbitMQ = rabbitMQ.withPluginsEnabled(properties.getEnabledPlugins().toArray(new String[0]));
        }

        network.ifPresent(rabbitMQ::withNetwork);
        rabbitMQ = (RabbitMQContainer) configureCommonsAndStart(rabbitMQ, properties, log);
        Integer mappedPort = rabbitMQ.getMappedPort(properties.getPort());
        Integer mappedHttpPort = rabbitMQ.getMappedPort(properties.getHttpPort());
        Map<Integer, Integer> additionalPorts = new LinkedHashMap<>();
        for (Integer port : properties.getAdditionalPorts()) {
            additionalPorts.put(port, rabbitMQ.getMappedPort(port));
        }
        String host = rabbitMQ.getHost();
        log.info("Started RabbitMQ server. Connection details: port={}, host={}, vhost={}, user={}, password={}, httpPort={}, networkAlias={}, internalPort={}, internalHttpPort={}, additionalPorts={}",
                mappedPort, host, properties.getVhost(), rabbitMQ.getAdminUsername(), rabbitMQ.getAdminPassword(), mappedHttpPort, RABBITMQ_NETWORK_ALIAS, properties.getPort(), properties.getHttpPort(), additionalPorts);
        return rabbitMQ;
    }

    @Bean
    public DynamicPropertyRegistrar rabbitmqDynamicPropertyRegistrar(@Qualifier(BEAN_NAME_EMBEDDED_RABBITMQ) RabbitMQContainer rabbitMQ, RabbitMQProperties properties) {
        return registry -> {
            registry.add("embedded.rabbitmq.port", () -> rabbitMQ.getMappedPort(properties.getPort()));
            registry.add("embedded.rabbitmq.host", rabbitMQ::getHost);
            registry.add("embedded.rabbitmq.vhost", properties::getVhost);
            registry.add("embedded.rabbitmq.user", rabbitMQ::getAdminUsername);
            registry.add("embedded.rabbitmq.password", rabbitMQ::getAdminPassword);
            registry.add("embedded.rabbitmq.httpPort", () -> rabbitMQ.getMappedPort(properties.getHttpPort()));
            registry.add("embedded.rabbitmq.networkAlias", () -> RABBITMQ_NETWORK_ALIAS);
            registry.add("embedded.rabbitmq.internalPort", properties::getPort);
            registry.add("embedded.rabbitmq.internalHttpPort", properties::getHttpPort);
            for (Integer port : properties.getAdditionalPorts()) {
                int mapped = rabbitMQ.getMappedPort(port);
                registry.add("embedded.rabbitmq.additionalPorts." + port, () -> mapped);
            }
        };
    }
}
