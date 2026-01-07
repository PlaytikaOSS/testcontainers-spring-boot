package com.playtika.testcontainer.nats;

import io.nats.client.Connection;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class EmbeddedNatsBootstrapConfigurationTest extends BaseNatsTest {

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Value("${embedded.nats.port}")
    String natsPort;

    @Value("${embedded.nats.host}")
    String natsHost;

    @Autowired
    Connection natsConnection;

    @Test
    void shouldConnect() throws InterruptedException {
        assertThat(natsConnection.getStatus()).isEqualTo(Connection.Status.CONNECTED);
        natsConnection.close();
    }

    @Test
    void propertiesAreAvailable() {
        assertThat(natsPort).isNotEmpty();
        assertThat(natsHost).isNotEmpty();
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }
}
