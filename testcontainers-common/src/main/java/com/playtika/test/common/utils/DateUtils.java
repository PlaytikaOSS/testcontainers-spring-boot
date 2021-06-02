package com.playtika.test.common.utils;


import org.testcontainers.shaded.org.apache.commons.lang.StringUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

/**
 * Convert ISO timestamps to human-readable relative times
 */
public class DateUtils {

    /**
     * @param isoFormattedDate ISO timestamp {@code "2021-12-31T23:59:59.123456789+18:00"}
     * @return Combine original timestamp at local time zone truncated to seconds and human-readable relative time: {@code "2021-12-31T07:59:59+02:00 (1 year 2 months ago)"}
     * @see #toTimeAgo(String)
     */
    public static String toDateAndTimeAgo(String isoFormattedDate) {
        Object instantOrString = parseToInstantOrString(isoFormattedDate);
        if (!(instantOrString instanceof Instant)) {
            return (String) instantOrString;
        }
        Instant instant = ((Instant) instantOrString);
        OffsetDateTime offsetDateTime = ((Instant) instantOrString).atZone(ZoneId.systemDefault()).toOffsetDateTime();
        return offsetDateTime + " (" + toTimeAgo(instant.toEpochMilli()) + ")";
    }

    /**
     * @param isoFormattedDate ISO timestamp {@code "2021-12-31T23:59:59.123456789+18:00"}
     * @return human-readable relative time: {@code "1 year 2 months ago"}
     * @see #toTimeAgo(long)
     */
    public static String toTimeAgo(String isoFormattedDate) {
        Object instantOrString = parseToInstantOrString(isoFormattedDate);
        return instantOrString instanceof Instant ? toTimeAgo(((Instant) instantOrString)
            .toEpochMilli()) : (String) instantOrString;
    }

    /**
     * @param isoFormattedDate ISO timestamp {@code "2021-12-31T23:59:59.123456789+18:00"}
     * @return <ul>
     * <li>Successfully parsed Instant object at UTC time zone truncated to seconds, i.e. {@code "2021-12-31T05:59:59Z"}</li>
     * <li>{@code null} if isoFormattedDate is {@code null}</li>
     * <li>{@code ""} empty string if isoFormattedDate is blank</li>
     * <li>String containing the DateTimeParseException error message if isoFormattedDate is invalid</li>
     * </ul>
     * @see DateTimeFormatter#parse(CharSequence)
     * @see Instant#from(TemporalAccessor)
     */
    static Object parseToInstantOrString(String isoFormattedDate) {
        if (isoFormattedDate == null) {
            return null;
        }
        if (StringUtils.isBlank(isoFormattedDate)) {
            return "";
        }
        try {
            return Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(isoFormattedDate))
                          .with(ChronoField.NANO_OF_SECOND, 0);
        } catch (DateTimeParseException e) {
            return e.getMessage();
        }
    }

    /**
     * <p>Thresholds are 60 seconds, 50 minutes, 20 hours, 3 weeks, 10 months.<br>
     * Everything else is rounded (1.5 becomes 2).<br>
     * Surplus months except 1 are not rounded (1 year 2 months ago).<br>
     * 1 month are 30.42 days.
     * <ul>
     *     <li>{@literal <}= 89 seconds: a minute ago</li>
     *     <li>{@literal <}= 50 minutes: 50 minutes ago</li>
     *     <li>{@literal <}= 89 minutes: an hour ago</li>
     *     <li>{@literal <}= 20 hours: 20 hours ago</li>
     *     <li>{@literal <}= 35 hours: yesterday</li>
     *     <li>{@literal <}= 5 days: 5 days ago</li>
     *     <li>{@literal <}= 10 days: a week ago</li>
     *     <li>{@literal <}= 24 days: 3 weeks ago</li>
     *     <li>{@literal <}= 45 days: a month ago</li>
     *     <li>{@literal <}= 10 months: 10 months ago</li>
     *     <li>{@literal <}= 13 months: a year ago</li>
     *     <li>== 14 months: 1 year 2 months ago</li>
     *     <li>== 23 months: 1 year 11 months ago</li>
     * </ul>
     * <pre>
     *  0 sec = a minute ago
     * 89 sec = a minute ago
     * 90 sec = 2 minutes ago
     * 50 min = 50 minutes ago
     * 51 min = an hour ago
     * 89 min = an hour ago
     * 90 min = 2 hours ago
     * 20 hrs = 20 hours ago
     * 21 hrs = yesterday
     * 35 hrs = yesterday
     * 36 hrs = 2 days ago
     *  5 day = 5 days ago
     *  6 day = a week ago
     * 10 day = a week ago
     * 11 day = 2 weeks ago
     * 17 day = 2 weeks ago
     * 18 day = 3 weeks ago
     * 24 day = 3 weeks ago
     * 25 day = a month ago
     * 45 day = a month ago
     * 46 day = 2 months ago
     * 10 mon = 10 months ago
     * 11 mon = a year ago
     * 13 mon = a year ago
     * 14 mon = 1 year 2 months ago
     * 23 mon = 1 year 11 months ago
     * 25 mon = 2 years ago
     * 26 mon = 2 years 2 months ago
     * </pre>
     * </p>
     *
     * @param epochMillis Unix time in milliseconds (since 1970-01-01)
     * @return human readable relative time: {@code "1 year 2 months ago"}
     */
    public static String toTimeAgo(long epochMillis) {
        long seconds = Instant.now().toEpochMilli() / 1000 - (epochMillis / 1000);
        long minutes = Math.round(seconds / 60.0);
        if (minutes <= 50) {
            return minutes <= 1 ? "a minute ago" : minutes + " minutes ago";
        }
        long hours = Math.round(seconds / 3600.0);
        if (hours <= 20) {
            return hours == 1 ? "an hour ago" : hours + " hours ago";
        }
        long days = Math.round(seconds / 86400.0);
        if (days <= 5) {
            return days <= 1 ? "yesterday" : days + " days ago";
        }
        long weeks = Math.round(seconds / 604800.0);
        if (weeks <= 3) {
            return weeks <= 1 ? "a week ago" : weeks + " weeks ago";
        }
        long months = Math.round(seconds / 2628288.0); // 30.42 days per month
        if (months <= 10) {
            return months <= 1 ? "a month ago" : months + " months ago";
        }
        long years = months / 12; // 11 -> 0, 23 -> 1
        months = months % 12;
        return months <= 1 || years == 0 ? ((years <= 1 ? "a year" : years + " years") + " ago") : years + ((years <= 1 ? " year " : " years ") + months + " months ago");
    }

}
