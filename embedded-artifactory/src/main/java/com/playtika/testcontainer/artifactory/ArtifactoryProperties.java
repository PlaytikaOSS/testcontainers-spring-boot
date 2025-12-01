package com.playtika.testcontainer.artifactory;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.artifactory")
public class ArtifactoryProperties extends CommonContainerProperties {

    static final String ARTIFACTORY_BEAN_NAME = "artifactory";

    boolean enabled = true;
    String networkAlias = "artifactory";
    String username = "admin";
    String password = "password";
    int restApiPort = 8081;
    int generalPort = 8082;
    String databaseName = "artifactory";
    String databaseUser = "artifactory";
    String databasePassword = "password";
    /**
     * Artifactory 7.x requires a master key for encryption (normally stored in system.yaml / master.key).
     * We provide it via env vars for ephemeral test containers.
     * Expected format: 32 bytes hex (64 chars).
     */
    String securityMasterKey = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    /**
     * Join key is required for JFrog platform services bootstrapping in container setups.
     * Expected format: 32 bytes hex (64 chars).
     */
    String securityJoinKey = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

    public ArtifactoryProperties() {
        setWaitTimeoutInSeconds(300);
    }

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "releases-docker.jfrog.io/jfrog/artifactory-oss:7.84.14";
    }
}
