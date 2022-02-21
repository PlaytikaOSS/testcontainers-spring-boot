package com.playtika.test.localstack;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.localstack.LocalStackProperties.BEAN_NAME_EMBEDDED_LOCALSTACK;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.localstack.enabled", matchIfMissing = true)
@EnableConfigurationProperties(LocalStackProperties.class)
public class EmbeddedLocalStackBootstrapConfiguration {
    @ConditionalOnMissingBean(name = BEAN_NAME_EMBEDDED_LOCALSTACK)
    @Bean(name = BEAN_NAME_EMBEDDED_LOCALSTACK, destroyMethod = "stop")
    public LocalStackContainer localStack(ConfigurableEnvironment environment,
                                                  LocalStackProperties properties) {
        LocalStackContainer localStackContainer = new LocalStackContainer(ContainerUtils.getDockerImageName(properties));
        localStackContainer
            .withExposedPorts(properties.getEdgePort())
            .withEnv("EDGE_PORT", String.valueOf(properties.getEdgePort()))
            .withEnv("HOSTNAME", properties.getHostname())
            .withEnv("HOSTNAME_EXTERNAL", properties.getHostnameExternal());

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
        String host = localStack.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.localstack.host", host);
        map.put("embedded.localstack.accessKey", localStack.getAccessKey());
        map.put("embedded.localstack.secretKey", localStack.getSecretKey());
        String prefix = "embedded.localstack.";
        Integer mappedPort = localStack.getMappedPort(properties.getEdgePort());
        for (LocalStackContainer.Service service : properties.services) {
            map.put(prefix + service, localStack.getEndpointConfiguration(service).getServiceEndpoint());
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
