package com.playtika.testcontainers.zookeeper;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.zookeeper.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZookeeperConfigurationProperties.class)
public class EmbeddedZookeeperBootstrapConfiguration {

    private static final String ZOOKEEPER_NETWORK_ALIAS = "zookeeper.testcontainer.docker";

    @Bean(destroyMethod = "stop")
    public GenericContainer<?> zooKeeperContainer(ZookeeperConfigurationProperties properties, Optional<Network> network) {
        WaitStrategy waitStrategy = new WaitAllStrategy()
                .withStrategy(new HostPortWaitStrategy())
                .withStrategy(Wait.forHttp("/commands/ruok")
                        .forPort(properties.adminServerPort)
                        .forStatusCode(200)
                )
                .withStartupTimeout(properties.getTimeoutDuration());
        GenericContainer<?> zookeeper = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.clientPort, properties.adminServerPort)
                .withEnv("ZOO_ADMINSERVER_ENABLED", String.valueOf(true))
                .withNetworkAliases(ZOOKEEPER_NETWORK_ALIAS)
                .waitingFor(waitStrategy);
        network.ifPresent(zookeeper::withNetwork);

        zookeeper = configureCommonsAndStart(zookeeper, properties, log);
        return zookeeper;
    }

    @Bean
    public DynamicPropertyRegistrar zookeeperDynamicPropertyRegistrar(@Qualifier("zooKeeperContainer") GenericContainer<?> zookeeper, ZookeeperConfigurationProperties properties) {
        return registry -> {
            String host = zookeeper.getHost();
            registry.add("embedded.zookeeper.port", () -> zookeeper.getMappedPort(properties.clientPort));
            registry.add("embedded.zookeeper.admin.port", () -> zookeeper.getMappedPort(properties.adminServerPort));
            registry.add("embedded.zookeeper.host", () -> host);
            registry.add("embedded.zookeeper.networkAlias", () -> ZOOKEEPER_NETWORK_ALIAS);
            registry.add("embedded.zookeeper.internalClientPort", properties::getClientPort);
            registry.add("embedded.zookeeper.internalAdminServerPort", properties::getAdminServerPort);
        };
    }
}
