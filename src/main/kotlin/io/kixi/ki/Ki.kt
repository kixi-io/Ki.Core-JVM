package io.kixi.ki

import io.kixi.ki.text.ParseException
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

        @JvmField val LOCAL_DATE = DateTimeFormatter.ofPattern("y/M/d")
        @JvmField val LOCAL_DATE_ZERO_PAD = DateTimeFormatter.ofPattern("yyyy/MM/dd")

        //-----------------------------------------------------------------------

        // Time Formatters & Parsers ////

        @JvmField val LOCAL_TIME  = DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            // .optionalStart()
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .optionalStart()
            .appendFraction(NANO_OF_SECOND, 0, 9, true)
            .toFormatter().withResolverStyle(ResolverStyle.LENIENT)

        @JvmField val LOCAL_TIME_PARSER  = DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .optionalStart()
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .optionalStart()
            .appendFraction(NANO_OF_SECOND, 0, 9, true)
            .toFormatter().withResolverStyle(ResolverStyle.LENIENT)

        @JvmField val LOCAL_TIME_ZERO_PAD  = DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            // .optionalStart()
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .optionalStart()
            .appendFraction(NANO_OF_SECOND, 9, 9, true)
            .toFormatter().withResolverStyle(ResolverStyle.LENIENT)

        @JvmField val LOCAL_TIME_ZERO_PAD_FORCE_NANO  = DateTimeFormatterBuilder()
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

        @JvmField val LOCAL_DATE_TIME = DateTimeFormatterBuilder()
            .append(LOCAL_DATE)
            .appendLiteral('@')
            .append(LOCAL_TIME) // DateTimeFormatter.ofPattern("H:mm:s.S"))
            .toFormatter() // TIME_FORMATTER) // DateTimeFormatter.ISO_LOCAL_TIME)

        @JvmField val LOCAL_DATE_TIME_PARSER = DateTimeFormatterBuilder()
            .append(LOCAL_DATE)
            .appendLiteral('@')
            .append(LOCAL_TIME_PARSER)
            .toFormatter()

        // ZonedDateTime Formatters & Parsers ////

        @JvmField val ZONED_DATE_TIME_OFFSET = DateTimeFormatterBuilder()
            .append(LOCAL_DATE_TIME)
            // .appendLiteral("'-'")
            .appendOffsetId()
            .toFormatter()

        @JvmField val ZONED_DATE_TIME_OFFSET_PARSER = DateTimeFormatterBuilder()
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
        @JvmStatic fun format(obj: Any?): String {
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

        @JvmStatic fun parseLocalDate(ldText:String) : LocalDate {
            return LocalDate.parse(ldText, LOCAL_DATE)
        }

        @JvmStatic fun parseLocalDateTime(ldtText:String) : LocalDateTime {
            return LocalDateTime.parse(ldtText.replace("_", ""), LOCAL_DATE_TIME_PARSER)
        }

        @JvmStatic fun parseZonedDateTime(zdtText:String) : ZonedDateTime {
            val dashIdx = zdtText.indexOf('-')

            // check for positive offset
            if(dashIdx == -1) {
                val plusIdx = zdtText.indexOf('+')

                if(plusIdx==-1) {
                    throw ParseException(
                        "ZonedDateTime requires a suffix of -Z, -UTF, " +
                                "-KiTZ (e.g. -JP/JST) or an offset (e.g. +3 or -2:30)"
                    )
                } else {
                    // We have a positive offset
                    val localDT = zdtText.substring(0, plusIdx).replace("_", "")
                    val offset = zdtText.substring(plusIdx)

                    return ZonedDateTime.parse(localDT + normalizeOffset(offset),
                        ZONED_DATE_TIME_OFFSET_PARSER)
                }
            }

            val localDT = zdtText.substring(0, dashIdx).replace("_", "")
            val tz = zdtText.substring(dashIdx)

            // check for negative offset
            if(tz.length>1 && tz[1].isDigit()) {
                return ZonedDateTime.parse(localDT + normalizeOffset(tz),
                    ZONED_DATE_TIME_OFFSET_PARSER)
            // check for Z time (UTC)
            } else if (tz=="-UTC" || tz=="-GMT" || tz=="-Z") {
                return ZonedDateTime.of(LocalDateTime.parse(localDT, LOCAL_DATE_TIME_PARSER),
                    ZoneOffset.UTC)
            // check for KiTZ (Ki Time Zone Spec https://github.com/kixi-io/Ki.Docs/wiki/Ki-Time-Zone-Specification)
            } else {
                val offset = KiTZ.offsets[tz.substring(1)]
                if(offset==null) {
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
        @JvmStatic @JvmOverloads
        fun formatLocalDate(localDate: LocalDate, zeroPad:Boolean = false) : String {
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
        @JvmStatic @JvmOverloads
        fun formatLocalDateTime(localDateTime: LocalDateTime, zeroPad:Boolean = false,
                                forceNano:Boolean = false) : String {

            var dateText = formatLocalDate(localDateTime.toLocalDate(), zeroPad)

            var timeText = ""

            if(!zeroPad && !forceNano) {
                timeText = LOCAL_TIME.format(localDateTime)
            } else if(zeroPad && !forceNano) {
                timeText = LOCAL_TIME_ZERO_PAD.format(localDateTime)
                if(localDateTime.nano == 0) timeText = timeText.removeSuffix(".000000000")
            } else if(zeroPad && forceNano) {
                timeText = LOCAL_TIME_ZERO_PAD_FORCE_NANO.format(localDateTime)
            }

            return dateText + "@" + timeText;
        }

        @JvmStatic @JvmOverloads
        fun formatZonedDateTime(zonedDateTime:ZonedDateTime, zeroPad:Boolean = false,
                                forceNano:Boolean = false) : String {

            val buf = StringBuffer()
            buf.append(formatLocalDateTime(zonedDateTime.toLocalDateTime(), zeroPad, forceNano))

            var zone = zonedDateTime.zone.toString().replace("GMT", "UTF")
            if(zone == "Z")
                zone = "-Z"
            buf.append(zone)

            // remove second component of time zone offset if :00
            return buf.toString().removeSuffix(":00")
        }

        private fun normalizeOffset(offset:String) : String {

            val plusOrMinus = offset[0]
            if(plusOrMinus!='-' && plusOrMinus!='+')
                throw ParseException("First character of an offset must be + or -.")

            val text = offset.substring(1)

            val length = text.length
            if(length == 1) {
                return "${plusOrMinus}0$text:00"
            }  else if(length==2) {
                return "${plusOrMinus}$text:00"
            } else if(length==4 && text[1]==':') {
                return "${plusOrMinus}0$text"
            }

            return offset
        }

        // Parsing Duration ////

        fun parseDuration(text: String): Duration {
            // TODO - Finish day and fractional seconds for compound durations and add single
            // unit Durations.

            var parts = text.split(':')
            return Duration.ofHours(parts[0].toLong())
                .plus(Duration.ofMinutes(parts[1].toLong()))
                .plus(Duration.ofSeconds(parts[2].toLong()))
        }

        // Formatting Duration ////

        @JvmStatic @JvmOverloads
        fun formatDuration(duration: Duration, zeroPad:Boolean = false): String {
            val seconds = duration.seconds
            val absSeconds = Math.abs(seconds)
            val positive = String.format(
                "%d:%02d:%02d",
                absSeconds / 3600,
                absSeconds % 3600 / 60,
                absSeconds % 60
            )
            return if (seconds < 0) "-$positive" else positive
        }
    }
}

// TODO - Convert to tests
fun main() {

    // Duration
    log("-- Durations ----")

    var dur1 = Ki.parseDuration("1:30:00")
    var dur2 = Ki.parseDuration("0:15:00")
    var dur3 = Ki.parseDuration("10:23:53")

    log(Ki.formatDuration(dur1))
    log(Ki.formatDuration(dur2))
    log(Ki.formatDuration(dur3))

    log("-- DateTimes ----")

    // LocalDate

    var date = LocalDate.of(2020,5,2)

    log(Ki.formatLocalDate(date), " = 2020/5/2")
    log(Ki.formatLocalDate(date,true), " = 2020/05/02")
    log("--- --- ---")

    // LocalDateTime

    var localDateTime1 = Ki.parseLocalDateTime("2020/05/02@8:05")
    var localDateTime2 = Ki.parseLocalDateTime("2020/5/2@8:05:00.023")

    log(Ki.formatLocalDateTime(localDateTime1), " = 2020/5/2@8:05:00")
    log(Ki.formatLocalDateTime(localDateTime1, zeroPad = true), " = 2020/05/02@08:05:00")
    log(Ki.formatLocalDateTime(localDateTime1, zeroPad = true, forceNano = true),
        " = 2020/05/02@08:05:00.000000000")
    log()
    log(Ki.formatLocalDateTime(localDateTime2), " = 2020/5/2@8:05:00.023")
    log(Ki.formatLocalDateTime(localDateTime2, zeroPad = true), " = 2020/05/02@8:05:00.023000000")
    // Should be same as above
    log(Ki.formatLocalDateTime(localDateTime2, zeroPad = true, forceNano = true), " = 2020/05/02@8:05:00.023000000")
    log("--- --- ---")

    // ZonedDateTime

    var zonedDateTime1 = Ki.parseZonedDateTime("2020/05/02@8:05+2")
    var zonedDateTime2 = Ki.parseZonedDateTime("2020/5/2@8:05:00.023-5:30")

    log(Ki.formatZonedDateTime(zonedDateTime1), " = 2020/5/2@8:05+02")
    log(Ki.formatZonedDateTime(zonedDateTime1, zeroPad = true), " = 2020/05/02@08:05+02")
    log(Ki.formatZonedDateTime(zonedDateTime1, zeroPad = true, forceNano = true),
        " = 2020/05/02@08:05:00.000000000+02")
    log()
    log(Ki.formatZonedDateTime(zonedDateTime2), " = 2020/5/2@8:05:00.023-05:30")
    log(Ki.formatZonedDateTime(zonedDateTime2, zeroPad = true), " = 2020/05/02@8:05:00.023000000-05:30")
    // Should be same as above
    log(Ki.formatZonedDateTime(zonedDateTime2, zeroPad = true, forceNano = true), " = 2020/05/02@8:05:00.023000000-05:30")
    log("--- --- ---")
}

