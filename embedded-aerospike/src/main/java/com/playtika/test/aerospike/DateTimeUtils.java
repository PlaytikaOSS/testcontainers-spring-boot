package com.playtika.test.aerospike;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class DateTimeUtils {
    private static volatile EmbeddedMillisProvider timeMillis;
    private static final SystemTimeMillisProvider SYSTEM_TIME = new SystemTimeMillisProvider();

    static {
        timeMillis = SYSTEM_TIME;
    }

    public static OffsetDateTime now() {
        return OffsetDateTime
                       .ofInstant(Instant.ofEpochMilli(currentTimeMillis()), ZoneId.systemDefault());
    }

    public static long currentTimeMillis() {
        return timeMillis.getMillis();
    }

    public static void setCurrentMillisSystem() throws SecurityException {
        timeMillis = SYSTEM_TIME;
    }

    public static void setCurrentMillisFixed(long currentMilliseconds) throws SecurityException {
        timeMillis = new FixedTimeMillisProvider(currentMilliseconds);
    }


    static class FixedTimeMillisProvider implements EmbeddedMillisProvider {
        private final long iMillis;

        FixedTimeMillisProvider(long millis) {
            this.iMillis = millis;
        }

        public long getMillis() {
            return this.iMillis;
        }
    }

    static class SystemTimeMillisProvider implements EmbeddedMillisProvider {
        SystemTimeMillisProvider() {
        }

        public long getMillis() {
            return System.currentTimeMillis();
        }
    }

    public interface EmbeddedMillisProvider {
        long getMillis();
    }

}
