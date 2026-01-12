package com.playtika.testcontainer.kafka.configuration;

import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.kafka.properties.KafkaConfigurationProperties;
import com.playtika.testcontainer.kafka.properties.SchemaRegistryConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import java.util.LinkedHashMap;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.kafka.configuration.KafkaContainerConfiguration.KAFKA_HOST_NAME;
import static com.playtika.testcontainer.kafka.properties.KafkaConfigurationProperties.KAFKA_BEAN_NAME;
import static com.playtika.testcontainer.kafka.properties.SchemaRegistryConfigurationProperties.SCHEMA_REGISTRY_BEAN_NAME;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "embedded.kafka.schema-registry.enabled", havingValue = "true")
@EnableConfigurationProperties(SchemaRegistryConfigurationProperties.class)
public class SchemaRegistryContainerConfiguration {

    public static final String SCHEMA_REGISTRY_HOST_NAME = "schema-registry.testcontainer.docker";

    @Bean(name = SCHEMA_REGISTRY_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer<?> schemaRegistry(
            SchemaRegistryConfigurationProperties properties,
            @Qualifier(KAFKA_BEAN_NAME) GenericContainer<?> kafka,
            KafkaConfigurationProperties kafkaProperties,
            Network network) {

        String kafkaContainerBrokerList = String.format("%s:%d", KAFKA_HOST_NAME, kafkaProperties.getContainerBrokerPort());

        GenericContainer<?> schemaRegistry = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
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
        registerSchemaRegistryEnvironment(schemaRegistry, properties);
        return schemaRegistry;
    }

    private void registerSchemaRegistryEnvironment(GenericContainer<?> schemaRegistry, SchemaRegistryConfigurationProperties properties) {

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
    }

    @Bean
    public DynamicPropertyRegistrar schemaRegistryDynamicPropertyRegistrar(@Qualifier(SCHEMA_REGISTRY_BEAN_NAME) GenericContainer<?> schemaRegistry, SchemaRegistryConfigurationProperties properties) {
        return registry -> {
            String host = schemaRegistry.getHost();
            Integer port = schemaRegistry.getMappedPort(properties.getPort());
            registry.add("embedded.kafka.schema-registry.host", () -> host);
            registry.add("embedded.kafka.schema-registry.port", () -> port);
            if (properties.isBasicAuthenticationEnabled()) {
                registry.add("embedded.kafka.schema-registry.username", () -> SchemaRegistryConfigurationProperties.USERNAME);
                registry.add("embedded.kafka.schema-registry.password", () -> SchemaRegistryConfigurationProperties.PASSWORD);
            }
            log.info("Started Schema Registry. Connection Details: host={}, port={}, username={}, password={}, Connection URI: http://{}:{}",
                host, port,
                properties.isBasicAuthenticationEnabled() ? SchemaRegistryConfigurationProperties.USERNAME : null,
                properties.isBasicAuthenticationEnabled() ? SchemaRegistryConfigurationProperties.PASSWORD : null,
                host, port);
        };
    }
}
