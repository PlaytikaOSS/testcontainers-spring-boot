package com.playtika.test.aerospike;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.policy.Policy;
import com.playtika.test.common.operations.NetworkTestOperations;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NetworkTestOperationsTest extends BaseAerospikeTest {

    @Autowired
    NetworkTestOperations networkTestOperations;

    @Test
    void validateNetworkLatencyIsApplied() {
        networkTestOperations.withNetworkLatency(Duration.ofSeconds(5), () -> {
            Policy policy = new Policy(client.getReadPolicyDefault());
            policy.setTimeout(100);//in millis

            assertThatThrownBy(() -> client.get(policy, new Key(namespace, "any-set", "any-key")))
                    .isInstanceOf(AerospikeException.Timeout.class);
        });
    }
}
