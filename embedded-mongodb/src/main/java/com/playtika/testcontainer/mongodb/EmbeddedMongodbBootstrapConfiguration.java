package com.playtika.testcontainer.mongodb;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.mongodb.MongodbProperties.BEAN_NAME_EMBEDDED_MONGODB;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(
        name = "embedded.mongodb.enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableConfigurationProperties(MongodbProperties.class)
public class EmbeddedMongodbBootstrapConfiguration {

    private static final String MONGODB_NETWORK_ALIAS = "mongodb.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "mongodb")
    ToxiproxyContainer.ContainerProxy mongodbContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                            @Qualifier(BEAN_NAME_EMBEDDED_MONGODB) GenericContainer<?> mongodb,
                                                            MongodbProperties properties,
                                                            ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(mongodb, properties.getPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mongodb.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.mongodb.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.mongodb.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedMongodbToxiProxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Mongodb ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(value = BEAN_NAME_EMBEDDED_MONGODB, destroyMethod = "stop")
    public GenericContainer<?> mongodb(ConfigurableEnvironment environment,
                                       MongodbProperties properties,
                                       MongodbStatusCheck mongodbStatusCheck,
                                       Optional<Network> network) {
        GenericContainer<?> mongodb =
                new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                        .withEnv("MONGO_INITDB_ROOT_USERNAME", properties.getUsername())
                        .withEnv("MONGO_INITDB_ROOT_PASSWORD", properties.getPassword())
                        .withEnv("MONGO_INITDB_DATABASE", properties.getDatabase())
                        .withExposedPorts(properties.getPort())
                        .waitingFor(mongodbStatusCheck)
                        .withNetworkAliases(MONGODB_NETWORK_ALIAS);

        network.ifPresent(mongodb::withNetwork);

        mongodb = configureCommonsAndStart(mongodb, properties, log);
        registerMongodbEnvironment(mongodb, environment, properties);
        return mongodb;
    }

    @Bean
    @ConditionalOnMissingBean
    MongodbStatusCheck mongodbStartupCheckStrategy(MongodbProperties properties) {
        return new MongodbStatusCheck(properties);
    }

    private void registerMongodbEnvironment(GenericContainer<?> mongodb, ConfigurableEnvironment environment, MongodbProperties properties) {
        Integer mappedPort = mongodb.getMappedPort(properties.getPort());
        String host = mongodb.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mongodb.port", mappedPort);
        map.put("embedded.mongodb.host", host);
        map.compute("embedded.mongodb.username", (k, v) -> properties.getUsername());
        map.compute("embedded.mongodb.password", (k, v) -> properties.getPassword());
        map.put("embedded.mongodb.database", properties.getDatabase());
        map.put("embedded.mongodb.networkAlias", MONGODB_NETWORK_ALIAS);
        map.put("embedded.mongodb.internalPort", properties.getPort());

        log.info("Started mongodb. Connection Details: {}, Connection URI: mongodb://{}:{}/{}", map, host, mappedPort, properties.getDatabase());

        MapPropertySource propertySource = new MapPropertySource("embeddedMongoInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
