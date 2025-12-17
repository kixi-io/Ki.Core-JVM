package io.kixi

import io.kixi.text.ParseException
import java.time.*
import java.time.temporal.TemporalAmount

/**
 * A date-time with a KiTZ timezone that preserves the timezone identity.
 *
 * Unlike [ZonedDateTime] which only stores the offset, `KiTZDateTime` preserves the
 * full KiTZ identifier (e.g., "US/PST" vs "CA/PST"), allowing for meaningful timezone
 * representation in formatted output.
 *
 * ## Creation
 * ```kotlin
 * // From components
 * val dt = KiTZDateTime(2024, 3, 15, 14, 30, 0, KiTZ.US_PST)
 *
 * // From LocalDateTime
 * val dt = KiTZDateTime(LocalDateTime.of(2024, 3, 15, 14, 30), KiTZ.JP_JST)
 *
 * // Parse from string
 * val dt = KiTZDateTime.parse("2024/3/15@14:30:00-US/PST")
 *
 * // Current time in a timezone
 * val now = KiTZDateTime.now(KiTZ.US_PST)
 * ```
 *
 * ## Formatting
 * ```kotlin
 * val dt = KiTZDateTime(2024, 3, 15, 14, 30, 0, KiTZ.US_PST)
 * dt.toString()     // "2024/3/15@14:30:00-US/PST"
 * dt.kiFormat()     // "2024/3/15@14:30:00-US/PST"
 * ```
 *
 * ## Conversion
 * ```kotlin
 * val dt = KiTZDateTime(2024, 3, 15, 14, 30, 0, KiTZ.US_PST)
 *
 * // Convert to different timezone (same instant)
 * val tokyo = dt.withKiTZ(KiTZ.JP_JST)  // 2024/3/16@7:30:00-JP/JST
 *
 * // To/from ZonedDateTime
 * val zdt = dt.toZonedDateTime()
 * val back = KiTZDateTime.fromZonedDateTime(zdt, KiTZ.US_PST)
 * ```
 *
 * @property localDateTime The local date and time
 * @property kpiTZ The KiTZ timezone
 */
