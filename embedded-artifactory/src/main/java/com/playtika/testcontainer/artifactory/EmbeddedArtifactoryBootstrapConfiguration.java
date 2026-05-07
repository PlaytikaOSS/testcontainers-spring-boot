package com.playtika.testcontainer.artifactory;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.postgresql.EmbeddedPostgreSQLBootstrapConfiguration;
import com.playtika.testcontainer.postgresql.PostgreSQLProperties;
import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import com.playtika.testcontainer.toxiproxy.ToxiproxyHelper;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

import static com.playtika.testcontainer.artifactory.ArtifactoryProperties.ARTIFACTORY_BEAN_NAME;
import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static org.testcontainers.postgresql.PostgreSQLContainer.POSTGRESQL_PORT;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter({DockerPresenceBootstrapConfiguration.class, EmbeddedPostgreSQLBootstrapConfiguration.class})
@ConditionalOnProperty(name = "embedded.artifactory.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ArtifactoryProperties.class)
public class EmbeddedArtifactoryBootstrapConfiguration {

    private static final String ARTIFACTORY_NETWORK_ALIAS = "artifactory.testcontainer.docker";

    @Bean
    @ConditionalOnMissingBean(Network.class)
    Network artifactoryNetwork() {
        Network network = Network.newNetwork();
        log.info("Created docker Network with id={}", network.getId());
        return network;
    }

    @Bean
    @ConditionalOnMissingBean(name = "artifactoryWaitStrategy")
    public WaitStrategy artifactoryWaitStrategy(ArtifactoryProperties properties) {
        return new HttpWaitStrategy()
                .forPath("/")
                .forPort(properties.getGeneralPort())
                .forStatusCode(200);
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "artifactory")
    ToxiproxyClientProxy artifactoryContainerProxy(ToxiproxyClient toxiproxyClient,
                                                    ToxiproxyContainer toxiproxyContainer,
                                                    @Qualifier(ARTIFACTORY_BEAN_NAME) GenericContainer<?> artifactory,
                                                    ArtifactoryProperties properties,
                                                    ConfigurableEnvironment environment) {
        ToxiproxyClientProxy proxy = ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                artifactory,
                properties.getRestApiPort(),
                "artifactory");

        ToxiproxyHelper.registerProxyEnvironment(proxy, "embedded.artifactory", "embeddedArtifactoryToxiproxyInfo", environment);

        return proxy;
    }

    @Bean(name = ARTIFACTORY_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer<?> artifactory(ConfigurableEnvironment environment,
                                           ArtifactoryProperties properties,
                                           PostgreSQLContainer postgreSQLContainer,
                                           PostgreSQLProperties postgresqlProperties,
                                           WaitStrategy artifactoryWaitStrategy,
                                           Network network) {

        String systemYaml = getSystemYaml(postgresqlProperties,postgreSQLContainer);

        GenericContainer<?> container =
                new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                        .withExposedPorts(properties.getRestApiPort(), properties.getGeneralPort())
                        .withNetwork(network)
                        .withNetworkAliases(properties.getNetworkAlias(), ARTIFACTORY_NETWORK_ALIAS)
                        .withCopyToContainer(
                                Transferable.of(systemYaml.getBytes(StandardCharsets.UTF_8), 0666),
                                "/opt/jfrog/artifactory/var/etc/system.yaml")
                        .waitingFor(artifactoryWaitStrategy);

        configureCommonsAndStart(container, properties, log);

        registerEnvironment(container, environment, properties);

        return container;
    }

    private static @NonNull String getSystemYaml(PostgreSQLProperties postgresqlProperties,
                                                 PostgreSQLContainer postgreSQLContainer) {
        String jdbcUrl = "jdbc:postgresql://%s:%d/%s"
            .formatted(postgreSQLContainer.getNetwork(), POSTGRESQL_PORT, postgresqlProperties.getDatabase());

        return """
            shared:
              database:
                type: "postgresql"
                driver: "org.postgresql.Driver"
                url: "%s"
                username: "%s"
                password: "%s"
            """.formatted(jdbcUrl, postgreSQLContainer.getUsername(), postgreSQLContainer.getPassword());
    }

    private void registerEnvironment(GenericContainer<?> artifactory,
                                     ConfigurableEnvironment environment,
                                     ArtifactoryProperties properties) {

        Integer mappedPort = artifactory.getMappedPort(properties.generalPort);
        String host = artifactory.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.artifactory.host", host);
        map.put("embedded.artifactory.port", mappedPort);
        map.put("embedded.artifactory.username", properties.getUsername());
        map.put("embedded.artifactory.password", properties.getPassword());
        map.put("embedded.artifactory.staticNetworkAlias", ARTIFACTORY_NETWORK_ALIAS);
        map.put("embedded.artifactory.internalRestApiPort", properties.getRestApiPort());
        map.put("embedded.artifactory.internalGeneralPort", properties.getGeneralPort());

        log.info("Started Artifactory server. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedArtifactoryInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
