package com.playtika.testcontainer.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.support.AcknowledgeablePubsubMessage;
import com.google.pubsub.v1.DeadLetterPolicy;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.TopicName;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.gcloud.PubSubEmulatorContainer;

import java.util.List;
import java.util.Map;

import static com.playtika.testcontainer.pubsub.PubsubProperties.BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(classes = EmbeddedPubsubBootstrapConfigurationTest.TestConfiguration.class)
@ActiveProfiles("enabled")
class EmbeddedPubsubBootstrapConfigurationTest {

    @Autowired
    private ConfigurableListableBeanFactory beanFactory;

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private PubSubTemplate template;

    @Autowired
    private PubSubResourcesGenerator resourcesGenerator;

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {
    }

    @Test
    void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.google.pubsub.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.google.pubsub.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.google.pubsub.project-id")).isNotEmpty();

        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[0].topic")).isEqualTo("topic0");
        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[0].subscription")).isEqualTo("subscription0");

        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[1].topic")).isEqualTo("topic1");
        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[1].subscription")).isEqualTo("subscription1");
        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[1].enable-message-ordering")).isEqualTo("true");
        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[1].dead-letter.topic")).isEqualTo("topic0");
        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[1].dead-letter.max-attempts")).isEqualTo("10");

        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[2].topic")).isEqualTo("topic2");
        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[2].subscription")).isEqualTo("subscription2-unfiltered");

        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[3].topic")).isEqualTo("topic2");
        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[3].subscription")).isEqualTo("subscription2-filtered");
        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[3].filter")).isEqualTo("attributes.eventType = \"CREATED\"");
    }

    @Test
    void topicsAndSubscriptionsAreAvailable() {
        ProjectSubscriptionName subscription0 = ProjectSubscriptionName.of(projectId, "subscription0");
        ProjectSubscriptionName subscription1 = ProjectSubscriptionName.of(projectId, "subscription1");

        assertThat(resourcesGenerator.getSubscription(subscription0)).isNotNull();
        assertThat(resourcesGenerator.getSubscription(subscription1)).isNotNull();
    }

    @Test
    void shouldHaveDeadLetterPolicySetUp() {
        ProjectSubscriptionName subscription1 = ProjectSubscriptionName.of(projectId, "subscription1");
        TopicName dlqTopic = TopicName.of(projectId, "topic0");
        Subscription subscription = resourcesGenerator.getSubscription(subscription1);
        assertThat(subscription.getDeadLetterPolicy())
                .isNotNull()
                .returns(dlqTopic.toString(), DeadLetterPolicy::getDeadLetterTopic)
                .returns(10, DeadLetterPolicy::getMaxDeliveryAttempts);
    }

    @Test
    void shouldNotHaveMessageOrderingEnabled() {
        ProjectSubscriptionName subscription0 = ProjectSubscriptionName.of(projectId, "subscription0");
        Subscription subscription = resourcesGenerator.getSubscription(subscription0);
        assertThat(subscription.getEnableMessageOrdering()).isFalse();
    }

    @Test
    void shouldHaveMessageOrderingEnabled() {
        ProjectSubscriptionName subscription1 = ProjectSubscriptionName.of(projectId, "subscription1");
        Subscription subscription = resourcesGenerator.getSubscription(subscription1);
        assertThat(subscription.getEnableMessageOrdering()).isTrue();
    }

    @Test
    void shouldPublishAndConsumeMessage() {
        template.publish("topic0", "hi");
        List<AcknowledgeablePubsubMessage> messages = template.pull("subscription0", 1, false);

        assertThat(messages.size()).isEqualTo(1);
    }

    @Test
    void shouldNotHaveFilterConfigured() {
        ProjectSubscriptionName subscription2Unfiltered = ProjectSubscriptionName.of(projectId, "subscription2-unfiltered");
        Subscription subscription = resourcesGenerator.getSubscription(subscription2Unfiltered);
        assertThat(subscription.getFilter()).isEmpty();
    }

    @Test
    void shouldHaveFilterConfigured() {
        ProjectSubscriptionName subscription2Filtered = ProjectSubscriptionName.of(projectId, "subscription2-filtered");
        Subscription subscription = resourcesGenerator.getSubscription(subscription2Filtered);
        assertThat(subscription.getFilter()).isEqualTo("attributes.eventType = \"CREATED\"");
    }

    @Test
    void shouldOnlyConsumeMessagesMatchingFilter() {
        template.publish("topic2", "non-matching", Map.of("eventType", "PING"));
        template.publish("topic2", "matching", Map.of("eventType", "CREATED"));

        List<AcknowledgeablePubsubMessage> filteredMessages = template.pull("subscription2-filtered", 10, false);

        assertThat(filteredMessages)
                .extracting(message -> message.getPubsubMessage().getData().toStringUtf8())
                .containsExactly("matching");

        List<AcknowledgeablePubsubMessage> unfilteredMessages = template.pull("subscription2-unfiltered", 10, false);

        assertThat(unfilteredMessages)
                .extracting(message -> message.getPubsubMessage().getData().toStringUtf8())
                .containsExactlyInAnyOrder("non-matching", "matching");
    }

    @Test
    void shouldSetupDependsOnForPubSubTemplate() {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, PubSubTemplate.class);
        assertThat(beanNamesForType)
                .as("pubSubTemplate should be present")
                .hasSize(1)
                .contains("pubSubTemplate");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    @Test
    void shouldHaveContainerWithExpectedDefaultProperties() {
        assertThat(beanFactory.getBean(BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB))
                .isNotNull()
                .isInstanceOf(PubSubEmulatorContainer.class)
                .satisfies(bean -> {
                    PubSubEmulatorContainer container = (PubSubEmulatorContainer) bean;
                    assertThat(container.getExposedPorts()).containsExactly(8085);
                    assertThat(container.getCommandParts())
                            .containsExactly("/bin/sh", "-c", "gcloud beta emulators pubsub start --host-port 0.0.0.0:8085");
                });
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB);
    }
}
