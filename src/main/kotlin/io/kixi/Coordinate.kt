package io.kixi

import io.kixi.text.ParseException

/**
 * A coordinate representing a position in a 2D grid (with optional z for future 3D support).
 *
 * Coordinate supports two addressing styles that refer to the same position:
 *
 * ## Standard Notation (Zero-Based)
 * Programmer-friendly x, y coordinates starting from 0:
 * ```
 * .coordinate(x=0, y=0)    // Top-left cell
 * .coordinate(x=4, y=0)    // Fifth column, first row
 * .coordinate(x=0, y=9)    // First column, tenth row
 * ```
 *
 * ## Sheet Notation (Letter Column, One-Based Row)
 * Spreadsheet-style addressing with letter columns (A, B, ..., Z, AA, AB, ...) and
 * one-based row numbers:
 * ```
 * .coordinate(c="A", r=1)   // Top-left cell (same as x=0, y=0)
 * .coordinate(c="E", r=1)   // Fifth column, first row (same as x=4, y=0)
 * .coordinate(c="A", r=10)  // First column, tenth row (same as x=0, y=9)
 * ```
 *
 * ## String Parsing
 * Both notations can be parsed from strings:
 * ```kotlin
 * Coordinate.parse("A1")      // Sheet notation
 * Coordinate.parse("AA100")   // Sheet notation with multi-letter column
 * Coordinate.parse("0,0")     // Standard notation
 * Coordinate.parse("4, 0")    // Standard notation with space
 * ```
 *
 * ## Internal Representation
 * Internally, coordinates are always stored as zero-based (x, y, z) values.
 * The sheet notation accessors ([column], [row]) convert on the fly.
 *
 * ## Usage
 * ```kotlin
 * // Create via standard notation
 * val coord1 = Coordinate.standard(x = 4, y = 0)
 *
 * // Create via sheet notation
 * val coord2 = Coordinate.sheet(c = "E", r = 1)
 *
 * // Both refer to the same cell
 * coord1 == coord2  // true
 *
 * // Access both representations
 * coord1.x       // 4
 * coord1.y       // 0
 * coord1.column  // "E"
 * coord1.row     // 1
 *
 * // Parse from string
 * val coord3 = Coordinate.parse("E1")
 * val coord4 = Coordinate.parse("4,0")
 * ```
 *
 * @property x The zero-based column index
 * @property y The zero-based row index
 * @property z The optional zero-based depth index (for future 3D support)
 *
 * @see Grid
 * @see Ki.parse
 * @see Ki.format
 */
