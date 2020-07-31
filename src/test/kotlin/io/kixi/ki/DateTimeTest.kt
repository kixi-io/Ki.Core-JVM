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
        var localDateTime1 = Ki.parseLocalDateTime("2021/8/5@9:05:00.0")
        var localDateTime2 = Ki.parseLocalDateTime("2021/8/05@09:05")
        var localDateTime3 = Ki.parseLocalDateTime("2021/8/05@09:05:00.000_001")
        assertEquals(localDateTime1, localDateTime2)
        assertEquals("2021-08-05T09:05:00", localDateTime1.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        assertEquals(1000, localDateTime3.nano)
    }

    @Test fun testLocalDateTimeFormatting() {
        var localDateTime1 = Ki.parseLocalDateTime("2020/05/02@8:05")
        var localDateTime2 = Ki.parseLocalDateTime("2021/08/05@09:05:06.001_000_000")

        assertEquals("2020/5/2@8:05:00", Ki.formatLocalDateTime(localDateTime1))
        assertEquals("2020/05/02@08:05:00", Ki.formatLocalDateTime(localDateTime1, zeroPad=true))
        assertEquals("2020/05/02@08:05:00.000000000", Ki.formatLocalDateTime(localDateTime1, zeroPad=true,
            forceNano=true))

        assertEquals("2021/8/5@9:05:06.001", Ki.formatLocalDateTime(localDateTime2))
        assertEquals("2021/08/05@09:05:06.001000000", Ki.formatLocalDateTime(localDateTime2, zeroPad=true))
    }

    // ZonedDateTime ////

    @Test fun testZonedDateTime() {
        var zonedDateTime1 = Ki.parseZonedDateTime("2021/8/5@9:05:06.001+2")
        var zonedDateTime2 = Ki.parseZonedDateTime("2021/08/05@09:05:06.001_000_000+2")
        var zonedDateTime3 = Ki.parseZonedDateTime("2021/8/05@09:05:18.23-4:30")
        var zonedDateTime4 = Ki.parseZonedDateTime("2021/8/05@09:05:00.23-IN/IST")
        var zonedDateTime5 = Ki.parseZonedDateTime("2020/8/10@9:02-Z")

        assertEquals(zonedDateTime1, zonedDateTime2)
        assertEquals(18, zonedDateTime3.second)
        assertEquals("+05:30", zonedDateTime4.zone.toString())
        assertEquals("2020/8/10@9:02:00-Z", Ki.formatZonedDateTime(zonedDateTime5))
    }
}
