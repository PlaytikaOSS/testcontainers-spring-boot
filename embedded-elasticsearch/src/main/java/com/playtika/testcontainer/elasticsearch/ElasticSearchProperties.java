package com.playtika.testcontainer.elasticsearch;

import com.playtika.testcontainer.common.properties.CommonContainerProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("embedded.elasticsearch")
public class ElasticSearchProperties extends CommonContainerProperties {
    public static final String BEAN_NAME_EMBEDDED_ELASTIC_SEARCH = "embeddedElasticSearch";

    String clusterName = "test_cluster";
    String host = "localhost";
    List<String> indices = new ArrayList<>();
    int httpPort = 9200;
    int transportPort = 9300;
    int clusterRamMb = 256;

    // https://hub.docker.com/_/elasticsearch
    @Override
    public String getDefaultDockerImage() {
        // Please don`t remove this comment.
        // renovate: datasource=docker
        return "docker.elastic.co/elasticsearch/elasticsearch:8.10.0";
    }
}
