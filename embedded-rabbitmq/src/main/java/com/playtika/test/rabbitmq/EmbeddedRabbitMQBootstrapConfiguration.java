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
import org.testcontainers.utility.DockerImageName;

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

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("rabbitmq:3.8-alpine");

    @Bean(name = BEAN_NAME_EMBEDDED_RABBITMQ, destroyMethod = "stop")
    public RabbitMQContainer rabbitmq(
            ConfigurableEnvironment environment,
            RabbitMQProperties properties) {
        log.info("Starting RabbitMQ server. Docker image: {}", properties.getDockerImage());

        RabbitMQContainer rabbitMQ =
                new RabbitMQContainer(getDockerImageName(properties))
                        .withAdminPassword(properties.getPassword())
                        .withEnv("RABBITMQ_DEFAULT_VHOST", properties.getVhost())
                        .withExposedPorts(properties.getPort(), properties.getHttpPort());
        rabbitMQ = (RabbitMQContainer) configureCommonsAndStart(rabbitMQ, properties, log);
        registerRabbitMQEnvironment(rabbitMQ, environment, properties);
        return rabbitMQ;
    }

    private static DockerImageName getDockerImageName(RabbitMQProperties properties) {
        DockerImageName imageName = DockerImageName.parse(properties.getDockerImage());
        if (imageName.isCompatibleWith(DEFAULT_IMAGE_NAME)) {
            return imageName;
        }

        return imageName.asCompatibleSubstituteFor(DEFAULT_IMAGE_NAME);
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
