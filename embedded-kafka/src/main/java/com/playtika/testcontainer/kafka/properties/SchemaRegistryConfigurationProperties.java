package com.playtika.testcontainer.kafka.properties;

import com.github.dockerjava.api.model.Capability;
import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;

import static com.playtika.testcontainer.kafka.properties.SchemaRegistryConfigurationProperties.Authentication.BASIC;
import static com.playtika.testcontainer.kafka.properties.SchemaRegistryConfigurationProperties.Authentication.NONE;
import static com.playtika.testcontainer.kafka.properties.SchemaRegistryConfigurationProperties.AvroCompatibilityLevel.BACKWARD;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.kafka.schema-registry")
public class SchemaRegistryConfigurationProperties extends CommonContainerProperties {

    public static final String SCHEMA_REGISTRY_BEAN_NAME = "schema-registry";
    public static final String USERNAME = "admin";
    public static final String PASSWORD = "letmein";

    /**
     * The container internal port. Will be overwritten with mapped port.
     */
    private int port = 8081;
    private AvroCompatibilityLevel avroCompatibilityLevel = BACKWARD;
    private Authentication authentication = NONE;

    public SchemaRegistryConfigurationProperties() {
        this.setCapabilities(Arrays.asList(Capability.NET_ADMIN));
    }

    public boolean isBasicAuthenticationEnabled() {
        return authentication == BASIC;
    }

    // https://hub.docker.com/r/confluentinc/cp-schema-registry
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "confluentinc/cp-schema-registry:7.5.1";
    }

    public enum AvroCompatibilityLevel {
        NONE, BACKWARD, BACKWARD_TRANSITIVE, FORWARD, FORWARD_TRANSITIVE, FULL, FULL_TRANSITIVE
    }

    public enum Authentication {
        NONE, BASIC
    }
}