class Coordinate private constructor(
    val x: Int,
    val y: Int,
    val z: Int? = null
) : Comparable<Coordinate> {

    init {
        require(x >= 0) { "x must be non-negative, got: $x" }
        require(y >= 0) { "y must be non-negative, got: $y" }
        require(z == null || z >= 0) { "z must be non-negative, got: $z" }
    }

    /**
     * The column as a letter string (A, B, ..., Z, AA, AB, ...).
     * This is the sheet notation equivalent of [x].
     */
    val column: String get() = indexToColumn(x)

    /**
     * The one-based row number.
     * This is the sheet notation equivalent of [y].
     */
    val row: Int get() = y + 1

    /**
     * Returns true if this coordinate has a z component.
     */
    val hasZ: Boolean get() = z != null

    /**
     * Returns true if this coordinate is at the origin (0, 0).
     */
    val isOrigin: Boolean get() = x == 0 && y == 0 && (z == null || z == 0)

    /**
     * Returns a new Coordinate with the specified z value.
     */
    fun withZ(z: Int): Coordinate = Coordinate(x, y, z)

    /**
     * Returns a new Coordinate without the z value.
     */
    fun withoutZ(): Coordinate = if (z == null) this else Coordinate(x, y, null)

    /**
     * Returns a new Coordinate offset by the given deltas.
     *
     * @throws IllegalArgumentException if the result would have negative coordinates
     */
    fun offset(dx: Int = 0, dy: Int = 0, dz: Int = 0): Coordinate {
        val newX = x + dx
        val newY = y + dy
        val newZ = if (z != null || dz != 0) (z ?: 0) + dz else null

        require(newX >= 0) { "Offset would result in negative x: $newX" }
        require(newY >= 0) { "Offset would result in negative y: $newY" }
        require(newZ == null || newZ >= 0) { "Offset would result in negative z: $newZ" }

        return Coordinate(newX, newY, newZ)
    }

    /**
     * Returns a new Coordinate moved right by n columns.
     */
    fun right(n: Int = 1): Coordinate = offset(dx = n)

    /**
     * Returns a new Coordinate moved left by n columns.
     * @throws IllegalArgumentException if the result would have negative x
     */
    fun left(n: Int = 1): Coordinate = offset(dx = -n)

    /**
     * Returns a new Coordinate moved down by n rows.
     */
    fun down(n: Int = 1): Coordinate = offset(dy = n)

    /**
     * Returns a new Coordinate moved up by n rows.
     * @throws IllegalArgumentException if the result would have negative y
     */
    fun up(n: Int = 1): Coordinate = offset(dy = -n)

    /**
     * Returns the Ki literal representation.
     *
     * Includes both standard and sheet notation as a comment:
     * ```
     * .coordinate(x=4, y=0)  // "E1" in sheet notation
     * ```
     */
    override fun toString(): String = toKiLiteral()

    /**
     * Returns the Ki literal representation with optional comment showing sheet notation.
     */
    fun toKiLiteral(includeComment: Boolean = false): String {
        val base = if (z != null) {
            ".coordinate(x=$x, y=$y, z=$z)"
        } else {
            ".coordinate(x=$x, y=$y)"
        }

        return if (includeComment) {
            "$base // \"${toSheetNotation()}\" in sheet notation"
        } else {
            base
        }
    }

    /**
     * Returns the sheet notation string (e.g., "A1", "E8", "AA100").
     */
    fun toSheetNotation(): String = "$column$row"

    /**
     * Returns the standard notation string (e.g., "0,0", "4,0").
     */
    fun toStandardNotation(): String = if (z != null) "$x,$y,$z" else "$x,$y"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Coordinate) return false
        return x == other.x && y == other.y && z == other.z
    }

    override fun hashCode(): Int {
        var result = x
        result = 31 * result + y
        result = 31 * result + (z ?: 0)
        return result
    }

    /**
     * Compares coordinates by y (row) first, then x (column), then z.
     * This gives a natural reading order (left-to-right, top-to-bottom).
     */
    override fun compareTo(other: Coordinate): Int {
        var result = y.compareTo(other.y)
        if (result != 0) return result

        result = x.compareTo(other.x)
        if (result != 0) return result

        return when {
            z == null && other.z == null -> 0
            z == null -> -1
            other.z == null -> 1
            else -> z.compareTo(other.z)
        }
    }

    /**
     * Creates a range from this coordinate to [other].
     */
    operator fun rangeTo(other: Coordinate): CoordinateRange = CoordinateRange(this, other)

    companion object : Parseable<Coordinate> {

        /** The origin coordinate (0, 0). */
        @JvmField
        val ORIGIN = Coordinate(0, 0, null)

        private const val COORDINATE_PREFIX = ".coordinate("
        private val SHEET_PATTERN = Regex("""^([A-Za-z]+)(\d+)$""")
        private val STANDARD_PATTERN = Regex("""^(-?\d+)\s*,\s*(-?\d+)(?:\s*,\s*(-?\d+))?$""")

        /**
         * Creates a Coordinate using standard zero-based (x, y) notation.
         *
         * @param x The zero-based column index
         * @param y The zero-based row index
         * @param z Optional zero-based depth index
         * @throws IllegalArgumentException if any coordinate is negative
         */
        @JvmStatic
        @JvmOverloads
        fun standard(x: Int, y: Int, z: Int? = null): Coordinate = Coordinate(x, y, z)

        /**
         * Creates a Coordinate using sheet notation (letter column, one-based row).
         *
         * @param c The column letter(s) (A, B, ..., Z, AA, AB, ...)
         * @param r The one-based row number
         * @param z Optional zero-based depth index
         * @throws IllegalArgumentException if c is invalid or r is less than 1
         */
        @JvmStatic
        @JvmOverloads
        fun sheet(c: String, r: Int, z: Int? = null): Coordinate {
            require(c.isNotEmpty() && c.all { it.isLetter() }) {
                "Column must be one or more letters, got: $c"
            }
            require(r >= 1) { "Row must be at least 1 (one-based), got: $r" }

            val x = columnToIndex(c)
            val y = r - 1
            return Coordinate(x, y, z)
        }

        /**
         * Parses a coordinate from either sheet notation ("A1", "AA100") or
         * standard notation ("0,0", "4, 0").
         *
         * @param text The coordinate string to parse
         * @return The parsed Coordinate
         * @throws ParseException if the string cannot be parsed
         */
        @JvmStatic
        fun parse(text: String): Coordinate {
            val trimmed = text.trim()

            if (trimmed.isEmpty()) {
                throw ParseException("Coordinate string cannot be empty", index = 0)
            }

            // Try sheet notation first (e.g., "A1", "AA100")
            SHEET_PATTERN.matchEntire(trimmed)?.let { match ->
                val column = match.groupValues[1].uppercase()
                val row = match.groupValues[2].toIntOrNull()
                    ?: throw ParseException("Invalid row number: ${match.groupValues[2]}", index = 0)

                if (row < 1) {
                    throw ParseException("Row must be at least 1, got: $row", index = 0)
                }

                return sheet(column, row)
            }

            // Try standard notation (e.g., "0,0", "4, 0")
            STANDARD_PATTERN.matchEntire(trimmed)?.let { match ->
                val x = match.groupValues[1].toIntOrNull()
                    ?: throw ParseException("Invalid x coordinate: ${match.groupValues[1]}", index = 0)
                val y = match.groupValues[2].toIntOrNull()
                    ?: throw ParseException("Invalid y coordinate: ${match.groupValues[2]}", index = 0)
                val z = match.groupValues[3].takeIf { it.isNotEmpty() }?.toIntOrNull()

                if (x < 0) throw ParseException("x must be non-negative, got: $x", index = 0)
                if (y < 0) throw ParseException("y must be non-negative, got: $y", index = 0)
                if (z != null && z < 0) throw ParseException("z must be non-negative, got: $z", index = 0)

                return standard(x, y, z)
            }

            throw ParseException(
                "Invalid coordinate format: '$trimmed'. " +
                        "Expected sheet notation (e.g., 'A1') or standard notation (e.g., '0,0')",
                index = 0
            )
        }

        /**
         * Parses a Ki coordinate literal.
         *
         * Supported formats:
         * ```
         * .coordinate(x=0, y=0)
         * .coordinate(x=0, y=0, z=5)
         * .coordinate(c="A", r=1)
         * .coordinate(c="AA", r=100, z=5)
         * ```
         *
         * @param text The Ki coordinate literal string
         * @return The parsed Coordinate
         * @throws ParseException if the literal is malformed
         */
        override fun parseLiteral(text: String): Coordinate {
            val trimmed = text.trim()

            if (!trimmed.startsWith(COORDINATE_PREFIX)) {
                throw ParseException(
                    "Coordinate literal must start with '.coordinate('",
                    index = 0
                )
            }

            if (!trimmed.endsWith(")")) {
                throw ParseException(
                    "Coordinate literal must end with ')'",
                    index = trimmed.length - 1
                )
            }

            val content = trimmed.substring(COORDINATE_PREFIX.length, trimmed.length - 1).trim()

            if (content.isEmpty()) {
                throw ParseException("Coordinate literal requires parameters", index = COORDINATE_PREFIX.length)
            }

            // Parse key=value pairs
            val params = parseParams(content)

            // Determine notation type and extract values
            return when {
                // Sheet notation: c="A", r=1
                params.containsKey("c") && params.containsKey("r") -> {
                    val c = params["c"]?.removeSurrounding("\"")
                        ?: throw ParseException("Missing column parameter 'c'", index = 0)
                    val r = params["r"]?.toIntOrNull()
                        ?: throw ParseException("Invalid row parameter 'r': ${params["r"]}", index = 0)
                    val z = params["z"]?.toIntOrNull()

                    sheet(c, r, z)
                }

                // Standard notation: x=0, y=0
                params.containsKey("x") && params.containsKey("y") -> {
                    val x = params["x"]?.toIntOrNull()
                        ?: throw ParseException("Invalid x parameter: ${params["x"]}", index = 0)
                    val y = params["y"]?.toIntOrNull()
                        ?: throw ParseException("Invalid y parameter: ${params["y"]}", index = 0)
                    val z = params["z"]?.toIntOrNull()

                    standard(x, y, z)
                }

                else -> throw ParseException(
                    "Coordinate requires either (x, y) or (c, r) parameters",
                    index = COORDINATE_PREFIX.length
                )
            }
        }

        /**
         * Parses coordinate literal parameters.
         */
        private fun parseParams(content: String): Map<String, String> {
            val params = mutableMapOf<String, String>()

            // Simple parser for key=value pairs separated by commas
            val parts = content.split(",").map { it.trim() }

            for (part in parts) {
                val eqIndex = part.indexOf('=')
                if (eqIndex == -1) {
                    throw ParseException("Invalid parameter format: '$part'. Expected key=value", index = 0)
                }

                val key = part.substring(0, eqIndex).trim()
                val value = part.substring(eqIndex + 1).trim()
                params[key] = value
            }

            return params
        }

        /**
         * Parses a coordinate literal, returning null on failure.
         */
        @JvmStatic
        fun parseOrNull(text: String): Coordinate? = try {
            parse(text)
        } catch (e: Exception) {
            null
        }

        /**
         * Parses a Ki coordinate literal, returning null on failure.
         */
        @JvmStatic
        fun parseLiteralOrNull(text: String): Coordinate? = try {
            parseLiteral(text)
        } catch (e: Exception) {
            null
        }

        /**
         * Checks if a string appears to be a Ki coordinate literal.
         */
        @JvmStatic
        fun isLiteral(text: String): Boolean {
            val trimmed = text.trim()
            return trimmed.startsWith(".coordinate(") && trimmed.endsWith(")")
        }

        /**
         * Converts a zero-based column index to a letter string (0 -> "A", 25 -> "Z", 26 -> "AA").
         */
        @JvmStatic
        fun indexToColumn(index: Int): String {
            require(index >= 0) { "Column index must be non-negative, got: $index" }

            val result = StringBuilder()
            var remaining = index

            do {
                result.insert(0, ('A' + (remaining % 26)))
                remaining = remaining / 26 - 1
            } while (remaining >= 0)

            return result.toString()
        }

        /**
         * Converts a letter column string to a zero-based index ("A" -> 0, "Z" -> 25, "AA" -> 26).
         */
        @JvmStatic
        fun columnToIndex(column: String): Int {
            require(column.isNotEmpty() && column.all { it.isLetter() }) {
                "Column must be one or more letters, got: $column"
            }

            val upper = column.uppercase()
            var result = 0

            for (c in upper) {
                result = result * 26 + (c - 'A' + 1)
            }

            return result - 1
        }
    }
}

