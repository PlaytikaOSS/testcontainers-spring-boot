package com.playtika.test.elasticsearch;

import com.playtika.test.common.utils.ContainerUtils;
import com.playtika.test.elasticsearch.rest.CreateIndex;
import com.playtika.test.elasticsearch.rest.WaitForGreenStatus;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

class ElasticSearchContainerFactory {

    static ElasticsearchContainer create(ElasticSearchProperties properties) {
        return new ElasticsearchContainer(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.httpPort, properties.transportPort)
                .withEnv("cluster.name", properties.getClusterName())
                .withEnv("discovery.type", "single-node")
                .withEnv("xpack.security.enabled", "false")
                .withEnv("ES_JAVA_OPTS", getJavaOpts(properties))
                .waitingFor(getCompositeWaitStrategy(properties));
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
