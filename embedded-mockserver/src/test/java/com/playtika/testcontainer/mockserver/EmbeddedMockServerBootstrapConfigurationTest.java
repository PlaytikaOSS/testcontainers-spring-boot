package com.playtika.testcontainer.mockserver;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class EmbeddedMockServerBootstrapConfigurationTest extends BaseMockServerTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    ConfigurableEnvironment environment;

    @Autowired
    MockServerClient mockServerClient;

    @Test
    void shouldConnect() {
        assertThat(mockServerClient.hasStarted()).isTrue();
    }

    @Test
    void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.mockserver.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.mockserver.host")).isNotEmpty();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
