package com.playtika.testcontainer.redis;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.common.utils.FileUtils;
import com.playtika.testcontainer.redis.wait.DefaultRedisClusterWaitStrategy;
import com.playtika.testcontainer.redis.wait.RedisStatusCheck;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
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
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ResourceLoader;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
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
    public GenericContainer<?> redis(@Qualifier(REDIS_WAIT_STRATEGY_BEAN_NAME) WaitStrategy redisStartupCheckStrategy,
                                     ConfigurableEnvironment environment,
                                     Optional<Network> network) throws Exception {
        GenericContainer<?> redis =
                new FixedHostPortGenericContainer(ContainerUtils.getDockerImageName(properties).asCanonicalNameString())
                        .withFixedExposedPort(properties.getPort(), properties.getPort())
                        .withExposedPorts(properties.getPort())
                        .withEnv("REDIS_USER", properties.getUser())
                        .withEnv("REDIS_PASSWORD", properties.getPassword())
                        .withCopyFileToContainer(MountableFile.forHostPath(prepareRedisConf()), "/data/redis.conf")
                        .withCopyFileToContainer(MountableFile.forHostPath(prepareNodesConf()), "/data/nodes.conf")
                        .withCommand("redis-server", "/data/redis.conf")
                        .waitingFor(redisStartupCheckStrategy)
                        .withNetworkAliases(REDIS_NETWORK_ALIAS);
        network.ifPresent(redis::withNetwork);
        redis = configureCommonsAndStart(redis, properties, log);
        registerRedisEnvironment(redis, environment, properties);
        return redis;
    }

    private void registerRedisEnvironment(GenericContainer<?> redis, ConfigurableEnvironment environment, RedisProperties properties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.redis.port", properties.getPort());
        map.put("embedded.redis.host", redis.getHost());
        map.put("embedded.redis.password", properties.getPassword());
        map.put("embedded.redis.user", properties.getUser());
        map.put("embedded.redis.networkAlias", REDIS_NETWORK_ALIAS);

        log.info("Started Redis. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedRedisInfo", map);
        environment.getPropertySources().addFirst(propertySource);
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
}
