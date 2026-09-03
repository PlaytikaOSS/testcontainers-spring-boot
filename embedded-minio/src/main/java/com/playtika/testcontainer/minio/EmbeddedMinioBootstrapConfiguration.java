package com.playtika.testcontainer.minio;

import com.playtika.testcontainer.common.spring.ContainerStartupCoordinator;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.LinkedHashMap;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommons;
import static com.playtika.testcontainer.minio.MinioProperties.BEAN_NAME_EMBEDDED_MINIO;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@EnableConfigurationProperties(MinioProperties.class)
@ConditionalOnProperty(value = "embedded.minio.enabled", matchIfMissing = true)
public class EmbeddedMinioBootstrapConfiguration {

    private static final String MINIO_NETWORK_ALIAS = "minio.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "minio")
    ToxiproxyClientProxy minioContainerProxy(ToxiproxyClient toxiproxyClient,
                                              ToxiproxyContainer toxiproxyContainer,
                                              @Qualifier(BEAN_NAME_EMBEDDED_MINIO) MinIOContainer minio,
                                              ConfigurableEnvironment environment,
                                              MinioProperties properties) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                minio,
                properties.getPort(),
                "minio");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.minio", "embeddedMinioToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = BEAN_NAME_EMBEDDED_MINIO, destroyMethod = "stop")
    public MinIOContainer minio(ConfigurableEnvironment environment,
                                MinioProperties properties,
                                Optional<Network> network,
                                ContainerStartupCoordinator startupCoordinator) {
        MinIOContainer minio =
                new MinIOContainer(ContainerUtils.getDockerImageName(properties))
                        .withUserName(properties.getAccessKey())
                        .withPassword(properties.getSecretKey())
                        .withEnv("MINIO_SITE_REGION", properties.getRegion())
                        .withEnv("MINIO_WORM", properties.getWorm())
                        .withEnv("MINIO_BROWSER", properties.getBrowser())
                        .waitingFor(Wait.forHttp("/minio/health/live")
                                .forPort(properties.getPort())
                                .withStartupTimeout(properties.getTimeoutDuration()))
                        .withNetworkAliases(MINIO_NETWORK_ALIAS);

        network.ifPresent(minio::withNetwork);
        MinIOContainer configuredMinio = (MinIOContainer) configureCommons(minio, properties, log);
        startupCoordinator.schedule(() -> {
            ContainerUtils.startAndLogTime(configuredMinio, log);
            registerEnvironment(configuredMinio, environment, properties);
        });
        return configuredMinio;
    }

    private void registerEnvironment(MinIOContainer container,
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
