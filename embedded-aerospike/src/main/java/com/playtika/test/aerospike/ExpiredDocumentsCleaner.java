package com.playtika.test.aerospike;

import com.aerospike.client.AerospikeClient;
import org.springframework.util.Assert;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;

public class ExpiredDocumentsCleaner {

    private final AerospikeClient client;
    private final String namespace;

    public ExpiredDocumentsCleaner(AerospikeClient client, String namespace) {
        Assert.notNull(client, "Aerospike client can not be null");
        Assert.notNull(namespace, "Namespace can not be null");
        this.client = client;
        this.namespace = namespace;
    }

    public void cleanExpiredDocumentsBefore(Instant expireTime) {
        cleanExpiredDocumentsBefore(expireTime.toEpochMilli());
    }

    public void cleanExpiredDocumentsBefore(long expireTimeMillis) {
        Calendar beforeLastUpdate = Calendar.getInstance();
        beforeLastUpdate.setTime(new Date(expireTimeMillis));
        client.truncate(null, namespace, null, beforeLastUpdate);
    }
}
