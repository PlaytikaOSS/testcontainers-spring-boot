package com.playtika.test.common.spring;

import com.playtika.test.common.properties.TestcontainersProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

//TODO: Drop this workaround after proper fix available https://github.com/spring-cloud/spring-cloud-commons/issues/752

@Slf4j
@AutoConfiguration
@AutoConfigureOrder(value = Ordered.LOWEST_PRECEDENCE)
@ConditionalOnProperty(prefix = "embedded.containers", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(TestcontainersProperties.class)
public class EmbeddedContainersShutdownAutoConfiguration {

    public static final String ALL_CONTAINERS = "allContainers";

    @Bean(name = ALL_CONTAINERS)
    public AllContainers allContainers(@Autowired(required = false) DockerPresenceMarker dockerAvailable,
                                       @Autowired(required = false) GenericContainer[] allContainers,
                                       TestcontainersProperties testcontainersProperties) {
        //Docker presence marker is not available == no spring cloud
        if (dockerAvailable == null)
            throw new NoDockerPresenceMarkerException("No docker presence marker available. " +
                    "Did you add spring cloud starter into classpath?");

        List<GenericContainer> containers = allContainers != null ? asList(allContainers) : emptyList();
        return new AllContainers(containers, testcontainersProperties);
    }
}
