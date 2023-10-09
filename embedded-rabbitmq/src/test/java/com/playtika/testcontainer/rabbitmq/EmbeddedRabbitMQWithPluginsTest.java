package com.playtika.testcontainer.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;


@Slf4j
@SpringBootTest(
        classes = EmbeddedRabbitMQWithPluginsTest.TestConfiguration.class
)
@ActiveProfiles({"enabled", "plugins"})
public class EmbeddedRabbitMQWithPluginsTest {

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }

    @Test
    public void testPluginLoadingWorks() {
        tryCreateConsistentHashExchange(rabbitAdmin);
    }

    // we call this from the other test (which doesn't have the plugin loaded), to verify that fails
    public static void tryCreateConsistentHashExchange(final RabbitAdmin rabbitAdmin) {
        rabbitAdmin.declareExchange(new CustomExchange("ch-exchange", "x-consistent-hash"));
    }
}
