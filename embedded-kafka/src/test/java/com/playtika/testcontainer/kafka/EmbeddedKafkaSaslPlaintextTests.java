package com.playtika.testcontainer.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.errors.SaslAuthenticationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Order(3)
@DisplayName("Test that embedded-kafka supports SASL_PLAINTEXT")
public class EmbeddedKafkaSaslPlaintextTests extends AbstractEmbeddedKafkaTest {

    private static final String SECURE_TOPIC = "secureTopic";
    private static final String SECURE_MESSAGE = "test secure message";

    @Value("${embedded.kafka.saslPlaintext.brokerList}")
    String saslPlaintextKafkaBrokerList;
    @Value("${embedded.kafka.saslPlaintext.user}")
    String saslPlaintextUser;
    @Value("${embedded.kafka.saslPlaintext.password}")
    String saslPlaintextUserPassword;

    @Test
    @DisplayName("allows to use SASL_PLAINTEXT for producer and consumer")
    public void shouldSendAndConsumeMessage() throws Exception {
        sendMessage(SECURE_TOPIC, SECURE_MESSAGE, getSaslPlaintextKafkaProducerConfiguration(
                saslPlaintextUser, saslPlaintextUserPassword
        ));

        String consumedMessage = consumeMessage(SECURE_TOPIC, getSaslPlaintextKafkaConsumerConfiguration(
                saslPlaintextUser, saslPlaintextUserPassword
        ));

        assertThat(consumedMessage).isEqualTo(SECURE_MESSAGE);
    }

    @Test
    @DisplayName("SASL_PLAINTEXT connection fails for invalid authentication on producer")
    public void shouldFailToSendMessageToSecureTopicWithInvalidCredentials() {
        assertThatThrownBy(() -> sendMessage(SECURE_TOPIC, SECURE_MESSAGE, getSaslPlaintextKafkaProducerConfiguration("unknownUser", "unknownPassword")))
                .hasCauseExactlyInstanceOf(SaslAuthenticationException.class);
    }

    @Test
    @DisplayName("SASL_PLAINTEXT connection fails for invalid authentication on consumer")
    public void shouldFailToConsumeMessageFromSecureTopicWithInvalidCredentials() {
        assertThatThrownBy(() -> consumeMessage(SECURE_TOPIC, getSaslPlaintextKafkaConsumerConfiguration("unknownUser", "unknownPassword")))
                .isExactlyInstanceOf(SaslAuthenticationException.class);
    }

    private Map<String, Object> getSaslPlaintextKafkaProducerConfiguration(String user, String password) {
        Map<String, Object> configs = getKafkaProducerConfiguration();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, saslPlaintextKafkaBrokerList);
        configs.put(SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        configs.put(SASL_MECHANISM, "PLAIN");
        configs.put(SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username=\"" + user + "\" " +
                "password=\"" + password + "\";"
        );
        return configs;
    }

    private Map<String, Object> getSaslPlaintextKafkaConsumerConfiguration(String user, String password) {
        Map<String, Object> configs = getKafkaConsumerConfiguration();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, saslPlaintextKafkaBrokerList);
        configs.put(SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        configs.put(SASL_MECHANISM, "PLAIN");
        configs.put(SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username=\"" + user + "\" " +
                "password=\"" + password + "\";"
        );
        return configs;
    }
}