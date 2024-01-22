package com.playtika.testcontainers.aerospike.enterprise;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.policy.ClientPolicy;
import com.aerospike.client.policy.WritePolicy;
import com.playtika.testcontainer.aerospike.AerospikeTestOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@SpringBootTest(
        classes = BaseEnterpriseAerospikeTest.TestConfiguration.class
)
public abstract class BaseEnterpriseAerospikeTest {

    protected static final String SET = "values";

    @Value("${embedded.aerospike.namespace}")
    protected String namespace;

    @Autowired
    ConfigurableListableBeanFactory beanFactory;

    @Autowired
    IAerospikeClient client;

    @Autowired
    WritePolicy writePolicyWithDurableDelete;

    @Autowired
    WritePolicy writePolicyWithoutDurableDelete;

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

        @Bean
        public WritePolicy writePolicyWithDurableDelete() {
            WritePolicy policy = new WritePolicy();
            policy.durableDelete = true;
            policy.totalTimeout = 200;//in millis
            return policy;
        }

        @Bean
        public WritePolicy writePolicyWithoutDurableDelete() {
            WritePolicy policy = new WritePolicy();
            policy.totalTimeout = 200;//in millis
            return policy;
        }
    }
}
