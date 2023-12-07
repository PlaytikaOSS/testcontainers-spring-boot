package com.playtika.testcontainer.couchbase;

import com.couchbase.client.core.env.SeedNode;
import com.couchbase.client.core.retry.BestEffortRetryStrategy;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.env.ClusterEnvironment;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.query.QueryResult;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class CouchbaseJavaClientTest extends EmbeddedCouchbaseBootstrapConfigurationTest {

    @Value("${embedded.couchbase.bucket}")
    String bucketName;

    @Value("${embedded.couchbase.user}")
    String user;

    @Value("${embedded.couchbase.password}")
    String password;

    @Value("${embedded.couchbase.host}")
    String host;

    @Value("${embedded.couchbase.bootstrapHttpDirectPort}")
    int mappedHttpPort;

    @Value("${embedded.couchbase.bootstrapCarrierDirectPort}")
    int mappedCarrierPort;

    @Test
    void plainJavaClientShouldWork() {
        Set<SeedNode> seedNodes = new HashSet<>(Collections.singletonList(
                SeedNode.create(host,
                        Optional.of(mappedCarrierPort),
                        Optional.of(mappedHttpPort))));

        ClusterEnvironment env = ClusterEnvironment.builder()
                .retryStrategy(BestEffortRetryStrategy.withExponentialBackoff(Duration.ofMillis(50), Duration.ofMillis(500), 2))
                .maxNumRequestsInRetry(5)
                .timeoutConfig(timeout -> timeout.kvTimeout(Duration.ofSeconds(20)))
                .build();

        ClusterOptions options = ClusterOptions
                .clusterOptions(user, password)
                .environment(env);

        Cluster cluster = Cluster.connect(seedNodes, options);

        Bucket bucket = cluster.bucket(bucketName);
        Collection collection = bucket.defaultCollection();

        MutationResult upsertResult = collection.upsert(
                "my-document",
                JsonObject.create().put("name", "mike")
        );

        assertThat(upsertResult).isNotNull();

        GetResult getResult = collection.get("my-document");

        assertThat(getResult).isNotNull();
        assertThat(getResult.contentAsObject()).isNotNull();
        assertThat(getResult.contentAsObject().get("name")).isEqualTo("mike");

        QueryResult result = cluster.query("select \"Hello World\" as greeting");

        assertThat(result).isNotNull();
        assertThat(result.rowsAsObject()).isNotNull();
    }
}
