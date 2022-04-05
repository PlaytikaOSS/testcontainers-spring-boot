package com.playtika.test.kafka;

import eu.rekawek.toxiproxy.model.Toxic;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.ToxiproxyContainer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static com.playtika.test.kafka.properties.KafkaConfigurationProperties.KAFKA_PLAIN_TEXT_TOXI_PROXY_BEAN_NAME;
import static com.playtika.test.kafka.properties.KafkaConfigurationProperties.KAFKA_SASL_TOXI_PROXY_BEAN_NAME;
import static java.util.Collections.singleton;
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
public class ToxiProxyEmbeddedKafkaTest extends EmbeddedKafkaTest {
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
    @Value("${embedded.kafka.saslPlaintext.brokerList}")
    protected String saslPlaintextKafkaBrokerList;
    @Value("${embedded.kafka.saslPlaintext.user}")
    protected String kafkaUser;
    @Value("${embedded.kafka.saslPlaintext.password}")
    protected String kafkaPassword;

    protected KafkaProducer<String, String> kafkaProducer;

    @AfterEach
    void tearDown() {
        removeToxics(kafkaPlainTextProxy);
        removeToxics(kafkaSaslProxy);
        if (kafkaProducer != null) {
            kafkaProducer.close(Duration.ZERO);
            kafkaProducer = null;
        }
        seekToEnd("topic3", getKafkaConsumerConfiguration());
        seekToEnd(SECURE_TOPIC, saslKafkaConsumerConfiguration());
    }

    @SneakyThrows
    private static void removeToxics(ToxiproxyContainer.ContainerProxy proxy) {
        for (Toxic toxic : proxy.toxics().getAll()) {
            toxic.remove();
        }
    }

    @Test
    @DisplayName("allows to emulate latency on send")
    public void shouldEmulateLatencyOnSend() throws Exception {
        kafkaProducer = new KafkaProducer<>(toxiProxyKafkaProducerConfiguration());

        assertThat(durationOf(() -> kafkaProducer.send(new ProducerRecord<>("topic3", "abc0")).get()))
                .isLessThan(500L);

        kafkaPlainTextProxy.toxics()
                .latency("latency", ToxicDirection.DOWNSTREAM, 1_100)
                .setJitter(50);

        assertThat(durationOf(() -> kafkaProducer.send(new ProducerRecord<>("topic3", "abc1")).get()))
                .isGreaterThan(1000L);

        removeToxics(kafkaPlainTextProxy);

        assertThat(durationOf(() -> kafkaProducer.send(new ProducerRecord<>("topic3", "abc2")).get()))
                .isLessThan(500L);

        assertThat(consumeMessages("topic3", toxiProxyKafkaConsumerConfiguration()))
                .containsExactlyInAnyOrder("abc0", "abc1", "abc2");
    }

    @Test
    @DisplayName("allows to emulate disconnect on send")
    public void shouldEmulateDisconnect() throws Exception {
        kafkaProducer = new KafkaProducer<>(toxiProxyKafkaProducerConfiguration());

        assertThat(durationOf(() -> kafkaProducer.send(new ProducerRecord<>("topic3", "abc0")).get()))
                .isLessThan(500L);

        kafkaPlainTextProxy.setConnectionCut(true);

        Future<RecordMetadata> resultFuture =
                kafkaProducer.send(new ProducerRecord<>("topic3", "abc1"));

        Thread.sleep(1000);

        assertThat(resultFuture.isDone()).isFalse();
        assertThat(consumeMessages("topic3")).containsExactly("abc0");

        kafkaPlainTextProxy.setConnectionCut(false);

        resultFuture.get();

        assertThat(consumeMessages("topic3")).containsExactly("abc1");
    }

    @Test
    @DisplayName("toxi proxy does not affect clients connected directly")
    public void toxiProxyDoesNotAffectDirectClient() throws Exception {
        kafkaProducer = new KafkaProducer<>(toxiProxyKafkaProducerConfiguration());

        assertThat(durationOf(() -> kafkaProducer.send(new ProducerRecord<>("topic3", "abc0")).get()))
                .isLessThan(500L);

        kafkaPlainTextProxy.setConnectionCut(true);

        kafkaProducer.send(new ProducerRecord<>("topic3", "abc1"));
        sendMessage("topic3", "abc2");

        assertThat(consumeMessages("topic3")).containsExactlyInAnyOrder("abc0", "abc2");
    }

