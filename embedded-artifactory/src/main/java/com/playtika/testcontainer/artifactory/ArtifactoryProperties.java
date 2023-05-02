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

    public ArtifactoryProperties() {
        setWaitTimeoutInSeconds(120);
    }

    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "releases-docker.jfrog.io/jfrog/artifactory-oss:7.55.10";
    }
}
