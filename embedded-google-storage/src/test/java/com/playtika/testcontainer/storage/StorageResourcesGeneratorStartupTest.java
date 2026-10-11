package com.playtika.testcontainer.storage;

import com.playtika.testcontainer.common.spring.ContainerStartupCoordinator;
import com.playtika.testcontainer.common.spring.ContainerStartupCoordinatorConfiguration;
import io.aiven.testcontainers.fakegcsserver.FakeGcsServerContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Arrays;
import java.util.Map;

import static com.playtika.testcontainer.storage.StorageProperties.BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StorageResourcesGeneratorStartupTest {

    @Test
    void shouldCreateStorageServerBeforeReadingEndpointWithLazyParallelStartup() {
        new ApplicationContextRunner()
                .withAllowBeanDefinitionOverriding(true)
                .withUserConfiguration(EmbeddedStorageBootstrapConfiguration.class,
                        ContainerStartupCoordinatorConfiguration.class, TestConfiguration.class)
                .withPropertyValues("embedded.containers.parallelStartup=true", "embedded.toxiproxy.enabled=false")
                .withInitializer(context -> context.addBeanFactoryPostProcessor(factory ->
                        Arrays.stream(factory.getBeanDefinitionNames())
                                .forEach(name -> factory.getBeanDefinition(name).setLazyInit(true))))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getEnvironment().getProperty("embedded.google.storage.endpoint")).isNull();

                    assertThat(context.getBean(StorageResourcesGenerator.class)).isNotNull();

                    assertThat(context.getEnvironment().getProperty("embedded.google.storage.endpoint"))
                            .isEqualTo("http://localhost:4443");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class TestConfiguration {

        @Bean(name = BEAN_NAME_EMBEDDED_GOOGLE_STORAGE_SERVER)
        FakeGcsServerContainer scheduledStorageServer(ContainerStartupCoordinator coordinator,
                                                     ConfigurableEnvironment environment) {
            coordinator.schedule(() -> environment.getPropertySources().addFirst(new MapPropertySource(
                    "testStorageInfo", Map.of("embedded.google.storage.endpoint", "http://localhost:4443"))));
            return mock(FakeGcsServerContainer.class);
        }
    }
}
