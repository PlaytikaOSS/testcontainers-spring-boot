package com.playtika.testcontainer.mongodb;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.mongodb.MongodbProperties.BEAN_NAME_EMBEDDED_MONGODB;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@AutoConfigureBefore(name = "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration")
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
                                               @Qualifier(BEAN_NAME_EMBEDDED_MONGODB) MongoDBContainer mongodb,
                                               MongodbProperties properties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                mongodb,
                properties.getPort(),
                "mongodb"
        );
    }

    @Bean(value = BEAN_NAME_EMBEDDED_MONGODB, destroyMethod = "stop")
    public MongoDBContainer mongodb(
                                    MongodbProperties properties,
                                    Optional<Network> network)  throws IOException, InterruptedException{

        MongoDBContainer mongodb = new MongoDBContainer(ContainerUtils.getDockerImageName(properties))
            .withEnv("MONGO_INITDB_ROOT_USERNAME", properties.getUsername())
            .withEnv("MONGO_INITDB_ROOT_PASSWORD", properties.getPassword())
            .withEnv("MONGO_INITDB_DATABASE", properties.getDatabase())
            .waitingFor(new MongodbWaitStrategy(properties))
            .withNetworkAliases(MONGODB_NETWORK_ALIAS);

        // Configure replica set if provided
        if (StringUtils.isNotBlank(properties.getReplicaSetName())) {
            mongodb = mongodb.withReuse(properties.isReuseContainer())
                .withEnv("MONGO_INITDB_REPL_SET_HOST", properties.getHost())
                .withCommand("-f", "/etc/mongod.conf")
                .withClasspathResourceMapping("/mongod/gen-keyfile.sh", "/docker-entrypoint-initdb.d/gen-keyfile.sh", BindMode.READ_ONLY)
                .withCopyToContainer(
                    Transferable.of(
                        new ClassPathResource("/mongod/mongod.conf")
                            .getContentAsString(Charset.defaultCharset())
                            .replace("${replica-set-name}", properties.getReplicaSetName())
                    )
                    , "/etc/mongod.conf");
        }

        network.ifPresent(mongodb::withNetwork);

        mongodb = (MongoDBContainer) configureCommonsAndStart(mongodb, properties, log);
        return mongodb;
    }

    @Bean
    public DynamicPropertyRegistrar mongodbDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_MONGODB) MongoDBContainer mongodb,
            MongodbProperties properties) {
        return registry -> {
            registry.add("embedded.mongodb.port", () -> mongodb.getMappedPort(properties.getPort()));
            registry.add("embedded.mongodb.host", mongodb::getHost);
            registry.add("embedded.mongodb.username", properties::getUsername);
            registry.add("embedded.mongodb.password", properties::getPassword);
            registry.add("embedded.mongodb.database", properties::getDatabase);
            registry.add("embedded.mongodb.networkAlias", () -> MONGODB_NETWORK_ALIAS);
            registry.add("embedded.mongodb.internalPort", properties::getPort);
            if (StringUtils.isNotBlank(properties.getReplicaSetName())) {
                registry.add("embedded.mongodb.replica-set-name", properties::getReplicaSetName);
            }
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "mongodb")
    public DynamicPropertyRegistrar mongodbToxiProxyDynamicPropertyRegistrar(
            @Qualifier("mongodbContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.mongodb");
    }

}
