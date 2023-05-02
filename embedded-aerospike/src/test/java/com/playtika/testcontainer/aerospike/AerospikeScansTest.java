package com.playtika.testcontainer.aerospike;

import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.query.Statement;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AerospikeScansTest extends BaseAerospikeTest {

    @Test
    void shouldDetectScans() {
        int initialScansCount = aerospikeTestOperations.getScans().size();

        queryWithoutFilter();

        assertThrows(AssertionError.class, () -> aerospikeTestOperations.assertNoScans());
        int scansCount = aerospikeTestOperations.getScans().size();

        assertThat(scansCount - initialScansCount).isEqualTo(1);
    }

    private void queryWithoutFilter() {
        QueryPolicy policy = new QueryPolicy();
        Statement statement = new Statement();
        statement.setNamespace(namespace);
        client.query(policy, statement);
    }
}