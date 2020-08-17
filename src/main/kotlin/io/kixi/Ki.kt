package io.kixi

import io.kixi.text.ParseException
import io.kixi.text.escape
import java.lang.Math.abs
import java.math.BigDecimal
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField.*
import java.util.*

/**
 * A set of convenience methods for working with Ki types, parsing and formatting.
 */
class Ki {
    companion object {

        @JvmField
        val LOCAL_DATE = DateTimeFormatter.ofPattern("y/M/d")
        @JvmField
        val LOCAL_DATE_ZERO_PAD = DateTimeFormatter.ofPattern("yyyy/MM/dd")

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

        // LocalDateTime Formatters & Parsers ////

        @JvmField
        val LOCAL_DATE_TIME = DateTimeFormatterBuilder()
            .append(LOCAL_DATE)
            .appendLiteral('@')
            .append(LOCAL_TIME) // DateTimeFormatter.ofPattern("H:mm:ss.S"))
            .toFormatter()

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
         * The Ki standard DATE_TIME format y/M/d-H:mm:ss.S(offset|-KiTZ|-Z)
         *
         * Note: Ki uses a 24 hour clock (0-23)
         */

        @JvmStatic
        fun format(obj: Any?): String {
            return when (obj) {
                null -> "nil"
                is String -> "\"${obj.toString().escape()}\""
                is Char -> "'$obj'"
                is BigDecimal -> "${obj}m"
                is Float -> "${obj}f"
                is Map<*,*> -> formatMap(obj)
                is Collection<*> -> formatCollection(obj)
                is LocalDate -> formatLocalDate(obj)
                is LocalDateTime -> formatLocalDateTime(obj)
                is ZonedDateTime -> formatZonedDateTime(obj)
                is Duration -> formatDuration(obj)
                is ByteArray -> formatBlob(obj)
                else -> obj.toString()
            }
        }

        // Format and parse a Blob literal

        /**
         * Formats a Blob literal as a base64 String
         */
        fun formatBlob(obj: ByteArray): String {
            val encodedText = Base64.getEncoder().encodeToString(obj)

            if(encodedText.length>30) {
                val lines = encodedText.chunked(50)
                val builder = StringBuilder(".blob(\n")
                for(line in lines)
                    builder.append("\t$line\n")
                return "$builder)"
            } else {
                return ".blob($encodedText)"
            }
        }

        private const val BLOB_PREFIX_LENGTH = ".blob(".length
        /**
         * Parse a Blob literal in the form .blob(base64String)
         *
         * The characters in base64string may contain whitespace.
         */
        fun parseBlob(blobLiteral: String): ByteArray {
            var encString = blobLiteral.replace(Regex("\\s+"), "")

            if(encString == ".blob()")
                return byteArrayOf()
            if(encString.isBlank())
                throw ParseException("Blob literal cannot be empty.", index = 0)
            if(!encString.startsWith(".blob"))
                throw ParseException("Blob literal must start with '.blob('", index = 0)
            if(encString[encString.length-1]!=')')
                throw ParseException("Blob literal must end with ')'", index = 0)

            encString = encString.substring(BLOB_PREFIX_LENGTH, encString.length-1)

            return Base64.getDecoder().decode(encString)
        }

        private fun formatMap(map: Map<*,*>): String {
            return "[${map.toString(", ", formatter={ obj -> format(obj) })}]"
        }

        private fun formatCollection(col: Collection<*>): String {
            return "[${col.toString(formatter={ obj -> format(obj) })}]"
        }

        // Parsing DateTime ////

        @JvmStatic
        fun parseLocalDate(ldText: String): LocalDate {
            return LocalDate.parse(ldText, LOCAL_DATE)
        }

        @JvmStatic
        fun parseLocalDateTime(ldtText: String): LocalDateTime {
            return LocalDateTime.parse(ldtText.replace("_", ""),
                LOCAL_DATE_TIME_PARSER
            )
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
                val offset = KiTZ.offsets[tz.substring(1)] ?:
                    throw ParseException("Unsupported KiTZ ID: ${tz.substring(1)}")

                return ZonedDateTime.of(LocalDateTime.parse(localDT,
                    LOCAL_DATE_TIME_PARSER
                ), offset)
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

            val dateText = formatLocalDate(localDateTime.toLocalDate(), zeroPad)

            var timeText = ""

            if (!zeroPad && !forceNano) {
                timeText = LOCAL_TIME.format(localDateTime)
            } else if (zeroPad && !forceNano) {
                timeText = LOCAL_TIME_ZERO_PAD.format(localDateTime)
                if (localDateTime.nano == 0) timeText = timeText.removeSuffix(".000000000")
            } else if (zeroPad && forceNano) {
                timeText = LOCAL_TIME_ZERO_PAD_FORCE_NANO.format(localDateTime)
            }

            return "$dateText@$timeText"
        }

        @JvmStatic
        @JvmOverloads
        fun formatZonedDateTime(
            zonedDateTime: ZonedDateTime, zeroPad: Boolean = false,
            forceNano: Boolean = false
        ): String {

            val buf = StringBuilder()
            buf.append(
                formatLocalDateTime(
                    zonedDateTime.toLocalDateTime(),
                    zeroPad,
                    forceNano
                )
            )

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

        // TODO: Fix treatment of negative durations (works in formatting but not parsing)

        /**
         * Parses an individual unit duration (1day, 3days, 23h, 3s, 5ms, 12ns) or a
         * compound duration such as `12:53:21` for 12 hours, 53 mins and 21 secs, or
         * `5days:08:23:08.321` for 5 days, 8 hours, 23 mins, 8.321 secs (zero pads
         * are optional).
         */
        fun parseDuration(text: String): Duration {
            val parts = text.replace("_", "").split(':')
            val sign = if (text[0]=='-') "-" else ""

            when (parts.size) {
                4 -> {
                    // is compound with day
                    val dayIndex = parts[0].indexOf("day")
                    if (dayIndex == -1) {
                        throw ParseException(
                            "Compound duration day components must be " +
                                    "suffixed with \"day\" or \"days\"."
                        )
                    }
                    return Duration.ofDays((parts[0].substring(0, dayIndex)).toLong())
                        .plus(Duration.ofHours((sign + parts[1]).toLong()))
                        .plus(Duration.ofMinutes((sign + (parts[2])).toLong())
                            .plus(
                                if(sign=="-") Duration.ofNanos(-secStringToNanos(parts[3]))
                                else Duration.ofNanos(secStringToNanos(parts[3]))
                            ))
                }
                3 -> {
                    // is compound without day
                    return Duration.ofHours((parts[0]).toLong())
                        .plus(Duration.ofMinutes((sign + parts[1]).toLong()))
                        .plus(
                            if(sign=="-") Duration.ofNanos(-secStringToNanos(parts[2]))
                            else Duration.ofNanos(secStringToNanos(parts[2]))
                        )
                }
                1 -> {
                    // TODO: Fractional units for single unit durations.
                    return when {
                        text.endsWith("day") ->
                            return Duration.ofDays((text.removeSuffix("day")).toLong())
                        text.endsWith("days") ->
                            return Duration.ofDays((text.removeSuffix("days")).toLong())
                        text.endsWith("h") ->
                            return Duration.ofHours((text.removeSuffix("h")).toLong())
                        text.endsWith("min") ->
                            return Duration.ofMinutes((text.removeSuffix("min")).toLong())
                        // We have to check "ms" and "ns" before "s"
                        text.endsWith("ms") ->
                            return Duration.ofMillis((text.removeSuffix("ms")).toLong())
                        text.endsWith("ns") ->
                            return Duration.ofNanos((text.removeSuffix("ns")).toLong())
                        text.endsWith("s") -> {
                            // Deal with fractional seconds
                            val secText = text.removeSuffix("s")
                            return  if(sign=="-") Duration.ofNanos(-secStringToNanos(
                                secText
                            )
                            )
                            else Duration.ofNanos(secStringToNanos(secText))
                        }
                        else -> throw ParseException("Unkown temporal unit in duration.")
                    }
                }
                else -> throw ParseException(
                    """
                        Can't parse Duration \"$text\": Wrong number of segments. Durations must
                        be single unit (e.g. 5h) or compound with days:hours:minutes:seconds. Days
                        are option and must be suffixed with "day" or "days"
                        (e.g. 2days:05:30:00).
                        """.trimIndent()
                )
            }

        }

        private const val NANO_ZEROS = "000000000"

        private fun secStringToNanos(s: String): Long {

            val dotIndex = s.indexOf('.')
            if(dotIndex==-1)
                return abs(s.toLong()) * 1_000_000_000L

            //

            var nanos = abs(s.substring(0,dotIndex).toLong()) * 1_000_000_000L

            val nanoText = s.substring(dotIndex+1)
            val zeroPadding = NANO_ZEROS.substring(0, NANO_ZEROS.length - nanoText.length)

            nanos+= (nanoText + zeroPadding).toLong()

            return nanos
        }

        // Formatting Duration ////

        /**
         * Format a Duration as a single unit (e.g. 5days, 6h, 7min, 8s, 12ms, 2ns) or
         * compound format such as 1day:15:23:42.532 The day(s) component is optional
         * in compound durations.
         *
         * @param zeroPad Boolean If true zero padding is added for hours, mins and secs.
         */
        @JvmStatic
        @JvmOverloads
        fun formatDuration(duration: Duration, zeroPad: Boolean = false): String {

            var totalNanos = duration.toNanos()
            val sign = if (totalNanos < 0L) "-" else ""
            totalNanos = abs(totalNanos)

            val nanosOfSec = totalNanos % 1_000_000_000L

            val fractionalSec = if(nanosOfSec==0L) ""
            else "." + trimTrailing0s(String.format("%09d", nanosOfSec))

            // This is required due to a bug in java.time.Duration that incorrectly
            // subtracts 1 from negative durations seconds that have nanosecond
            // components.
            var secs = abs(duration.toSecondsPart())
            if(sign=="-" && nanosOfSec>0) {
                secs--
            }

            // Single unit Durations: day(s), h, m, s, ms, ns

            // Check for ns single unit
            if (totalNanos < 1_000_000L) {
                return "$sign${totalNanos}ns"

            // Check for ms single unit
            } else if (totalNanos < 1_000_000_000L) {
                return "$sign${totalNanos / 1_000_000L}ms"

            // Check for seconds (including fractional) single unit
            } else if (totalNanos < 60_000_000_000L) {
                // val secs = abs(secs)
                return "${sign}${secs}${fractionalSec}s"
            }

            val days = abs(duration.toDaysPart())
            val hrs = abs(duration.toHoursPart())
            val mins = abs(duration.toMinutesPart())


            // Compound Durations (day:)h/min/s.S ////

            if (days != 0L) {
                if (all0(hrs, mins, secs) && fractionalSec.isEmpty())
                    return "$sign${days}${if (days == 1L) "day" else "days"}"

                return if (zeroPad) "$sign${days}${if (days == 1L) "day" else "days"}:" +
                          "${padL2(hrs)}:${padL2(
                              mins
                          )}:${padL2(secs)}${fractionalSec}"
                    else "$sign${days}${if (days == 1L) "day" else "days"}:$hrs:$mins:$secs${fractionalSec}"
            } else if (hrs != 0) {
                if (all0(days, mins, secs) && fractionalSec.isEmpty())
                    return "$sign${hrs}h"

                // return "$sign$hrs:$mins:$secs${fractionalSec}"
                return if (zeroPad) "$sign${padL2(hrs)}:${padL2(
                    mins
                )}:${padL2(secs)}${fractionalSec}"
                        else "$sign$hrs:$mins:$secs${fractionalSec}"
            } else if (mins != 0) {
                if (all0(days, hrs, secs) && fractionalSec.isEmpty())
                    return "$sign${mins}min"

                return if (zeroPad) "$sign${padL2(hrs)}:${padL2(
                    mins
                )}:${padL2(secs)}${fractionalSec}"
                    else "$sign$hrs:$mins:$secs${fractionalSec}"
            } else if (secs != 0 && fractionalSec.isEmpty()) {
                if (all0(days, hrs, mins))
                    return "$sign${secs}s"

                return if (zeroPad) "$sign${padL2(hrs)}:${padL2(
                    mins
                )}:${padL2(secs)}${fractionalSec}"
                    else "$sign$hrs:$mins:$secs${fractionalSec}"
            }

            return "0:0:0"
        }

        private fun padL2(num:Any) : String {
            return String.format("%02d", num)
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
    }
}

