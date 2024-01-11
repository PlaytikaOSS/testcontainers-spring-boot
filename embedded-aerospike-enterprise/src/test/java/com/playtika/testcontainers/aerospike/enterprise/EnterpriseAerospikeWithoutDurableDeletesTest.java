package com.playtika.testcontainers.aerospike.enterprise;

import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "embedded.aerospike.enterprise.durableDeletes=false")
public class EnterpriseAerospikeWithoutDurableDeletesTest  extends BaseEnterpriseAerospikeTest {

    @Test
    public void shouldDeleteWithoutDurableDeleteFlag() {
        Key key = new Key(namespace, SET, "key2");
        Bin bin = new Bin("mybin", "myvalue");
        client.put(writePolicyWithoutDurableDelete, key, bin);

        Record actualRecord = client.get(null, key);

        assertThat(actualRecord.bins).hasSize(1);
        assertThat(actualRecord.bins.get("mybin")).isEqualTo("myvalue");

        client.delete(writePolicyWithoutDurableDelete, key);

        actualRecord = client.get(null, key);
        assertThat(actualRecord).isNull();
    }
}
