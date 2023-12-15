package com.playtika.testcontainer.opensearch;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.opensearch")
public class OpenSearchProperties extends CommonContainerProperties {
    public static final String BEAN_NAME_EMBEDDED_OPEN_SEARCH = "embeddedOpenSearch";

    String clusterName = "test_cluster";
    String host = "localhost";
    List<String> indices = new ArrayList<>();
    int httpPort = 9200;
    int transportPort = 9300;
    int clusterRamMb = 256;
    boolean credentialsEnabled = false;
    boolean allowInsecure = false;
    String username = "admin";
    String password = "admin";

    // https://hub.docker.com/r/opensearchproject/opensearch
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "opensearchproject/opensearch:2.11.1";
    }
}
