package io.kixi

import io.kixi.text.ParseException

/**
 * Exception thrown when a grid row has an incorrect number of columns.
 *
 * In KD grid literals, the first row establishes the expected column count.
 * All subsequent rows must have exactly the same number of columns.
 *
 * ## Example
 * ```
 * // This will throw WrongRowLengthException because row 2 has 4 columns instead of 3
 * .grid(
 *     1   2   3
 *     4   5   6   7
 *     8   9   10
 * )
 * ```
 *
 * @property expectedLength The expected row length (from the first row)
 * @property actualLength The actual length of the offending row
 * @property rowIndex The zero-based index of the row with the wrong length
 *
 * @see Grid
 * @see ParseException
 */
class WrongRowLengthException(
    val expectedLength: Int,
    val actualLength: Int,
    val rowIndex: Int,
    line: Int = -1,
    index: Int = -1,
    cause: Throwable? = null,
    suggestion: String? = null
) : ParseException(
    "Row $rowIndex has $actualLength columns, expected $expectedLength",
    line,
    index,
    cause,
    suggestion ?: "Ensure all rows in the grid have exactly $expectedLength columns to match the first row."
) {
    companion object {
        /**
         * Creates a WrongRowLengthException with a custom message.
         */
        @JvmStatic
        fun create(
            expectedLength: Int,
            actualLength: Int,
            rowIndex: Int,
            line: Int = -1,
            index: Int = -1
        ): WrongRowLengthException = WrongRowLengthException(
            expectedLength,
            actualLength,
            rowIndex,
            line,
            index
        )
    }
}