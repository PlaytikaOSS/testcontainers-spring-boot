package com.playtika.testcontainer.artifactory;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.Optional;

import static com.playtika.testcontainer.artifactory.ArtifactoryProperties.ARTIFACTORY_BEAN_NAME;
import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.artifactory.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ArtifactoryProperties.class)
public class EmbeddedArtifactoryBootstrapConfiguration {

    private static final String ARTIFACTORY_NETWORK_ALIAS = "artifactory.testcontainer.docker";

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
    ToxiproxyClientProxy artifactoryContainerProxy(ToxiproxyClient toxiproxyClient,
                                                    ToxiproxyContainer toxiproxyContainer,
                                                    @Qualifier(ARTIFACTORY_BEAN_NAME) GenericContainer<?> artifactory,
                                                    ArtifactoryProperties properties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                artifactory,
                properties.getRestApiPort(),
                "artifactory");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "artifactory")
    public DynamicPropertyRegistrar artifactoryToxiProxyDynamicPropertyRegistrar(@Qualifier("artifactoryContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.artifactory");
    }

    @Bean(name = ARTIFACTORY_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer<?> artifactory(ArtifactoryProperties properties,
                                           WaitStrategy artifactoryWaitStrategy,
                                           Optional<Network> network) {
        GenericContainer<?> container =
                new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                        .withExposedPorts(properties.getRestApiPort(), properties.getGeneralPort())
                        .withNetwork(Network.SHARED)
                        .withNetworkAliases(properties.getNetworkAlias(), ARTIFACTORY_NETWORK_ALIAS)
                        .waitingFor(artifactoryWaitStrategy);
        network.ifPresent(container::withNetwork);
        configureCommonsAndStart(container, properties, log);
        Integer mappedPort = container.getMappedPort(properties.generalPort);
        String host = container.getHost();
        log.info("Started Artifactory server. Connection details: host={}, port={}, username={}, password={}, staticNetworkAlias={}, internalRestApiPort={}, internalGeneralPort={}",
                host, mappedPort, properties.getUsername(), properties.getPassword(), ARTIFACTORY_NETWORK_ALIAS, properties.getRestApiPort(), properties.getGeneralPort());
        return container;
    }

    @Bean
    public DynamicPropertyRegistrar artifactoryDynamicPropertyRegistrar(@Qualifier(ARTIFACTORY_BEAN_NAME) GenericContainer<?> artifactory, ArtifactoryProperties properties) {
        return registry -> {
            registry.add("embedded.artifactory.host", artifactory::getHost);
            registry.add("embedded.artifactory.port", () -> artifactory.getMappedPort(properties.generalPort));
            registry.add("embedded.artifactory.username", properties::getUsername);
            registry.add("embedded.artifactory.password", properties::getPassword);
            registry.add("embedded.artifactory.staticNetworkAlias", () -> ARTIFACTORY_NETWORK_ALIAS);
            registry.add("embedded.artifactory.internalRestApiPort", properties::getRestApiPort);
            registry.add("embedded.artifactory.internalGeneralPort", properties::getGeneralPort);
        };
    }
}
