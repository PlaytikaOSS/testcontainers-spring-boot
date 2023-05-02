package com.playtika.testcontainer.common.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilsTest {

    private static Map<Long, String> relativeTimeToStrings = createRelativeTimeToStringExamples();

    private static Map<Long, String> createRelativeTimeToStringExamples() {
        Map<Long, String> map = new LinkedHashMap<>();
        map.put(1000L * 0, " 0 sec = a minute ago");
        map.put(1000L * 89, "89 sec = a minute ago");
        map.put(1000L * 90, "90 sec = 2 minutes ago");
        map.put(1000L * 60 * 50, "50 min = 50 minutes ago");
        map.put(1000L * 60 * 51, "51 min = an hour ago");
        map.put(1000L * 60 * 89, "89 min = an hour ago");
        map.put(1000L * 60 * 90, "90 min = 2 hours ago");
        map.put(1000L * 60 * 60 * 20, "20 hrs = 20 hours ago");
        map.put(1000L * 60 * 60 * 21, "21 hrs = yesterday");
        map.put(1000L * 60 * 60 * 35, "35 hrs = yesterday");
        map.put(1000L * 60 * 60 * 36, "36 hrs = 2 days ago");
        map.put(1000L * 60 * 60 * 24 * 5, " 5 day = 5 days ago");
        map.put(1000L * 60 * 60 * 24 * 6, " 6 day = a week ago");
        map.put(1000L * 60 * 60 * 24 * 10, "10 day = a week ago");
        map.put(1000L * 60 * 60 * 24 * 11, "11 day = 2 weeks ago");
        map.put(1000L * 60 * 60 * 24 * 17, "17 day = 2 weeks ago");
        map.put(1000L * 60 * 60 * 24 * 18, "18 day = 3 weeks ago");
        map.put(1000L * 60 * 60 * 24 * 24, "24 day = 3 weeks ago");
        map.put(1000L * 60 * 60 * 24 * 25, "25 day = a month ago");
        map.put(1000L * 60 * 60 * 24 * 45, "45 day = a month ago");
        map.put(1000L * 60 * 60 * 24 * 46, "46 day = 2 months ago");
        map.put(1000L * 60 * 60 * 24 * 30 * 10, "10 mon = 10 months ago");
        map.put(1000L * 60 * 60 * 24 * 30 * 11, "11 mon = a year ago");
        map.put(1000L * 60 * 60 * 24 * 30 * 13, "13 mon = a year ago");
        map.put(1000L * 60 * 60 * 24 * 30 * 14, "14 mon = 1 year 2 months ago");
        map.put(1000L * 60 * 60 * 24 * 30 * 23, "23 mon = 1 year 11 months ago");
        map.put(1000L * 60 * 60 * 24 * 30 * 25, "25 mon = 2 years ago");
        map.put(1000L * 60 * 60 * 24 * 30 * 26, "26 mon = 2 years 2 months ago");
        return map;
    }

    private static OffsetDateTime toOffsetDateTime(Instant now, Long time) {
        return now.minusMillis(time).with(ChronoField.NANO_OF_SECOND, 0).atZone(ZoneId.systemDefault())
                  .toOffsetDateTime();
    }

    private static OffsetDateTime toOffsetDateTimePlusNanoseconds(Instant now, Long time) {
        return now.minusMillis(time).atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    @Test
    void toDateAndTimeAgo() {
        Instant now = Instant.now();
        relativeTimeToStrings.forEach((time, text) -> assertThat(DateUtils
            .toDateAndTimeAgo(toOffsetDateTimePlusNanoseconds(now, time).toString()))
            .as(toOffsetDateTime(now, time) + " = " + text).isEqualTo(String
                .format("%s (%s)", toOffsetDateTime(now, time), text.split(" = ")[1])));
    }

    @Test
    void toTimeAgoLong() {
        Instant now = Instant.now();
        relativeTimeToStrings.forEach((time, text) -> assertThat(DateUtils.toTimeAgo(now.toEpochMilli() - time))
            .as(toOffsetDateTime(now, time) + " = " + text).isEqualTo(text.split(" = ")[1]));
    }

    @Test
    void toTimeAgoString() {
        Instant now = Instant.now();
        relativeTimeToStrings.forEach((time, text) -> assertThat(DateUtils
            .toTimeAgo(toOffsetDateTimePlusNanoseconds(now, time).toString()))
            .as(toOffsetDateTime(now, time) + " = " + text).isEqualTo(text.split(" = ")[1]));
    }

    @Test
    void parseToInstantOrString() {
        assertThat(DateUtils.parseToInstantOrString(null)).isNull();
        assertThat(DateUtils.parseToInstantOrString("")).isEqualTo("");
        assertThat(DateUtils.parseToInstantOrString("   ")).isEqualTo("");
        assertThat(DateUtils.parseToInstantOrString("yesterday"))
            .isEqualTo("Text 'yesterday' could not be parsed at index 0");
        assertThat(DateUtils.parseToInstantOrString("2021-12-31T23:59:59+23:00"))
            .isEqualTo("Text '2021-12-31T23:59:59+23:00' could not be parsed: Zone offset not in valid range: -18:00 to +18:00");
        assertThat(DateUtils.parseToInstantOrString("2021-12-31T23:59:59"))
            .as("Zone offset like +05:00 is required")
            .isEqualTo("Text '2021-12-31T23:59:59' could not be parsed at index 19");

        Instant now = Instant.now();
        relativeTimeToStrings.forEach((time, text) -> assertThat(DateUtils
            .parseToInstantOrString(toOffsetDateTimePlusNanoseconds(now, time).toString()))
            .as(toOffsetDateTime(now, time) + " = " + text)
            .isEqualTo(now.minusMillis(time).with(ChronoField.NANO_OF_SECOND, 0)));
    }

}
