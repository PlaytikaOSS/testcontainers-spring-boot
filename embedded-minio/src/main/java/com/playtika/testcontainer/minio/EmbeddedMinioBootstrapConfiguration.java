package com.playtika.testcontainer.minio;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Map;
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
    ToxiproxyContainer.ContainerProxy minioContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                          @Qualifier(BEAN_NAME_EMBEDDED_MINIO) GenericContainer<?> minio,
                                                          ConfigurableEnvironment environment,
                                                          MinioProperties properties) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(minio, properties.getPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.minio.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.minio.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.minio.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedMinioToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Minio ToxiProxy connection details {}", map);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_MINIO, destroyMethod = "stop")
    public GenericContainer<?> minio(MinioWaitStrategy minioWaitStrategy,
                                     ConfigurableEnvironment environment,
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
        registerEnvironment(minio, environment, properties);
        return minio;
    }

    private void registerEnvironment(GenericContainer<?> container,
                                     ConfigurableEnvironment environment,
                                     MinioProperties properties) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.minio.host", container.getHost());
        map.put("embedded.minio.port", container.getMappedPort(properties.port));
        map.put("embedded.minio.consolePort", container.getMappedPort(properties.consolePort));
        map.put("embedded.minio.accessKey", properties.accessKey);
        map.put("embedded.minio.secretKey", properties.secretKey);
        map.put("embedded.minio.region", properties.region);
        map.put("embedded.minio.networkAlias", MINIO_NETWORK_ALIAS);
        map.put("embedded.minio.internalPort", properties.getPort());
        map.put("embedded.minio.internalConsolePort", properties.getConsolePort());

        log.info("Started Minio server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedMinioInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
