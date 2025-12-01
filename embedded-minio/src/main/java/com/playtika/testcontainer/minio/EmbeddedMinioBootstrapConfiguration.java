package com.playtika.testcontainer.minio;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.minio.MinioProperties.BEAN_NAME_EMBEDDED_MINIO;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(value = "embedded.minio.enabled", matchIfMissing = true)
public class EmbeddedMinioBootstrapConfiguration {

    private static final String MINIO_NETWORK_ALIAS = "minio.testcontainer.docker";

    @Bean
    @ConditionalOnMissingBean
    MinioProperties minioProperties() {
        return new MinioProperties();
    }

    @Bean(name = "minioWaitStrategy")
    @ConditionalOnMissingBean
    public MinioWaitStrategy minioWaitStrategy(MinioProperties properties) {
        return new DefaultMinioWaitStrategy(properties);
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "minio")
    ToxiproxyClientProxy minioContainerProxy(ToxiproxyClient toxiproxyClient,
                                              ToxiproxyContainer toxiproxyContainer,
                                              @Qualifier(BEAN_NAME_EMBEDDED_MINIO) GenericContainer<?> minio,
                                              MinioProperties properties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                minio,
                properties.getPort(),
                "minio");
    }

    @Bean(name = BEAN_NAME_EMBEDDED_MINIO, destroyMethod = "stop")
    public GenericContainer<?> minio(MinioWaitStrategy minioWaitStrategy,
                                     MinioProperties properties,
                                     Optional<Network> network) {
        GenericContainer<?> minio =
                new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                        .withExposedPorts(properties.getPort(), properties.getConsolePort())
                        .withEnv("MINIO_ROOT_USER", properties.getAccessKey())
                        .withEnv("MINIO_ROOT_PASSWORD", properties.getSecretKey())
                        .withEnv("MINIO_SITE_REGION", properties.getRegion())
                        .withEnv("MINIO_WORM", properties.getWorm())
                        .withEnv("MINIO_BROWSER", properties.getBrowser())
                        .withCommand("server", properties.getDirectory(), "--console-address", ":" + properties.getConsolePort())
                        .waitingFor(minioWaitStrategy)
                        .withNetworkAliases(MINIO_NETWORK_ALIAS);

        network.ifPresent(minio::withNetwork);
        minio = configureCommonsAndStart(minio, properties, log);
        return minio;
    }

    @Bean
    public DynamicPropertyRegistrar minioDynamicPropertyRegistrar(
            @Qualifier(BEAN_NAME_EMBEDDED_MINIO) GenericContainer<?> minio,
            MinioProperties properties) {
        return registry -> {
            registry.add("embedded.minio.host", minio::getHost);
            registry.add("embedded.minio.port", () -> minio.getMappedPort(properties.port));
            registry.add("embedded.minio.consolePort", () -> minio.getMappedPort(properties.consolePort));
            registry.add("embedded.minio.accessKey", properties::getAccessKey);
            registry.add("embedded.minio.secretKey", properties::getSecretKey);
            registry.add("embedded.minio.region", properties::getRegion);
            registry.add("embedded.minio.networkAlias", () -> MINIO_NETWORK_ALIAS);
            registry.add("embedded.minio.internalPort", properties::getPort);
            registry.add("embedded.minio.internalConsolePort", properties::getConsolePort);
        };
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "minio")
    public DynamicPropertyRegistrar minioToxiProxyDynamicPropertyRegistrar(
            @Qualifier("minioContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.minio");
    }

}
