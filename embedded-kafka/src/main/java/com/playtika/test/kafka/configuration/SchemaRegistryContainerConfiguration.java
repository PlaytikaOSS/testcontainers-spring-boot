package com.playtika.test.kafka.configuration;

import com.playtika.test.common.utils.ContainerUtils;
import com.playtika.test.kafka.properties.SchemaRegistryConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.kafka.properties.SchemaRegistryConfigurationProperties.SCHEMA_REGISTRY_BEAN_NAME;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "embedded.kafka.schema-registry.enabled", havingValue = "true")
@EnableConfigurationProperties(SchemaRegistryConfigurationProperties.class)
public class SchemaRegistryContainerConfiguration {

    public static final String SCHEMA_REGISTRY_HOST_NAME = "schema-registry.testcontainer.docker";

    @Bean(name = SCHEMA_REGISTRY_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer schemaRegistry(
            ConfigurableEnvironment environment,
            SchemaRegistryConfigurationProperties properties,
            @Value("${embedded.kafka.containerBrokerList}") String kafkaContainerBrokerList,
            Network network) {

        GenericContainer schemaRegistry = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withCreateContainerCmdModifier(cmd -> cmd.withHostName(SCHEMA_REGISTRY_HOST_NAME))
                .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://" + kafkaContainerBrokerList)
                .withEnv("SCHEMA_REGISTRY_HOST_NAME", SCHEMA_REGISTRY_HOST_NAME)
                .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:" + properties.getPort())
                .withEnv("SCHEMA_REGISTRY_AVRO_COMPATIBILITY_LEVEL", properties.getAvroCompatibilityLevel().name())
                .withExposedPorts(properties.getPort())
                .withNetwork(network)
                .withNetworkAliases(SCHEMA_REGISTRY_HOST_NAME);

        if (properties.isBasicAuthenticationEnabled()) {
            schemaRegistry
                    .withEnv("SCHEMA_REGISTRY_AUTHENTICATION_METHOD", "BASIC")
                    .withEnv("SCHEMA_REGISTRY_AUTHENTICATION_REALM", "SchemaRegistry-Props")
                    .withEnv("SCHEMA_REGISTRY_AUTHENTICATION_ROLES", "admin")
                    .withCopyFileToContainer(forClasspathResource("schema-registry/jaas_config.file"), "/etc/schema-registry/jaas_config.file")
                    .withCopyFileToContainer(forClasspathResource("schema-registry/password-file"), "/etc/schema-registry/password-file")
                    .withEnv("SCHEMA_REGISTRY_OPTS", "-Djava.security.auth.login.config=/etc/schema-registry/jaas_config.file");
        }

        schemaRegistry = configureCommonsAndStart(schemaRegistry, properties, log);
        registerSchemaRegistryEnvironment(schemaRegistry, environment, properties);
        return schemaRegistry;
    }

    private void registerSchemaRegistryEnvironment(GenericContainer schemaRegistry, ConfigurableEnvironment environment,
                                                   SchemaRegistryConfigurationProperties properties) {

        String host = schemaRegistry.getHost();
        Integer port = schemaRegistry.getMappedPort(properties.getPort());

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.kafka.schema-registry.host", host);
        map.put("embedded.kafka.schema-registry.port", port);
        if (properties.isBasicAuthenticationEnabled()) {
            map.put("embedded.kafka.schema-registry.username", SchemaRegistryConfigurationProperties.USERNAME);
            map.put("embedded.kafka.schema-registry.password", SchemaRegistryConfigurationProperties.PASSWORD);
        }

        log.info("Started Schema Registry. Connection Details: {}, Connection URI: http://{}:{}", map, host, port);

        MapPropertySource propertySource = new MapPropertySource("embeddedSchemaRegistryInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
