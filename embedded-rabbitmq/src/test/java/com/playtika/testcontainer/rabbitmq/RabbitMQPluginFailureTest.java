package com.playtika.testcontainer.rabbitmq;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.Container;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RabbitMQPluginFailureTest {

    @Test
    void shouldReportNonZeroPluginCommandExitCodeAndStderr() throws Exception {
        RabbitMQContainer rabbitMQ = mock(RabbitMQContainer.class);
        Container.ExecResult result = mock(Container.ExecResult.class);
        when(result.getExitCode()).thenReturn(70);
        when(result.getStderr()).thenReturn("plugin not found");
        when(rabbitMQ.execInContainer("rabbitmq-plugins", "enable", "missing-plugin"))
                .thenReturn(result);

        assertThatThrownBy(() -> enablePlugin(rabbitMQ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("missing-plugin")
                .hasMessageContaining("exit code: 70")
                .hasMessageContaining("plugin not found");
    }

    @Test
    void shouldPreserveInterruptWhenPluginCommandIsInterrupted() throws Exception {
        RabbitMQContainer rabbitMQ = mock(RabbitMQContainer.class);
        when(rabbitMQ.execInContainer("rabbitmq-plugins", "enable", "missing-plugin"))
                .thenThrow(new InterruptedException("interrupted"));

        try {
            assertThatThrownBy(() -> enablePlugin(rabbitMQ))
                    .isInstanceOf(IllegalStateException.class)
                    .hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    private void enablePlugin(RabbitMQContainer rabbitMQ) {
        RabbitMQProperties properties = new RabbitMQProperties();
        properties.setEnabledPlugins(List.of("missing-plugin"));
        ReflectionTestUtils.invokeMethod(new EmbeddedRabbitMQBootstrapConfiguration(),
                "enablePlugins", rabbitMQ, properties);
    }
}
