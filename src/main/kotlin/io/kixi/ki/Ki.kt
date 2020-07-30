package io.kixi.ki

import io.kixi.ki.text.ParseException
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
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

        /**
         * The Ki local time format H:mm:s.S
         * The Ki zoned time format is H:mm:s.S-z
         *
         * "z" is a zone, which may be:
         * 1. Z (Zulu time = UTC+00:00)
         * 2. UTC or GMT (same as above)
         * 3. An offset from Z such as +4 or -2:30
         * 4. A KiTZ (See https://github.com/kixi-io/Ki.Docs/wiki/Ki-Time-Zone-Specification)
         *
         * Note: Ki time uses a 24 hour clock (0-23)
         *
         * Note: This is not the same as a duration. This format is used
         * for the time component of a date_time instance
         */
        // val TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm:s.S-z")

        @JvmField val LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("y/M/d")

        //-----------------------------------------------------------------------
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


        @JvmField val LOCAL_DATE_TIME_FORMATTER = DateTimeFormatterBuilder()
            .append(LOCAL_DATE_FORMATTER)
            .appendLiteral('@')
            .append(LOCAL_TIME) // DateTimeFormatter.ofPattern("H:mm:s.S"))
            .toFormatter() // TIME_FORMATTER) // DateTimeFormatter.ISO_LOCAL_TIME)

        @JvmField val ZONED_DATE_TIME_OFFSET_FORMATTER = DateTimeFormatterBuilder()
            .append(LOCAL_DATE_TIME_FORMATTER)
            // .appendPattern("'-'")
            .appendOffsetId()
            .toFormatter()

        /**
         * The Ki standard DATE_TIME format yyyy/MM/dd-HH:mm:ss.nnnnnnnnnVV
         *
         * Note: Ki uses a 24 hour clock (0-23)
         */
        // const val DATE_TIME_FORMAT = DATE_FORMAT + "@" + TIME_FORMAT

        // TODO: Durations (others?)
        @JvmStatic fun format(obj: Any?): String {
            return when (obj) {
                null -> "nil"
                is String -> "\"$obj\""
                is Char -> "'$obj'"
                is BigDecimal -> "${obj}m"
                is Float -> "${obj}f"
                is LocalDate -> LOCAL_DATE_FORMATTER.format(obj)
                is LocalDateTime -> LOCAL_DATE_TIME_FORMATTER.format(obj)
                is ZonedDateTime -> Ki.formatZonedDateTime(obj)
                else -> obj.toString()
            }
        }

        @JvmStatic fun parseLocalDateTime(ldtText:String) : LocalDateTime {
            return LocalDateTime.parse(ldtText.replace("_", ""), LOCAL_DATE_TIME_FORMATTER)
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
                        ZONED_DATE_TIME_OFFSET_FORMATTER)
                }
            }

            val localDT = zdtText.substring(0, dashIdx).replace("_", "")
            val tz = zdtText.substring(dashIdx)

            // check for negative offset
            if(tz.length>1 && tz[1].isDigit()) {
                return ZonedDateTime.parse(localDT + normalizeOffset(tz),
                    ZONED_DATE_TIME_OFFSET_FORMATTER)
            // check for Z time (UTC)
            } else if (tz=="-UTC" || tz=="-GMT" || tz=="-Z") {
                return ZonedDateTime.of(LocalDateTime.parse(localDT, LOCAL_DATE_TIME_FORMATTER),
                    ZoneOffset.UTC)
            // check for KiTZ (Ki Time Zone Spec https://github.com/kixi-io/Ki.Docs/wiki/Ki-Time-Zone-Specification)
            } else {
                val offset = KiTZ.offsets[tz.substring(1)]
                if(offset==null) {
                    throw ParseException("Unsupported KiTZ ID: ${tz.substring(1)}")
                }

                return ZonedDateTime.of(LocalDateTime.parse(localDT, LOCAL_DATE_TIME_FORMATTER), offset)
            }
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

        // TODO - Remove leading zero in offsets
        @JvmStatic fun formatZonedDateTime(zonedDateTime:ZonedDateTime) : String {
            val buf = StringBuffer()
            buf.append(LOCAL_DATE_TIME_FORMATTER.format(zonedDateTime))

            var zone = zonedDateTime.zone.toString().replace("GMT", "UTF")
            if(zone == "Z")
                zone = "-Z"
            buf.append(zone)

            return buf.toString().removeSuffix(":00")
        }
    }
}

