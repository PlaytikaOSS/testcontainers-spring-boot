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

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;

import java.util.concurrent.TimeUnit;

public class AerospikeTimeTravelService {

    private ExpiredDocumentsCleaner expiredDocumentsCleaner;

    public AerospikeTimeTravelService(ExpiredDocumentsCleaner expiredDocumentsCleaner) {
        this.expiredDocumentsCleaner = expiredDocumentsCleaner;
    }

    public void timeTravelTo(DateTime futureTime) {
        DateTime now = DateTime.now();
        if(futureTime.isBeforeNow()) {
            throw new IllegalArgumentException("Time should be in future. Now is: " + now + " time is:" + futureTime);
        } else {
            timeTravel(futureTime);
        }
    }

    public void nextDay() {
        timeTravel(DateTime.now().plusDays(1));
    }

    public void addDays(int days) {
        addHours((int) TimeUnit.DAYS.toHours((long)days));
    }

    public void addHours(int hours) {
        timeTravel(DateTime.now().plusHours(hours).plusMinutes(1));
    }

    public void rollbackTime() {
        DateTimeUtils.setCurrentMillisSystem();
    }

    private void timeTravel(DateTime newNow) {
        DateTimeUtils.setCurrentMillisFixed(newNow.getMillis());
        expiredDocumentsCleaner.cleanExpiredDocumentsBefore(newNow.getMillis());
    }
}
