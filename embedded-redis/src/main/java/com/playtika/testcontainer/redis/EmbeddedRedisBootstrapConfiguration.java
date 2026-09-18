package com.playtika.testcontainer.redis;

import com.playtika.testcontainer.common.spring.ContainerStartupCoordinator;
import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.common.utils.FileUtils;
import com.playtika.testcontainer.redis.wait.DefaultRedisClusterWaitStrategy;
import com.playtika.testcontainer.redis.wait.RedisStatusCheck;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import com.redis.testcontainers.RedisContainer;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.RequiredArgsConstructor;
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
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.toxiproxy.ToxiproxyContainer;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommons;
import static com.playtika.testcontainer.redis.EnvUtils.registerRedisEnvironment;
import static com.playtika.testcontainer.redis.RedisProperties.BEAN_NAME_EMBEDDED_REDIS;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.redis.enabled", matchIfMissing = true)
@EnableConfigurationProperties(RedisProperties.class)
@RequiredArgsConstructor
public class EmbeddedRedisBootstrapConfiguration {

    public static final String REDIS_NETWORK_ALIAS = "redis.testcontainer.docker";
    public static final String REDIS_WAIT_STRATEGY_BEAN_NAME = "redisStartupCheckStrategy";

    private final ResourceLoader resourceLoader;
    private final RedisProperties properties;

    @Bean(name = REDIS_WAIT_STRATEGY_BEAN_NAME)
    @ConditionalOnMissingBean(name = REDIS_WAIT_STRATEGY_BEAN_NAME)
    @ConditionalOnProperty(name = "embedded.redis.clustered", havingValue = "false", matchIfMissing = true)
    public WaitStrategy redisStartupCheckStrategy(RedisProperties properties) {
        return new RedisStatusCheck(properties);
    }

    @Bean(name = REDIS_WAIT_STRATEGY_BEAN_NAME)
    @ConditionalOnMissingBean(name = REDIS_WAIT_STRATEGY_BEAN_NAME)
    @ConditionalOnProperty(name = "embedded.redis.clustered", havingValue = "true")
    public WaitStrategy redisClusterWaitStrategy(RedisProperties properties) {
        return new DefaultRedisClusterWaitStrategy(properties);
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "redis")
    ToxiproxyClientProxy redisContainerProxy(ToxiproxyClient toxiproxyClient,
                                              ToxiproxyContainer toxiproxyContainer,
                                              @Qualifier(BEAN_NAME_EMBEDDED_REDIS) GenericContainer<?> redis,
                                              RedisProperties properties,
                                              ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                redis,
                properties.getPort(),
                "redis");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.redis", "embeddedRedisToxiProxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_REDIS, destroyMethod = "stop")
    public RedisContainer redis(ConfigurableEnvironment environment,
                                     @Qualifier(REDIS_WAIT_STRATEGY_BEAN_NAME) WaitStrategy redisStartupCheckStrategy,
                                     Optional<Network> network,
                                     ContainerStartupCoordinator startupCoordinator) throws Exception {

        // CLUSTER SLOTS command returns IP:port for each node, so ports outside and inside
        // container must be the same
        RedisContainer redis =
            new RedisContainerWithExposedPort(ContainerUtils.getDockerImageName(properties).asCanonicalNameString())
                        .withFixedExposedPort(properties.getPort(), properties.getPort())
                        .withExposedPorts(properties.getPort())
                        .withEnv("REDIS_USER", properties.getUser())
                        .withEnv("REDIS_PASSWORD", properties.getPassword())
                        // Redis 8.8+ drops privileges before startup and must still read/write these files.
                        .withCopyFileToContainer(MountableFile.forHostPath(prepareRedisConf(), 0444), "/data/redis.conf")
                        .withCopyFileToContainer(MountableFile.forHostPath(prepareNodesConf(), 0666), "/data/nodes.conf")
                        .withCommand("redis-server", "/data/redis.conf")
                        .waitingFor(redisStartupCheckStrategy)
                        .withNetworkAliases(REDIS_NETWORK_ALIAS);
        network.ifPresent(redis::withNetwork);
        RedisContainer configuredRedis = (RedisContainer) configureCommons(redis, properties, log);

        startupCoordinator.schedule(() -> {
            ContainerUtils.startAndLogTime(configuredRedis, log);
            Map<String, Object> redisEnv = registerRedisEnvironment(environment, configuredRedis, properties, properties.getPort());
            log.info("Started Redis cluster. Connection details: {}", redisEnv);
        });

        return configuredRedis;
    }

    private Path prepareRedisConf() throws IOException {
        return FileUtils.resolveTemplateAsPath(resourceLoader, "redis.conf", content -> content
                .replace("{{requirepass}}", properties.isRequirepass() ? "yes" : "no")
                .replace("{{password}}", properties.isRequirepass() ? "requirepass " + properties.getPassword() : "")
                .replace("{{clustered}}", properties.isClustered() ? "yes" : "no")
                .replace("{{port}}", String.valueOf(properties.getPort())));
    }

    private Path prepareNodesConf() throws IOException {
        return FileUtils.resolveTemplateAsPath(resourceLoader, "nodes.conf", content -> content
                .replace("{{port}}", String.valueOf(properties.getPort()))
                .replace("{{busPort}}", String.valueOf(properties.getPort() + 10000)));
    }

    private static class RedisContainerWithExposedPort extends RedisContainer {
        public RedisContainerWithExposedPort(String dockerImageName) {
            super(dockerImageName);
        }

        public RedisContainer withFixedExposedPort(int hostPort, int containerPort) {
            super.addFixedExposedPort(hostPort, containerPort);

            return self();
        }
    }
}
