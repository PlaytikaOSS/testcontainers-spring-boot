package com.playtika.testcontainer.localstack;

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
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.playtika.testcontainer.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.testcontainer.localstack.LocalStackProperties.BEAN_NAME_EMBEDDED_LOCALSTACK;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.localstack.enabled", matchIfMissing = true)
@EnableConfigurationProperties(LocalStackProperties.class)
public class EmbeddedLocalStackBootstrapConfiguration {

    @Bean
    @ConditionalOnToxiProxyEnabled(module = "localstack")
    ToxiproxyContainer.ContainerProxy localstackContainerProxy(ToxiproxyContainer toxiproxyContainer,
                                                          @Qualifier(BEAN_NAME_EMBEDDED_LOCALSTACK) LocalStackContainer localStack,
                                                          LocalStackProperties properties,
                                                          ConfigurableEnvironment environment) {
        ToxiproxyContainer.ContainerProxy proxy = toxiproxyContainer.getProxy(localStack, properties.getEdgePort());

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.localstack.toxiproxy.host", proxy.getContainerIpAddress());
        map.put("embedded.localstack.toxiproxy.port", proxy.getProxyPort());
        map.put("embedded.localstack.toxiproxy.proxyName", proxy.getName());

        MapPropertySource propertySource = new MapPropertySource("embeddedLocalstackToxiproxyInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        log.info("Started Localstack ToxiProxy connection details {}", map);

        return proxy;
    }

    @ConditionalOnMissingBean(name = BEAN_NAME_EMBEDDED_LOCALSTACK)
    @Bean(name = BEAN_NAME_EMBEDDED_LOCALSTACK, destroyMethod = "stop")
    public LocalStackContainer localStack(ConfigurableEnvironment environment,
                                          LocalStackProperties properties,
                                          Optional<Network> network) {
        LocalStackContainer localStackContainer = new LocalStackContainer(ContainerUtils.getDockerImageName(properties));
        localStackContainer
                .withExposedPorts(properties.getEdgePort())
                .withEnv("EDGE_PORT", String.valueOf(properties.getEdgePort()))
                .withEnv("HOSTNAME", properties.getHostname())
                .withEnv("HOSTNAME_EXTERNAL", properties.getHostnameExternal());

        network.ifPresent(localStackContainer::withNetwork);

        for (LocalStackContainer.Service service : properties.services) {
            localStackContainer.withServices(service);
        }
        localStackContainer = (LocalStackContainer) configureCommonsAndStart(localStackContainer, properties, log);
        registerLocalStackEnvironment(localStackContainer, environment, properties);
        return localStackContainer;
    }

    private void registerLocalStackEnvironment(LocalStackContainer localStack,
                                               ConfigurableEnvironment environment,
                                               LocalStackProperties properties) {
        String host = localStack.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.localstack.host", host);
        map.put("embedded.localstack.accessKey", localStack.getAccessKey());
        map.put("embedded.localstack.secretKey", localStack.getSecretKey());
        String prefix = "embedded.localstack.";
        Integer mappedPort = localStack.getMappedPort(properties.getEdgePort());
        for (LocalStackContainer.Service service : properties.services) {
            map.put(prefix + service, localStack.getEndpointOverride(service));
            map.put(prefix + service + ".port", mappedPort);
        }
        log.info("Started Localstack. Connection details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedLocalStackInfo", map);
        environment.getPropertySources().addFirst(propertySource);
        setSystemProperties(localStack);
    }

    private static void setSystemProperties(LocalStackContainer localStack) {
        System.setProperty("aws.accessKeyId", localStack.getAccessKey());
        System.setProperty("aws.secretKey", localStack.getAccessKey());
    }

}
