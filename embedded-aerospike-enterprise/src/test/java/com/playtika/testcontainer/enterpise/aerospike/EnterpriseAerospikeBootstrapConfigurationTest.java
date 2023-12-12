package com.playtika.testcontainer.enterpise.aerospike;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.ResultCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class EnterpriseAerospikeBootstrapConfigurationTest extends BaseEnterpriseAerospikeTest {

    @Test
    public void shouldDeleteWithDurableDeleteFlag() {
        Key key = new Key(namespace, SET, "key1");
        Bin bin = new Bin("mybin", "myvalue");
        client.put(writePolicyWithDurableDelete, key, bin);

        Record actualRecord = client.get(writePolicyWithDurableDelete, key);

        assertThat(actualRecord.bins).hasSize(1);
        assertThat(actualRecord.bins.get("mybin")).isEqualTo("myvalue");

        client.delete(writePolicyWithDurableDelete, key);

        actualRecord = client.get(writePolicyWithDurableDelete, key);
        assertThat(actualRecord).isNull();
    }

    @Test
    public void shouldNotDeleteWithoutDurableDeleteFlag() {
        Key key = new Key(namespace, SET, "key2");
        Bin bin = new Bin("mybin", "myvalue");
        client.put(writePolicyWithDurableDelete, key, bin);

        Record actualRecord = client.get(writePolicyWithoutDurableDelete, key);

        assertThat(actualRecord.bins).hasSize(1);
        assertThat(actualRecord.bins.get("mybin")).isEqualTo("myvalue");

        assertThatExceptionOfType(AerospikeException.class)
                .isThrownBy(() -> client.delete(writePolicyWithoutDurableDelete, key))
                .extracting(AerospikeException::getResultCode)
                .isEqualTo(ResultCode.FAIL_FORBIDDEN);
    }
}
