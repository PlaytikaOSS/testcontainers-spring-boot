package com.playtika.testcontainer.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Language;
import com.aerospike.client.Value;
import com.aerospike.client.policy.WritePolicy;
import com.aerospike.client.query.Statement;
import com.aerospike.client.task.ExecuteTask;
import com.aerospike.client.task.RegisterTask;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class AerospikeExpiredDocumentsCleaner implements ExpiredDocumentsCleaner {

    private static final String PACKAGE_NAME = "remove_expired";
    private static final String FUNC_NAME = "remove_expired";
    private static final String RESOURCE_PATH = "udf/remove_expired.lua";
    private static final String SERVER_PATH = "remove_expired.lua";
    private static final int SLEEP_INTERVAL = 100;
    private static final int TIMEOUT = 10_000;

    private final AerospikeClient client;
    private final String namespace;
    private final boolean durableDelete;

    public AerospikeExpiredDocumentsCleaner(AerospikeClient client, String namespace, boolean durableDelete) {
        Assert.notNull(client, "Aerospike client can not be null");
        Assert.notNull(namespace, "Namespace can not be null");
        this.client = client;
        this.namespace = namespace;
        this.durableDelete = durableDelete;

        registerUdf();
    }

    public AerospikeExpiredDocumentsCleaner(AerospikeClient client, String namespace) {
        this(client, namespace, false);
    }

    private void registerUdf() {
        ClassLoader classLoader = AerospikeExpiredDocumentsCleaner.class.getClassLoader();
        RegisterTask registerTask = client.register(null, classLoader, RESOURCE_PATH, SERVER_PATH, Language.LUA);
        registerTask.waitTillComplete(SLEEP_INTERVAL, TIMEOUT);
    }

    @Override
    public void cleanExpiredDocumentsBefore(Instant expireTime) {
        cleanExpiredDocumentsBefore(expireTime.toEpochMilli());
    }

    @Override
    public void cleanExpiredDocumentsBefore(long expireTimeMillis) {
        long duration = expireTimeMillis - System.currentTimeMillis();
        int expiration = (int) TimeUnit.MILLISECONDS.toSeconds(duration);
        Value value = Value.get(expiration);

        Statement statement = new Statement();
        statement.setNamespace(namespace);
        WritePolicy writePolicy = new WritePolicy(client.getWritePolicyDefault());
        writePolicy.durableDelete = durableDelete;
        ExecuteTask executeTask = client.execute(writePolicy, statement, PACKAGE_NAME, FUNC_NAME, value);
        executeTask.waitTillComplete(SLEEP_INTERVAL, TIMEOUT);
    }
}
