package io.kixi

import io.kixi.Ki.Companion.formatDuration as format
import io.kixi.Ki.Companion.parseDuration as parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class DurationTest {

    @Test fun durationSingleUnits() {
        var dur = Duration.ofDays(1)
        assertEquals(dur, parse("1day"))
        assertEquals("1day", format(dur))

        dur = Duration.ofDays(5)
        assertEquals(dur, parse("5days"))
        assertEquals("5days", format(dur))

        dur = Duration.ofHours(6)
        assertEquals(dur, parse("6h"))
        assertEquals("6h", format(dur))

        dur = Duration.ofMinutes(7)
        assertEquals(dur, parse("7min"))
        assertEquals("7min", format(dur))

        dur = Duration.ofSeconds(9)
        assertEquals(dur, parse("9s"))
        assertEquals("9s", format(dur))

        dur = Duration.ofSeconds(9).plusMillis(123)
        assertEquals(dur, parse("9.123s"))
        assertEquals("9.123s", format(dur))

        dur = Duration.ofMillis(10)
        assertEquals(dur, parse("10ms"))
        assertEquals("10ms", format(dur))

        dur = Duration.ofNanos(11)
        assertEquals(dur, parse("11ns"))
        assertEquals("11ns", format(dur))
    }


    @Test fun durationCompound() {
        var durHMS = Duration.ofHours(5).plusMinutes(6).plusSeconds(7)
        assertEquals("5:6:7", format(durHMS))
        assertEquals("05:06:07", format(durHMS, zeroPad = true))

        var durHMSM = Duration.ofHours(5).plusMinutes(6).plusSeconds(7)
            .plusMillis(999)
        assertEquals("5:6:7.999", format(durHMSM))
        assertEquals("05:06:07.999", format(durHMSM, zeroPad = true))

        var durHMSMN = Duration.ofHours(5).plusMinutes(6).plusSeconds(7)
            .plusMillis(999).plusNanos(888)
        assertEquals("5:6:7.999000888", format(durHMSMN))
        assertEquals("05:06:07.999000888", format(durHMSMN, zeroPad = true))

        var durDHMS = Duration.ofDays(5).plusHours(4).plusMinutes(3)
            .plusSeconds(2)
        assertEquals("5days:4:3:2", format(durDHMS))
        assertEquals("5days:04:03:02", format(durDHMS, zeroPad = true))
    }

    @Test fun equalsForVaryingFormats() {
        assertEquals(parse("5h"), parse("5:0:0"))
        assertEquals(parse("48h"), parse("2days"))
        assertEquals(parse("53:04:03.111000"), parse("2days:5:4:3.111"))
    }

    @Test fun negativeDurationParsing() {
        // Parsing
        assertEquals(Duration.ofDays(-1), parse("-1day"))
        assertEquals(Duration.ofDays(-2), parse("-2days"))
        assertEquals(Duration.ofHours(-3), parse("-3h"))
        assertEquals(Duration.ofMinutes(-4), parse("-4min"))
        assertEquals(Duration.ofSeconds(-5), parse("-5s"))
        assertEquals(Duration.ofSeconds(-5).plusMillis(-936), parse("-5.936s"))
        assertEquals(
            Duration.ofDays(-1).plusHours(-2).plusMinutes(-3)
                .plusSeconds(-4).plusMillis(-567), parse("-1day:2:3:4.567"))
    }

    @Test fun negativeDurationFormatting() {
        // Individual Unit
        assertEquals("-1day", format(Duration.ofDays(-1)))
        assertEquals("-2days", format(Duration.ofDays(-2)))
        assertEquals("-3h", format(Duration.ofHours(-3)))
        assertEquals("-4min", format(Duration.ofMinutes(-4)))
        assertEquals("-5s", format(Duration.ofSeconds(-5)))
        assertEquals("-5.123456789s",format(Duration.ofSeconds(-5).
            plusNanos(-123456789)))
        assertEquals("-6ms", format(Duration.ofMillis(-6)))
        assertEquals("-7ns", format(Duration.ofNanos(-7)))

        // Compound
        assertEquals("-1day:2:3:4", format(compound(-1,-2,-3, -4)))
        assertEquals("-1:2:3", format(compound(-1,-2, -3)))
        assertEquals("-1:2:3.234", format(compound(-1,-2, -3)
            .plusMillis(-234)))
        assertEquals("-1:2:3.000000234", format(compound(-1,-2, -3)
            .plusNanos(-234)))
    }

    private fun compound(h:Long, min:Long, s:Long): Duration =
        Duration.ofHours(h).plusMinutes(min).plusSeconds(s)

    private fun compound(days: Long, h:Long, min:Long, s:Long): Duration =
        Duration.ofDays(days).plusHours(h).plusMinutes(min).plusSeconds(s)
}
