package com.playtika.test.aerospike;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Language;
import com.aerospike.client.Value;
import com.aerospike.client.query.Statement;
import com.aerospike.client.task.ExecuteTask;
import com.aerospike.client.task.RegisterTask;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class ExpiredDocumentsCleaner {

    private static final String PACKAGE_NAME = "remove_expired";
    private static final String FUNC_NAME = "remove_expired";
    private static final String RESOURCE_PATH = "udf/remove_expired.lua";
    private static final String SERVER_PATH = "remove_expired.lua";
    private static final int SLEEP_INTERVAL = 100;
    private static final int TIMEOUT = 10_000;

    private final AerospikeClient client;
    private final String namespace;

    public ExpiredDocumentsCleaner(AerospikeClient client, String namespace) {
        Assert.notNull(client, "Aerospike client can not be null");
        Assert.notNull(namespace, "Namespace can not be null");
        this.client = client;
        this.namespace = namespace;

        registerUdf();
    }

    private void registerUdf() {
        ClassLoader classLoader = ExpiredDocumentsCleaner.class.getClassLoader();
        RegisterTask registerTask = client.register(null, classLoader, RESOURCE_PATH, SERVER_PATH, Language.LUA);
        registerTask.waitTillComplete(SLEEP_INTERVAL, TIMEOUT);
    }

    public void cleanExpiredDocumentsBefore(Instant expireTime) {
        cleanExpiredDocumentsBefore(expireTime.toEpochMilli());
    }

    public void cleanExpiredDocumentsBefore(long expireTimeMillis) {
        long duration = expireTimeMillis - System.currentTimeMillis();
        int expiration = (int) TimeUnit.MILLISECONDS.toSeconds(duration);
        Value value = Value.get(expiration);

        Statement statement = new Statement();
        statement.setNamespace(namespace);

        ExecuteTask executeTask = client.execute(null, statement, PACKAGE_NAME, FUNC_NAME, value);
        executeTask.waitTillComplete(SLEEP_INTERVAL, TIMEOUT);
    }
}
