package com.playtika.test.kafka;

import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.ToxiproxyContainer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.playtika.test.kafka.properties.KafkaConfigurationProperties.KAFKA_PLAIN_TEXT_TOXI_PROXY_BEAN_NAME;
import static com.playtika.test.kafka.properties.KafkaConfigurationProperties.KAFKA_SASL_TOXI_PROXY_BEAN_NAME;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_JAAS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.SASL_MECHANISM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@Order(7)
@TestPropertySource(properties = {
        "embedded.toxiproxy.proxies.kafka.enabled=true"
})
@TestInstance(PER_CLASS)
@DisplayName("ToxiProxy embedded-kafka test")
public class ToxiProxyEmbeddedKafkaTest extends AbstractEmbeddedKafkaTest {
    private static final String SECURE_TOPIC = "secureTopic";

    @Autowired
    @Qualifier(KAFKA_PLAIN_TEXT_TOXI_PROXY_BEAN_NAME)
    protected ToxiproxyContainer.ContainerProxy kafkaPlainTextProxy;
    @Autowired
    @Qualifier(KAFKA_SASL_TOXI_PROXY_BEAN_NAME)
    protected ToxiproxyContainer.ContainerProxy kafkaSaslProxy;

    @Value("${embedded.kafka.toxiproxy.brokerList}")
    protected List<String> toxiproxyBrokerList;
    @Value("${embedded.kafka.toxiproxy.saslPlaintext.brokerList}")
    protected List<String> toxiproxySaslBrokerList;
    @Value("${embedded.kafka.saslPlaintext.user}")
    protected String kafkaUser;
    @Value("${embedded.kafka.saslPlaintext.password}")
    protected String kafkaPassword;

    @Override
    protected Map<String, Object> getKafkaProducerConfiguration() {
        Map<String, Object> conf = super.getKafkaProducerConfiguration();
        conf.put(BOOTSTRAP_SERVERS_CONFIG, toxiproxyBrokerList);
        return conf;
    }

    @Test
    @DisplayName("allows to emulate latency on send")
    public void shouldEmulateLatencyOnSend() throws Exception {
        kafkaPlainTextProxy.toxics()
                .latency("latency", ToxicDirection.DOWNSTREAM, 1_100)
                .setJitter(50);

        try {
            assertThat(durationOf(() -> sendMessage("topic3", "abc0")))
                    .isGreaterThan(1000L);
        } finally {
            kafkaPlainTextProxy.toxics().get("latency").remove();
        }

        assertThat(durationOf(() -> sendMessage("topic3", "abc1")))
                .isLessThan(200L);

        assertThat(consumeMessages("topic3"))
                .containsExactly("abc0", "abc1");
    }

    @Test
    @DisplayName("allows to emulate latency on send to SASL-Plaintext secure topic")
    public void shouldEmulateLatencyOnSendToSecureTopic() throws Exception {
        kafkaSaslProxy.toxics()
                .latency("latency", ToxicDirection.DOWNSTREAM, 1_100)
                .setJitter(50);

        try {
            assertThat(durationOf(() -> sendMessage(SECURE_TOPIC, "abc0", saslKafkaProducerConfiguration())))
                    .isGreaterThan(1000L);
        } finally {
            kafkaSaslProxy.toxics().get("latency").remove();
        }

        assertThat(durationOf(() -> sendMessage(SECURE_TOPIC, "abc1", saslKafkaProducerConfiguration())))
                .isLessThan(200L);

        assertThat(consumeMessages(SECURE_TOPIC, saslKafkaConsumerConfiguration()))
                .containsExactly("abc0", "abc1");
    }

    protected Map<String, Object> saslKafkaProducerConfiguration() {
        Map<String, Object> conf = getKafkaProducerConfiguration();
        conf.putAll(createSaslPlaintextKafkaConfigurationProperties());
        return conf;
    }

    protected Map<String, Object> saslKafkaConsumerConfiguration() {
        Map<String, Object> conf = getKafkaConsumerConfiguration();
        conf.putAll(createSaslPlaintextKafkaConfigurationProperties());
        return conf;
    }

    private Map<String, Object> createSaslPlaintextKafkaConfigurationProperties() {
        Map<String, Object> conf = new HashMap<>();
        conf.put(BOOTSTRAP_SERVERS_CONFIG, toxiproxySaslBrokerList);
        conf.put(SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        conf.put(SASL_MECHANISM, "PLAIN");
        conf.put(SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username=\"" + kafkaUser + "\" " +
                "password=\"" + kafkaPassword + "\";"
        );
        return conf;
    }
}