data class KiTZDateTime(
    val localDateTime: LocalDateTime,
    val kiTZ: KiTZ
) : Comparable<KiTZDateTime> {

    /**
     * The UTC offset for this datetime.
     */
    val offset: ZoneOffset get() = kiTZ.offset

    /**
     * The local date component.
     */
    val localDate: LocalDate get() = localDateTime.toLocalDate()

    /**
     * The local time component.
     */
    val localTime: LocalTime get() = localDateTime.toLocalTime()

    /**
     * The year.
     */
    val year: Int get() = localDateTime.year

    /**
     * The month (1-12).
     */
    val monthValue: Int get() = localDateTime.monthValue

    /**
     * The month as an enum.
     */
    val month: Month get() = localDateTime.month

    /**
     * The day of month (1-31).
     */
    val dayOfMonth: Int get() = localDateTime.dayOfMonth

    /**
     * The day of year (1-366).
     */
    val dayOfYear: Int get() = localDateTime.dayOfYear

    /**
     * The day of week.
     */
    val dayOfWeek: DayOfWeek get() = localDateTime.dayOfWeek

    /**
     * The hour (0-23).
     */
    val hour: Int get() = localDateTime.hour

    /**
     * The minute (0-59).
     */
    val minute: Int get() = localDateTime.minute

    /**
     * The second (0-59).
     */
    val second: Int get() = localDateTime.second

    /**
     * The nanosecond (0-999,999,999).
     */
    val nano: Int get() = localDateTime.nano

    /**
     * Creates a KiTZDateTime from date/time components.
     */
    constructor(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        kiTZ: KiTZ
    ) : this(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second), kiTZ)

    /**
     * Creates a KiTZDateTime from date/time components including nanoseconds.
     */
    constructor(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        nanoOfSecond: Int = 0,
        kiTZ: KiTZ
    ) : this(LocalDateTime.of(year, month, dayOfMonth, hour, minute, second, nanoOfSecond), kiTZ)

    /**
     * Creates a KiTZDateTime from a LocalDate, LocalTime, and KiTZ.
     */
    constructor(
        localDate: LocalDate,
        localTime: LocalTime,
        kiTZ: KiTZ
    ) : this(LocalDateTime.of(localDate, localTime), kiTZ)

    /**
     * Creates a KiTZDateTime from a LocalDate at midnight.
     */
    constructor(localDate: LocalDate, kiTZ: KiTZ) : this(localDate.atStartOfDay(), kiTZ)

    /**
     * Creates a KiTZDateTime from year, month, day and KiTZ at midnight.
     */
    constructor(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        kiTZ: KiTZ
    ) : this(LocalDate.of(year, month, dayOfMonth), kiTZ)

    /**
     * Private constructor for parsing - takes a pre-parsed Pair to avoid double parsing.
     */
    private constructor(parsed: Pair<LocalDateTime, KiTZ>) : this(parsed.first, parsed.second)

    /**
     * Parses a Ki datetime literal with a KiTZ suffix.
     *
     * ```kotlin
     * val dt = KiTZDateTime("2024/3/15@14:30:00-US/PST")
     * val dt = KiTZDateTime("2024/3/15@9:00:00-JP/JST")
     * ```
     *
     * @param text The Ki datetime literal
     * @throws ParseException if the text is malformed or the KiTZ is invalid
     */
    constructor(text: String) : this(parseToComponents(text))

    /**
     * Converts this KiTZDateTime to a [ZonedDateTime].
     *
     * Note: The ZonedDateTime will only preserve the offset, not the KiTZ identity.
     */
    fun toZonedDateTime(): ZonedDateTime = ZonedDateTime.of(localDateTime, offset)

    /**
     * Converts this KiTZDateTime to an [Instant].
     */
    fun toInstant(): Instant = toZonedDateTime().toInstant()

    /**
     * Converts this KiTZDateTime to epoch milliseconds.
     */
    fun toEpochMilli(): Long = toInstant().toEpochMilli()

    /**
     * Converts this KiTZDateTime to epoch seconds.
     */
    fun toEpochSecond(): Long = toZonedDateTime().toEpochSecond()

    /**
     * Returns a copy with a different KiTZ, adjusting the local time to represent
     * the same instant.
     *
     * ```kotlin
     * val pst = KiTZDateTime(2024, 3, 15, 14, 30, 0, KiTZ.US_PST)
     * val jst = pst.withKiTZ(KiTZ.JP_JST)  // 2024/3/16@7:30:00-JP/JST
     * ```
     */
    fun withKiTZ(newKiTZ: KiTZ): KiTZDateTime {
        val zdt = toZonedDateTime().withZoneSameInstant(newKiTZ.offset)
        return KiTZDateTime(zdt.toLocalDateTime(), newKiTZ)
    }

    /**
     * Returns a copy with the same local time but a different KiTZ.
     * The instant will change.
     *
     * ```kotlin
     * val pst = KiTZDateTime(2024, 3, 15, 14, 30, 0, KiTZ.US_PST)
     * val jst = pst.withKiTZSameLocal(KiTZ.JP_JST)  // 2024/3/15@14:30:00-JP/JST
     * ```
     */
    fun withKiTZSameLocal(newKiTZ: KiTZ): KiTZDateTime = KiTZDateTime(localDateTime, newKiTZ)

    // Temporal arithmetic ////

    /**
     * Returns a copy with the specified amount added.
     */
    fun plus(amount: TemporalAmount): KiTZDateTime =
        KiTZDateTime(localDateTime.plus(amount), kiTZ)

    /**
     * Returns a copy with the specified duration added.
     */
    fun plus(duration: Duration): KiTZDateTime =
        KiTZDateTime(localDateTime.plus(duration), kiTZ)

    /**
     * Returns a copy with the specified period added.
     */
    fun plus(period: Period): KiTZDateTime =
        KiTZDateTime(localDateTime.plus(period), kiTZ)

    /**
     * Returns a copy with the specified amount subtracted.
     */
    fun minus(amount: TemporalAmount): KiTZDateTime =
        KiTZDateTime(localDateTime.minus(amount), kiTZ)

    /**
     * Returns a copy with the specified duration subtracted.
     */
    fun minus(duration: Duration): KiTZDateTime =
        KiTZDateTime(localDateTime.minus(duration), kiTZ)

    /**
     * Returns a copy with the specified period subtracted.
     */
    fun minus(period: Period): KiTZDateTime =
        KiTZDateTime(localDateTime.minus(period), kiTZ)

    // With methods for adjusting components ////

    fun withYear(year: Int): KiTZDateTime =
        KiTZDateTime(localDateTime.withYear(year), kiTZ)

    fun withMonth(month: Int): KiTZDateTime =
        KiTZDateTime(localDateTime.withMonth(month), kiTZ)

    fun withDayOfMonth(dayOfMonth: Int): KiTZDateTime =
        KiTZDateTime(localDateTime.withDayOfMonth(dayOfMonth), kiTZ)

    fun withDayOfYear(dayOfYear: Int): KiTZDateTime =
        KiTZDateTime(localDateTime.withDayOfYear(dayOfYear), kiTZ)

    fun withHour(hour: Int): KiTZDateTime =
        KiTZDateTime(localDateTime.withHour(hour), kiTZ)

    fun withMinute(minute: Int): KiTZDateTime =
        KiTZDateTime(localDateTime.withMinute(minute), kiTZ)

    fun withSecond(second: Int): KiTZDateTime =
        KiTZDateTime(localDateTime.withSecond(second), kiTZ)

    fun withNano(nanoOfSecond: Int): KiTZDateTime =
        KiTZDateTime(localDateTime.withNano(nanoOfSecond), kiTZ)

    // Plus/minus individual units ////

    fun plusYears(years: Long): KiTZDateTime =
        KiTZDateTime(localDateTime.plusYears(years), kiTZ)

    fun plusMonths(months: Long): KiTZDateTime =
        KiTZDateTime(localDateTime.plusMonths(months), kiTZ)

    fun plusWeeks(weeks: Long): KiTZDateTime =
        KiTZDateTime(localDateTime.plusWeeks(weeks), kiTZ)

    fun plusDays(days: Long): KiTZDateTime =
        KiTZDateTime(localDateTime.plusDays(days), kiTZ)

    fun plusHours(hours: Long): KiTZDateTime =
        KiTZDateTime(localDateTime.plusHours(hours), kiTZ)

    fun plusMinutes(minutes: Long): KiTZDateTime =
        KiTZDateTime(localDateTime.plusMinutes(minutes), kiTZ)

    fun plusSeconds(seconds: Long): KiTZDateTime =
        KiTZDateTime(localDateTime.plusSeconds(seconds), kiTZ)

    fun plusNanos(nanos: Long): KiTZDateTime =
        KiTZDateTime(localDateTime.plusNanos(nanos), kiTZ)

    fun minusYears(years: Long): KiTZDateTime = plusYears(-years)
    fun minusMonths(months: Long): KiTZDateTime = plusMonths(-months)
    fun minusWeeks(weeks: Long): KiTZDateTime = plusWeeks(-weeks)
    fun minusDays(days: Long): KiTZDateTime = plusDays(-days)
    fun minusHours(hours: Long): KiTZDateTime = plusHours(-hours)
    fun minusMinutes(minutes: Long): KiTZDateTime = plusMinutes(-minutes)
    fun minusSeconds(seconds: Long): KiTZDateTime = plusSeconds(-seconds)
    fun minusNanos(nanos: Long): KiTZDateTime = plusNanos(-nanos)

    // Comparison ////

    /**
     * Compares this KiTZDateTime to another based on the instant they represent.
     */
    override fun compareTo(other: KiTZDateTime): Int =
        toInstant().compareTo(other.toInstant())

    /**
     * Returns true if this instant is before the other.
     */
    fun isBefore(other: KiTZDateTime): Boolean = toInstant().isBefore(other.toInstant())

    /**
     * Returns true if this instant is after the other.
     */
    fun isAfter(other: KiTZDateTime): Boolean = toInstant().isAfter(other.toInstant())

    /**
     * Returns true if this represents the same instant as the other.
     */
    fun isEqual(other: KiTZDateTime): Boolean = toInstant() == other.toInstant()

    // Formatting ////

    /**
     * Formats this KiTZDateTime as a Ki literal string.
     *
     * @param zeroPad Whether to zero-pad date/time components
     * @param forceNano Whether to always include nanoseconds
     */
    @JvmOverloads
    fun kiFormat(zeroPad: Boolean = false, forceNano: Boolean = false): String {
        val buf = StringBuilder()
        buf.append(Ki.formatLocalDateTime(localDateTime, zeroPad, forceNano))
        buf.append("-")
        buf.append(kiTZ.id)
        return buf.toString()
    }

    @JvmOverloads
    fun informalFormat(zeroPad: Boolean = false, forceNano: Boolean = false,
                       clock24: Boolean = false): String {
        val buf = StringBuilder()

        // Date: month/day/year
        if (zeroPad) {
            buf.append(String.format("%02d/%02d/%04d", monthValue, dayOfMonth, year))
        } else {
            buf.append("$monthValue/$dayOfMonth/$year")
        }

        buf.append(" ")

        // Time
        if (clock24) {
            if (zeroPad) {
                buf.append(String.format("%02d:%02d:%02d", hour, minute, second))
            } else {
                buf.append(String.format("%d:%02d:%02d", hour, minute, second))
            }
        } else {
            val hour12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val amPm = if (hour < 12) "AM" else "PM"

            if (zeroPad) {
                buf.append(String.format("%02d:%02d:%02d %s", hour12, minute, second, amPm))
            } else {
                buf.append(String.format("%d:%02d:%02d %s", hour12, minute, second, amPm))
            }
        }

        // Nanoseconds
        if (forceNano || nano != 0) {
            buf.append(String.format(".%09d", nano).trimEnd('0'))
        }

        buf.append(" ")
        buf.append(kiTZ.id)

        return buf.toString()
    }

    /**
     * Returns the Ki literal representation of this datetime.
     */
    override fun toString(): String = kiFormat()

    companion object : Parseable<KiTZDateTime> {
        /**
         * Internal helper to parse text into components for constructor delegation.
         */
        private fun parseToComponents(text: String): Pair<LocalDateTime, KiTZ> {
            val trimmed = text.trim()
            val dashIdx = trimmed.lastIndexOf('-')

            if (dashIdx == -1 || dashIdx == 0) {
                throw ParseException("KiTZDateTime requires a KiTZ suffix (e.g., -US/PST)")
            }

            val localDTText = trimmed.substring(0, dashIdx).replace("_", "")
            val kitzId = trimmed.substring(dashIdx + 1)

            val kiTZ = when (kitzId) {
                "Z", "UTC", "GMT" -> KiTZ.UTC
                else -> KiTZ[kitzId]
                    ?: throw ParseException("Invalid KiTZ identifier: $kitzId")
            }

            val localDT = LocalDateTime.parse(localDTText, Ki.LOCAL_DATE_TIME_PARSER)
            return Pair(localDT, kiTZ)
        }

        /**
         * Creates a KiTZDateTime for the current instant in the specified timezone.
         *
         * ```kotlin
         * val now = KiTZDateTime.now(KiTZ.US_PST)
         * ```
         */
        @JvmStatic
        fun now(kiTZ: KiTZ): KiTZDateTime {
            val zdt = ZonedDateTime.now(kiTZ.offset)
            return KiTZDateTime(zdt.toLocalDateTime(), kiTZ)
        }

        /**
         * Creates a KiTZDateTime from a ZonedDateTime, using the specified KiTZ.
         *
         * If the ZonedDateTime's offset doesn't match the KiTZ offset, the instant
         * is converted to the KiTZ timezone.
         */
        @JvmStatic
        fun fromZonedDateTime(zdt: ZonedDateTime, kiTZ: KiTZ): KiTZDateTime {
            val adjusted = if (zdt.offset == kiTZ.offset) zdt
            else zdt.withZoneSameInstant(kiTZ.offset)
            return KiTZDateTime(adjusted.toLocalDateTime(), kiTZ)
        }

        /**
         * Creates a KiTZDateTime from an Instant in the specified timezone.
         */
        @JvmStatic
        fun ofInstant(instant: Instant, kiTZ: KiTZ): KiTZDateTime {
            val zdt = ZonedDateTime.ofInstant(instant, kiTZ.offset)
            return KiTZDateTime(zdt.toLocalDateTime(), kiTZ)
        }

        /**
         * Creates a KiTZDateTime from epoch milliseconds in the specified timezone.
         */
        @JvmStatic
        fun ofEpochMilli(epochMilli: Long, kiTZ: KiTZ): KiTZDateTime =
            ofInstant(Instant.ofEpochMilli(epochMilli), kiTZ)

        /**
         * Creates a KiTZDateTime from epoch seconds in the specified timezone.
         */
        @JvmStatic
        fun ofEpochSecond(epochSecond: Long, kiTZ: KiTZ): KiTZDateTime =
            ofInstant(Instant.ofEpochSecond(epochSecond), kiTZ)

        /**
         * Creates a KiTZDateTime at the start of the specified day.
         */
        @JvmStatic
        fun atStartOfDay(year: Int, month: Int, dayOfMonth: Int, kiTZ: KiTZ): KiTZDateTime =
            KiTZDateTime(LocalDate.of(year, month, dayOfMonth), kiTZ)

        /**
         * Creates a KiTZDateTime at the start of the specified day.
         */
        @JvmStatic
        fun atStartOfDay(localDate: LocalDate, kiTZ: KiTZ): KiTZDateTime =
            KiTZDateTime(localDate, kiTZ)

        /**
         * Parses a Ki datetime literal with a KiTZ suffix.
         *
         * ```kotlin
         * val dt = KiTZDateTime.parse("2024/3/15@14:30:00-US/PST")
         * val dt = KiTZDateTime.parse("2024/3/15@9:00:00-JP/JST")
         * ```
         *
         * @param text The Ki datetime literal
         * @return The parsed KiTZDateTime
         * @throws ParseException if the text is malformed or the KiTZ is invalid
         */
        @JvmStatic
        fun parse(text: String): KiTZDateTime {
            val (localDT, kiTZ) = parseToComponents(text)
            return KiTZDateTime(localDT, kiTZ)
        }

        /**
         * Parses a Ki datetime literal string into a KiTZDateTime instance.
         *
         * @param text The Ki datetime literal string to parse
         * @return The parsed KiTZDateTime
         * @throws ParseException if the text cannot be parsed as a valid KiTZDateTime
         */
        override fun parseLiteral(text: String): KiTZDateTime = parse(text)

        /**
         * Parses a Ki datetime literal, returning null on failure.
         */
        @JvmStatic
        fun parseOrNull(text: String): KiTZDateTime? = try {
            parse(text)
        } catch (e: Exception) {
            null
        }
    }
}