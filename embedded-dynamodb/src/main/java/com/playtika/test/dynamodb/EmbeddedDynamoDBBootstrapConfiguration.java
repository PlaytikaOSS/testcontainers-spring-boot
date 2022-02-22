package com.playtika.test.dynamodb;


import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.dynamodb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(DynamoDBProperties.class)
public class EmbeddedDynamoDBBootstrapConfiguration {

    @Bean(name = DynamoDBProperties.BEAN_NAME_EMBEDDED_DYNAMODB, destroyMethod = "stop")
    public GenericContainer dynamoDb(ConfigurableEnvironment environment,
                                     DynamoDBProperties properties) {

        GenericContainer container = new GenericContainer(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.port)
                .waitingFor(new HostPortWaitStrategy());

        container = configureCommonsAndStart(container, properties, log);

        registerDynamodbEnvironment(container, environment, properties);
        return container;
    }

    private void registerDynamodbEnvironment(GenericContainer container,
                                             ConfigurableEnvironment environment,
                                             DynamoDBProperties properties) {
        Integer mappedPort = container.getMappedPort(properties.port);
        String host = container.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.dynamodb.port", mappedPort);
        map.put("embedded.dynamodb.host", host);
        map.put("embedded.dynamodb.accessKey", properties.getAccessKey());
        map.put("embedded.dynamodb.secretKey", properties.getSecretKey());

        log.info("Started DynamoDb server. Connection details: {}, ", map);
        log.info("Consult with the doc " +
                "https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html " +
                "for more details");

        MapPropertySource propertySource = new MapPropertySource("embeddedDynamodbInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
