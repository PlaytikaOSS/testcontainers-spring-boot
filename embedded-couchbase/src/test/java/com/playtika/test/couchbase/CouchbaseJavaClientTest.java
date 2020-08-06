/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Playtika
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
package com.playtika.test.couchbase;

import com.couchbase.client.core.env.SeedNode;
import com.couchbase.client.core.env.TimeoutConfig;
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
                .timeoutConfig(TimeoutConfig.kvTimeout(Duration.ofSeconds(20)))
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
