package com.playtika.testcontainer.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.WritePolicy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = AerospikeExpiredDocumentsCleanerWithDurableDeleteTest.TestConfiguration.class
)
class AerospikeExpiredDocumentsCleanerWithDurableDeleteTest {

    static final String SET_NAME = "some-set";

    @Value("${embedded.aerospike.namespace}")
    String namespace;
    @Autowired
    ExpiredDocumentsCleaner durableDeleteExpiredDocumentsCleaner;
    @Autowired
    AerospikeClient aerospikeClient;

    @Test
    public void shouldNotRemoveExpiredWithDurableDeleteFlagBecauseOfAerospikeCommunityEdition() {
        Key key = new Key(namespace, "set", "shouldRemoveExpired");
        putBin(key, (int) TimeUnit.DAYS.toSeconds(1));

        Instant plus23 = Instant.now().plus(23, ChronoUnit.HOURS);
        durableDeleteExpiredDocumentsCleaner.cleanExpiredDocumentsBefore(plus23);
        assertThat(aerospikeClient.get(null, key)).isNotNull();

        Instant plus25 = Instant.now().plus(25, ChronoUnit.HOURS);
        durableDeleteExpiredDocumentsCleaner.cleanExpiredDocumentsBefore(plus25);
        assertThat(aerospikeClient.get(null, key)).isNotNull();
    }
    private void putBin(Key key, int expiration) {
        Bin bin = new Bin("mybin", "myvalue");

        WritePolicy writePolicy = new WritePolicy(aerospikeClient.getWritePolicyDefault());
        writePolicy.expiration = expiration;
        aerospikeClient.put(writePolicy, key, bin);
    }

    @EnableAutoConfiguration
    @Configuration
    static class TestConfiguration {

        @Primary
        @Bean(destroyMethod = "close")
        public AerospikeClient aerospikeClient(@Value("${embedded.aerospike.host}") String host,
                                               @Value("${embedded.aerospike.port}") int port) {
            ClientPolicy clientPolicy = new ClientPolicy();
            clientPolicy.timeout = 10_000;//in millis
            return new AerospikeClient(clientPolicy, host, port);
        }

        @Bean
        public ExpiredDocumentsCleaner durableDeleteExpiredDocumentsCleaner(AerospikeClient client,
                                                                            AerospikeProperties properties) {
            return new AerospikeExpiredDocumentsCleaner(client, properties.getNamespace(), true);
        }
    }

}
