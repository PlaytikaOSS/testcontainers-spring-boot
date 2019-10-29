package com.playtika.test.pubsub;

import com.google.pubsub.v1.ProjectSubscriptionName;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static com.playtika.test.pubsub.PubsubProperties.BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = EmbeddedPubsubBootstrapConfigurationTest.TestConfiguration.class)
@ActiveProfiles("enabled")
public class EmbeddedPubsubBootstrapConfigurationTest {

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
    public void propertiesAreAvailable() {
        assertThat(environment.getProperty("embedded.google.pubsub.host")).isNotEmpty();
        assertThat(environment.getProperty("embedded.google.pubsub.port")).isNotEmpty();
        assertThat(environment.getProperty("embedded.google.pubsub.project-id")).isNotEmpty();

        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[0].topic")).isEqualTo("topic0");
        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[0].subscription")).isEqualTo("subscription0");

        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[1].topic")).isEqualTo("topic1");
        assertThat(environment.getProperty("embedded.google.pubsub.topics-and-subscriptions[1].subscription")).isEqualTo("subscription1");
    }

    @Test
    public void topicsAndSubscriptionsAreAvailable() {
        ProjectSubscriptionName subscription0 = ProjectSubscriptionName.of(projectId, "subscription0");
        ProjectSubscriptionName subscription1 = ProjectSubscriptionName.of(projectId, "subscription1");

        assertThat(resourcesGenerator.getSubscription(subscription0)).isNotNull();
        assertThat(resourcesGenerator.getSubscription(subscription1)).isNotNull();
    }

    @Test
    public void shouldPublishAndConsumeMessage() {
        template.publish("topic0", "hi");
        List<AcknowledgeablePubsubMessage> messages = template.pull("subscription0", 1, false);

        assertThat(messages.size()).isEqualTo(1);
    }

    @Test
    public void shouldSetupDependsOnForPubSubTemplate() {
        String[] beanNamesForType = beanFactory.getBeanNamesForType(PubSubTemplate.class);
        assertThat(beanNamesForType)
                .as("pubSubTemplate should be present")
                .hasSize(1)
                .contains("pubSubTemplate");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(BEAN_NAME_EMBEDDED_GOOGLE_PUBSUB);
    }
}