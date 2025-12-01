package com.playtika.testcontainer.temporal;

import io.temporal.api.history.v1.HistoryEvent;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.Container;

import java.util.List;

import static io.temporal.api.enums.v1.EventType.EVENT_TYPE_WORKFLOW_EXECUTION_STARTED;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedTemporalBootstrapConfigurationTest {

    @SpringBootTest(
            webEnvironment = WebEnvironment.NONE,
            classes = EmbeddedTemporalBootstrapConfigurationTest.TestConfiguration.class
    )
    @Nested
    class Headless {

        @Test
        void workflowExecutionStarts(@Autowired WorkflowClient client) {
            String workflowId = startWorkflow(client);

            assertThat(client.streamHistory(workflowId.toString()))
                    .extracting(HistoryEvent::getEventType)
                    .contains(EVENT_TYPE_WORKFLOW_EXECUTION_STARTED);
        }

        @Test
        void uiDisabled(@Autowired Environment environment) {
            assertThat(environment.getProperty("embedded.temporal.uiPort")).isNull();
        }
    }

    @SpringBootTest(
            webEnvironment = WebEnvironment.NONE,
            classes = EmbeddedTemporalBootstrapConfigurationTest.TestConfiguration.class,
            properties = "embedded.temporal.ui-enabled=true"
    )
    @Nested
    class Headed {

        @Test
        void workflowExecutionStarts(
                @Autowired WorkflowClient serverClient,
                @Autowired WebTestClient uiClient) {

            String workflowId = startWorkflow(serverClient);

            uiClient.get().uri("/api/v1/namespaces/default/workflows/{workflowId}/history", workflowId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.history.events[*].eventType")
                    .value(events -> {
                        List<String> eventTypes = (List<String>) events;
                        assertThat(eventTypes).contains(EVENT_TYPE_WORKFLOW_EXECUTION_STARTED.toString());
                    });
        }
    }

    @SpringBootTest(
            webEnvironment = WebEnvironment.NONE,
            classes = EmbeddedTemporalBootstrapConfigurationTest.TestConfiguration.class,
            properties = "embedded.temporal.enabled=false"
    )
    @Nested
    class Disabled {

        @Test
        void contextLoads(@Autowired ConfigurableApplicationContext context) {
            assertThat(AssertableApplicationContext.get(() -> context)).hasNotFailed().doesNotHaveBean(Container.class);
        }
    }

    @EnableAutoConfiguration
    @ConditionalOnProperty(name = "embedded.temporal.enabled", havingValue = "true", matchIfMissing = true)
    @Configuration
    static class TestConfiguration {

        @Bean
        WorkflowClient workflowClient(@Value("${embedded.temporal.host}") String host,
                                      @Value("${embedded.temporal.port}") Integer port) {

            WorkflowServiceStubsOptions options = WorkflowServiceStubsOptions.newBuilder()
                    .setTarget("%s:%s".formatted(host, port))
                    .build();

            WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(options);

            return WorkflowClient.newInstance(service);
        }

        @Bean
        @ConditionalOnProperty(name = "embedded.temporal.ui-enabled", havingValue = "true")
        WebTestClient uiClient(@Value("${embedded.temporal.host}") String host,
                               @Value("${embedded.temporal.uiPort}") Integer uiPort) {

            return WebTestClient.bindToServer()
                    .baseUrl("http://%s:%s".formatted(host, uiPort))
                    .build();
        }
    }

    String startWorkflow(WorkflowClient client) {
        Workflow workflow = client.newWorkflowStub(Workflow.class, WorkflowOptions.newBuilder()
                .setTaskQueue("task-queue")
                .setWorkflowId(randomUUID().toString())
                .build());

        return WorkflowClient.start(workflow::execute).getWorkflowId();
    }

    @WorkflowInterface
    public interface Workflow {

        @WorkflowMethod
        void execute();
    }

    public static class TestWorkflow implements Workflow {

        @Override
        public void execute() {
        }
    }
}
