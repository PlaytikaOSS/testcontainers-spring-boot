package com.playtika.test.pulsar;

import com.playtika.test.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Configuration
@ConditionalOnExpression("${embedded.containers.enabled:true}")
@ConditionalOnProperty(name = "embedded.pulsar.enabled", matchIfMissing = true)
@EnableConfigurationProperties(PulsarProperties.class)
public class EmbeddedPulsarBootstrapConfiguration {

    @Bean(name = PulsarProperties.EMBEDDED_PULSAR)
    public PulsarContainer embeddedPulsar(final PulsarProperties pulsarProperties,
                                          final ConfigurableEnvironment environment) {
        DockerImageName image = TestcontainersConfiguration.getInstance()
                .getPulsarDockerImageName()
                .withTag(pulsarProperties.imageTag);
        PulsarContainer pulsarContainer = new PulsarContainer(image);
        pulsarContainer = (PulsarContainer) ContainerUtils.configureCommonsAndStart(pulsarContainer, pulsarProperties, log);
        registerEmbeddedPulsarEnvironment(environment, pulsarContainer);
        return pulsarContainer;
    }

    private static void registerEmbeddedPulsarEnvironment(final ConfigurableEnvironment environment,
                                                          final PulsarContainer pulsarContainer) {
        String pulsarBrokerUrl = pulsarContainer.getPulsarBrokerUrl();
        String pulsarHttpServiceUrl = pulsarContainer.getHttpServiceUrl();

        Map<String, Object> pulsarEnv = new LinkedHashMap<>();
        pulsarEnv.put("embedded.pulsar.brokerUrl", pulsarBrokerUrl);
        pulsarEnv.put("embedded.pulsar.httpServiceUrl", pulsarHttpServiceUrl);

        MapPropertySource propertySource = new MapPropertySource("embeddedPulsarInfo", pulsarEnv);
        environment.getPropertySources().addFirst(propertySource);
    }
}
