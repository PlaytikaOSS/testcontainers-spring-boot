package com.playtika.test.consul;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import com.playtika.test.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.consul.ConsulProperties.BEAN_NAME_EMBEDDED_CONSUL;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@EnableConfigurationProperties(ConsulProperties.class)
@ConditionalOnProperty(name = "embedded.consul.enabled", matchIfMissing = true)
public class EmbeddedConsulBootstrapConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_CONSUL, destroyMethod = "stop")
    public GenericContainer consulContainer(ConfigurableEnvironment environment, ConsulProperties properties) {
        GenericContainer consul = new GenericContainer<>(ContainerUtils.getDockerImageName(properties))
                .withExposedPorts(properties.getPort())
                .waitingFor(
                        Wait.forHttp("/v1/status/leader")
                                .forStatusCode(200)
                ).withStartupTimeout(properties.getTimeoutDuration());

        if (properties.getConfigurationFile() != null) {
            consul = consul.withClasspathResourceMapping(
                    properties.getConfigurationFile(), "/consul/config/test.hcl",
                    BindMode.READ_ONLY);
        }

        consul = configureCommonsAndStart(consul, properties, log);
        registerConsulEnvironment(consul, environment, properties);
        return consul;
    }

    private void registerConsulEnvironment(GenericContainer consul, ConfigurableEnvironment environment,
                                           ConsulProperties properties) {
        Integer mappedPort = consul.getMappedPort(properties.getPort());
        String host = consul.getHost();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.consul.port", mappedPort);
        map.put("embedded.consul.host", host);

        log.info("Started consul. Connection Details: {}", map);

        MapPropertySource propertySource = new MapPropertySource("embeddedConsulInfo", map);
        environment.getPropertySources().addFirst(propertySource);
    }
}
