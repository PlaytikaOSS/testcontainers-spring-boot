package com.playtika.testcontainer.nats;

import io.nats.client.Connection;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class EmbeddedNatsBootstrapConfigurationTest extends BaseNatsTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    ConfigurableEnvironment environment;

    @Autowired
    Connection natsConnection;

    @Test
    void shouldConnect() throws InterruptedException {
        assertThat(natsConnection.getStatus()).isEqualTo(Connection.Status.CONNECTED);
        natsConnection.close();
    }

    @Test
    void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.nats.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.nats.host")).isNotEmpty();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
