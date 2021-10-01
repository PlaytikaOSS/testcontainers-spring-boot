package com.playtika.test.elasticsearch;

import com.github.dockerjava.api.model.Capability;
import com.playtika.test.elasticsearch.rest.CreateIndex;
import com.playtika.test.elasticsearch.rest.WaitForGreenStatus;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

class ElasticSearchContainerFactory {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch");

    static ElasticsearchContainer create(ElasticSearchProperties properties) {
        return new ElasticsearchContainer(getDockerImageName(properties))
                .withExposedPorts(properties.httpPort, properties.transportPort)
                .withEnv("cluster.name", properties.getClusterName())
                .withEnv("discovery.type", "single-node")
                .withEnv("ES_JAVA_OPTS", getJavaOpts(properties))
                .waitingFor(getCompositeWaitStrategy(properties));
    }

    private static DockerImageName getDockerImageName(ElasticSearchProperties properties) {
        DockerImageName imageName = DockerImageName.parse(properties.dockerImage);
        if (imageName.getRegistry().isEmpty()
                && (imageName.getUnversionedPart().isEmpty()
                || imageName.getUnversionedPart().equals("elasticsearch"))) {
            return DEFAULT_IMAGE_NAME.withTag(imageName.getVersionPart());
        } else if (imageName.isCompatibleWith(DEFAULT_IMAGE_NAME)) {
            return imageName;
        }
        return imageName.asCompatibleSubstituteFor(DEFAULT_IMAGE_NAME);
    }

    private static String getJavaOpts(ElasticSearchProperties properties) {
        return "-Xms" + properties.getClusterRamMb() + "m -Xmx" + properties.getClusterRamMb() + "m";
    }

    private static WaitStrategy getCompositeWaitStrategy(ElasticSearchProperties properties) {
        WaitAllStrategy strategy = new WaitAllStrategy()
                .withStrategy(new HostPortWaitStrategy());
        properties.indices.forEach(index -> strategy.withStrategy(new CreateIndex(properties, index)));
        return strategy
                .withStrategy(new WaitForGreenStatus(properties))
                .withStartupTimeout(properties.getTimeoutDuration());
    }
}
