package com.playtika.test.artifactory;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.artifactory")
public class ArtifactoryProperties extends CommonContainerProperties {

    static final String ARTIFACTORY_BEAN_NAME = "artifactory";

    boolean enabled = true;
    String dockerImage = "releases-docker.jfrog.io/jfrog/artifactory-oss:7.25.6";
    String networkAlias = "artifactory";
    String username = "admin";
    String password = "password";
    int restApiPort = 8081;
    int generalPort = 8082;
}
