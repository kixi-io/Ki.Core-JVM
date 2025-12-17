package io.kixi

import io.kixi.text.ParseException
import io.kixi.text.escape
import io.kixi.text.resolveEscapes
import io.kixi.uom.Quantity
import java.lang.Math.abs
import java.math.BigDecimal as Dec
import java.net.URL
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField.*
import java.util.*

/**
 * A set of constants, enums and convenience methods for working with the Ki Type system
 * and related core functionality (formatting and parsing KTS literals, etc.)
 */
@Suppress("UNCHECKED_CAST", "unused")
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
            .append(LOCAL_TIME)
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
            .appendOffsetId()
            .toFormatter()!!

        @JvmField
        val ZONED_DATE_TIME_OFFSET_PARSER = DateTimeFormatterBuilder()
            .append(LOCAL_DATE_TIME_PARSER)
            .appendOffsetId()
            .toFormatter()!!

        /**
         * Format an object using its Ki canonical form. For example, a String will be
         * given quotes. Its newlines, carriage returns, tabs and backslashes
         * will be escaped. DateTime and Durations will use
         * [their canonical Ki form](https://github.com/kixi-io/Ki.Docs/wiki/Ki-Types#Date).
         */
        @JvmStatic
        fun format(obj: Any?): String {
            return when (obj) {
                null -> "nil"
                is String -> "\"${obj.escape()}\""
                is Char -> "'$obj'"
                is Dec -> "${obj.stripTrailingZeros().toPlainString()}bd"
                is Float -> "${obj}f"
                is Long -> "${obj}L"
                is Map<*, *> -> formatMap(obj)
                is Collection<*> -> formatCollection(obj)
                is Array<*> -> formatArray(obj)
                is LocalDate -> formatLocalDate(obj)
                is LocalDateTime -> formatLocalDateTime(obj)
                is KiTZDateTime -> obj.kiFormat()
                is ZonedDateTime -> formatZonedDateTime(obj)
                is Duration -> formatDuration(obj)
                is Blob -> obj.toString()
                is GeoPoint -> obj.toString()
                is URL -> "<$obj>"
                is Version -> obj.toString()
                is Range<*> -> obj.toString()
                is Quantity<*> -> obj.toString()
                else -> obj.toString()
            }
        }

        /**
         * Get the Ki Type of a value.
         *
         * @param value Any? The value to check
         * @return Type The Ki Type, or Type.nil for null
         */
        @JvmStatic
        fun typeOf(value: Any?): Type = Type.typeOf(value) ?: Type.nil

        /**
         * Parse a Ki Type literal string and return the appropriate typed value.
         *
         * Supported literals:
         * - nil, null -> null
         * - true, false -> Boolean
         * - Integers (with optional L suffix for Long)
         * - Decimals (with optional f/F, d/D, or bd/BD suffix)
         * - Strings (quoted with " or ')
         * - Chars (single character in single quotes)
         * - URLs (enclosed in < >)
         * - Dates (y/M/d format)
         * - DateTimes (y/M/d@H:mm:ss format)
         * - Durations (compound or single unit)
         * - Versions (x.y.z format)
         * - Blobs (.blob(...))
         * - GeoPoints (.geo(lat, lon) or .geo(lat, lon, alt))
         * - Quantities (number + unit symbol)
         *
         * @param text The literal text to parse
         * @return Any? The parsed value
         * @throws ParseException if the text cannot be parsed
         */
        @JvmStatic
        fun parse(text: String): Any? {
            val trimmed = text.trim()

            if (trimmed.isEmpty()) {
                throw ParseException("Cannot parse empty string")
            }

            // nil / null
            if (trimmed == "nil" || trimmed == "null") return null

            // Boolean
            if (trimmed == "true") return true
            if (trimmed == "false") return false

            val firstChar = trimmed[0]
            val lastChar = trimmed[trimmed.length - 1]

            // String (double quoted)
            if (firstChar == '"' && lastChar == '"' && trimmed.length >= 2) {
                return trimmed.substring(1, trimmed.length - 1).resolveEscapes('"')
            }

            // Char (single quoted, single character)
            if (firstChar == '\'' && lastChar == '\'' && trimmed.length == 3) {
                return trimmed[1]
            }

            // URL
            if (firstChar == '<' && lastChar == '>') {
                return URL(trimmed.substring(1, trimmed.length - 1))
            }

            // Blob
            if (trimmed.startsWith(".blob(")) {
                return parseBlob(trimmed)
            }

            // GeoPoint
            if (trimmed.startsWith(".geo(")) {
                return GeoPoint.parse(trimmed)
            }

            // Version (x.y.z pattern, no @ symbol, contains dots)
            if (trimmed.contains('.') && !trimmed.contains('@') &&
                trimmed[0].isDigit() && !trimmed.any { it.isLetter() && it !in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-" }
            ) {
                try {
                    // Check if it looks like a version (digits and dots, optional qualifier)
                    val parts = trimmed.split('.')
                    if (parts.isNotEmpty() && parts[0].all { it.isDigit() }) {
                        return Version.parse(trimmed)
                    }
                } catch (e: Exception) {
                    // Not a version, continue
                }
            }

            // DateTime (contains @)
            if (trimmed.contains('@')) {
                return if (trimmed.contains('-') || trimmed.contains('+')) {
                    parseZonedDateTime(trimmed)
                } else {
                    parseLocalDateTime(trimmed)
                }
            }

            // Date (y/M/d format, no @)
            if (trimmed.contains('/') && !trimmed.contains('@') && !trimmed.contains(':')) {
                return parseLocalDate(trimmed)
            }

            // Duration (contains : or ends with time unit)
            if (trimmed.contains(':') ||
                trimmed.endsWith("day") || trimmed.endsWith("days") ||
                trimmed.endsWith("h") || trimmed.endsWith("min") ||
                trimmed.endsWith("s") || trimmed.endsWith("ms") || trimmed.endsWith("ns")
            ) {
                return parseDuration(trimmed)
            }

            // Numbers
            if (firstChar.isDigit() || firstChar == '-' || firstChar == '+') {
                return parseNumber(trimmed)
            }

            throw ParseException("Cannot parse literal: $text")
        }

        /**
         * Try to parse a Ki literal, returning null on failure instead of throwing.
         */
        @JvmStatic
        fun parseOrNull(text: String): Any? {
            return try {
                parse(text)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Parse a number literal (Int, Long, Float, Double, Dec, or Quantity)
         */
        private fun parseNumber(text: String): Any {
            val cleaned = text.replace("_", "")

            // Check for type suffix
            return when {
                cleaned.endsWith("bd", ignoreCase = true) ->
                    Dec(cleaned.dropLast(2))
                cleaned.endsWith("f", ignoreCase = true) ->
                    cleaned.dropLast(1).toFloat()
                cleaned.endsWith("d", ignoreCase = true) ->
                    cleaned.dropLast(1).toDouble()
                cleaned.endsWith("L") ->
                    cleaned.dropLast(1).toLong()

                // Check for quantity (number followed by unit letters)
                cleaned.any { it.isLetter() } -> {
                    Quantity.parse(cleaned)
                }

                // Check for decimal point
                cleaned.contains('.') ->
                    cleaned.toDouble()

                // Integer - use Long if too large for Int
                else -> {
                    val longVal = cleaned.toLong()
                    if (longVal in Int.MIN_VALUE..Int.MAX_VALUE) {
                        longVal.toInt()
                    } else {
                        longVal
                    }
                }
            }
        }

        // Format and parse a Blob literal ////

        /**
         * Formats a Blob as a Ki literal string.
         *
         * @param blob The Blob to format
         * @return The Ki literal representation (e.g., `.blob(SGVsbG8=)`)
         * @see Blob.toString
         */
        @JvmStatic
        fun formatBlob(blob: Blob): String = blob.toString()

        /**
         * Formats a ByteArray as a Ki Blob literal string.
         *
         * @param bytes The byte array to format
         * @return The Ki literal representation (e.g., `.blob(SGVsbG8=)`)
         */
        @JvmStatic
        fun formatBlob(bytes: ByteArray): String = Blob.of(bytes).toString()

        /**
         * Parse a Blob literal in the form `.blob(base64String)`.
         *
         * Supports both standard Base64 (`+/`) and URL-safe Base64 (`-_`).
         * Whitespace within the literal is ignored.
         *
         * @param blobLiteral The Ki blob literal string
         * @return The parsed Blob
         * @throws ParseException if the literal is malformed or contains invalid Base64
         * @see Blob.parseLiteral
         */
        @JvmStatic
        fun parseBlob(blobLiteral: String): Blob = Blob.parseLiteral(blobLiteral)

        /**
         * Parse a GeoPoint literal in the form `.geo(lat, lon)` or `.geo(lat, lon, alt)`.
         *
         * @param geoLiteral The Ki geo literal string
         * @return The parsed GeoPoint
         * @throws ParseException if the literal is malformed or coordinates are invalid
         * @see GeoPoint.parse
         */
        @JvmStatic
        fun parseGeoPoint(geoLiteral: String): GeoPoint = GeoPoint.parse(geoLiteral)

        private fun formatMap(map: Map<*, *>): String {
            return "[${map.toString(", ", formatter = { obj -> format(obj) })}]"
        }

        private fun formatCollection(col: Collection<*>): String {
            return "[${col.toString(formatter = { obj -> format(obj) })}]"
        }

        private fun formatArray(arr: Array<*>): String {
            return "[${arr.joinToString(", ") { format(it) }}]"
        }

        // Parsing DateTime ////

        @JvmStatic
        fun parseLocalDate(ldText: String): LocalDate {
            return LocalDate.parse(ldText.replace("_", ""), LOCAL_DATE)
        }

        @JvmStatic
        fun parseLocalDateTime(ldtText: String): LocalDateTime {
            return LocalDateTime.parse(
                ldtText.replace("_", ""),
                LOCAL_DATE_TIME_PARSER
            )
        }

        @JvmStatic
        fun parseZonedDateTime(zdtText: String): ZonedDateTime {
            val dashIdx = zdtText.lastIndexOf('-')

            // check for positive offset
            if (dashIdx == -1) {
                val plusIdx = zdtText.indexOf('+')

                if (plusIdx == -1) {
                    throw ParseException(
                        "ZonedDateTime requires a suffix of -Z, -UTC, " +
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
                // check for KiTZ
            } else {
                val kitzId = tz.substring(1)
                val kiTZ = KiTZ[kitzId]
                    ?: throw ParseException("Unsupported KiTZ ID: $kitzId")

                return ZonedDateTime.of(
                    LocalDateTime.parse(localDT, LOCAL_DATE_TIME_PARSER),
                    kiTZ.offset
                )
            }
        }

        /**
         * Parse a Ki datetime literal with a KiTZ suffix, returning a [KiTZDateTime]
         * that preserves the timezone identity.
         *
         * ```kotlin
         * val dt = Ki.parseKiTZDateTime("2024/3/15@14:30:00-US/PST")
         * println(dt.kiTZ.country)  // "United States"
         * ```
         *
         * @param text The Ki datetime literal
         * @return The parsed KiTZDateTime
         * @throws ParseException if the text is malformed or the KiTZ is invalid
         */
        @JvmStatic
        fun parseKiTZDateTime(text: String): KiTZDateTime = KiTZDateTime.parse(text)

        // Formatting DateTime ////

        /**
         * Format a LocalDate using Ki standard formatting: `y/M/d` or `yyyy/MM/dd` if
         * `zeroPad=true`.
         */
        @JvmStatic
        @JvmOverloads
        fun formatLocalDate(localDate: LocalDate, zeroPad: Boolean = false): String {
            return if (zeroPad) LOCAL_DATE_ZERO_PAD.format(localDate)
            else LOCAL_DATE.format(localDate)
        }

        /**
         * Format a LocalDateTime using Ki standard formatting.
         */
        @JvmStatic
        @JvmOverloads
        fun formatLocalDateTime(
            localDateTime: LocalDateTime,
            zeroPad: Boolean = false,
            forceNano: Boolean = false
        ): String {
            val dateText = formatLocalDate(localDateTime.toLocalDate(), zeroPad)

            val timeText = when {
                !zeroPad && !forceNano -> LOCAL_TIME.format(localDateTime)
                zeroPad && !forceNano -> {
                    val formatted = LOCAL_TIME_ZERO_PAD.format(localDateTime)
                    if (localDateTime.nano == 0) formatted.removeSuffix(".000000000")
                    else formatted
                }
                zeroPad && forceNano -> LOCAL_TIME_ZERO_PAD_FORCE_NANO.format(localDateTime)
                else -> LOCAL_TIME.format(localDateTime)
            }

            return "$dateText@$timeText"
        }

        /**
         * Format a ZonedDateTime using Ki standard formatting with offset notation.
         *
         * For KiTZ-based formatting, use [formatZonedDateTimeKiTZ].
         */
        @JvmStatic
        @JvmOverloads
        fun formatZonedDateTime(
            zonedDateTime: ZonedDateTime,
            zeroPad: Boolean = false,
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

            var zone = zonedDateTime.zone.toString().replace("GMT", "UTC")
            if (zone == "Z") zone = "-Z"
            buf.append(zone)

            // remove second component of time zone offset if :00
            return buf.toString().removeSuffix(":00")
        }

        /**
         * Format a ZonedDateTime using KiTZ timezone identifiers.
         *
         * If the offset has a KiTZ mapping, it will be used (e.g., `-US/PST`).
         * Otherwise, falls back to standard offset notation (e.g., `-08:00`).
         *
         * ## Example
         * ```kotlin
         * val zdt = ZonedDateTime.of(2024, 3, 15, 14, 30, 0, 0, ZoneOffset.ofHours(-8))
         * Ki.formatZonedDateTimeKiTZ(zdt)  // "2024/3/15@14:30:00-US/PST"
         *
         * val jst = ZonedDateTime.of(2024, 3, 15, 9, 0, 0, 0, ZoneOffset.ofHours(9))
         * Ki.formatZonedDateTimeKiTZ(jst)  // "2024/3/15@9:00:00-JP/JST"
         * ```
         *
         * @param zonedDateTime The ZonedDateTime to format
         * @param zeroPad Whether to zero-pad date/time components
         * @param forceNano Whether to always include nanoseconds
         * @param preferredKiTZ Optional KiTZ to use (overrides automatic detection)
         * @return The formatted string with KiTZ timezone or offset fallback
         */
        @JvmStatic
        @JvmOverloads
        fun formatZonedDateTimeKiTZ(
            zonedDateTime: ZonedDateTime,
            zeroPad: Boolean = false,
            forceNano: Boolean = false,
            preferredKiTZ: KiTZ? = null
        ): String {
            val buf = StringBuilder()
            buf.append(
                formatLocalDateTime(
                    zonedDateTime.toLocalDateTime(),
                    zeroPad,
                    forceNano
                )
            )

            val offset = zonedDateTime.offset

            // Handle UTC specially
            if (offset == ZoneOffset.UTC && preferredKiTZ == null) {
                buf.append("-Z")
                return buf.toString()
            }

            // Use preferred KiTZ or find one from offset
            val kiTZ = preferredKiTZ ?: KiTZ.fromOffset(offset)

            if (kiTZ != null) {
                buf.append("-")
                buf.append(kiTZ.id)
            } else {
                // Fall back to offset notation
                val zone = offset.toString()
                buf.append(zone)
                // remove second component of time zone offset if :00
                return buf.toString().removeSuffix(":00")
            }

            return buf.toString()
        }

        /**
         * Format a KiTZDateTime using Ki standard formatting.
         */
        @JvmStatic
        @JvmOverloads
        fun formatKiTZDateTime(
            kiTZDateTime: KiTZDateTime,
            zeroPad: Boolean = false,
            forceNano: Boolean = false
        ): String = kiTZDateTime.kiFormat(zeroPad, forceNano)

        // Creating ZonedDateTime with KiTZ ////

        /**
         * Creates a ZonedDateTime from date/time components and a KiTZ.
         *
         * ## Example
         * ```kotlin
         * val pst = Ki.zonedDateTime(2024, 3, 15, 14, 30, 0, KiTZ.US_PST)
         * // 2024/3/15@14:30:00-US/PST
         * ```
         *
         * @param year The year
         * @param month The month (1-12)
         * @param dayOfMonth The day of month (1-31)
         * @param hour The hour (0-23)
         * @param minute The minute (0-59)
         * @param second The second (0-59)
         * @param kiTZ The KiTZ timezone
         * @return A ZonedDateTime at the specified time in the KiTZ timezone
         */
        @JvmStatic
        fun zonedDateTime(
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hour: Int,
            minute: Int,
            second: Int,
            kiTZ: KiTZ
        ): ZonedDateTime = zonedDateTime(year, month, dayOfMonth, hour, minute, second, 0, kiTZ)

        /**
         * Creates a ZonedDateTime from date/time components (including nanoseconds) and a KiTZ.
         */
        @JvmStatic
        fun zonedDateTime(
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hour: Int,
            minute: Int,
            second: Int,
            nanoOfSecond: Int,
            kiTZ: KiTZ
        ): ZonedDateTime = ZonedDateTime.of(
            year, month, dayOfMonth,
            hour, minute, second, nanoOfSecond,
            kiTZ.offset
        )

        /**
         * Creates a ZonedDateTime from a LocalDateTime and a KiTZ.
         */
        @JvmStatic
        fun zonedDateTime(localDateTime: LocalDateTime, kiTZ: KiTZ): ZonedDateTime =
            ZonedDateTime.of(localDateTime, kiTZ.offset)

        /**
         * Creates a ZonedDateTime from a LocalDate, LocalTime, and a KiTZ.
         */
        @JvmStatic
        fun zonedDateTime(localDate: LocalDate, localTime: LocalTime, kiTZ: KiTZ): ZonedDateTime =
            ZonedDateTime.of(localDate, localTime, kiTZ.offset)

        /**
         * Creates a ZonedDateTime for a specific date at midnight in a KiTZ timezone.
         */
        @JvmStatic
        fun zonedDateAtStartOfDay(
            year: Int,
            month: Int,
            dayOfMonth: Int,
            kiTZ: KiTZ
        ): ZonedDateTime = ZonedDateTime.of(
            LocalDate.of(year, month, dayOfMonth),
            LocalTime.MIDNIGHT,
            kiTZ.offset
        )

        /**
         * Creates a ZonedDateTime for a LocalDate at midnight in a KiTZ timezone.
         */
        @JvmStatic
        fun zonedDateAtStartOfDay(localDate: LocalDate, kiTZ: KiTZ): ZonedDateTime =
            ZonedDateTime.of(localDate, LocalTime.MIDNIGHT, kiTZ.offset)

        /**
         * Gets the current instant as a ZonedDateTime in the specified KiTZ timezone.
         *
         * ## Example
         * ```kotlin
         * val nowInTokyo = Ki.nowInKiTZ(KiTZ.JP_JST)
         * val nowInLA = Ki.nowInKiTZ(KiTZ.US_PST)
         * ```
         *
         * @param kiTZ The KiTZ timezone
         * @return The current date/time in the specified timezone
         */
        @JvmStatic
        fun nowInKiTZ(kiTZ: KiTZ): ZonedDateTime = ZonedDateTime.now(kiTZ.offset)

        /**
         * Converts a ZonedDateTime to a different KiTZ timezone.
         *
         * This adjusts both the offset and the local date/time so that the instant
         * remains the same.
         *
         * ## Example
         * ```kotlin
         * val pst = Ki.zonedDateTime(2024, 3, 15, 14, 30, 0, KiTZ.US_PST)
         * val jst = Ki.convertToKiTZ(pst, KiTZ.JP_JST)
         * // Same instant, but displayed as 2024/3/16@7:30:00-JP/JST
         * ```
         *
         * @param zonedDateTime The source ZonedDateTime
         * @param kiTZ The target KiTZ timezone
         * @return A new ZonedDateTime representing the same instant in the target timezone
         */
        @JvmStatic
        fun convertToKiTZ(zonedDateTime: ZonedDateTime, kiTZ: KiTZ): ZonedDateTime =
            zonedDateTime.withZoneSameInstant(kiTZ.offset)

        // Creating KiTZDateTime ////

        /**
         * Creates a KiTZDateTime from date/time components and a KiTZ.
         *
         * ## Example
         * ```kotlin
         * val dt = Ki.kiTZDateTime(2024, 3, 15, 14, 30, 0, KiTZ.US_PST)
         * println(dt)  // 2024/3/15@14:30:00-US/PST
         * println(dt.kiTZ.country)  // United States
         * ```
         */
        @JvmStatic
        fun kiTZDateTime(
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hour: Int,
            minute: Int,
            second: Int,
            kiTZ: KiTZ
        ): KiTZDateTime = KiTZDateTime(year, month, dayOfMonth, hour, minute, second, kiTZ)

        /**
         * Creates a KiTZDateTime from date/time components (including nanoseconds) and a KiTZ.
         */
        @JvmStatic
        fun kiTZDateTime(
            year: Int,
            month: Int,
            dayOfMonth: Int,
            hour: Int,
            minute: Int,
            second: Int,
            nanoOfSecond: Int,
            kiTZ: KiTZ
        ): KiTZDateTime = KiTZDateTime(year, month, dayOfMonth, hour, minute, second, nanoOfSecond, kiTZ)

        /**
         * Creates a KiTZDateTime from a LocalDateTime and a KiTZ.
         */
        @JvmStatic
        fun kiTZDateTime(localDateTime: LocalDateTime, kiTZ: KiTZ): KiTZDateTime =
            KiTZDateTime(localDateTime, kiTZ)

        /**
         * Creates a KiTZDateTime from a LocalDate, LocalTime, and a KiTZ.
         */
        @JvmStatic
        fun kiTZDateTime(localDate: LocalDate, localTime: LocalTime, kiTZ: KiTZ): KiTZDateTime =
            KiTZDateTime(localDate, localTime, kiTZ)

        /**
         * Creates a KiTZDateTime for a specific date at midnight in a KiTZ timezone.
         */
        @JvmStatic
        fun kiTZDateAtStartOfDay(
            year: Int,
            month: Int,
            dayOfMonth: Int,
            kiTZ: KiTZ
        ): KiTZDateTime = KiTZDateTime.atStartOfDay(year, month, dayOfMonth, kiTZ)

        /**
         * Creates a KiTZDateTime for a LocalDate at midnight in a KiTZ timezone.
         */
        @JvmStatic
        fun kiTZDateAtStartOfDay(localDate: LocalDate, kiTZ: KiTZ): KiTZDateTime =
            KiTZDateTime.atStartOfDay(localDate, kiTZ)

        /**
         * Gets the current instant as a KiTZDateTime in the specified timezone.
         *
         * ## Example
         * ```kotlin
         * val nowInTokyo = Ki.nowAsKiTZDateTime(KiTZ.JP_JST)
         * val nowInLA = Ki.nowAsKiTZDateTime(KiTZ.US_PST)
         * ```
         */
        @JvmStatic
        fun nowAsKiTZDateTime(kiTZ: KiTZ): KiTZDateTime = KiTZDateTime.now(kiTZ)

        private fun normalizeOffset(offset: String): String {
            val plusOrMinus = offset[0]
            if (plusOrMinus != '-' && plusOrMinus != '+')
                throw ParseException("First character of an offset must be + or -.")

            val text = offset.substring(1)

            return when (text.length) {
                1 -> "${plusOrMinus}0$text:00"
                2 -> "${plusOrMinus}$text:00"
                4 -> if (text[1] == ':') "${plusOrMinus}0$text" else offset
                else -> offset
            }
        }

        // Parsing Duration ////

        /**
         * Parses an individual unit duration (1day, 3days, 23h, 3s, 5ms, 12ns) or a
         * compound duration such as `12:53:21` for 12 hours, 53 mins and 21 secs.
         */
        @JvmStatic
        fun parseDuration(text: String): Duration {
            val parts = text.replace("_", "").split(':')
            val sign = if (text[0] == '-') "-" else ""

            return when (parts.size) {
                4 -> {
                    // is compound with day
                    val dayIndex = parts[0].indexOf("day")
                    if (dayIndex == -1) {
                        throw ParseException(
                            "Compound duration day components must be " +
                                    "suffixed with \"day\" or \"days\"."
                        )
                    }
                    Duration.ofDays((parts[0].substring(0, dayIndex)).toLong())
                        .plus(Duration.ofHours((sign + parts[1]).toLong()))
                        .plus(
                            Duration.ofMinutes((sign + parts[2]).toLong())
                                .plus(
                                    if (sign == "-") Duration.ofNanos(-secStringToNanos(parts[3]))
                                    else Duration.ofNanos(secStringToNanos(parts[3]))
                                )
                        )
                }
                3 -> {
                    // is compound without day
                    Duration.ofHours(parts[0].toLong())
                        .plus(Duration.ofMinutes((sign + parts[1]).toLong()))
                        .plus(
                            if (sign == "-") Duration.ofNanos(-secStringToNanos(parts[2]))
                            else Duration.ofNanos(secStringToNanos(parts[2]))
                        )
                }
                1 -> {
                    when {
                        text.endsWith("days") ->
                            Duration.ofDays(text.removeSuffix("days").toLong())
                        text.endsWith("day") ->
                            Duration.ofDays(text.removeSuffix("day").toLong())
                        text.endsWith("h") ->
                            Duration.ofHours(text.removeSuffix("h").toLong())
                        text.endsWith("min") ->
                            Duration.ofMinutes(text.removeSuffix("min").toLong())
                        text.endsWith("ms") ->
                            Duration.ofMillis(text.removeSuffix("ms").toLong())
                        text.endsWith("ns") ->
                            Duration.ofNanos(text.removeSuffix("ns").toLong())
                        text.endsWith("s") -> {
                            val secText = text.removeSuffix("s")
                            if (sign == "-") Duration.ofNanos(-secStringToNanos(secText))
                            else Duration.ofNanos(secStringToNanos(secText))
                        }
                        else -> throw ParseException("Unknown temporal unit in duration.")
                    }
                }
                else -> throw ParseException(
                    "Can't parse Duration \"$text\": Wrong number of segments."
                )
            }
        }

        private const val NANO_ZEROS = "000000000"

        private fun secStringToNanos(s: String): Long {
            val dotIndex = s.indexOf('.')
            if (dotIndex == -1)
                return abs(s.toLong()) * 1_000_000_000L

            var nanos = abs(s.substring(0, dotIndex).toLong()) * 1_000_000_000L
            val nanoText = s.substring(dotIndex + 1)
            val zeroPadding = NANO_ZEROS.substring(0, NANO_ZEROS.length - nanoText.length)
            nanos += (nanoText + zeroPadding).toLong()

            return nanos
        }

        // Formatting Duration ////

        /**
         * Format a Duration as a single unit (e.g. 5days, 6h, 7min, 8s, 12ms, 2ns) or
         * compound format such as 1day:15:23:42.532
         */
        @JvmStatic
        @JvmOverloads
        fun formatDuration(duration: Duration, zeroPad: Boolean = false): String {
            var totalNanos = duration.toNanos()
            val sign = if (totalNanos < 0L) "-" else ""
            totalNanos = abs(totalNanos)

            val nanosOfSec = totalNanos % 1_000_000_000L

            val fractionalSec = if (nanosOfSec == 0L) ""
            else "." + trimTrailing0s(String.format("%09d", nanosOfSec))

            var secs = abs(duration.toSecondsPart())
            if (sign == "-" && nanosOfSec > 0) {
                secs--
            }

            // Single unit Durations
            if (totalNanos < 1_000_000L) {
                return "$sign${totalNanos}ns"
            } else if (totalNanos < 1_000_000_000L) {
                return "$sign${totalNanos / 1_000_000L}ms"
            } else if (totalNanos < 60_000_000_000L) {
                return "${sign}${secs}${fractionalSec}s"
            }

            val days = abs(duration.toDaysPart())
            val hrs = abs(duration.toHoursPart())
            val mins = abs(duration.toMinutesPart())

            // Compound Durations
            if (days != 0L) {
                if (all0(hrs, mins, secs) && fractionalSec.isEmpty())
                    return "$sign${days}${if (days == 1L) "day" else "days"}"

                return if (zeroPad) {
                    "$sign${days}${if (days == 1L) "day" else "days"}:" +
                            "${padL2(hrs)}:${padL2(mins)}:${padL2(secs)}$fractionalSec"
                } else {
                    "$sign${days}${if (days == 1L) "day" else "days"}:$hrs:$mins:$secs$fractionalSec"
                }
            } else if (hrs != 0) {
                if (all0(days, mins, secs) && fractionalSec.isEmpty())
                    return "$sign${hrs}h"

                return if (zeroPad) {
                    "$sign${padL2(hrs)}:${padL2(mins)}:${padL2(secs)}$fractionalSec"
                } else {
                    "$sign$hrs:$mins:$secs$fractionalSec"
                }
            } else if (mins != 0) {
                if (all0(days, hrs, secs) && fractionalSec.isEmpty())
                    return "$sign${mins}min"

                return if (zeroPad) {
                    "$sign${padL2(hrs)}:${padL2(mins)}:${padL2(secs)}$fractionalSec"
                } else {
                    "$sign$hrs:$mins:$secs$fractionalSec"
                }
            } else if (secs != 0 && fractionalSec.isEmpty()) {
                if (all0(days, hrs, mins))
                    return "$sign${secs}s"

                return if (zeroPad) {
                    "$sign${padL2(hrs)}:${padL2(mins)}:${padL2(secs)}$fractionalSec"
                } else {
                    "$sign$hrs:$mins:$secs$fractionalSec"
                }
            }

            return "0:0:0"
        }

        private fun padL2(num: Any): String = String.format("%02d", num)

        private fun trimTrailing0s(numText: String): String {
            var end = numText.length

            for (i in end - 1 downTo 0) {
                if (numText[i] != '0') {
                    end = i + 1
                    break
                } else if (i == 0) {
                    end = 0
                    break
                }
            }

            return numText.substring(0, end)
        }

        private fun all0(vararg args: Number): Boolean {
            for (num in args) {
                when (num) {
                    is Int -> if (num != 0) return false
                    is Long -> if (num != 0L) return false
                }
            }
            return true
        }
    }
}