package io.kixi

import io.kixi.text.ParseException

/**
 * A Ki Range represents a bounded or open-ended interval over comparable values.
 *
 * Ranges support four exclusivity modes (via [Bound]) and work with any
 * comparable bound (Int, Long, Double, String, Char, etc.). Open ends are
 * represented by null. Reversed ranges (e.g. `5..1`) represent downward
 * progressions.
 *
 * **Closed ranges** (both ends bounded):
 * ```
 *     0..5     // >= 0 and <= 5
 *     0<..<5   // > 0 and < 5
 *     0..<5    // >= 0 and < 5
 *     0<..5    // > 0 and <= 5
 *     5..0     // reversed: <= 5 and >= 0
 * ```
 *
 * **Open ranges** (one end unbounded, represented by null):
 * ```
 *     _..5     // <= 5       (start is null)
 *     0.._     // >= 0       (end is null)
 *     _..<5    // < 5        (start is null)
 *     0<.._    // > 0        (end is null)
 * ```
 *
 * ## Design Notes
 *
 * - Open ends are represented as `nil` \u2014 no sentinel values or separate enum
 * - No compile-time `Comparable` bound on `T` to allow `Range<Any?>` in dynamic
 *   contexts; comparison is checked at runtime
 * - Cross-platform: identical API on JVM, .NET, and Swift
 *
 * @param start The start of the range, or null for left-open ranges
 * @param end The end of the range, or null for right-open ranges
 * @param bound The exclusivity mode for the range boundaries
 */
