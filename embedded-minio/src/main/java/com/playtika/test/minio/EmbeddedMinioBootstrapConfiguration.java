package com.playtika.test.minio;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.*;
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

    @Bean(name = MINIO_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer<?> minio(MinioWaitStrategy minioWaitStrategy,
                                  ConfigurableEnvironment environment,
                                  MinioProperties properties) {

        GenericContainer<?> minio =
                new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                        .withExposedPorts(properties.port, properties.consolePort)
                        .withEnv("MINIO_ROOT_USER", properties.accessKey)
                        .withEnv("MINIO_ROOT_PASSWORD", properties.secretKey)
                        .withEnv("MINIO_SITE_REGION", properties.region)
                        .withEnv("MINIO_WORM", properties.worm)
                        .withEnv("MINIO_BROWSER", properties.browser)
                        .withCommand("server", properties.directory, "--console-address", ":" + properties.consolePort)
                        .waitingFor(minioWaitStrategy);

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
