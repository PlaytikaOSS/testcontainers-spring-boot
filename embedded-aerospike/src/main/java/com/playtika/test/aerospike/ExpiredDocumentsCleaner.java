package com.playtika.test.aerospike;

import java.time.Instant;

public interface ExpiredDocumentsCleaner {
    void cleanExpiredDocumentsBefore(long millis);
    void cleanExpiredDocumentsBefore(Instant expireTime);
}
