package com.playtika.test.vertica;

import com.playtika.test.common.spring.DockerPresenceBootstrapConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import java.util.LinkedHashMap;

import static com.playtika.test.common.utils.ContainerUtils.configureCommonsAndStart;
import static com.playtika.test.vertica.VerticaProperties.BEAN_NAME_EMBEDDED_VERTICA;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@AutoConfigureAfter(DockerPresenceBootstrapConfiguration.class)
@ConditionalOnProperty(name = "embedded.vertica.enabled", matchIfMissing = true)
@EnableConfigurationProperties(VerticaProperties.class)
public class EmbeddedVerticaBootstrapConfiguration {

    @Bean(name = BEAN_NAME_EMBEDDED_VERTICA, destroyMethod = "stop")
    public GenericContainer embeddedVertica(ConfigurableEnvironment environment, VerticaProperties properties) {
        log.info("Starting Vertica server. Docker image: {}", properties.getDockerImage());

        GenericContainer verticaContainer = configureCommonsAndStart(createContainer(properties), properties, log);

        registerVerticaEnvironment(verticaContainer, environment, properties);

        return verticaContainer;
    }

    private GenericContainer createContainer(VerticaProperties properties) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("DATABASE_NAME", properties.getDatabase());
        map.put("DATABASE_PASSWORD", properties.getPassword());

        return new GenericContainer(properties.getDockerImage())
                .withExposedPorts(properties.getPort())
                .withEnv(map)
                .waitingFor(new HostPortWaitStrategy());
    }

    private void registerVerticaEnvironment(GenericContainer verticaContainer,
                                            ConfigurableEnvironment environment,
                                            VerticaProperties properties) {
        Integer mappedPort = verticaContainer.getMappedPort(properties.getPort());
        String host = verticaContainer.getContainerIpAddress();

        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("embedded.vertica.port", mappedPort);
        map.put("embedded.vertica.host", host);
        map.put("embedded.vertica.database", properties.getDatabase());
        map.put("embedded.vertica.user", properties.getUser());
        map.put("embedded.vertica.password", properties.getPassword());


        MapPropertySource propertySource = new MapPropertySource("embeddedVerticaInfo", map);
        environment.getPropertySources().addFirst(propertySource);

        log.info("Started Vertica server. Connection details: {}, ", map);
    }
}
