package com.playtika.testcontainers.zookeeper;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.zookeeper")
public class ZookeeperConfigurationProperties extends CommonContainerProperties {

    protected int clientPort = 2181;
    protected int adminServerPort = 8080;

    protected String host = "localhost";

    // https://hub.docker.com/_/zookeeper
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "zookeeper:3.9.3";
    }
}
