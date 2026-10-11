package com.playtika.testcontainer.rabbitmq;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;

import static com.playtika.testcontainer.rabbitmq.RabbitMQProperties.BEAN_NAME_EMBEDDED_RABBITMQ;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mirrors the key assertions of {@link EmbeddedRabbitMQBootstrapConfigurationTest}, but with
 * {@code embedded.containers.parallelStartup} enabled, to prove the deferred/scheduled startup
 * path (including plugin enabling and environment registration) behaves identically to the
 * default sequential one.
 */
@Slf4j
@SpringBootTest(
        classes = EmbeddedRabbitMQParallelStartupTest.TestConfiguration.class,
        properties = "embedded.containers.parallelStartup=true"
)
@ActiveProfiles("enabled")
class EmbeddedRabbitMQParallelStartupTest {

    private static final String QUEUE_NAME = "PARALLEL_STARTUP_QUEUE";

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }

    @Test
    void testRabbitTemplate() {
        rabbitAdmin.declareQueue(new Queue(QUEUE_NAME));

        String expectedMessageBody = "Hello RabbitMQ!";
        rabbitTemplate.convertAndSend(QUEUE_NAME, expectedMessageBody);

        Message message = rabbitTemplate.receive(QUEUE_NAME);

        assertThat(expectedMessageBody).isEqualTo(new String(message.getBody()));
    }

    @Test
    void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.rabbitmq.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.rabbitmq.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.rabbitmq.vhost")).isNotEmpty();
        assertThat(environment.getProperty("embedded.rabbitmq.user")).isNotEmpty();
        assertThat(environment.getProperty("embedded.rabbitmq.password")).isNotEmpty();
        assertThat(environment.getProperty("embedded.rabbitmq.httpPort")).isNotEmpty();
    }

    @Test
    void shouldSetupDependsOnForRabbitTemplate() {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, RabbitTemplate.class);
        assertThat(beanNamesForType)
                .hasSize(1)
                .contains("rabbitTemplate");
        asList(beanNamesForType).forEach(beanName ->
                assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                        .isNotNull()
                        .isNotEmpty()
                        .contains(BEAN_NAME_EMBEDDED_RABBITMQ));
    }
}
