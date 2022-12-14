package com.playtika.test.artifactory;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.artifactory.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ArtifactoryProperties.class)
public class EmbeddedArtifactoryBootstrapConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "artifactoryWaitStrategy")
    public WaitStrategy artifactoryWaitStrategy(ArtifactoryProperties properties) {
        return new HttpWaitStrategy()
                .forPath("/")
                .forPort(properties.getGeneralPort())
                .forStatusCode(200);
    }

    @Bean(name = ArtifactoryProperties.ARTIFACTORY_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer<?> artifactory(ConfigurableEnvironment environment,
                                        ArtifactoryProperties properties,
                                        WaitStrategy artifactoryWaitStrategy) {

        GenericContainer<?> container =
                new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                        .withExposedPorts(properties.getRestApiPort(), properties.getGeneralPort())
                        .withNetwork(Network.SHARED)
                        .withNetworkAliases(properties.getNetworkAlias())
                        .waitingFor(artifactoryWaitStrategy);

        configureCommonsAndStart(container, properties, log);

        registerEnvironment(container, environment, properties);

        return container;
    }

    private void registerEnvironment(GenericContainer<?> artifactory,
                                     ConfigurableEnvironment environment,
                                     ArtifactoryProperties properties) {

        Integer mappedPort = artifactory.getMappedPort(properties.generalPort);
        String host = artifactory.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.artifactory.host", host);
        map.put("embedded.artifactory.port", mappedPort);
        map.put("embedded.artifactory.username", properties.getUsername());
        map.put("embedded.artifactory.password", properties.getPassword());

        log.info("Started Artifactory server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedArtifactoryInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
