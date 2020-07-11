package ki

import java.time.format.DateTimeFormatter

class Ki {
    companion object {
        /**
         * The Ki standard time format HH:mm:ss.SSS-z
         *
         * Note: Ki time uses a 24 hour clock (0-23)
         *
         * Note: This is not the same as a duration. This format is used
         * for the time component of a date_time instance
         */
        const val TIME_FORMAT = "HH:mm:ss.SSS-z"

        /**
         * The Ki standard date format: yyyy/MM/dd
         */
        const val DATE_FORMAT = "yyyy/MM/dd"

        // TODO: Separate patterns for parsing and formatting (formatting should use padding as appropriate)
        val LOCAL_DATE_FORMATTER = DateTimeFormatter.ofPattern("y/M/d")
        val LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("y/M/d@H:m:s.nnnnnnnnn")
        val ZONED_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("y/M/d@H:m:s.nnnnnnnnn/VV")

        /**
         * The Ki standard DATE_TIME format yyyy/MM/dd-HH:mm:ss.SSS-z
         *
         * Note: Ki uses a 24 hour clock (0-23)
         */
        const val DATE_TIME_FORMAT = DATE_FORMAT + "-" +
                TIME_FORMAT

        // TODO support all KD types
        fun format(obj: Any?): String {
            return when (obj) {
                null -> "nil"
                is String -> "\"$obj\""
                is Char -> "'$obj'"
                else -> obj.toString()
            }
        }
    }
}