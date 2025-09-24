package com.playtika.testcontainer.dynamodb;


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
    ToxiproxyClientProxy dynamodbContainerProxy(ToxiproxyClient toxiproxyClient,
                                                 ToxiproxyContainer toxiproxyContainer,
                                                 @Qualifier(BEAN_NAME_EMBEDDED_DYNAMODB) GenericContainer<?> dynamoDb,
                                                 DynamoDBProperties properties,
                                                 ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                dynamoDb,
                properties.getPort(),
                "dynamodb");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.dynamodb", "embeddedDynamoDBToxiproxyInfo", environment);

        return proxy;
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "dynamodb")
    public DynamicPropertyRegistrar dynamodbToxiProxyDynamicPropertyRegistrar(
        @Qualifier("dynamodbContainerProxy") ToxiproxyContainer.ContainerProxy proxy) {
        return registry -> {
            registry.add("embedded.dynamodb.toxiproxy.host", proxy::getContainerIpAddress);
            registry.add("embedded.dynamodb.toxiproxy.port", proxy::getProxyPort);
            registry.add("embedded.dynamodb.toxiproxy.proxyName", proxy::getName);
        };
    }

    @Bean(name = BEAN_NAME_EMBEDDED_DYNAMODB, destroyMethod = "stop")
    public GenericContainer<?> dynamoDb(DynamoDBProperties properties, Optional<Network> network) {
        GenericContainer<?> dynamodbContainer = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getPort())
                .waitingFor(new HostPortWaitStrategy())
                .withNetworkAliases(DYNAMODB_NETWORK_ALIAS);

        network.ifPresent(dynamodbContainer::withNetwork);

        dynamodbContainer = configureCommonsAndStart(dynamodbContainer, properties, log);
        return dynamodbContainer;
    }

    @Bean
    public DynamicPropertyRegistrar dynamodbDynamicPropertyRegistrar(
        @Qualifier(BEAN_NAME_EMBEDDED_DYNAMODB) GenericContainer<?> container, DynamoDBProperties properties) {
        return registry -> {
            var mappedPort = container.getMappedPort(properties.port);
            var host = container.getHost();
            var accessKey = properties.getAccessKey();
            var secretKey = properties.getSecretKey();

            registry.add("embedded.dynamodb.port", () -> mappedPort);
            registry.add("embedded.dynamodb.host", () -> host);
            registry.add("embedded.dynamodb.accessKey", () -> accessKey);
            registry.add("embedded.dynamodb.secretKey", () -> secretKey);
            registry.add("embedded.dynamodb.networkAlias", () -> DYNAMODB_NETWORK_ALIAS);
            registry.add("embedded.dynamodb.internalPort", properties::getPort);

            log.info("Started DynamoDb server. Connection details: host: {}, port: {}, accessKey:{}, secretKey: {}",
                host, mappedPort, accessKey, secretKey);
            log.info("Consult with the doc " +
                     "https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.UsageNotes.html " +
                     "for more details");
        };
    }

}
