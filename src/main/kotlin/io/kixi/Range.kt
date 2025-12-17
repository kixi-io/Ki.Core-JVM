package io.kixi

import io.kixi.text.ParseException

/**
 * A Ki Range can be inclusive or exclusive on both ends, may be reversed (e.g. 5..1),
 * and can be open on the left or right. Note: Ranges that are open should set the same
 * value for left and right. This is necessary because Comparable types don't require
 * a max and min value.
 *
 * Reversed ranges represent downward progressions. Open ended ranges indicate that one
 * end is not bounded.
 *
 * **Examples**
 * ```
 *     0..5     # >= 0 and <= 5
 *     5..0     # <= 5 and >= 0
 *     0<..<5   # > 0 and < 5
 *     0..<5    # >= 0 and < 5
 *     0<..5    # > 0 and <= 5
 *     0.._     # >= 0
 *     _..5     # <= 5
 *     0<.._    # > 0
 *     _..<5    # < 5
 * ```
 */
data class Range<T : Comparable<T>>(
    val left: T,
    val right: T,
    val type: Type = Type.Inclusive,
    val openLeft: Boolean = false,
    val openRight: Boolean = false
) {

    enum class Type(val operator: String) {
        Inclusive(".."),
        Exclusive("<..<"),
        ExclusiveLeft("<.."),
        ExclusiveRight("..<")
    }

    /** The minimum value in this range (regardless of direction) */
    val min: T = if (left.compareTo(right) < 0) left else right

    /** The maximum value in this range (regardless of direction) */
    val max: T = if (left.compareTo(right) > 0) left else right

    /** True if this range goes from high to low (e.g., 5..1) */
    val reversed: Boolean = left.compareTo(right) > 0

    /** True if this range is open on either end */
    val isOpen: Boolean = openLeft || openRight

    /** True if this range is bounded on both ends */
    val isClosed: Boolean = !openLeft && !openRight

    override fun toString(): String {
        val leftString = if (openLeft) "_" else Ki.format(left)
        val rightString = if (openRight) "_" else Ki.format(right)

        return leftString + type.operator + rightString
    }

    operator fun contains(element: T): Boolean {
        if (openLeft) {
            return when (type) {
                Type.Inclusive -> element <= right
                Type.ExclusiveRight -> element < right
                else -> throw IllegalArgumentException(
                    "Left open ranges can only use .. and ..< operators."
                )
            }
        } else if (openRight) {
            return when (type) {
                Type.Inclusive -> element >= left
                Type.ExclusiveLeft -> element > left
                else -> throw IllegalArgumentException(
                    "Right open ranges can only use .. and <.. operators."
                )
            }
        } else {
            return when (type) {
                Type.Inclusive -> element in min..max
                Type.Exclusive -> element > min && element < max
                Type.ExclusiveLeft -> if (reversed) element < left && element >= right
                else element > left && element <= right
                Type.ExclusiveRight -> if (reversed) element <= left && element > right
                else element >= left && element < right
            }
        }
    }

    /**
     * Returns true if this range overlaps with the other range.
     * Both ranges must be closed (not open on either end).
     */
    fun overlaps(other: Range<T>): Boolean {
        require(isClosed && other.isClosed) { "Both ranges must be closed for overlap check" }

        // Two ranges overlap if each range's min is <= the other's max
        return min <= other.max && other.min <= max
    }

    /**
     * Returns the intersection of this range with another, or null if they don't overlap.
     * Both ranges must be closed and inclusive.
     */
    fun intersect(other: Range<T>): Range<T>? {
        require(isClosed && other.isClosed) { "Both ranges must be closed for intersection" }
        require(type == Type.Inclusive && other.type == Type.Inclusive) {
            "Both ranges must be inclusive for intersection"
        }

        if (!overlaps(other)) return null

        val newMin = if (min > other.min) min else other.min
        val newMax = if (max < other.max) max else other.max

        return Range(newMin, newMax, Type.Inclusive)
    }

    /**
     * Clamps a value to be within this range.
     * Only works for closed, inclusive ranges.
     */
    fun clamp(value: T): T {
        require(isClosed) { "Cannot clamp to an open range" }
        require(type == Type.Inclusive) { "Cannot clamp to an exclusive range" }

        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }

    companion object : Parseable<Range<Int>> {
        // Regex patterns for parsing range operators
        private val EXCLUSIVE_PATTERN = Regex("^(.+)<\\.\\.<(.+)$")
        private val EXCLUSIVE_LEFT_PATTERN = Regex("^(.+)<\\.\\.(.+)$")
        private val EXCLUSIVE_RIGHT_PATTERN = Regex("^(.+)\\.\\.<(.+)$")
        private val INCLUSIVE_PATTERN = Regex("^(.+)\\.\\.(.+)$")

        /**
         * Creates an inclusive range from left to right.
         */
        fun <T : Comparable<T>> inclusive(left: T, right: T): Range<T> =
            Range(left, right, Type.Inclusive)

        /**
         * Creates an exclusive range (excludes both endpoints).
         */
        fun <T : Comparable<T>> exclusive(left: T, right: T): Range<T> =
            Range(left, right, Type.Exclusive)

        /**
         * Creates a range open on the right (>= left).
         */
        fun <T : Comparable<T>> openRight(left: T): Range<T> =
            Range(left, left, Type.Inclusive, openLeft = false, openRight = true)

        /**
         * Creates a range open on the left (<= right).
         */
        fun <T : Comparable<T>> openLeft(right: T): Range<T> =
            Range(right, right, Type.Inclusive, openLeft = true, openRight = false)

        /**
         * Parse a Ki Range literal for integers.
         *
         * Supported formats:
         * - Inclusive: `0..5` (>= 0 and <= 5)
         * - Exclusive: `0<..<5` (> 0 and < 5)
         * - ExclusiveLeft: `0<..5` (> 0 and <= 5)
         * - ExclusiveRight: `0..<5` (>= 0 and < 5)
         * - Open left: `_..5` (<= 5)
         * - Open right: `0.._` (>= 0)
         *
         * ```kotlin
         * Range.parse("0..10")    // inclusive range
         * Range.parse("0<..<10") // exclusive range
         * Range.parse("_..10")   // open left
         * Range.parse("0.._")    // open right
         * ```
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
                val (leftStr, rightStr) = match.destructured
                return parseRangeComponents(leftStr.trim(), rightStr.trim(), Type.Exclusive)
            }

            EXCLUSIVE_LEFT_PATTERN.matchEntire(trimmed)?.let { match ->
                val (leftStr, rightStr) = match.destructured
                return parseRangeComponents(leftStr.trim(), rightStr.trim(), Type.ExclusiveLeft)
            }

            EXCLUSIVE_RIGHT_PATTERN.matchEntire(trimmed)?.let { match ->
                val (leftStr, rightStr) = match.destructured
                return parseRangeComponents(leftStr.trim(), rightStr.trim(), Type.ExclusiveRight)
            }

            INCLUSIVE_PATTERN.matchEntire(trimmed)?.let { match ->
                val (leftStr, rightStr) = match.destructured
                return parseRangeComponents(leftStr.trim(), rightStr.trim(), Type.Inclusive)
            }

            throw ParseException("Invalid range format: $trimmed. Expected format like '0..10' or '0<..<10'")
        }

        private fun parseRangeComponents(leftStr: String, rightStr: String, type: Type): Range<Int> {
            val openLeft = leftStr == "_"
            val openRight = rightStr == "_"

            // Validate open ranges have compatible operators
            if (openLeft && type !in listOf(Type.Inclusive, Type.ExclusiveRight)) {
                throw ParseException("Left open ranges can only use .. and ..< operators.")
            }
            if (openRight && type !in listOf(Type.Inclusive, Type.ExclusiveLeft)) {
                throw ParseException("Right open ranges can only use .. and <.. operators.")
            }

            try {
                val left = if (openLeft) 0 else leftStr.toInt()
                val right = if (openRight) 0 else rightStr.toInt()

                // For open ranges, use the same value for both endpoints
                val actualLeft = if (openLeft) right else left
                val actualRight = if (openRight) left else right

                return Range(actualLeft, actualRight, type, openLeft, openRight)
            } catch (e: NumberFormatException) {
                throw ParseException(
                    "Invalid integer in range: ${e.message}",
                    cause = e
                )
            }
        }

        /**
         * Parses a Ki Range literal string into a Range<Int> instance.
         *
         * **Note:** Due to type erasure in Kotlin/Java, the Parseable interface
         * implementation only supports parsing Integer ranges. For other types,
         * use the type-specific factory methods or parse the values separately.
         *
         * @param text The Ki range literal string to parse
         * @return The parsed Range<Int>
         * @throws ParseException if the text cannot be parsed as a valid Range
         */
        override fun parseLiteral(text: String): Range<Int> = parse(text)

        /**
         * Parse a Range literal, returning null on failure instead of throwing.
         *
         * @param text The Range literal string
         * @return The parsed Range<Int>, or null if parsing fails
         */
        @JvmStatic
        fun parseOrNull(text: String): Range<Int>? = try {
            parse(text)
        } catch (e: Exception) {
            null
        }

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

            // Try each pattern in order of specificity
            EXCLUSIVE_PATTERN.matchEntire(trimmed)?.let { match ->
                val (leftStr, rightStr) = match.destructured
                return parseLongRangeComponents(leftStr.trim(), rightStr.trim(), Type.Exclusive)
            }

            EXCLUSIVE_LEFT_PATTERN.matchEntire(trimmed)?.let { match ->
                val (leftStr, rightStr) = match.destructured
                return parseLongRangeComponents(leftStr.trim(), rightStr.trim(), Type.ExclusiveLeft)
            }

            EXCLUSIVE_RIGHT_PATTERN.matchEntire(trimmed)?.let { match ->
                val (leftStr, rightStr) = match.destructured
                return parseLongRangeComponents(leftStr.trim(), rightStr.trim(), Type.ExclusiveRight)
            }

            INCLUSIVE_PATTERN.matchEntire(trimmed)?.let { match ->
                val (leftStr, rightStr) = match.destructured
                return parseLongRangeComponents(leftStr.trim(), rightStr.trim(), Type.Inclusive)
            }

            throw ParseException("Invalid range format: $trimmed")
        }

        private fun parseLongRangeComponents(leftStr: String, rightStr: String, type: Type): Range<Long> {
            val openLeft = leftStr == "_"
            val openRight = rightStr == "_"

            if (openLeft && type !in listOf(Type.Inclusive, Type.ExclusiveRight)) {
                throw ParseException("Left open ranges can only use .. and ..< operators.")
            }
            if (openRight && type !in listOf(Type.Inclusive, Type.ExclusiveLeft)) {
                throw ParseException("Right open ranges can only use .. and <.. operators.")
            }

            try {
                val left = if (openLeft) 0L else leftStr.removeSuffix("L").toLong()
                val right = if (openRight) 0L else rightStr.removeSuffix("L").toLong()

                val actualLeft = if (openLeft) right else left
                val actualRight = if (openRight) left else right

                return Range(actualLeft, actualRight, type, openLeft, openRight)
            } catch (e: NumberFormatException) {
                throw ParseException(
                    "Invalid long in range: ${e.message}",
                    cause = e
                )
            }
        }
    }
}