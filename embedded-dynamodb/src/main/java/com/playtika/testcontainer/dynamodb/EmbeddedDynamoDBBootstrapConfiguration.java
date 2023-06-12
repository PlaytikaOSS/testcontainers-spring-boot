package com.playtika.testcontainer.dynamodb;


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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.dynamodb.DynamoDBProperties.BEAN_NAME_EMBEDDED_DYNAMODB;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.dynamodb.enabled", matchIfMissing = true)
@EnableConfigurationProperties(DynamoDBProperties.class)
public class EmbeddedDynamoDBBootstrapConfiguration {

    private static final String DYNAMODB_NETWORK_ALIAS = "dynamodb.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "dynamodb")
    ToxiproxyContainer.ContainerProxy dynamodbContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                             @Qualifier(BEAN_NAME_EMBEDDED_DYNAMODB) GenericContainer<?> dynamoDb,
                                                             DynamoDBProperties properties,
                                                             ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(dynamoDb, properties.getPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.dynamodb.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.dynamodb.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.dynamodb.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedDynamoDBToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started DynamoDB ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_DYNAMODB, destroyMethod = "stop")
    public GenericContainer<?> dynamoDb(ConfigurableEnvironment environment,
                                        DynamoDBProperties properties,
                                        Optional<Network> network) {

        GenericContainer<?> dynamodbContainer = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getPort())
                .waitingFor(new HostPortWaitStrategy())
                .withNetworkAliases(DYNAMODB_NETWORK_ALIAS);

        network.ifPresent(dynamodbContainer::withNetwork);

        dynamodbContainer = configureCommonsAndStart(dynamodbContainer, properties, log);

        registerDynamodbEnvironment(dynamodbContainer, environment, properties);
        return dynamodbContainer;
    }

    private void registerDynamodbEnvironment(GenericContainer<?> container,
                                             ConfigurableEnvironment environment,
                                             DynamoDBProperties properties) {
        Integer mappedPort = container.getMappedPort(properties.port);
        String host = container.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.dynamodb.port", mappedPort);
        map.put("embedded.dynamodb.host", host);
        map.put("embedded.dynamodb.accessKey", properties.getAccessKey());
        map.put("embedded.dynamodb.secretKey", properties.getSecretKey());
        map.put("embedded.dynamodb.networkAlias", DYNAMODB_NETWORK_ALIAS);
        map.put("embedded.dynamodb.internalPort", properties.getPort());

        log.info("Started DynamoDb server. Connection details: {}, ", map);
        log.info("Consult with the doc " +
                "https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html " +
                "for more details");

        MapPropertySource propertySource = new MapPropertySource("embeddedDynamodbInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
