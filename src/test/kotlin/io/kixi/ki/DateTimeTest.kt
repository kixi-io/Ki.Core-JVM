package io.kixi.ki

import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test;
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DateTimeTest {

    // LocalDate ////

    @Test fun testLocalDate() {
        var date1 = Ki.parseLocalDate("2021/8/5")
        var date2 = Ki.parseLocalDate("2021/08/05")
        assertEquals(date1, date2)
        assertEquals("2021-08-05", date1.format(DateTimeFormatter.ISO_DATE))
    }

    @Test fun testLocalDateFormatting() {
        var date1 = Ki.parseLocalDate("2021/8/5")
        assertEquals("2021/8/5", Ki.formatLocalDate(date1, false))
        assertEquals("2021/08/05", Ki.formatLocalDate(date1, true))
    }

    // LocalDateTime ////

    @Test fun testLocalDateTime() {
        var date1Time = Ki.parseLocalDateTime("2021/8/5@9:05:00.0")
        var date2Time = Ki.parseLocalDateTime("2021/8/05@09:05")
        assertEquals(date1Time, date2Time)
        assertEquals("2021-08-05T09:05:00", date1Time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }

    @Test fun testLocalDateTimeFormatting() { }

    // ZonedDateTime ////

    @Test fun testZonedDateTime() { }

    @Test fun testZonedDateTimeFormatting() { }
}