/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 Playtika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.playtika.test.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;
import org.assertj.core.data.Offset;
import org.junit.Test;

import static com.playtika.test.aerospike.AerospikeProperties.AEROSPIKE_BEAN_NAME;
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
        String[] beanNamesForType = beanFactory.getBeanNamesForType(AerospikeClient.class);
        assertThat(beanNamesForType)
                .as("AerospikeClient should be present")
                .hasSize(1)
                .contains("aerospikeClient");
        asList(beanNamesForType).forEach(this::hasDependsOn);
    }

    @Test
    public void shouldAddLatency() throws Exception {
        aerospikeTestOperations.addNetworkLatencyForResponses(1500);

        long total = getExecutionTimeOfOperation();

        assertThat(total).isCloseTo(1500L, Offset.offset(20L));

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