// TODO - Convert to tests
fun main() {
    val localDateTime = "2019/07/16@13:29:15"
    var timestamp = LocalDateTime.parse(localDateTime,
        Ki.LOCAL_DATE_TIME_FORMATTER)
    System.out.println(localDateTime.format(timestamp))

    val localDateTimeWithFractSecs = "2019/7/16@13:29:15.1112223"
    timestamp = LocalDateTime.parse(localDateTimeWithFractSecs,
        Ki.LOCAL_DATE_TIME_FORMATTER)
    System.out.println(localDateTimeWithFractSecs.format(timestamp))

    // ZonedDateTime

    var zonedDateTime = "2019/07/16@13:29:15.1112-UTC"
    var zonedTimestamp = Ki.parseZonedDateTime(zonedDateTime)
    System.out.println(Ki.formatZonedDateTime(zonedTimestamp))

    zonedDateTime = "2019/07/16@13:29:15.1112-GMT"
    zonedTimestamp = Ki.parseZonedDateTime(zonedDateTime)
    System.out.println(Ki.formatZonedDateTime(zonedTimestamp))

    zonedDateTime = "2019/07/16@13:29:15.1112-Z"
    zonedTimestamp = Ki.parseZonedDateTime(zonedDateTime)
    System.out.println(Ki.formatZonedDateTime(zonedTimestamp))

    zonedDateTime = "2019/07/16@13:29:15.1112+3"
    zonedTimestamp = Ki.parseZonedDateTime(zonedDateTime)
    System.out.println(Ki.formatZonedDateTime(zonedTimestamp))

    zonedDateTime = "2019/07/16@13:29:15.1112-3"
    zonedTimestamp = Ki.parseZonedDateTime(zonedDateTime)
    System.out.println(Ki.formatZonedDateTime(zonedTimestamp))

    zonedDateTime = "2019/07/16@13:29:15.1112+04"
    zonedTimestamp = Ki.parseZonedDateTime(zonedDateTime)
    System.out.println(Ki.formatZonedDateTime(zonedTimestamp))

    zonedDateTime = "2019/07/16@13:29:15.1112-5:30"
    zonedTimestamp = Ki.parseZonedDateTime(zonedDateTime)
    System.out.println(Ki.formatZonedDateTime(zonedTimestamp))

    zonedDateTime = "2019/07/16@13:29:15.1112+11"
    zonedTimestamp = Ki.parseZonedDateTime(zonedDateTime)
    System.out.println(Ki.formatZonedDateTime(zonedTimestamp))

    zonedDateTime = "2019/07/6@09:00-1"
    zonedTimestamp = Ki.parseZonedDateTime(zonedDateTime)
    System.out.println(Ki.formatZonedDateTime(zonedTimestamp))

    zonedDateTime = "2019/07/16@9:30-JP/JST"
    zonedTimestamp = Ki.parseZonedDateTime(zonedDateTime)
    System.out.println(Ki.formatZonedDateTime(zonedTimestamp))

    zonedDateTime = "2019/07/16@10:05:24.525-IN/IST"
    zonedTimestamp = Ki.parseZonedDateTime(zonedDateTime)
    System.out.println(Ki.formatZonedDateTime(zonedTimestamp))

    zonedDateTime = "2019/07/16@10:05:24.525_563_643-IN/IST"
    zonedTimestamp = Ki.parseZonedDateTime(zonedDateTime)
    System.out.println(Ki.formatZonedDateTime(zonedTimestamp))
}

