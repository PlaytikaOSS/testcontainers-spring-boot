/*
* The MIT License (MIT)
*
* Copyright (c) 2018 Playtika
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

import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.policy.WritePolicy;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.Duration.standardDays;
import static org.joda.time.Duration.standardHours;

public class AerospikeTimeTravelTest extends EmbeddedAerospikeBootstrapConfigurationTest {

    @Autowired
    private ExpiredDocumentsCleaner expiredDocumentsCleaner;

    @Autowired
    private AerospikeTestOperations testOperations;

    @After
    public void tearDown() {
        testOperations.rollbackTime();
    }

    @Test
    public void shouldRemoveExpired() throws Exception {
        Key key = new Key(namespace, SET, "shouldRemoveExpired");
        putBin(key, (int) TimeUnit.DAYS.toSeconds(1));

        Instant plus23 = Instant.now().plus(23, ChronoUnit.HOURS);
        expiredDocumentsCleaner.cleanExpiredDocumentsBefore(plus23);
        assertThat(client.get(null, key)).isNotNull();

        Instant plus25 = Instant.now().plus(25, ChronoUnit.HOURS);
        expiredDocumentsCleaner.cleanExpiredDocumentsBefore(plus25);
        assertThat(client.get(null, key)).isNull();
    }

    @Test
    public void shouldNotRemoveNonExpiringDocuments() throws Exception {
        Key key = new Key(namespace, SET, "shouldRemoveExpired");
        putBin(key, 0);

        Instant twoDays = Instant.now().plus(2, ChronoUnit.DAYS);
        expiredDocumentsCleaner.cleanExpiredDocumentsBefore(twoDays);

        assertThat(client.get(null, key)).isNotNull();
    }

    @Test
    public void shouldAddHours() {
        Key key = new Key(namespace, SET, "shouldTimeTravel");
        putBin(key, (int) TimeUnit.HOURS.toSeconds(25));

        testOperations.addDuration(standardHours(24));
        assertThat(client.get(null, key)).isNotNull();

        testOperations.addDuration(standardHours(2));
        assertThat(client.get(null, key)).isNull();
    }

    @Test
    public void shouldAddDays() {
        Key key = new Key(namespace, SET, "shouldAddDays");
        putBin(key, (int) TimeUnit.HOURS.toSeconds(25));

        testOperations.addDuration(standardDays(1));
        assertThat(client.get(null, key)).isNotNull();

        testOperations.addDuration(standardDays(1));
        assertThat(client.get(null, key)).isNull();
    }

    @Test
    public void shouldSetFutureTime() {
        Key key = new Key(namespace, SET, "shouldSetFutureTime");
        putBin(key, (int) TimeUnit.HOURS.toSeconds(25));

        testOperations.timeTravelTo(DateTime.now().plusHours(24));
        assertThat(client.get(null, key)).isNotNull();

        testOperations.timeTravelTo(DateTime.now().plusHours(2));
        assertThat(client.get(null, key)).isNull();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotSetFutureTime() {
        DateTime minusDay = DateTime.now().minusDays(1);
        testOperations.timeTravelTo(minusDay);
    }

    @Test
    public void shouldRollbackTime() {
        Key key = new Key(namespace, SET, "shouldRollbackTime");
        putBin(key, (int) TimeUnit.HOURS.toSeconds(3));

        testOperations.addDuration(standardHours(2));
        assertThat(client.get(null, key)).isNotNull();

        testOperations.rollbackTime();
        testOperations.addDuration(standardHours(2));
        assertThat(client.get(null, key)).isNotNull();
    }

    private void putBin(Key key, int expiration) {
        Bin bin = new Bin("mybin", "myvalue");

        WritePolicy writePolicy = new WritePolicy(client.getWritePolicyDefault());
        writePolicy.expiration = expiration;
        client.put(writePolicy, key, bin);
    }

}