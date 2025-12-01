package com.playtika.testcontainer.git;

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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.toxiproxy.ToxiproxyContainer;

import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.git.GitProperties.BEAN_NAME_EMBEDDED_GIT;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.springframework.util.ObjectUtils.isEmpty;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.git.enabled", matchIfMissing = true)
@EnableConfigurationProperties(GitProperties.class)
public class EmbeddedGitBootstrapConfiguration {

    private static final String GIT_NETWORK_ALIAS = "git.testcontainer.docker";

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "git")
    ToxiproxyClientProxy gitContainerProxy(ToxiproxyClient toxiproxyClient,
                                            ToxiproxyContainer toxiproxyContainer,
                                            @Qualifier(BEAN_NAME_EMBEDDED_GIT) GenericContainer<?> embeddedGit,
                                           GitProperties gitProperties) {
        return ToxiproxyHelper.createProxy(
                toxiproxyClient,
                toxiproxyContainer,
                embeddedGit,
                gitProperties.getPort(),
                "git");
    }

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "git")
    public DynamicPropertyRegistrar gitToxiProxyDynamicPropertyRegistrar(@Qualifier("gitContainerProxy") ToxiproxyClientProxy proxy) {
        return ToxiproxyHelper.createToxiProxyDynamicPropertyRegistrar(proxy, "embedded.git");
    }

    @ConditionalOnMissingBean(name = BEAN_NAME_EMBEDDED_GIT)
    @Bean(name = BEAN_NAME_EMBEDDED_GIT, destroyMethod = "stop")
    public GenericContainer<?> embeddedGit(GitProperties properties, Optional<Network> network) {
        GenericContainer<?> gitContainer = configureCommonsAndStart(createContainer(properties, network), properties, log);
        return gitContainer;
    }

    @Bean
    public DynamicPropertyRegistrar gitDynamicPropertyRegistrar(@Qualifier(BEAN_NAME_EMBEDDED_GIT) GenericContainer<?> gitContainer, GitProperties properties) {
        return registry -> {
            Integer mappedPort = gitContainer.getMappedPort(properties.getPort());
            String host = gitContainer.getHost();
            String password = properties.getPassword();
            registry.add("embedded.git.port", () -> mappedPort);
            registry.add("embedded.git.host", () -> host);
            registry.add("embedded.git.password", () -> password);
            registry.add("embedded.git.networkAlias", () -> GIT_NETWORK_ALIAS);
            registry.add("embedded.git.internalPort", properties::getPort);
        };
    }

    private GenericContainer<?> createContainer(GitProperties properties, Optional<Network> network) {
        if (isEmpty(properties.getPathToRepositories())) {
            throw new RuntimeException("embedded.git.path-to-repositories is required");
        }
        GenericContainer<?> container = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withCopyFileToContainer(forClasspathResource(properties.getPathToSshdConfig()), "/etc/ssh/sshd_config")
                .withFileSystemBind(properties.getPathToRepositories(), "/projects")
                .withEnv("GIT_REPOSITORIES_PATH", "/projects")
                .withEnv("GIT_PASSWORD", properties.getPassword())
                .withExposedPorts(properties.getPort())
                .withNetworkAliases(GIT_NETWORK_ALIAS)
                .waitingFor(new HostPortWaitStrategy());
        network.ifPresent(container::withNetwork);
        if (isNotEmpty(properties.getPathToAuthorizedKeys())) {
            container.withFileSystemBind(properties.getPathToAuthorizedKeys(), "/home/git/.ssh/authorized_keys");
        }
        return container;
    }
}
