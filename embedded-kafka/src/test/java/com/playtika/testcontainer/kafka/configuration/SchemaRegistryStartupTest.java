package com.playtika.testcontainer.kafka.configuration;

import com.playtika.testcontainer.common.spring.ContainerStartupCoordinator;
import com.playtika.testcontainer.common.spring.ContainerStartupCoordinatorConfiguration;
import com.playtika.testcontainer.common.utils.ContainerUtils;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.Map;

import static com.playtika.testcontainer.kafka.properties.KafkaConfigurationProperties.KAFKA_BEAN_NAME;
import static com.playtika.testcontainer.kafka.properties.SchemaRegistryConfigurationProperties.SCHEMA_REGISTRY_BEAN_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class SchemaRegistryStartupTest {

    @Test
    void shouldCreateKafkaBeforeConfiguringSchemaRegistryWithLazyParallelStartup() {
        try (MockedStatic<ContainerUtils> utils = mockStatic(ContainerUtils.class)) {
            utils.when(() -> ContainerUtils.getDockerImageName(any()))
                    .thenReturn(DockerImageName.parse("confluentinc/cp-schema-registry:7.9.9"));
            utils.when(() -> ContainerUtils.configureCommons(any(), any(), any()))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            new ApplicationContextRunner()
                    .withUserConfiguration(SchemaRegistryContainerConfiguration.class,
                            ContainerStartupCoordinatorConfiguration.class, TestConfiguration.class)
                    .withPropertyValues("embedded.containers.parallelStartup=true",
                            "embedded.kafka.schema-registry.enabled=true")
                    .withInitializer(context -> context.addBeanFactoryPostProcessor(factory ->
                            Arrays.stream(factory.getBeanDefinitionNames())
                                    .forEach(name -> factory.getBeanDefinition(name).setLazyInit(true))))
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context.getEnvironment().getProperty("embedded.kafka.containerBrokerList")).isNull();

                        GenericContainer<?> registry = context.getBean(SCHEMA_REGISTRY_BEAN_NAME, GenericContainer.class);

                        assertThat(registry.getEnvMap())
                                .containsEntry("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:9092");
                    });
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {

        @Bean(name = KAFKA_BEAN_NAME)
        GenericContainer<?> scheduledKafka(ContainerStartupCoordinator coordinator,
                                           ConfigurableEnvironment environment) {
            coordinator.schedule(() -> environment.getPropertySources().addFirst(new MapPropertySource(
                    "testKafkaInfo", Map.of("embedded.kafka.containerBrokerList", "kafka:9092"))));
            return mock(GenericContainer.class);
        }

        @Bean
        Network network() {
            return mock(Network.class);
        }
    }
}
