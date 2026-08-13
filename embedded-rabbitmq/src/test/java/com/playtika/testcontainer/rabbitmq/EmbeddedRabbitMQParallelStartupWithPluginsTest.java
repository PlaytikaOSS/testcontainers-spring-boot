package com.playtika.testcontainer.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

/**
 * Mirrors {@link EmbeddedRabbitMQWithPluginsTest}, but with {@code embedded.containers.parallelStartup}
 * enabled, to prove the plugin-enabling step (moved into the scheduled startup task) still runs
 * before the environment is registered and the context is usable.
 */
@Slf4j
@SpringBootTest(
        classes = EmbeddedRabbitMQParallelStartupWithPluginsTest.TestConfiguration.class,
        properties = "embedded.containers.parallelStartup=true"
)
@ActiveProfiles({"enabled", "plugins"})
class EmbeddedRabbitMQParallelStartupWithPluginsTest {

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }

    @Test
    void testPluginLoadingWorks() {
        EmbeddedRabbitMQWithPluginsTest.tryCreateConsistentHashExchange(rabbitAdmin);
    }
}
