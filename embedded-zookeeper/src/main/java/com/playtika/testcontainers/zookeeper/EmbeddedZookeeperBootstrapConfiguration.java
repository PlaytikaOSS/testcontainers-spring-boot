package com.playtika.testcontainers.zookeeper;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.LinkedHashMap;
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
    public GenericContainer<?> zooKeeperContainer(ConfigurableEnvironment environment,
                                         ZookeeperConfigurationProperties properties,
                                         Optional<Network> network) {
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
        registerZookeeperEnvironment(zookeeper, environment, properties);

        return zookeeper;
    }

    private void registerZookeeperEnvironment(GenericContainer<?> zookeeper, ConfigurableEnvironment environment,
                                              ZookeeperConfigurationProperties properties) {
        String host = zookeeper.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.zookeeper.port", zookeeper.getMappedPort(properties.clientPort));
        map.put("embedded.zookeeper.admin.port", zookeeper.getMappedPort(properties.adminServerPort));
        map.put("embedded.zookeeper.host", host);
        map.put("embedded.zookeeper.networkAlias", ZOOKEEPER_NETWORK_ALIAS);
        map.put("embedded.zookeeper.internalClientPort", properties.getClientPort());
        map.put("embedded.zookeeper.internalAdminServerPort", properties.getAdminServerPort());

        log.info("Started Zookeeper. Connection Details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedZookeeperInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
