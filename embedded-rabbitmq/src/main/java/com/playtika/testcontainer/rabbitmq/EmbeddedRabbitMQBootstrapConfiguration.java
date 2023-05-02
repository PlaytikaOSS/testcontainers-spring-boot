package com.playtika.testcontainer.rabbitmq;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.rabbitmq.RabbitMQProperties.BEAN_NAME_EMBEDDED_RABBITMQ;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.rabbitmq.enabled", matchIfMissing = true)
@EnableConfigurationProperties(RabbitMQProperties.class)
public class EmbeddedRabbitMQBootstrapConfiguration {

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "rabbitmq")
    ToxiproxyContainer.ContainerProxy rabbitmqContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                               @Qualifier(BEAN_NAME_EMBEDDED_RABBITMQ) RabbitMQContainer rabbitmq,
                                                               ConfigurableEnvironment environment,
                                                               RabbitMQProperties properties) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(rabbitmq, properties.getPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.rabbitmq.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.rabbitmq.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.rabbitmq.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedRabbitmqToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Rabbitmq ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_RABBITMQ, destroyMethod = "stop")
    public RabbitMQContainer rabbitmq(ConfigurableEnvironment environment,
                                      RabbitMQProperties properties,
                                      Optional<Network> network) {
        RabbitMQContainer rabbitMQ =
                new RabbitMQContainer(ContainerUtils.getDockerImageName(properties))
                        .withAdminPassword(properties.getPassword())
                        .withEnv("RABBITMQ_DEFAULT_VHOST", properties.getVhost())
                        .withExposedPorts(properties.getPort(), properties.getHttpPort());

        network.ifPresent(rabbitMQ::withNetwork);
        rabbitMQ = (RabbitMQContainer) configureCommonsAndStart(rabbitMQ, properties, log);
        registerRabbitMQEnvironment(rabbitMQ, environment, properties);
        return rabbitMQ;
    }

    private void registerRabbitMQEnvironment(RabbitMQContainer rabbitMQ,
                                             ConfigurableEnvironment environment,
                                             RabbitMQProperties properties) {
        Integer mappedPort = rabbitMQ.getMappedPort(properties.getPort());
        Integer mappedHttpPort = rabbitMQ.getMappedPort(properties.getHttpPort());
        String host = rabbitMQ.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.rabbitmq.port", mappedPort);
        map.put("embedded.rabbitmq.host", host);
        map.put("embedded.rabbitmq.vhost", properties.getVhost());
        map.put("embedded.rabbitmq.user", rabbitMQ.getAdminUsername());
        map.put("embedded.rabbitmq.password", rabbitMQ.getAdminPassword());
        map.put("embedded.rabbitmq.httpPort", mappedHttpPort);

        log.info("Started RabbitMQ server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedRabbitMqInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
