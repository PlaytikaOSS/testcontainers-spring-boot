package com.playtika.test.pulsar;

import org.apache.pulsar.client.api.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddedPulsarBrokerTest extends AbstractEmbeddedPulsarTest {

    private static final String TEST_MESSAGE = "My message";
    private static final String TOPIC_NAME = "test-topic";
    private static final String SUBSCRIPTION_NAME = "test-subscription";

    @Value("${embedded.pulsar.brokerUrl}")
    private String brokerUrl;

    private PulsarClient client;
    private Consumer<String> consumer;
    private Producer<String> producer;

    @BeforeEach
    public void setUp() throws PulsarClientException {
        client = PulsarClient.builder()
                .serviceUrl(brokerUrl)
                .build();

        consumer = client.newConsumer(Schema.STRING)
                .topic(TOPIC_NAME)
                .subscriptionName(SUBSCRIPTION_NAME)
                .subscriptionType(SubscriptionType.Exclusive)
                .subscribe();

        producer = client.newProducer(Schema.STRING)
                .topic(TOPIC_NAME)
                .create();
    }

    @AfterEach
    public void tearDown() throws PulsarClientException {
        client.close();
        producer.close();
        consumer.close();
    }

    @Test
    void shouldPublishMessage() throws PulsarClientException {
        producer.send(TEST_MESSAGE);
        Message<String> receive = consumer.receive();
        assertThat(receive.getValue()).isEqualTo(TEST_MESSAGE);
    }
}
