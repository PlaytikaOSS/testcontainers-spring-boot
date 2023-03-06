package com.playtika.test.artifactory;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import com.playtika.test.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
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
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.test.artifactory.ArtifactoryProperties.ARTIFACTORY_BEAN_NAME;
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

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "artifactory")
    ToxiproxyContainer.ContainerProxy artifactoryContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                                @Qualifier(ARTIFACTORY_BEAN_NAME) GenericContainer<?> artifactory,
                                                                ArtifactoryProperties properties,
                                                                ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(artifactory, properties.getRestApiPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.artifactory.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.artifactory.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.artifactory.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedArtifactoryToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Artifactory ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = ARTIFACTORY_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer<?> artifactory(ConfigurableEnvironment environment,
                                           ArtifactoryProperties properties,
                                           WaitStrategy artifactoryWaitStrategy,
                                           Optional<Network> network) {

        GenericContainer<?> container =
                new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                        .withExposedPorts(properties.getRestApiPort(), properties.getGeneralPort())
                        .withNetwork(Network.SHARED)
                        .withNetworkAliases(properties.getNetworkAlias())
                        .waitingFor(artifactoryWaitStrategy);

        network.ifPresent(container::withNetwork);
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
