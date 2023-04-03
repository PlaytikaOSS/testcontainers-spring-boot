package com.playtika.test.git;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import com.playtika.test.toxiproxy.condition.ConditionalOnToxiProxyEnabled;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.git.GitProperties.BEAN_NAME_EMBEDDED_GIT;

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
        return new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getPort())
                .waitingFor(new HostPortWaitStrategy());
    }

    private void registerGitEnvironment(GenericContainer<?> gitContainer,
                                        ConfigurableEnvironment environment,
                                        GitProperties properties) {
        Integer mappedPort = gitContainer.getMappedPort(properties.getPort());
        String host = gitContainer.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.git.port", mappedPort);
        map.put("embedded.git.host", host);

        MapPropertySource propertySource = new MapPropertySource("embeddedGitInfo", map);
        environment.getPropertySources().addFirst(propertySource);

        log.info("Started Git server. Connection details: {}, ", map);
    }
}
