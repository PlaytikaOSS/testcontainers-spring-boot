package com.playtika.testcontainer.git;

import com.playtika.testcontainer.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import com.playtika.testcontainer.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.git.GitProperties.BEAN_NAME_EMBEDDED_GIT;
import static org.testcontainers.shaded.org.apache.commons.lang3.StringUtils.isEmpty;
import static org.testcontainers.shaded.org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.testcontainers.utility.MountableFile.forClasspathResource;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.git.enabled", matchIfMissing = true)
@EnableConfigurationProperties(GitProperties.class)
public class EmbeddedGitBootstrapConfiguration {

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "git")
    ToxiproxyContainer.ContainerProxy gitContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                        @Qualifier(BEAN_NAME_EMBEDDED_GIT) GenericContainer<?> embeddedGit,
                                                        ConfigurableEnvironment environment,
                                                        GitProperties gitProperties) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(embeddedGit, gitProperties.getPort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.git.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.git.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.git.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedGitToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Git ToxiProxy connection details {}", map);

        return proxy;
    }

    @ConditionalOnMissingBean(name = BEAN_NAME_EMBEDDED_GIT)
    @Bean(name = BEAN_NAME_EMBEDDED_GIT, destroyMethod = "stop")
    public GenericContainer<?> embeddedGit(ConfigurableEnvironment environment,
                                           GitProperties properties,
                                           Optional<Network> network) {
        GenericContainer<?> gitContainer = configureCommonsAndStart(createContainer(properties), properties, log);
        network.ifPresent(gitContainer::withNetwork);
        registerGitEnvironment(gitContainer, environment, properties);
        return gitContainer;
    }

    private GenericContainer<?> createContainer(GitProperties properties) {
        if (isEmpty(properties.getPathToRepositories())) {
            throw new RuntimeException("embedded.git.path-to-repositories is required");
        }
        GenericContainer<?> container = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withCopyFileToContainer(forClasspathResource(properties.getPathToSshdConfig()), "/etc/ssh/sshd_config")
                .withFileSystemBind(properties.getPathToRepositories(), "/projects")
                .withEnv("GIT_REPOSITORIES_PATH", "/projects")
                .withEnv("GIT_PASSWORD", properties.getPassword())
                .withExposedPorts(properties.getPort())
                .waitingFor(new HostPortWaitStrategy());
        if (isNotEmpty(properties.getPathToAuthorizedKeys())) {
            container.withFileSystemBind(properties.getPathToAuthorizedKeys(), "/home/git/.ssh/authorized_keys");
        }
        return container;
    }

    private void registerGitEnvironment(GenericContainer<?> gitContainer,
                                        ConfigurableEnvironment environment,
                                        GitProperties properties) {
        Integer mappedPort = gitContainer.getMappedPort(properties.getPort());
        String host = gitContainer.getHost();
        String password = properties.getPassword();
        String connectionString = "ssh://git@" + host + ":" + mappedPort + "/projects/%YOUR_REPO_NAME%";

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.git.port", mappedPort);
        map.put("embedded.git.host", host);
        map.put("embedded.git.password", password);

        MapPropertySource propertySource = new MapPropertySource("embeddedGitInfo", map);
        environment.getPropertySources().addFirst(propertySource);

        log.info("Started Git server. You can clone repo by using the following link: {}. " +
                "%YOUR_REPO_NAME% is a name of a git repository folder inside {}",
                connectionString, properties.getPathToRepositories());
    }
}
