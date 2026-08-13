package com.playtika.testcontainer.rabbitmq;

import com.playtika.testcontainer.common.spring.ContainerStartupCoordinator;
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
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommons;
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
                                                 @Qualifier(BEAN_NAME_EMBEDDED_RABBITMQ) GenericContainer<?> rabbitmq,
                                                 ConfigurableEnvironment environment,
                                                 RabbitMQProperties properties) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                rabbitmq,
                properties.getPort(),
                "rabbitmq");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.rabbitmq", "embeddedRabbitmqToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_RABBITMQ, destroyMethod = "stop")
    public RabbitMQContainer rabbitmq(ConfigurableEnvironment environment,
                                      RabbitMQProperties properties,
                                      Optional<Network> network,
                                      ContainerStartupCoordinator startupCoordinator) {

        Integer[] exposedPorts = Stream.concat(properties.getAdditionalPorts().stream(), Stream.of(properties.getPort(), properties.getHttpPort()))
                .distinct()
                .toArray(Integer[]::new);

        RabbitMQContainer rabbitMQ =
                new RabbitMQContainer(ContainerUtils.getDockerImageName(properties))
                        .withAdminPassword(properties.getPassword())
                        .withEnv("RABBITMQ_DEFAULT_VHOST", properties.getVhost())
                        .withExposedPorts(exposedPorts)
                        .withNetworkAliases(RABBITMQ_NETWORK_ALIAS);

        network.ifPresent(rabbitMQ::withNetwork);
        RabbitMQContainer configuredRabbitMQ = (RabbitMQContainer) configureCommons(rabbitMQ, properties, log);

        startupCoordinator.schedule(() -> {
            ContainerUtils.startAndLogTime(configuredRabbitMQ, log);
            enablePlugins(configuredRabbitMQ, properties);
            registerRabbitMQEnvironment(configuredRabbitMQ, environment, properties);
        });
        return configuredRabbitMQ;
    }

    private void enablePlugins(RabbitMQContainer rabbitMQ, RabbitMQProperties properties) {
        if (properties.getEnabledPlugins() != null && !properties.getEnabledPlugins().isEmpty()) {
            List<String> command = new ArrayList<>(properties.getEnabledPlugins().size() + 2);
            command.add("rabbitmq-plugins");
            command.add("enable");
            command.addAll(properties.getEnabledPlugins());
            try {
                rabbitMQ.execInContainer(command.toArray(new String[0]));
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new IllegalStateException("Failed to enable RabbitMQ plugins: " + properties.getEnabledPlugins(), e);
            }
        }
    }

    private void registerRabbitMQEnvironment(RabbitMQContainer rabbitMQ,
                                             ConfigurableEnvironment environment,
                                             RabbitMQProperties properties) {
        Integer mappedPort = rabbitMQ.getMappedPort(properties.getPort());
        Integer mappedHttpPort = rabbitMQ.getMappedPort(properties.getHttpPort());

        Map<Integer, Integer> additionalPorts = new LinkedHashMap<>();
        for (Integer port : properties.getAdditionalPorts()) {
            additionalPorts.put(port, rabbitMQ.getMappedPort(port));
        }

        String host = rabbitMQ.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.rabbitmq.port", mappedPort);
        map.put("embedded.rabbitmq.host", host);
        map.put("embedded.rabbitmq.vhost", properties.getVhost());
        map.put("embedded.rabbitmq.user", rabbitMQ.getAdminUsername());
        map.put("embedded.rabbitmq.password", rabbitMQ.getAdminPassword());
        map.put("embedded.rabbitmq.httpPort", mappedHttpPort);
        map.put("embedded.rabbitmq.networkAlias", RABBITMQ_NETWORK_ALIAS);
        map.put("embedded.rabbitmq.internalPort", properties.getPort());
        map.put("embedded.rabbitmq.internalHttpPort", properties.getHttpPort());
        for (Integer port : additionalPorts.keySet()) {
            map.put("embedded.rabbitmq.additionalPorts." + port, additionalPorts.get(port));
        }

        log.info("Started RabbitMQ server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedRabbitMqInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
