package com.playtika.testcontainer.artifactory;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.postgresql.EmbeddedPostgreSQLBootstrapConfiguration;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.nio.charset.StandardCharsets;

import static com.playtika.testcontainer.artifactory.ArtifactoryProperties.ARTIFACTORY_BEAN_NAME;
import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static java.time.Duration.ofSeconds;
import static org.testcontainers.postgresql.PostgreSQLContainer.POSTGRESQL_PORT;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter({DockerPresenceBootstrapConfiguration.class, EmbeddedPostgreSQLBootstrapConfiguration.class})
@ConditionalOnProperty(name = "embedded.artifactory.enabled", matchIfMissing = true)
@EnableConfigurationProperties(ArtifactoryProperties.class)
public class EmbeddedArtifactoryBootstrapConfiguration {

    private static final String ARTIFACTORY_NETWORK_ALIAS = "artifactory.testcontainer.docker";
    private static final String POSTGRESQL_NETWORK_ALIAS = "postgresql.testcontainer.docker";

    @Bean
    @ConditionalOnMissingBean(name = "artifactoryWaitStrategy")
    public WaitStrategy artifactoryWaitStrategy(ArtifactoryProperties properties) {
        return new HttpWaitStrategy()
                .forPath("/")
                .forPort(properties.getGeneralPort())
                .forStatusCode(200)
                .withStartupTimeout(ofSeconds(properties.getWaitTimeoutInSeconds()));
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "artifactory")
    ToxiproxyClientProxy artifactoryContainerProxy(ToxiproxyClient toxiproxyClient,
                                                    ToxiproxyContainer toxiproxyContainer,
                                                    @Qualifier(ARTIFACTORY_BEAN_NAME) GenericContainer<?> artifactory,
                                                    ArtifactoryProperties properties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                artifactory,
                properties.getRestApiPort(),
                "artifactory");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "artifactory")
    public DynamicPropertyRegistrar artifactoryToxiProxyDynamicPropertyRegistrar(@Qualifier("artifactoryContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.artifactory");
    }

    @Bean(name = ARTIFACTORY_BEAN_NAME, destroyMethod = "stop")
    public GenericContainer<?> artifactory(ArtifactoryProperties properties,
                                           WaitStrategy artifactoryWaitStrategy,
                                           Network network,
                                           @Qualifier("embeddedPostgreSql") PostgreSQLContainer postgresql) {
        String databaseUrl = String.format("jdbc:postgresql://%s:%d/%s",
                POSTGRESQL_NETWORK_ALIAS,
                POSTGRESQL_PORT,
                properties.getDatabaseName());

        String systemYaml = ""
                + "shared:\n"
                + "  database:\n"
                + "    type: postgresql\n"
                + "    driver: org.postgresql.Driver\n"
                + "    url: " + databaseUrl + "\n"
                + "    username: " + properties.getDatabaseUser() + "\n"
                + "    password: " + properties.getDatabasePassword() + "\n"
                + "  security:\n"
                + "    masterKey: " + properties.getSecurityMasterKey() + "\n"
                + "    joinKey: " + properties.getSecurityJoinKey() + "\n";
        GenericContainer<?> container =
                new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                        .withExposedPorts(properties.getRestApiPort(), properties.getGeneralPort())
                        .withNetwork(network)
                        .withNetworkAliases(properties.getNetworkAlias(), ARTIFACTORY_NETWORK_ALIAS)
                        .withCopyToContainer(
                                Transferable.of(systemYaml.getBytes(StandardCharsets.UTF_8), 0666),
                                "/opt/jfrog/artifactory/var/etc/system.yaml")
                        .withExtraHost("localhost", "127.0.0.1")
                        .withEnv("JF_ROUTER_ENTRYPOINTS_INTERNALHOST", "::1")
                        .withEnv("JF_SHARED_DATABASE_TYPE", "postgresql")
                        .withEnv("JF_SHARED_DATABASE_URL", databaseUrl)
                        .withEnv("JF_SHARED_DATABASE_USERNAME", properties.getDatabaseUser())
                        .withEnv("JF_SHARED_DATABASE_PASSWORD", properties.getDatabasePassword())
                        .withEnv("JF_SHARED_SECURITY_MASTER_KEY", properties.getSecurityMasterKey())
                        .withEnv("JF_SHARED_SECURITY_JOIN_KEY", properties.getSecurityJoinKey())
                        // Some internal services resolve config key as `shared.security.masterKey` / `shared.security.joinKey`
                        // (camelCase). Provide both variants.
                        .withEnv("JF_SHARED_SECURITY_MASTERKEY", properties.getSecurityMasterKey())
                        .withEnv("JF_SHARED_SECURITY_JOINKEY", properties.getSecurityJoinKey())
                        // Some Artifactory 7.x components still require the key files to exist.
                        // Provide them explicitly for ephemeral test containers.
                        .withCopyToContainer(
                                Transferable.of((properties.getSecurityMasterKey() + "\n").getBytes(StandardCharsets.UTF_8), 0666),
                                "/opt/jfrog/artifactory/var/etc/security/master.key")
                        .withCopyToContainer(
                                Transferable.of((properties.getSecurityJoinKey() + "\n").getBytes(StandardCharsets.UTF_8), 0666),
                                "/opt/jfrog/artifactory/var/etc/security/join.key")
                        .dependsOn(postgresql)
                        .waitingFor(artifactoryWaitStrategy);
        configureCommonsAndStart(container, properties, log);
        Integer mappedPort = container.getMappedPort(properties.generalPort);
        String host = container.getHost();
        log.info("Started Artifactory server. Connection details: host={}, port={}, username={}, password={}, staticNetworkAlias={}, internalRestApiPort={}, internalGeneralPort={}",
                host, mappedPort, properties.getUsername(), properties.getPassword(), ARTIFACTORY_NETWORK_ALIAS, properties.getRestApiPort(), properties.getGeneralPort());
        return container;
    }

    @Bean
    public DynamicPropertyRegistrar artifactoryDynamicPropertyRegistrar(@Qualifier(ARTIFACTORY_BEAN_NAME) GenericContainer<?> artifactory, ArtifactoryProperties properties) {
        return registry -> {
            registry.add("embedded.artifactory.host", artifactory::getHost);
            registry.add("embedded.artifactory.port", () -> artifactory.getMappedPort(properties.generalPort));
            registry.add("embedded.artifactory.username", properties::getUsername);
            registry.add("embedded.artifactory.password", properties::getPassword);
            registry.add("embedded.artifactory.staticNetworkAlias", () -> ARTIFACTORY_NETWORK_ALIAS);
            registry.add("embedded.artifactory.internalRestApiPort", properties::getRestApiPort);
            registry.add("embedded.artifactory.internalGeneralPort", properties::getGeneralPort);
        };
    }
}