    @Test
    @DisplayName("allows to emulate latency on send to SASL-Plaintext secure topic")
    public void shouldEmulateLatencyOnSendToSecureTopic() throws Exception {
        kafkaProducer = new KafkaProducer<>(toxiProxySaslKafkaProducerConfiguration());

        assertThat(durationOf(() -> kafkaProducer.send(new ProducerRecord<>(SECURE_TOPIC, "abc0")).get()))
                .isLessThan(500L);

        kafkaSaslProxy.toxics()
                .latency("latency", ToxicDirection.DOWNSTREAM, 1_100)
                .setJitter(50);

        assertThat(durationOf(() -> kafkaProducer.send(new ProducerRecord<>(SECURE_TOPIC, "abc1")).get()))
                .isGreaterThan(1000L);

        removeToxics(kafkaSaslProxy);

        assertThat(durationOf(() -> kafkaProducer.send(new ProducerRecord<>(SECURE_TOPIC, "abc2")).get()))
                .isLessThan(500L);

        assertThat(consumeMessages(SECURE_TOPIC, toxiProxySaslKafkaConsumerConfiguration()))
                .containsExactlyInAnyOrder("abc0", "abc1", "abc2");
    }

    @Test
    @DisplayName("toxi proxy does not affect SASL-Plaintext clients connected directly")
    public void toxiProxyDoesNotAffectDirectSaslClient() throws Exception {
        kafkaProducer = new KafkaProducer<>(toxiProxySaslKafkaProducerConfiguration());

        assertThat(durationOf(() -> kafkaProducer.send(new ProducerRecord<>(SECURE_TOPIC, "abc0")).get()))
                .isLessThan(500L);

        kafkaSaslProxy.setConnectionCut(true);

        kafkaProducer.send(new ProducerRecord<>(SECURE_TOPIC, "abc1"));
        sendMessage(SECURE_TOPIC, "abc2", saslKafkaProducerConfiguration());

        assertThat(consumeMessages(SECURE_TOPIC, saslKafkaConsumerConfiguration()))
                .containsExactlyInAnyOrder("abc0", "abc2");
    }

    private Map<String, Object> toxiProxyKafkaProducerConfiguration() {
        Map<String, Object> conf = super.getKafkaProducerConfiguration();
        conf.put(BOOTSTRAP_SERVERS_CONFIG, toxiproxyBrokerList);
        return conf;
    }

    private Map<String, Object> toxiProxyKafkaConsumerConfiguration() {
        Map<String, Object> conf = super.getKafkaConsumerConfiguration();
        conf.put(BOOTSTRAP_SERVERS_CONFIG, toxiproxyBrokerList);
        return conf;
    }

    private Map<String, Object> toxiProxySaslKafkaProducerConfiguration() {
        Map<String, Object> conf = saslKafkaProducerConfiguration();
        conf.put(BOOTSTRAP_SERVERS_CONFIG, toxiproxySaslBrokerList);
        return conf;
    }

    private Map<String, Object> saslKafkaProducerConfiguration() {
        Map<String, Object> conf = getKafkaProducerConfiguration();
        conf.putAll(createSaslPlaintextKafkaConfigurationProperties());
        return conf;
    }

    private Map<String, Object> toxiProxySaslKafkaConsumerConfiguration() {
        Map<String, Object> conf = saslKafkaConsumerConfiguration();
        conf.put(BOOTSTRAP_SERVERS_CONFIG, toxiproxySaslBrokerList);
        return conf;
    }

    private Map<String, Object> saslKafkaConsumerConfiguration() {
        Map<String, Object> conf = getKafkaConsumerConfiguration();
        conf.putAll(createSaslPlaintextKafkaConfigurationProperties());
        return conf;
    }

    private Map<String, Object> createSaslPlaintextKafkaConfigurationProperties() {
        Map<String, Object> conf = new HashMap<>();
        conf.put(BOOTSTRAP_SERVERS_CONFIG, saslPlaintextKafkaBrokerList);
        conf.put(SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        conf.put(SASL_MECHANISM, "PLAIN");
        conf.put(SASL_JAAS_CONFIG, "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username=\"" + kafkaUser + "\" " +
                "password=\"" + kafkaPassword + "\";"
        );
        return conf;
    }

    private void seekToEnd(String topic, Map<String, Object> conf) {
        try (KafkaConsumer<String, String> consumer = createConsumer(topic, conf)) {
            consumer.subscribe(singleton(topic));
            consumer.poll(Duration.ofMillis(100));
        }
    }
}
