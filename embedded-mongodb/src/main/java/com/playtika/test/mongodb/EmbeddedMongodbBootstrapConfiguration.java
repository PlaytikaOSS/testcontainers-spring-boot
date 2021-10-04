package com.playtika.test.mongodb;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import lombok.extern.slf4j.Slf4j;
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

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.mongodb.MongodbProperties.BEAN_NAME_EMBEDDED_MONGODB;

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

    @Bean(value = BEAN_NAME_EMBEDDED_MONGODB, destroyMethod = "stop")
    public GenericContainer mongodb(
            ConfigurableEnvironment environment,
            MongodbProperties properties,
            MongodbStatusCheck mongodbStatusCheck) {
        GenericContainer mongodb =
                new GenericContainer<>(properties.getDockerImage())
                        .withEnv("MONGO_INITDB_ROOT_USERNAME", properties.getUsername())
                        .withEnv("MONGO_INITDB_ROOT_PASSWORD", properties.getPassword())
                        .withEnv("MONGO_INITDB_DATABASE", properties.getDatabase())
                        .withExposedPorts(properties.getPort())
                        .waitingFor(mongodbStatusCheck);

        mongodb = configureCommonsAndStart(mongodb, properties, log);
        registerMongodbEnvironment(mongodb, environment, properties);
        return mongodb;
    }

    @Bean
    @ConditionalOnMissingBean
    MongodbStatusCheck mongodbStartupCheckStrategy(MongodbProperties properties) {
        return new MongodbStatusCheck();
    }

    private void registerMongodbEnvironment(GenericContainer mongodb, ConfigurableEnvironment environment, MongodbProperties properties) {
        Integer mappedPort = mongodb.getMappedPort(properties.getPort());
        String host = mongodb.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.mongodb.port", mappedPort);
        map.put("embedded.mongodb.host", host);
        map.compute("embedded.mongodb.username", (k, v) -> properties.getUsername());
        map.compute("embedded.mongodb.password", (k, v) -> properties.getPassword());
        map.put("embedded.mongodb.database", properties.getDatabase());

        log.info("Started mongodb. Connection Details: {}, Connection URI: mongodb://{}:{}/{}", map, host, mappedPort, properties.getDatabase());

        MapPropertySource propertySource = new MapPropertySource("embeddedMongoInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
