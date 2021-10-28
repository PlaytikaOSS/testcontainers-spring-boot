package com.playtika.test.rabbitmq;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.RabbitMQContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.rabbitmq.RabbitMQProperties.BEAN_NAME_EMBEDDED_RABBITMQ;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.rabbitmq.enabled", matchIfMissing = true)
@EnableConfigurationProperties(RabbitMQProperties.class)
public class EmbeddedRabbitMQBootstrapConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_RABBITMQ, destroyMethod = "stop")
    public RabbitMQContainer rabbitmq(
            ConfigurableEnvironment environment,
            RabbitMQProperties properties) {
        log.info("Starting RabbitMQ server. Docker image: {}", properties.getDockerImage());

        RabbitMQContainer rabbitMQ =
                new RabbitMQContainer(properties.getDockerImage())
                        .withAdminPassword(properties.getPassword())
                        .withEnv("RABBITMQ_DEFAULT_VHOST", properties.getVhost())
                        .withExposedPorts(properties.getPort(), properties.getHttpPort());
        rabbitMQ = (RabbitMQContainer) configureCommonsAndStart(rabbitMQ, properties, log);
        registerRabbitMQEnvironment(rabbitMQ, environment, properties);
        return rabbitMQ;
    }


    private void registerRabbitMQEnvironment(RabbitMQContainer rabbitMQ,
                                             ConfigurableEnvironment environment,
                                             RabbitMQProperties properties) {
        Integer mappedPort = rabbitMQ.getMappedPort(properties.getPort());
        Integer mappedHttpPort = rabbitMQ.getMappedPort(properties.getHttpPort());
        String host = rabbitMQ.getContainerIpAddress();

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
