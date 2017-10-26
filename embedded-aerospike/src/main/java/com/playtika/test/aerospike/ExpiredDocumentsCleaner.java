/*
* The MIT License (MIT)
*
* Copyright (c) 2017 Playtika
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
