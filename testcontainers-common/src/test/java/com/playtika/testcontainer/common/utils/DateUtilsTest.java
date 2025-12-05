package com.playtika.testcontainer.common.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DateUtilsTest {

    static Stream<Arguments> relativeTimeTestData() {
        return Stream.of(
            Arguments.of(1000L * 0, " 0 sec = a minute ago"),
            Arguments.of(1000L * 89, "89 sec = a minute ago"),
            Arguments.of(1000L * 90, "90 sec = 2 minutes ago"),
            Arguments.of(1000L * 60 * 50, "50 min = 50 minutes ago"),
            Arguments.of(1000L * 60 * 51, "51 min = an hour ago"),
            Arguments.of(1000L * 60 * 89, "89 min = an hour ago"),
            Arguments.of(1000L * 60 * 90, "90 min = 2 hours ago"),
            Arguments.of(1000L * 60 * 60 * 20, "20 hrs = 20 hours ago"),
            Arguments.of(1000L * 60 * 60 * 21, "21 hrs = yesterday"),
            Arguments.of(1000L * 60 * 60 * 35, "35 hrs = yesterday"),
            Arguments.of(1000L * 60 * 60 * 36, "36 hrs = 2 days ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 5, " 5 day = 5 days ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 6, " 6 day = a week ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 10, "10 day = a week ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 11, "11 day = 2 weeks ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 17, "17 day = 2 weeks ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 18, "18 day = 3 weeks ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 24, "24 day = 3 weeks ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 25, "25 day = a month ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 45, "45 day = a month ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 46, "46 day = 2 months ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 30 * 10, "10 mon = 10 months ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 30 * 11, "11 mon = a year ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 30 * 13, "13 mon = a year ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 30 * 14, "14 mon = 1 year 2 months ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 30 * 23, "23 mon = 1 year 11 months ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 30 * 25, "25 mon = 2 years ago"),
            Arguments.of(1000L * 60 * 60 * 24 * 30 * 26, "26 mon = 2 years 2 months ago")
        );
    }

    private static OffsetDateTime toOffsetDateTime(Instant now, Long time) {
        return now.minusMillis(time).with(ChronoField.NANO_OF_SECOND, 0).atZone(ZoneId.systemDefault())
                  .toOffsetDateTime();
    }

    private static OffsetDateTime toOffsetDateTimePlusNanoseconds(Instant now, Long time) {
        return now.minusMillis(time).atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("relativeTimeTestData")
    void toDateAndTimeAgo(Long time, String description) {
        Instant now = Instant.now();
        String expectedTimeAgo = description.split(" = ")[1];
        OffsetDateTime dateTime = toOffsetDateTime(now, time);
        String expected = String.format("%s (%s)", dateTime, expectedTimeAgo);

        assertThat(DateUtils.toDateAndTimeAgo(toOffsetDateTimePlusNanoseconds(now, time).toString()))
            .as(dateTime + " = " + description)
            .isEqualTo(expected);
    }

    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("relativeTimeTestData")
    void toTimeAgoLong(Long time, String description) {
        Instant now = Instant.now();
        String expectedTimeAgo = description.split(" = ")[1];

        assertThat(DateUtils.toTimeAgo(now.toEpochMilli() - time))
            .as(toOffsetDateTime(now, time) + " = " + description)
            .isEqualTo(expectedTimeAgo);
    }

    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("relativeTimeTestData")
    void toTimeAgoString(Long time, String description) {
        Instant now = Instant.now();
        String expectedTimeAgo = description.split(" = ")[1];

        assertThat(DateUtils.toTimeAgo(toOffsetDateTimePlusNanoseconds(now, time).toString()))
            .as(toOffsetDateTime(now, time) + " = " + description)
            .isEqualTo(expectedTimeAgo);
    }

    @Test
    void parseToInstantOrString_edgeCases() {
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
    }

    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("relativeTimeTestData")
    void parseToInstantOrString(Long time, String description) {
        Instant now = Instant.now();
        Instant expected = now.minusMillis(time).with(ChronoField.NANO_OF_SECOND, 0);

        assertThat(DateUtils.parseToInstantOrString(toOffsetDateTimePlusNanoseconds(now, time).toString()))
            .as(toOffsetDateTime(now, time) + " = " + description)
            .isEqualTo(expected);
    }

}
