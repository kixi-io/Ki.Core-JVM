package io.kixi.ki

import io.kixi.ki.text.ParseException
import java.lang.Math.abs
import java.math.BigDecimal
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField.*

/**
 * A set of convenience methods for working with Ki types, parsing and formatting.
 *
 * TODO: DateTime types need different formatters for formatting and parsing. The
 * formatting should be strict, but the parsing should be lenient.
 */
class Ki {
    companion object {

        @JvmField
        val LOCAL_DATE = DateTimeFormatter.ofPattern("y/M/d")
        @JvmField
        val LOCAL_DATE_ZERO_PAD = DateTimeFormatter.ofPattern("yyyy/MM/dd")

        //-----------------------------------------------------------------------

        // Time Formatters & Parsers ////

        @JvmField
        val LOCAL_TIME = DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            // .optionalStart()
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .optionalStart()
            .appendFraction(NANO_OF_SECOND, 0, 9, true)
            .toFormatter().withResolverStyle(ResolverStyle.LENIENT)

        @JvmField
        val LOCAL_TIME_PARSER = DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .optionalStart()
            .appendFraction(NANO_OF_SECOND, 0, 9, true)
            .toFormatter().withResolverStyle(ResolverStyle.LENIENT)

        @JvmField
        val LOCAL_TIME_ZERO_PAD = DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            // .optionalStart()
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .optionalStart()
            .appendFraction(NANO_OF_SECOND, 9, 9, true)
            .toFormatter().withResolverStyle(ResolverStyle.LENIENT)

        @JvmField
        val LOCAL_TIME_ZERO_PAD_FORCE_NANO = DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .appendFraction(NANO_OF_SECOND, 9, 9, true)
            .toFormatter().withResolverStyle(ResolverStyle.LENIENT)

        /*
        @JvmField val LOCAL_TIME  = DateTimeFormatterBuilder()
                .appendValue(HOUR_OF_DAY)
                .appendLiteral(':')
                .appendValue(MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(SECOND_OF_MINUTE, 2)
                .optionalStart()
                .appendFraction(NANO_OF_SECOND, 0, 9, true)
                .toFormatter().withResolverStyle(ResolverStyle.LENIENT)

        @JvmField val LOCAL_TIME_NO_SECONDS  = DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .toFormatter().withResolverStyle(ResolverStyle.LENIENT)
        */

        // LocalDateTime Formatters & Parsers ////

        @JvmField
        val LOCAL_DATE_TIME = DateTimeFormatterBuilder()
            .append(LOCAL_DATE)
            .appendLiteral('@')
            .append(LOCAL_TIME) // DateTimeFormatter.ofPattern("H:mm:s.S"))
            .toFormatter() // TIME_FORMATTER) // DateTimeFormatter.ISO_LOCAL_TIME)

        @JvmField
        val LOCAL_DATE_TIME_PARSER = DateTimeFormatterBuilder()
            .append(LOCAL_DATE)
            .appendLiteral('@')
            .append(LOCAL_TIME_PARSER)
            .toFormatter()

        // ZonedDateTime Formatters & Parsers ////

        @JvmField
        val ZONED_DATE_TIME_OFFSET = DateTimeFormatterBuilder()
            .append(LOCAL_DATE_TIME)
            // .appendLiteral("'-'")
            .appendOffsetId()
            .toFormatter()

        @JvmField
        val ZONED_DATE_TIME_OFFSET_PARSER = DateTimeFormatterBuilder()
            .append(LOCAL_DATE_TIME_PARSER)
            // .appendLiteral("'-'")
            .appendOffsetId()
            .toFormatter()

        /**
         * The Ki standard DATE_TIME format yyyy/MM/dd-HH:mm:ss.nnnnnnnnnVV
         *
         * Note: Ki uses a 24 hour clock (0-23)
         */
        // const val DATE_TIME_FORMAT = DATE_FORMAT + "@" + TIME_FORMAT

        // TODO: Bin64
        @JvmStatic
        fun format(obj: Any?): String {
            return when (obj) {
                null -> "nil"
                is String -> "\"$obj\""
                is Char -> "'$obj'"
                is BigDecimal -> "${obj}m"
                is Float -> "${obj}f"
                is LocalDate -> formatLocalDate(obj)
                is LocalDateTime -> formatLocalDateTime(obj)
                is ZonedDateTime -> formatZonedDateTime(obj)
                is Duration -> formatDuration(obj)
                else -> obj.toString()
            }
        }

        // Parsing DateTime ////

        @JvmStatic
        fun parseLocalDate(ldText: String): LocalDate {
            return LocalDate.parse(ldText, LOCAL_DATE)
        }

        @JvmStatic
        fun parseLocalDateTime(ldtText: String): LocalDateTime {
            return LocalDateTime.parse(ldtText.replace("_", ""), LOCAL_DATE_TIME_PARSER)
        }

        @JvmStatic
        fun parseZonedDateTime(zdtText: String): ZonedDateTime {
            val dashIdx = zdtText.indexOf('-')

            // check for positive offset
            if (dashIdx == -1) {
                val plusIdx = zdtText.indexOf('+')

                if (plusIdx == -1) {
                    throw ParseException(
                        "ZonedDateTime requires a suffix of -Z, -UTF, " +
                                "-KiTZ (e.g. -JP/JST) or an offset (e.g. +3 or -2:30)"
                    )
                } else {
                    // We have a positive offset
                    val localDT = zdtText.substring(0, plusIdx).replace("_", "")
                    val offset = zdtText.substring(plusIdx)

                    return ZonedDateTime.parse(
                        localDT + normalizeOffset(offset),
                        ZONED_DATE_TIME_OFFSET_PARSER
                    )
                }
            }

            val localDT = zdtText.substring(0, dashIdx).replace("_", "")
            val tz = zdtText.substring(dashIdx)

            // check for negative offset
            if (tz.length > 1 && tz[1].isDigit()) {
                return ZonedDateTime.parse(
                    localDT + normalizeOffset(tz),
                    ZONED_DATE_TIME_OFFSET_PARSER
                )
                // check for Z time (UTC)
            } else if (tz == "-UTC" || tz == "-GMT" || tz == "-Z") {
                return ZonedDateTime.of(
                    LocalDateTime.parse(localDT, LOCAL_DATE_TIME_PARSER),
                    ZoneOffset.UTC
                )
                // check for KiTZ (Ki Time Zone Spec https://github.com/kixi-io/Ki.Docs/wiki/Ki-Time-Zone-Specification)
            } else {
                val offset = KiTZ.offsets[tz.substring(1)]
                if (offset == null) {
                    throw ParseException("Unsupported KiTZ ID: ${tz.substring(1)}")
                }

                return ZonedDateTime.of(LocalDateTime.parse(localDT, LOCAL_DATE_TIME_PARSER), offset)
            }
        }

        // Formatting DateTime ////

        /**
         * Format a LocalDateTime using Ki standard formatting: `y/M/d` or `yyyy/MM/dd` if
         * `zeroPad=true`.
         *
         * @param zeroPad Boolean Prefixes all displayed components with zero pads e.g.
         * 2021/05/08
         */
        @JvmStatic
        @JvmOverloads
        fun formatLocalDate(localDate: LocalDate, zeroPad: Boolean = false): String {
            return if (zeroPad) LOCAL_DATE_ZERO_PAD.format(localDate)
            else LOCAL_DATE.format(localDate)
        }

        /**
         * Format a LocalDateTime using Ki standard formatting. Hours, minutes and seconds
         * are always displayed. hours are optionally zero padded to two digits. Nanos
         * (shown as fractional seconds) are only displayed when present
         * unless forceNano=true. If nanos are displayed and zeroPad = true they are
         * padded as 9 digit fractional seconds.
         *
         *     **zeroPad=false, forceNano=false (default)**
         *         y/M/d@H:mm:ss(.S)? # Fractional seconds displayed only if non-zero
         *         Example: `2020/5/2@8:05:00`, `2020/5/2@8:05:00.001`
         *
         *     **zeroPad=true, forceNano=false**
         *         y/M/d@HH:mm:ss(.S)? # Fractional seconds displayed only if non-zero
         *         Example: `2020/05/02@08:31:00`, `2020/05/02@08:31:00.001000000`
         *
         *     **zeroPad=true, forceNano=true**
         *         y/M/d@HH:mm:ss(.n)? # Fractional seconds always shown, 9 digits
         *         Example: `2020/05/02@08:31:00.000000000`, `2020/05/02@08:31:00.001000000`
         *
         *
         * @param zeroPad Boolean Prefixes all displayed components with zero pads e.g. 2021/05/08@08:05:00
         * @param forceNano Boolean Forces nanos to be displayed even if they are 0 (9 zeros of padding)
         */
        @JvmStatic
        @JvmOverloads
        fun formatLocalDateTime(
            localDateTime: LocalDateTime, zeroPad: Boolean = false,
            forceNano: Boolean = false
        ): String {

            var dateText = formatLocalDate(localDateTime.toLocalDate(), zeroPad)

            var timeText = ""

            if (!zeroPad && !forceNano) {
                timeText = LOCAL_TIME.format(localDateTime)
            } else if (zeroPad && !forceNano) {
                timeText = LOCAL_TIME_ZERO_PAD.format(localDateTime)
                if (localDateTime.nano == 0) timeText = timeText.removeSuffix(".000000000")
            } else if (zeroPad && forceNano) {
                timeText = LOCAL_TIME_ZERO_PAD_FORCE_NANO.format(localDateTime)
            }

            return dateText + "@" + timeText;
        }

        @JvmStatic
        @JvmOverloads
        fun formatZonedDateTime(
            zonedDateTime: ZonedDateTime, zeroPad: Boolean = false,
            forceNano: Boolean = false
        ): String {

            val buf = StringBuilder()
            buf.append(formatLocalDateTime(zonedDateTime.toLocalDateTime(), zeroPad, forceNano))

            var zone = zonedDateTime.zone.toString().replace("GMT", "UTF")
            if (zone == "Z")
                zone = "-Z"
            buf.append(zone)

            // remove second component of time zone offset if :00
            return buf.toString().removeSuffix(":00")
        }

        private fun normalizeOffset(offset: String): String {

            val plusOrMinus = offset[0]
            if (plusOrMinus != '-' && plusOrMinus != '+')
                throw ParseException("First character of an offset must be + or -.")

            val text = offset.substring(1)

            val length = text.length
            if (length == 1) {
                return "${plusOrMinus}0$text:00"
            } else if (length == 2) {
                return "${plusOrMinus}$text:00"
            } else if (length == 4 && text[1] == ':') {
                return "${plusOrMinus}0$text"
            }

            return offset
        }

        // Parsing Duration ////

        // TODO: Support fractional seconds and single units.
        // TODO: Update docs to remove optional labels other than day in compound
        //       durations.
        fun parseDuration(text: String): Duration {
            var parts = text.split(':')

            var day = ""

            if (parts.size == 4) {
                // is compound with day
                var dayIndex = parts[0].indexOf("day")
                if (dayIndex == -1)
                    throw ParseException(
                        "Compound duration with day component must be " +
                                "suffixed with \"day\" or \"days\"."
                    )
                return Duration.ofDays(parts[0].substring(0, dayIndex).toLong())
                    .plus(Duration.ofHours(parts[1].toLong()))
                    .plus(Duration.ofMinutes(parts[2].toLong()))
                    .plus(Duration.ofSeconds(parts[3].toLong()))
            } else if (parts.size == 3) {
                // is compound without day
                return Duration.ofHours(parts[0].toLong())
                    .plus(Duration.ofMinutes(parts[1].toLong()))
                    .plus(Duration.ofSeconds(parts[2].toLong()))
            } else if (parts.size == 1) {
                // TODO: Fractional units for single unit durations.
                return when {
                    text.endsWith("day") -> {
                        return Duration.ofDays(text.removeSuffix("day").toLong())
                    }
                    text.endsWith("days") -> {
                        return Duration.ofDays(text.removeSuffix("days").toLong())
                    }
                    text.endsWith("h") -> {
                        return Duration.ofHours(text.removeSuffix("h").toLong())
                    }
                    text.endsWith("min") -> {
                        return Duration.ofMinutes(text.removeSuffix("min").toLong())
                    }
                    // We have to check "ms" and "ns" before "s"
                    text.endsWith("ms") -> {
                        return Duration.ofMillis(text.removeSuffix("ms").toLong())
                    }
                    text.endsWith("ns") -> {
                        return Duration.ofNanos(text.removeSuffix("ns").toLong())
                    }
                    text.endsWith("s") -> {
                        return Duration.ofSeconds(text.removeSuffix("s").toLong())
                    }
                    else -> throw ParseException("Unkown temporal unit in duration.")
                }
            }

            throw ParseException(
                """
                Can't parse Duration \"$text\": Wrong number of segments. Durations must
                be single unit (e.g. 5h) or compound with days:hours:minutes:seconds. Days
                are option and must be suffixed with "day" or "days"
                (e.g. 2days:05:30:00).
            """.trimIndent()
            )
        }

        // Formatting Duration ////

        @JvmStatic
        @JvmOverloads
        fun formatDuration(duration: Duration, zeroPad: Boolean = false): String {

            // TODO: Implement zeroPad option

            var totalNanos = duration.toNanos()

            val sign = if (totalNanos < 0L) "-" else ""

            totalNanos = abs(duration.toNanos())
            val nanosOfSec = totalNanos % 1_000_000_000L

            var fractionalSec = if(nanosOfSec==0L)
                ""
            else "." + trimTrailing0s(String.format("%09d", nanosOfSec))

            // Single unit Durations: day(s), h, m, s, ms, ns

            // Check for ns single unit
            if (totalNanos < 1_000_000L) {
                return "$sign${totalNanos}ns"

            // Check for ms single unit
            } else if (totalNanos < 1_000_000_000L) {
                return "$sign${totalNanos / 1_000_000L}ms"

            // Check for seconds (including fractional) single unit
            } else if (totalNanos < 60_000_000_000L) {
                val secs = abs(duration.toSecondsPart())
                return "${sign}${secs}${fractionalSec}s"
            }

            // Compound Durations (day:)h/min/s.S ////

            val days = abs(duration.toDaysPart())
            val hrs = abs(duration.toHoursPart())
            val mins = abs(duration.toMinutesPart())
            val secs = abs(duration.toSecondsPart())

            val millis = abs(duration.toMillisPart())
            val nanos = abs(duration.toNanosPart())

            var dtext = StringBuilder()

            if (days != 0L) {
                if (all0(hrs, mins, secs) && fractionalSec.isEmpty())
                    return "$sign${days}${if (days == 1L) "day" else "days"}"

                return "$sign${days}${if (days == 1L) "day" else "days"}:$hrs:$mins:$secs${fractionalSec}"
            } else if (hrs != 0) {
                if (all0(days, mins, secs)  && fractionalSec.isEmpty())
                    return "$sign${hrs}h"

                return "$sign$hrs:$mins:$secs${fractionalSec}"
            } else if (mins != 0) {
                if (all0(days, hrs, secs) && fractionalSec.isEmpty())
                    return "$sign${mins}min"

                return "$sign$hrs:$mins:$secs${fractionalSec}"
            } else if (secs != 0 && fractionalSec.isEmpty()) {
                if (all0(days, hrs, mins))
                    return "$sign${secs}s"

                return "$sign$hrs:$mins:$secs${fractionalSec}"
            }

            return "0:0:0"
        }

        private fun trimTrailing0s(numText:String) : String {
            var end = numText.length

            for (i in end-1 downTo 0) {
                if(numText[i]!='0') {
                    end = i+1
                    break
                } else if(i==0) {
                    end = 0
                    break
                }
            }

            return numText.substring(0, end)
        }

        private fun all0(vararg args:Number) : Boolean {
            for(num in args) {
                if(num is Int && num!=0) {
                    return false
                } else if(num is Long && num!=0L) {
                    return false
                }
            }
            return true
        }

        /**
         * @param duration Duration
         * @param zeroPad Boolean If true string will have 0 prefix padding as appropriate for the unit
         * @return String The unit string or null if its not a single unit duration
         */
        /*
        private fun checkSingleUnitDuration(duration: Duration, zeroPad: Boolean = false): String {
            var nanos = duration.toNanos()

            // Check for ns only
            if (nanos < 1_000_000) {
                return "${nanos}ns"

            // Check for ms only
            } else if (nanos < 1_000_000_000) {
                return "${nanos / 1_000_000}ms"

            // We have a duration of a second or longer
            } else {

            }
        }
        */
    }
}

