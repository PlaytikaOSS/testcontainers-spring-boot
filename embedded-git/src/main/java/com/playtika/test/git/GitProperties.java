package com.playtika.test.git;

import com.playtika.test.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.git")
public class GitProperties extends CommonContainerProperties {
    static final String BEAN_NAME_EMBEDDED_GIT = "embeddedGit";
    Integer port = 22;

    // https://hub.docker.com/r/rockstorm/git-server
    // https://github.com/rockstorm101/git-server-docker
    @Override
    public String getDefaultDockerImage() {
        return "rockstorm/git-server:latest";
    }
}
