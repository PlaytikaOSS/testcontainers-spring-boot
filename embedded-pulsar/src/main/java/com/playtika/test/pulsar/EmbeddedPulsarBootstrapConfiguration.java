package com.playtika.test.pulsar;

import com.playtika.test.common.utils.ContainerUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.PulsarContainer;

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
                                          final ConfigurableEnvironment environment,
                                          @Deprecated @Value("${embedded.pulsar.imageTag:#{null}}") String deprImageTag) {
        if (deprImageTag != null) {
            throw new IllegalArgumentException("Property `embedded.pulsar.imageTag` is deprecated. Please replace property `embedded.pulsar.imageTag` with `embedded.pulsar.dockerImageVersion`.");
        }
        PulsarContainer pulsarContainer = new PulsarContainer(ContainerUtils.getDockerImageName(pulsarProperties));
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
