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

import lombok.RequiredArgsConstructor;
import org.joda.time.DateTime;
import org.joda.time.Duration;

/**
 * Please instead use AerospikeTestOperations
 */
@Deprecated
@RequiredArgsConstructor
public class AerospikeTimeTravelService {

    private final AerospikeTestOperations testOperations;

    /**
     * Please instead use AerospikeTestOperations
     */
    @Deprecated
    public void timeTravelTo(DateTime futureTime) {
        testOperations.timeTravelTo(futureTime);
    }

    /**
     * Please instead use AerospikeTestOperations
     */
    @Deprecated
    public void nextDay() {
        testOperations.timeTravelTo(DateTime.now().plusDays(1));
    }

    /**
     * Please instead use AerospikeTestOperations
     */
    @Deprecated
    public void addDays(int days) {
        testOperations.addDuration(Duration.standardDays(days));
    }

    /**
     * Please instead use AerospikeTestOperations
     */
    @Deprecated
    public void addHours(int hours) {
        testOperations.addDuration(Duration.standardHours(hours));
    }

    /**
     * Please instead use AerospikeTestOperations
     */
    @Deprecated
    public void rollbackTime() {
        testOperations.rollbackTime();
    }


}