// TODO - Convert to tests
fun main() {

    // Duration
    log("-- Durations ----")

    log("-- compound")

    var dur1 = Ki.parseDuration("1:30:00")
    var dur2 = Ki.parseDuration("0:15:00")
    var dur3 = Ki.parseDuration("10:23:53")
    var dur4 = dur3.plus(Duration.ofMillis(123))
    var dur5 = dur3.plus(Duration.ofNanos(123456789))

    log(dur1.kiFormat())
    log(dur2.kiFormat())
    log(dur3.kiFormat())
    log(dur4.kiFormat())
    log(dur5.kiFormat())

    log("-- compound with days")

    dur1 = Ki.parseDuration("1day:1:30:00")
    dur2 = Ki.parseDuration("5days:2:15:3")
    dur3 = dur2.plus(Duration.ofMillis(23))
    dur4 = dur2.plus(Duration.ofNanos(456789))

    log(Ki.formatDuration(dur1))
    log(Ki.formatDuration(dur2))
    log(Ki.formatDuration(dur3))
    log(Ki.formatDuration(dur4))

    log("-- single unit")

    dur1 = Ki.parseDuration("1day")
    dur2 = Ki.parseDuration("3days")
    dur3 = Ki.parseDuration("5h")
    dur4 = Ki.parseDuration("12min")
    dur5 = Ki.parseDuration("23s")

    var dur6 = Duration.ofMillis(25)
    var dur7 = Duration.ofNanos(52)

    var dur8 = dur5.plus(dur6)
    var dur9 = dur5.plus(dur7)
    var dur10 = dur5.plus(dur6).plus(dur7)

    log(Ki.formatDuration(dur1))
    log(Ki.formatDuration(dur2))
    log(Ki.formatDuration(dur3))
    log(Ki.formatDuration(dur4))
    log(Ki.formatDuration(dur5))
    log(Ki.formatDuration(dur6))
    log(Ki.formatDuration(dur7))
    log(Ki.formatDuration(dur8))
    log(Ki.formatDuration(dur9))
    log(Ki.formatDuration(dur10))
}

