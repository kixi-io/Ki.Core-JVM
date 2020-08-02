package io.kixi.ki

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

class DurationTest {

    @Test fun testDurationSingleUnits() {

        var dur = Duration.ofDays(1)
        assertEquals(
            dur,
            Ki.parseDuration("1day")
        )
        assertEquals("1day", Ki.formatDuration(dur))

        dur = Duration.ofDays(5)
        assertEquals(
            dur,
            Ki.parseDuration("5days")
        )
        assertEquals("5days", Ki.formatDuration(dur))

        dur = Duration.ofHours(6)
        assertEquals(
            dur,
            Ki.parseDuration("6h")
        )
        assertEquals("6h", Ki.formatDuration(dur))

        dur = Duration.ofMinutes(7)
        assertEquals(
            Duration.ofMinutes(7),
            Ki.parseDuration("7min")
        )
        assertEquals("7min", Ki.formatDuration(dur))

        dur = Duration.ofSeconds(9)
        assertEquals(
            Duration.ofSeconds(9),
            Ki.parseDuration("9s")
        )
        assertEquals("9s", Ki.formatDuration(dur))

        dur = Duration.ofMillis(10)
        assertEquals(
            Duration.ofMillis(10),
            Ki.parseDuration("10ms")
        )
        assertEquals("10ms", Ki.formatDuration(dur))

        dur = Duration.ofNanos(11)
        assertEquals(
            Duration.ofNanos(11),
            Ki.parseDuration("11ns")
        )
        assertEquals("11ns", Ki.formatDuration(dur))
    }
}
