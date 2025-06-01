package com.playtika.testcontainer.mongodb;

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
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
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
    ToxiproxyClientProxy mongodbContainerProxy(ToxiproxyClient toxiproxyClient,
                                               ToxiproxyContainer toxiproxyContainer,
                                               @Qualifier(BEAN_NAME_EMBEDDED_MONGODB) GenericContainer<?> mongodb,
                                               MongodbProperties properties,
                                               ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                mongodb,
                properties.getPort(),
                "mongodb"
        );

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.mongodb", "embeddedMongodbToxiProxyInfo", environment);

        return proxy;
    }

    @Bean(value = BEAN_NAME_EMBEDDED_MONGODB, destroyMethod = "stop")
    public GenericContainer<?> mongodb(MongodbProperties properties,
                                       Optional<Network> network) throws IOException, InterruptedException {

        GenericContainer<?> mongodb;
        if (StringUtils.isBlank(properties.getReplicaSetName())) {
            mongodb = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                    .withEnv("MONGO_INITDB_ROOT_USERNAME", properties.getUsername())
                    .withEnv("MONGO_INITDB_ROOT_PASSWORD", properties.getPassword())
                    .withEnv("MONGO_INITDB_DATABASE", properties.getDatabase())
                    .withExposedPorts(properties.getPort())
                    .waitingFor(new MongodbWaitStrategy(properties))
                    .withNetworkAliases(MONGODB_NETWORK_ALIAS);
        } else {
            mongodb = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                    .withCommand("-f", "/etc/mongod.conf")
                    .withClasspathResourceMapping("/mongod/gen-keyfile.sh", "/docker-entrypoint-initdb.d/gen-keyfile.sh", BindMode.READ_ONLY)
                    .withCopyToContainer(
                            Transferable.of(
                                    new ClassPathResource("/mongod/mongod.conf")
                                            .getContentAsString(Charset.defaultCharset())
                                            .replace("${replica-set-name}", properties.getReplicaSetName())
                            )
                            , "/etc/mongod.conf")
                    .withEnv("MONGO_INITDB_ROOT_USERNAME", properties.getUsername())
                    .withEnv("MONGO_INITDB_ROOT_PASSWORD", properties.getPassword())
                    .withEnv("MONGO_INITDB_DATABASE", properties.getDatabase())
                    .withEnv("MONGO_INITDB_REPL_SET_HOST", properties.getHost())
                    .withExposedPorts(properties.getPort())
                    .waitingFor(new MongodbWaitStrategy(properties))
                    .withNetworkAliases(MONGODB_NETWORK_ALIAS);
        }

        network.ifPresent(mongodb::withNetwork);

        mongodb = configureCommonsAndStart(mongodb, properties, log);
        return mongodb;
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
        if (StringUtils.isNotBlank(properties.getReplicaSetName())) {
            map.put("embedded.mongodb.replica-set-name", properties.getReplicaSetName());
        }

        log.info("Started mongodb. Connection Details: {}, Connection URI: mongodb://{}:{}/{}{}", map, host, mappedPort, properties.getDatabase(), Optional.ofNullable(
                properties.getReplicaSetName()).map("?directConnection=true&authSource=admin&rs="::concat).orElse("")
        );

        MapPropertySource propertySource = new MapPropertySource("embeddedMongoInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
    @Bean
    public DynamicPropertyRegistrar mongodbDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_MONGODB) GenericContainer<?> mongodb,
            MongodbProperties properties) {
        return registry -> {
            registry.add("embedded.mongodb.port", () -> mongodb.getMappedPort(properties.getPort()));
            registry.add("embedded.mongodb.host", mongodb::getHost);
            registry.add("embedded.mongodb.username", properties::getUsername);
            registry.add("embedded.mongodb.password", properties::getPassword);
            registry.add("embedded.mongodb.database", properties::getDatabase);
            registry.add("embedded.mongodb.networkAlias", () -> MONGODB_NETWORK_ALIAS);
            registry.add("embedded.mongodb.internalPort", properties::getPort);
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "mongodb")
    public DynamicPropertyRegistrar mongodbToxiProxyDynamicPropertyRegistrar(
            @Qualifier("mongodbContainerProxy") ToxiproxyContainer.ContainerProxy proxy) {
        return registry -> {
            registry.add("embedded.mongodb.toxiproxy.host", proxy::getContainerIpAddress);
            registry.add("embedded.mongodb.toxiproxy.port", proxy::getProxyPort);
            registry.add("embedded.mongodb.toxiproxy.proxyName", proxy::getName);
        };
    }

    @Bean
    @ConditionalOnMissingBean
    MongodbStatusCheck mongodbStartupCheckStrategy(MongodbProperties properties) {
        return new MongodbStatusCheck(properties);
    }
}
