package com.playtika.test.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.GetResult;
import com.couchbase.client.java.kv.MutationResult;
import com.couchbase.client.java.query.QueryResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

public class CouchbaseJavaClientTest extends EmbeddedCouchbaseBootstrapConfigurationTest{

    @Value("${embedded.couchbase.bucket}")
    String bucketName;

    @Value("${embedded.couchbase.user}")
    String user;

    @Value("${embedded.couchbase.password}")
    String password;

    @Value("${embedded.couchbase.host}")
    String host;

    @Test
    void plainJavaClientShouldWork() {
        Cluster cluster = Cluster.connect(host, user, password);
        Bucket bucket = cluster.bucket(bucketName);
        Collection collection = bucket.defaultCollection();

        MutationResult upsertResult = collection.upsert(
                "my-document",
                JsonObject.create().put("name", "mike")
        );
        GetResult getResult = collection.get("my-document");
        System.out.println(getResult);

        QueryResult result = cluster.query("select \"Hello World\" as greeting");

        System.out.println(result.rowsAsObject());
    }
}
