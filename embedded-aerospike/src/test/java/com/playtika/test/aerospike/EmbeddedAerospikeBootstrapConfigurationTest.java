package com.playtika.test.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactoryUtils;


import static com.playtika.test.aerospike.AerospikeProperties.AEROSPIKE_BEAN_NAME;
import static java.time.Duration.ofMillis;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;


public class EmbeddedAerospikeBootstrapConfigurationTest extends BaseAerospikeTest {

    @Test
    public void shouldSave() throws Exception {
        Key key = new Key(namespace, SET, "key1");
        Bin bin = new Bin("mybin", "myvalue");
        client.put(policy, key, bin);

        Record actual = client.get(policy, key);

        assertThat(actual.bins).hasSize(1);
        assertThat(actual.bins.get("mybin")).isEqualTo("myvalue");
    }

    @Test
    public void shouldSetupDependsOnForAerospikeClient() throws Exception {
        String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, AerospikeClient.class);
        assertThat(beanNamesForType)
                .as("AerospikeClient should be present")
                .hasSize(1)
                .contains("aerospikeClient");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    @Test
    public void shouldAddLatency() throws Exception {
        aerospikeTestOperations.addNetworkLatencyForResponses(ofMillis(1000));

        long total = getExecutionTimeOfOperation();

        assertThat(total).isCloseTo(1000L, Offset.offset(20L));

        aerospikeTestOperations.removeNetworkLatencyForResponses();

        total = getExecutionTimeOfOperation();
        assertThat(total).isLessThan(20);
    }

    private long getExecutionTimeOfOperation() {
        Key key = new Key(namespace, SET, "key1");

        long start = System.currentTimeMillis();
        Policy policy = new Policy();
        // we do not need any timeout on this operation, cause we're checking added network latency
        policy.totalTimeout = 0;
        client.get(policy, key);
        return System.currentTimeMillis() - start;
    }

    private void hasDependsOn(String beanName) {
        assertThat(beanFactory.getBeanDefinition(beanName).getDependsOn())
                .isNotNull()
                .isNotEmpty()
                .contains(AEROSPIKE_BEAN_NAME);
    }

}