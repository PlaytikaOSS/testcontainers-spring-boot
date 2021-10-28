package com.playtika.test.aerospike;

import lombok.RequiredArgsConstructor;
import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * @deprecated please instead use AerospikeTestOperations
 */
@Deprecated
@RequiredArgsConstructor
public class AerospikeTimeTravelService {

    private final AerospikeTestOperations testOperations;

    /**
     * @deprecated please instead use AerospikeTestOperations
     */
    @Deprecated
    public void timeTravelTo(DateTime futureTime) {
        testOperations.timeTravelTo(futureTime);
    }

    /**
     * @deprecated please instead use AerospikeTestOperations
     */
    @Deprecated
    public void nextDay() {
        testOperations.timeTravelTo(DateTime.now().plusDays(1));
    }

    /**
     * @deprecated please instead use AerospikeTestOperations
     */
    @Deprecated
    public void addDays(int days) {
        testOperations.addDuration(Duration.standardDays(days));
    }

    /**
     * @deprecated please instead use AerospikeTestOperations
     */
    @Deprecated
    public void addHours(int hours) {
        testOperations.addDuration(Duration.standardHours(hours));
    }

    /**
     * @deprecated please instead use AerospikeTestOperations
     */
    @Deprecated
    public void rollbackTime() {
        testOperations.rollbackTime();
    }


}