/**
 * A range of coordinates from [start] to [endInclusive].
 *
 * The range includes all coordinates within the rectangular region defined by
 * the two corners. Iteration proceeds in reading order (left-to-right, top-to-bottom).
 *
 * ```kotlin
 * val range = Coordinate.parse("A1")..Coordinate.parse("C3")
 * for (coord in range) {
 *     println(coord.toSheetNotation())  // A1, B1, C1, A2, B2, C2, A3, B3, C3
 * }
 * ```
 */
class CoordinateRange(
    override val start: Coordinate,
    override val endInclusive: Coordinate
) : ClosedRange<Coordinate>, Iterable<Coordinate> {

    /** The minimum x coordinate in this range. */
    val minX: Int = minOf(start.x, endInclusive.x)

    /** The maximum x coordinate in this range. */
    val maxX: Int = maxOf(start.x, endInclusive.x)

    /** The minimum y coordinate in this range. */
    val minY: Int = minOf(start.y, endInclusive.y)

    /** The maximum y coordinate in this range. */
    val maxY: Int = maxOf(start.y, endInclusive.y)

    /** The width of this range (number of columns). */
    val width: Int = maxX - minX + 1

    /** The height of this range (number of rows). */
    val height: Int = maxY - minY + 1

    /** The total number of coordinates in this range. */
    val size: Int = width * height

    /** The top-left coordinate of this range. */
    val topLeft: Coordinate get() = Coordinate.standard(minX, minY)

    /** The bottom-right coordinate of this range. */
    val bottomRight: Coordinate get() = Coordinate.standard(maxX, maxY)

    override fun contains(value: Coordinate): Boolean =
        value.x in minX..maxX && value.y in minY..maxY

    override fun isEmpty(): Boolean = false // A coordinate range is never empty

    /**
     * Returns an iterator over all coordinates in this range.
     * Iteration proceeds in reading order (left-to-right, top-to-bottom).
     */
    override fun iterator(): Iterator<Coordinate> = object : Iterator<Coordinate> {
        private var currentX = minX
        private var currentY = minY

        override fun hasNext(): Boolean = currentY <= maxY

        override fun next(): Coordinate {
            if (!hasNext()) throw NoSuchElementException()

            val coord = Coordinate.standard(currentX, currentY)

            currentX++
            if (currentX > maxX) {
                currentX = minX
                currentY++
            }

            return coord
        }
    }

    /**
     * Returns all coordinates in this range as a list.
     */
    fun toList(): List<Coordinate> = iterator().asSequence().toList()

    override fun toString(): String = "${start.toSheetNotation()}..${endInclusive.toSheetNotation()}"
}