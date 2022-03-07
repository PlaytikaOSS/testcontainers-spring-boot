package com.playtika.test.redis;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import com.playtika.test.common.utils.FileUtils;
import com.playtika.test.redis.wait.DefaultRedisClusterWaitStrategy;
import com.playtika.test.redis.wait.RedisStatusCheck;
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
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.redis.EnvUtils.registerRedisEnvironment;
import static com.playtika.test.redis.RedisProperties.BEAN_NAME_EMBEDDED_REDIS;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.redis.enabled", matchIfMissing = true)
@EnableConfigurationProperties(RedisProperties.class)
@RequiredArgsConstructor
public class EmbeddedRedisBootstrapConfiguration {

    public final static String REDIS_WAIT_STRATEGY_BEAN_NAME = "redisStartupCheckStrategy";

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

    @Bean(name = BEAN_NAME_EMBEDDED_REDIS, destroyMethod = "stop")
    public GenericContainer redis(ConfigurableEnvironment environment,
                                  @Qualifier(REDIS_WAIT_STRATEGY_BEAN_NAME) WaitStrategy redisStartupCheckStrategy) throws Exception {

        // CLUSTER SLOTS command returns IP:port for each node, so ports outside and inside
        // container must be the same
        GenericContainer redis =
                new FixedHostPortGenericContainer(ContainerUtils.getDockerImageName(properties).asCanonicalNameString())
                        .withFixedExposedPort(properties.getPort(), properties.getPort())
                        .withExposedPorts(properties.getPort())
                        .withEnv("REDIS_USER", properties.getUser())
                        .withEnv("REDIS_PASSWORD", properties.getPassword())
                        .withCopyFileToContainer(MountableFile.forHostPath(prepareRedisConf()), "/data/redis.conf")
                        .withCopyFileToContainer(MountableFile.forHostPath(prepareNodesConf()), "/data/nodes.conf")
                        .withCommand("redis-server", "/data/redis.conf")
                        .waitingFor(redisStartupCheckStrategy);
        redis = configureCommonsAndStart(redis, properties, log);
        Map<String, Object> redisEnv = registerRedisEnvironment(environment, redis, properties, properties.getPort());
        log.info("Started Redis cluster. Connection details: {}", redisEnv);
        return redis;
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