data class Range<T>(
    val start: T?,
    val end: T?,
    val bound: Bound = Bound.Inclusive
) {

    /**
     * Exclusivity mode for range boundaries.
     *
     *     Inclusive:      ..    start <= x <= end
     *     Exclusive:      <..<  start < x < end
     *     ExclusiveStart: <..   start < x <= end
     *     ExclusiveEnd:   ..<   start <= x < end
     */
    enum class Bound(val operator: String) {
        Inclusive(".."),
        Exclusive("<..<"),
        ExclusiveStart("<.."),
        ExclusiveEnd("..<")
    }

    init {
        // Both ends can't be null
        require(start != null || end != null) {
            "Range must have at least one bounded end"
        }

        // Can't exclude a boundary that doesn't exist
        if (start == null) {
            require(bound == Bound.Inclusive || bound == Bound.ExclusiveEnd) {
                "Open-start ranges can only use Inclusive (..) or ExclusiveEnd (..<), " +
                        "not $bound (cannot exclude a boundary that doesn't exist)"
            }
        }
        if (end == null) {
            require(bound == Bound.Inclusive || bound == Bound.ExclusiveStart) {
                "Open-end ranges can only use Inclusive (..) or ExclusiveStart (<..), " +
                        "not $bound (cannot exclude a boundary that doesn't exist)"
            }
        }
    }

    // ========================================================================
    // Openness Properties
    // ========================================================================

    /** True if the start is unbounded (null). */
    val isOpenStart: Boolean get() = start == null

    /** True if the end is unbounded (null). */
    val isOpenEnd: Boolean get() = end == null

    /** True if this range is open on either end. */
    val isOpen: Boolean get() = start == null || end == null

    /** True if this range is bounded on both ends. */
    val isClosed: Boolean get() = start != null && end != null

    // ========================================================================
    // Computed Properties
    // ========================================================================

    /**
     * The minimum value in this range (regardless of direction).
     * Null if either end is open.
     */
    val min: T?
        get() {
            if (start == null || end == null) return null
            return if (compareValues(start, end) <= 0) start else end
        }

    /**
     * The maximum value in this range (regardless of direction).
     * Null if either end is open.
     */
    val max: T?
        get() {
            if (start == null || end == null) return null
            return if (compareValues(start, end) >= 0) start else end
        }

    /**
     * True if this range goes from high to low (e.g. `5..1`).
     * Always false for open ranges.
     */
    val reversed: Boolean
        get() {
            if (start == null || end == null) return false
            return compareValues(start, end) > 0
        }

    // ========================================================================
    // Containment
    // ========================================================================

    /**
     * Returns true if [element] is within this range, respecting exclusivity
     * and openness.
     *
     * Null elements always return false.
     */
    operator fun contains(element: T): Boolean {
        if (element == null) return false

        val startOk = if (start == null) {
            true // unbounded below
        } else {
            val excludeStart = bound == Bound.ExclusiveStart || bound == Bound.Exclusive
            if (reversed) {
                // For reversed ranges (5..1), start is the high end
                if (excludeStart) compareValues(element, start) < 0
                else compareValues(element, start) <= 0
            } else {
                if (excludeStart) compareValues(element, start) > 0
                else compareValues(element, start) >= 0
            }
        }

        val endOk = if (end == null) {
            true // unbounded above
        } else {
            val excludeEnd = bound == Bound.ExclusiveEnd || bound == Bound.Exclusive
            if (reversed) {
                // For reversed ranges (5..1), end is the low end
                if (excludeEnd) compareValues(element, end) > 0
                else compareValues(element, end) >= 0
            } else {
                if (excludeEnd) compareValues(element, end) < 0
                else compareValues(element, end) <= 0
            }
        }

        return startOk && endOk
    }

    // ========================================================================
    // Operations
    // ========================================================================

    /**
     * Returns true if this range overlaps with [other].
     * Both ranges must be closed.
     */
    fun overlaps(other: Range<T>): Boolean {
        require(isClosed && other.isClosed) {
            "Both ranges must be closed for overlap check"
        }
        return compareValues(min!!, other.max!!) <= 0 &&
                compareValues(other.min!!, max!!) <= 0
    }

    /**
     * Returns the intersection of this range with [other], or null if they
     * don't overlap. Both ranges must be closed and inclusive.
     */
    fun intersect(other: Range<T>): Range<T>? {
        require(isClosed && other.isClosed) {
            "Both ranges must be closed for intersection"
        }
        require(bound == Bound.Inclusive && other.bound == Bound.Inclusive) {
            "Both ranges must be inclusive for intersection"
        }

        if (!overlaps(other)) return null

        val newMin = if (compareValues(min!!, other.min!!) >= 0) min!! else other.min!!
        val newMax = if (compareValues(max!!, other.max!!) <= 0) max!! else other.max!!

        return Range(newMin, newMax, Bound.Inclusive)
    }

    /**
     * Clamps [value] to be within this range.
     * Only works for closed, inclusive ranges.
     */
    fun clamp(value: T): T {
        require(isClosed) { "Cannot clamp to an open range" }
        require(bound == Bound.Inclusive) { "Cannot clamp to an exclusive range" }

        return when {
            compareValues(value, min!!) < 0 -> min!!
            compareValues(value, max!!) > 0 -> max!!
            else -> value
        }
    }

    // ========================================================================
    // Display
    // ========================================================================

    override fun toString(): String {
        val startStr = if (start == null) "_" else Ki.format(start)
        val endStr = if (end == null) "_" else Ki.format(end)
        return startStr + bound.operator + endStr
    }

    // ========================================================================
    // Private Helpers
    // ========================================================================

    /**
     * Compare two non-null values dynamically.
     *
     * Values must implement [Comparable] at runtime. This allows [Range] to
     * work without a compile-time `Comparable` bound, supporting `Range<Any?>`
     * in dynamic/scripting contexts.
     *
     * @throws ClassCastException if values are not comparable
     */
    @Suppress("UNCHECKED_CAST")
    private fun compareValues(a: T, b: T): Int =
        (a as Comparable<Any>).compareTo(b as Any)

    // ========================================================================
    // Companion \u2014 Factory Methods and Parsing
    // ========================================================================

    companion object : Parseable<Range<Int>> {

        // ================================================================
        // Factory Methods
        // ================================================================

        /** Creates an inclusive range: `start..end`. */
        fun <T> inclusive(start: T, end: T): Range<T> =
            Range(start, end, Bound.Inclusive)

        /** Creates an exclusive range: `start<..<end`. */
        fun <T> exclusive(start: T, end: T): Range<T> =
            Range(start, end, Bound.Exclusive)

        /** Creates a left-open range: `_..end` or `_..<end`. */
        fun <T> openStart(end: T, bound: Bound = Bound.Inclusive): Range<T> =
            Range(null, end, bound)

        /** Creates a right-open range: `start.._` or `start<.._`. */
        fun <T> openEnd(start: T, bound: Bound = Bound.Inclusive): Range<T> =
            Range(start, null, bound)

        // ================================================================
        // Int Parsing
        // ================================================================

        private val EXCLUSIVE_PATTERN = Regex("^(.+)<\\.\\.<(.+)$")
        private val EXCLUSIVE_START_PATTERN = Regex("^(.+)<\\.\\.(.+)$")
        private val EXCLUSIVE_END_PATTERN = Regex("^(.+)\\.\\.<(.+)$")
        private val INCLUSIVE_PATTERN = Regex("^(.+)\\.\\.(.+)$")

        /**
         * Parse a Ki Range literal for integers.
         *
         * Supported formats:
         * - Inclusive: `0..5`
         * - Exclusive: `0<..<5`
         * - ExclusiveStart: `0<..5`
         * - ExclusiveEnd: `0..<5`
         * - Open start: `_..5`, `_..<5`
         * - Open end: `0.._`, `0<.._`
         *
         * @param text The Ki range literal string
         * @return The parsed Range<Int>
         * @throws ParseException if the literal is malformed
         */
        @JvmStatic
        fun parse(text: String): Range<Int> {
            val trimmed = text.trim()

            if (trimmed.isEmpty())
                throw ParseException("Range literal cannot be empty.", index = 0)

            // Try each pattern in order of specificity
            EXCLUSIVE_PATTERN.matchEntire(trimmed)?.let { match ->
                val (startStr, endStr) = match.destructured
                return parseIntComponents(startStr.trim(), endStr.trim(), Bound.Exclusive)
            }

            EXCLUSIVE_START_PATTERN.matchEntire(trimmed)?.let { match ->
                val (startStr, endStr) = match.destructured
                return parseIntComponents(startStr.trim(), endStr.trim(), Bound.ExclusiveStart)
            }

            EXCLUSIVE_END_PATTERN.matchEntire(trimmed)?.let { match ->
                val (startStr, endStr) = match.destructured
                return parseIntComponents(startStr.trim(), endStr.trim(), Bound.ExclusiveEnd)
            }

            INCLUSIVE_PATTERN.matchEntire(trimmed)?.let { match ->
                val (startStr, endStr) = match.destructured
                return parseIntComponents(startStr.trim(), endStr.trim(), Bound.Inclusive)
            }

            throw ParseException(
                "Invalid range format: $trimmed. Expected format like '0..10' or '0<..<10'"
            )
        }

        private fun parseIntComponents(
            startStr: String, endStr: String, bound: Bound
        ): Range<Int> {
            val openStart = startStr == "_"
            val openEnd = endStr == "_"

            try {
                val start = if (openStart) null else startStr.toInt()
                val end = if (openEnd) null else endStr.toInt()
                return Range(start, end, bound)
            } catch (e: NumberFormatException) {
                throw ParseException("Invalid integer in range: ${e.message}", cause = e)
            }
        }

        /**
         * Parses a Ki Range literal string into a `Range<Int>`.
         *
         * **Note:** Due to bound erasure in Kotlin/Java, the [Parseable] interface
         * implementation only supports parsing Integer ranges. For other bounds,
         * use the bound-specific factory methods or parse the values separately.
         */
        override fun parseLiteral(text: String): Range<Int> = parse(text)

        /**
         * Parse a Range literal, returning null on failure instead of throwing.
         */
        @JvmStatic
        fun parseOrNull(text: String): Range<Int>? = try {
            parse(text)
        } catch (e: Exception) {
            null
        }

        // ================================================================
        // Long Parsing
        // ================================================================

        /**
         * Parse a Long Range literal.
         *
         * @param text The Range literal string
         * @return The parsed Range<Long>
         * @throws ParseException if the literal is malformed
         */
        @JvmStatic
        fun parseLong(text: String): Range<Long> {
            val trimmed = text.trim()

            if (trimmed.isEmpty())
                throw ParseException("Range literal cannot be empty.", index = 0)

            EXCLUSIVE_PATTERN.matchEntire(trimmed)?.let { match ->
                val (startStr, endStr) = match.destructured
                return parseLongComponents(startStr.trim(), endStr.trim(), Bound.Exclusive)
            }

            EXCLUSIVE_START_PATTERN.matchEntire(trimmed)?.let { match ->
                val (startStr, endStr) = match.destructured
                return parseLongComponents(
                    startStr.trim(), endStr.trim(), Bound.ExclusiveStart
                )
            }

            EXCLUSIVE_END_PATTERN.matchEntire(trimmed)?.let { match ->
                val (startStr, endStr) = match.destructured
                return parseLongComponents(
                    startStr.trim(), endStr.trim(), Bound.ExclusiveEnd
                )
            }

            INCLUSIVE_PATTERN.matchEntire(trimmed)?.let { match ->
                val (startStr, endStr) = match.destructured
                return parseLongComponents(startStr.trim(), endStr.trim(), Bound.Inclusive)
            }

            throw ParseException("Invalid range format: $trimmed")
        }

        private fun parseLongComponents(
            startStr: String, endStr: String, bound: Bound
        ): Range<Long> {
            val openStart = startStr == "_"
            val openEnd = endStr == "_"

            try {
                val start = if (openStart) null
                else startStr.removeSuffix("L").removeSuffix("l").toLong()
                val end = if (openEnd) null
                else endStr.removeSuffix("L").removeSuffix("l").toLong()
                return Range(start, end, bound)
            } catch (e: NumberFormatException) {
                throw ParseException("Invalid long in range: ${e.message}", cause = e)
            }
        }
    }
}