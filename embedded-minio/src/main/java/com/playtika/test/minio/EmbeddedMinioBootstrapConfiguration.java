package com.playtika.test.minio;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import com.playtika.test.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
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

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.minio.MinioProperties.MINIO_BEAN_NAME;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(value = "embedded.minio.enabled", matchIfMissing = true)
public class EmbeddedMinioBootstrapConfiguration {

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
                                                          @Qualifier(MINIO_BEAN_NAME) GenericContainer<?> minio,
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

    @Bean(name = MINIO_BEAN_NAME, destroyMethod = "stop")
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
                        .waitingFor(minioWaitStrategy);

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

        log.info("Started Minio server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedMinioInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }

}
