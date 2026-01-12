package com.playtika.testcontainer.nativekafka;

import com.playtika.testcontainer.toxiproxy.ToxiproxyClientProxy;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestPropertySource(properties = {
    "embedded.toxiproxy.proxies.kafka.enabled=true",
    "embedded.kafka.topicsToCreate=test-topic"
})
class NativeKafkaWithToxiproxyTest extends AbstractEmbeddedNativeKafkaTest {

    @Value("${embedded.kafka.toxiproxy.bootstrapServers:}")
    String toxiproxyBootstrapServers;

    @Autowired(required = false)
    @Qualifier("nativeKafkaContainerProxy")
    ToxiproxyClientProxy proxy;

    @Test
    void shouldWorkWithToxiproxy() throws ExecutionException, InterruptedException {
        assertThat(toxiproxyBootstrapServers).isNotEmpty();
        assertThat(proxy).isNotNull();

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, toxiproxyBootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ProducerRecord<String, String> record =
                new ProducerRecord<>("test-topic", "test-key", "test-value");
            producer.send(record).get();
        }
    }

    @Test
    void shouldSimulateNetworkLatency() throws IOException {
        // Add 1 second latency
        proxy.toxics()
            .latency("high-latency", ToxicDirection.DOWNSTREAM, 1000);

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, toxiproxyBootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "500");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ProducerRecord<String, String> record =
                new ProducerRecord<>("test-topic", "test-key", "test-value");

            // Should timeout due to latency
            assertThrows(Exception.class, () ->
                producer.send(record).get(600, TimeUnit.MILLISECONDS));
        }
    }

    @Test
    void shouldHandleConnectionCut() throws IOException {
        // Cut the connection
        proxy.setConnectionCut(true);

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, toxiproxyBootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "1000");
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, "1000");

        try (KafkaProducer<String, String> producer = new KafkaProducer<>(props)) {
            ProducerRecord<String, String> record =
                new ProducerRecord<>("test-topic", "test-key", "test-value");

            // Should fail due to connection cut
            assertThrows(Exception.class, () ->
                producer.send(record).get());
        }

        // Restore connection
        proxy.setConnectionCut(false);
    }
}
