package com.playtika.test.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.WritePolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@SpringBootTest(
        classes = BaseAerospikeTest.TestConfiguration.class,
        properties = {
                "embedded.aerospike.install.enabled=true",
                "embedded.toxiproxy.proxies.aerospike.enabled=true"
        }
)
public abstract class BaseAerospikeTest {

    protected static final String SET = "values";

    @Value("${embedded.aerospike.namespace}")
    protected String namespace;

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    AerospikeClient client;

    @Autowired
    WritePolicy policy;

    @Autowired
    AerospikeTestOperations aerospikeTestOperations;

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

        @Bean(destroyMethod = "close")
        public AerospikeClient aerospikeToxicClient(@Value("${embedded.aerospike.toxiproxy.host}") String host,
                                                    @Value("${embedded.aerospike.toxiproxy.port}") int port) {
            ClientPolicy clientPolicy = new ClientPolicy();
            clientPolicy.timeout = 10_000;//in millis
            return new AerospikeClient(clientPolicy, host, port);
        }

        @Bean
        public WritePolicy policy() {
            WritePolicy policy = new WritePolicy();
            policy.totalTimeout = 200;//in millis
            return policy;
        }
    }
}
