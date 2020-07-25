package io.kixi.ki

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * A set of convenience methods for working with Ki types and formatting.
 */
class Ki {
    companion object {
        /**
         * The Ki standard time format HH:mm:ss.SSS/z
         *
         * Note: Ki time uses a 24 hour clock (0-23)
         *
         * Note: This is not the same as a duration. This format is used
         * for the time component of a date_time instance
         */
        const val TIME_FORMAT = "HH:mm:ss.SSS/z"

        /**
         * The Ki standard date format: yyyy/MM/dd
         */
        const val DATE_FORMAT = "yyyy/MM/dd"

        // TODO: Separate patterns for parsing and formatting (formatting should use padding as appropriate)
        // TODO: Allow legacy named zones for 10 largest time zones
        @JvmField val LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("y/M/d")
        @JvmField val LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("y/M/d@H:m:s.nnnnnnnnn")
        @JvmField val ZONED_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("y/M/d@H:m:s.nnnnnnnnnVV")

        /**
         * The Ki standard DATE_TIME format yyyy/MM/dd-HH:mm:ss.nnnnnnnnnVV
         *
         * Note: Ki uses a 24 hour clock (0-23)
         */
        const val DATE_TIME_FORMAT = DATE_FORMAT + "@" + TIME_FORMAT

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
                is ZonedDateTime -> ZONED_DATE_TIME_FORMATTER.format(obj)
                else -> obj.toString()
            }
        }
    }
}