package com.playtika.test.aerospike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.Duration.standardDays;
import static org.joda.time.Duration.standardHours;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.policy.WritePolicy;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AerospikeTimeTravelTest extends BaseAerospikeTest {

    @Autowired
    private ExpiredDocumentsCleaner expiredDocumentsCleaner;

    @AfterEach
    public void tearDown() {
        aerospikeTestOperations.rollbackTime();
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

        aerospikeTestOperations.addDuration(standardHours(24));
        assertThat(client.get(null, key)).isNotNull();

        aerospikeTestOperations.addDuration(standardHours(2));
        assertThat(client.get(null, key)).isNull();
    }

    @Test
    public void shouldAddDays() {
        Key key = new Key(namespace, SET, "shouldAddDays");
        putBin(key, (int) TimeUnit.HOURS.toSeconds(25));

        aerospikeTestOperations.addDuration(standardDays(1));
        assertThat(client.get(null, key)).isNotNull();

        aerospikeTestOperations.addDuration(standardDays(1));
        assertThat(client.get(null, key)).isNull();
    }

    @Test
    public void shouldSetFutureTime() {
        Key key = new Key(namespace, SET, "shouldSetFutureTime");
        putBin(key, (int) TimeUnit.HOURS.toSeconds(25));

        aerospikeTestOperations.timeTravelTo(DateTime.now().plusHours(24));
        assertThat(client.get(null, key)).isNotNull();

        aerospikeTestOperations.timeTravelTo(DateTime.now().plusHours(2));
        assertThat(client.get(null, key)).isNull();
    }

    @Test
    public void shouldNotSetFutureTime() {
        DateTime minusDay = DateTime.now().minusDays(1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> aerospikeTestOperations.timeTravelTo(minusDay));
    }

    @Test
    public void shouldRollbackTime() {
        Key key = new Key(namespace, SET, "shouldRollbackTime");
        putBin(key, (int) TimeUnit.HOURS.toSeconds(3));

        aerospikeTestOperations.addDuration(standardHours(2));
        assertThat(client.get(null, key)).isNotNull();

        aerospikeTestOperations.rollbackTime();
        aerospikeTestOperations.addDuration(standardHours(2));
        assertThat(client.get(null, key)).isNotNull();
    }

    private void putBin(Key key, int expiration) {
        Bin bin = new Bin("mybin", "myvalue");

        WritePolicy writePolicy = new WritePolicy(client.getWritePolicyDefault());
        writePolicy.expiration = expiration;
        client.put(writePolicy, key, bin);
    }

}