package com.playtika.test.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.AerospikeException;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.Policy;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.testcontainers.containers.ToxiproxyContainer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ToxiProxyAerospikeTest extends BaseAerospikeTest {

    @Autowired
    ToxiproxyContainer.ContainerProxy aerospikeContainerProxy;

    @Qualifier("aerospikeToxicClient")
    @Autowired
    AerospikeClient aerospikeToxicClient;

    @Test
    void addsLatency() throws Exception {
        Policy policy = new Policy();
        policy.setTimeout(200);
        Key key = new Key(namespace, "any", "any");
        Record record = aerospikeToxicClient.get(policy, key);

        aerospikeContainerProxy.toxics()
                .latency("latency", ToxicDirection.DOWNSTREAM, 1_100)
                .setJitter(100);

        assertThatThrownBy(() -> aerospikeToxicClient.get(policy, key))
                .isInstanceOf(AerospikeException.Timeout.class);

        aerospikeContainerProxy.toxics()
                .get("latency").remove();

        record = aerospikeToxicClient.get(policy, key);

    }
}
