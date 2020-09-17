/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.playtika.test.kafka.configuration;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.kafka.properties.SchemaRegistryConfigurationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.FixedHostPortGenericContainer;
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

        log.info("Starting schema registry server. Docker image: {}", properties.getDockerImage());

        GenericContainer schemaRegistry = new FixedHostPortGenericContainer<>(properties.getDockerImage())
                .withCreateContainerCmdModifier(cmd -> cmd.withHostName(SCHEMA_REGISTRY_HOST_NAME))
                .withCreateContainerCmdModifier(cmd -> cmd.withCapAdd(Capability.NET_ADMIN))
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

        String host = schemaRegistry.getContainerIpAddress();
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
