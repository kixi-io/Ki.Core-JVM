package io.kixi

import io.kixi.text.ParseException

/**
 * A two-dimensional grid of values with efficient flat array storage.
 *
 * ## Ki Literal Format
 * ```
 * .grid(
 *     2   4   6
 *     8   10  12
 *     14  16  18
 * )
 * ```
 *
 * Empty cells can be represented with `nil` or `-`:
 * ```
 * .grid(
 *     1    2    nil
 *     -    5    6
 *     7    8    9
 * )
 * ```
 *
 * Typed grids (optional - type is inferred if not specified):
 * ```
 * .grid<Int>(
 *     1  2  3
 *     4  5  6
 * )
 * ```
 *
 * ## Accessing Cells
 * Grid supports multiple access styles:
 * ```kotlin
 * // Standard (zero-based x, y)
 * grid[2, 34]
 *
 * // Sheet column (letter) and row (one-based)
 * grid["E", 8]
 *
 * // Sheet notation string
 * grid["E8"]
 *
 * // Coordinate object
 * grid[Coordinate.parse("E8")]
 * ```
 *
 * ## Row and Column Access
 * ```kotlin
 * // Row view (lightweight, O(1))
 * val row = grid.rows[8]
 *
 * // Column view (lightweight, O(1))
 * val column = grid.columns["CG"]
 *
 * // Get copies of row/column data
 * val rowCopy = grid.getRowCopy(8)
 * val columnCopy = grid.getColumnCopy(4)
 * ```
 *
 * ## Change Listeners
 * ```kotlin
 * grid.addChangeListener(object : GridChangeListener<Int> {
 *     override fun onCellChanged(event: CellChangeEvent<Int>) {
 *         println("Cell changed: ${event.coordinate}")
 *     }
 * })
 * ```
 *
 * ## Common Operations
 * ```kotlin
 * val transposed = grid.transpose()
 * val subgrid = grid.subgrid(0, 0, 5, 5)
 * val mapped = grid.map { it * 2 }
 * grid.fill(0)
 * grid.forEach { x, y, value -> println("$x,$y = $value") }
 * val found = grid.find { it > 100 }
 * ```
 *
 * ## Storage
 * Grid uses a flat array with row-major ordering for optimal cache locality
 * and memory efficiency. Access is O(1) for all operations.
 *
 * @param T The type of values stored in the grid
 * @property width The number of columns
 * @property height The number of rows
 *
 * @see Coordinate
 * @see GridChangeListener
 * @see Ki.parse
 * @see Ki.format
 */
class Grid<T> constructor(
    val width: Int,
    val height: Int,
    val data: Array<Any?>,
    val elementType: Class<*>?
) {
    init {
        require(width > 0) { "Width must be positive, got: $width" }
        require(height > 0) { "Height must be positive, got: $height" }
        require(data.size == width * height) {
            "Data array size ${data.size} doesn't match grid dimensions $width x $height"
        }
    }

    // Lazy initialization for listeners - zero overhead when not used
    private var listeners: MutableList<GridChangeListener<T>>? = null

    /** The total number of cells in this grid. */
    val size: Int get() = width * height

    /** True if all cells are null. */
    val isEmpty: Boolean get() = data.all { it == null }

    /** True if any cell is non-null. */
    val isNotEmpty: Boolean get() = data.any { it != null }

    /** Provides indexed access to rows. */
    val rows: RowAccessor<T> = RowAccessor(this)

    /** Provides indexed access to columns. */
    val columns: ColumnAccessor<T> = ColumnAccessor(this)

    // ===== Cell Access Methods =====

    /**
     * Gets the value at the specified (x, y) coordinate.
     *
     * @param x The zero-based column index
     * @param y The zero-based row index
     * @return The value at (x, y), or null if the cell is empty
     * @throws IndexOutOfBoundsException if coordinates are out of range
     */
    @Suppress("UNCHECKED_CAST")
    operator fun get(x: Int, y: Int): T? {
        checkBounds(x, y)
        return data[index(x, y)] as T?
    }

    /**
     * Sets the value at the specified (x, y) coordinate.
     *
     * @param x The zero-based column index
     * @param y The zero-based row index
     * @param value The value to set (may be null)
     * @throws IndexOutOfBoundsException if coordinates are out of range
     */
    operator fun set(x: Int, y: Int, value: T?) {
        checkBounds(x, y)
        val idx = index(x, y)
        @Suppress("UNCHECKED_CAST")
        val oldValue = data[idx] as T?
        data[idx] = value
        fireOnCellChanged(Coordinate.standard(x, y), oldValue, value)
    }

    /**
     * Gets the value at the specified Coordinate.
     */
    operator fun get(coord: Coordinate): T? = get(coord.x, coord.y)

    /**
     * Sets the value at the specified Coordinate.
     */
    operator fun set(coord: Coordinate, value: T?) = set(coord.x, coord.y, value)

    /**
     * Gets the value using sheet notation (letter column, one-based row).
     *
     * @param column The column letter(s) (A, B, ..., Z, AA, ...)
     * @param row The one-based row number
     */
    operator fun get(column: String, row: Int): T? {
        val x = Coordinate.columnToIndex(column)
        val y = row - 1
        return get(x, y)
    }

    /**
     * Sets the value using sheet notation (letter column, one-based row).
     *
     * @param column The column letter(s) (A, B, ..., Z, AA, ...)
     * @param row The one-based row number
     * @param value The value to set
     */
    operator fun set(column: String, row: Int, value: T?) {
        val x = Coordinate.columnToIndex(column)
        val y = row - 1
        set(x, y, value)
    }

    /**
     * Gets the value using a sheet notation string (e.g., "A1", "E8", "AA100").
     *
     * @param ref The sheet notation reference
     */
    operator fun get(ref: String): T? {
        val coord = Coordinate.parse(ref)
        return get(coord)
    }

    /**
     * Sets the value using a sheet notation string (e.g., "A1", "E8", "AA100").
     *
     * @param ref The sheet notation reference
     * @param value The value to set
     */
    operator fun set(ref: String, value: T?) {
        val coord = Coordinate.parse(ref)
        set(coord, value)
    }

    /**
     * Gets all values within a coordinate range as a new Grid.
     */
    operator fun get(range: CoordinateRange): Grid<T> = subgrid(
        range.minX, range.minY, range.width, range.height
    )

    // ===== Row and Column Data Access =====

    /**
     * Returns a copy of the specified row's data.
     *
     * @param y The zero-based row index
     * @return A new list containing copies of the row's values
     */
    @Suppress("UNCHECKED_CAST")
    fun getRowCopy(y: Int): List<T?> {
        require(y in 0 until height) { "Row index out of bounds: $y" }
        val start = y * width
        return (0 until width).map { data[start + it] as T? }
    }

    /**
     * Returns a copy of the specified column's data.
     *
     * @param x The zero-based column index
     * @return A new list containing copies of the column's values
     */
    @Suppress("UNCHECKED_CAST")
    fun getColumnCopy(x: Int): List<T?> {
        require(x in 0 until width) { "Column index out of bounds: $x" }
        return (0 until height).map { data[it * width + x] as T? }
    }

    /**
     * Returns a copy of the column's data by letter.
     *
     * @param column The column letter(s) (A, B, ..., Z, AA, ...)
     */
    fun getColumnCopy(column: String): List<T?> =
        getColumnCopy(Coordinate.columnToIndex(column))

    /**
     * Sets an entire row's values.
     *
     * @param y The zero-based row index
     * @param values The values to set (must have exactly [width] elements)
     */
    fun setRow(y: Int, values: List<T?>) {
        require(y in 0 until height) { "Row index out of bounds: $y" }
        require(values.size == width) {
            "Row values must have exactly $width elements, got ${values.size}"
        }

        @Suppress("UNCHECKED_CAST")
        val oldValues = getRowCopy(y)
        val start = y * width
        values.forEachIndexed { i, value -> data[start + i] = value }
        fireOnRowChanged(y, RowChangeType.MODIFIED, oldValues, values)
    }

    /**
     * Sets an entire column's values.
     *
     * @param x The zero-based column index
     * @param values The values to set (must have exactly [height] elements)
     */
    fun setColumn(x: Int, values: List<T?>) {
        require(x in 0 until width) { "Column index out of bounds: $x" }
        require(values.size == height) {
            "Column values must have exactly $height elements, got ${values.size}"
        }

        @Suppress("UNCHECKED_CAST")
        val oldValues = getColumnCopy(x)
        values.forEachIndexed { i, value -> data[i * width + x] = value }
        fireOnColumnChanged(x, ColumnChangeType.MODIFIED, oldValues, values)
    }

    /**
     * Sets a column's values by letter.
     *
     * @param column The column letter(s) (A, B, ..., Z, AA, ...)
     * @param values The values to set
     */
    fun setColumn(column: String, values: List<T?>) =
        setColumn(Coordinate.columnToIndex(column), values)

    // ===== Common Operations =====

    /**
     * Creates a transposed copy of this grid (rows become columns).
     */
    fun transpose(): Grid<T> {
        val newData = Array<Any?>(size) { null }

        for (y in 0 until height) {
            for (x in 0 until width) {
                // Original position: (x, y) -> y * width + x
                // Transposed position: (y, x) in grid of (height, width)
                val oldIdx = y * width + x
                val newIdx = x * height + y
                newData[newIdx] = data[oldIdx]
            }
        }

        return Grid(height, width, newData, elementType)
    }

    /**
     * Extracts a rectangular subgrid.
     *
     * @param startX Starting column (inclusive)
     * @param startY Starting row (inclusive)
     * @param subWidth Width of the subgrid
     * @param subHeight Height of the subgrid
     * @return A new Grid containing the specified region
     */
    fun subgrid(startX: Int, startY: Int, subWidth: Int, subHeight: Int): Grid<T> {
        require(startX >= 0 && startY >= 0) { "Start coordinates must be non-negative" }
        require(subWidth > 0 && subHeight > 0) { "Subgrid dimensions must be positive" }
        require(startX + subWidth <= width) { "Subgrid extends past right edge" }
        require(startY + subHeight <= height) { "Subgrid extends past bottom edge" }

        val newData = Array<Any?>(subWidth * subHeight) { null }

        for (y in 0 until subHeight) {
            for (x in 0 until subWidth) {
                val srcIdx = (startY + y) * width + (startX + x)
                val dstIdx = y * subWidth + x
                newData[dstIdx] = data[srcIdx]
            }
        }

        return Grid(subWidth, subHeight, newData, elementType)
    }

    /**
     * Creates a new grid by applying a transformation to each cell.
     *
     * @param transform The transformation function
     * @return A new Grid with transformed values
     */
    inline fun <R> map(transform: (T?) -> R?): Grid<R> {
        val newData = Array<Any?>(size) { null }

        for (i in data.indices) {
            @Suppress("UNCHECKED_CAST")
            newData[i] = transform(data[i] as T?)
        }

        return Grid(width, height, newData, null)
    }

    /**
     * Creates a new grid by applying a transformation that includes coordinates.
     *
     * @param transform The transformation function receiving (x, y, value)
     * @return A new Grid with transformed values
     */
    inline fun <R> mapIndexed(transform: (x: Int, y: Int, value: T?) -> R?): Grid<R> {
        val newData = Array<Any?>(size) { null }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                @Suppress("UNCHECKED_CAST")
                newData[idx] = transform(x, y, data[idx] as T?)
            }
        }

        return Grid(width, height, newData, null)
    }

    /**
     * Iterates over all cells in the grid.
     *
     * @param action The action to perform for each cell value
     */
    inline fun forEach(action: (T?) -> Unit) {
        for (i in data.indices) {
            @Suppress("UNCHECKED_CAST")
            action(data[i] as T?)
        }
    }

    /**
     * Iterates over all cells with their coordinates.
     *
     * @param action The action to perform for each (x, y, value)
     */
    inline fun forEachIndexed(action: (x: Int, y: Int, value: T?) -> Unit) {
        for (y in 0 until height) {
            for (x in 0 until width) {
                @Suppress("UNCHECKED_CAST")
                action(x, y, data[y * width + x] as T?)
            }
        }
    }

    /**
     * Finds the first cell matching the predicate.
     *
     * @param predicate The condition to match
     * @return A Pair of (Coordinate, value), or null if not found
     */
    inline fun find(predicate: (T?) -> Boolean): Pair<Coordinate, T?>? {
        for (y in 0 until height) {
            for (x in 0 until width) {
                @Suppress("UNCHECKED_CAST")
                val value = data[y * width + x] as T?
                if (predicate(value)) {
                    return Pair(Coordinate.standard(x, y), value)
                }
            }
        }
        return null
    }

    /**
     * Finds all cells matching the predicate.
     *
     * @param predicate The condition to match
     * @return List of (Coordinate, value) pairs
     */
    inline fun findAll(predicate: (T?) -> Boolean): List<Pair<Coordinate, T?>> {
        val results = mutableListOf<Pair<Coordinate, T?>>()

        for (y in 0 until height) {
            for (x in 0 until width) {
                @Suppress("UNCHECKED_CAST")
                val value = data[y * width + x] as T?
                if (predicate(value)) {
                    results.add(Pair(Coordinate.standard(x, y), value))
                }
            }
        }

        return results
    }

    /**
     * Fills all cells with the specified value.
     */
    fun fill(value: T?) {
        for (i in data.indices) {
            data[i] = value
        }
        fireOnBulkChange(BulkChangeType.FILL, null, "Filled with value: $value")
    }

    /**
     * Fills a row with the specified value.
     *
     * @param y The zero-based row index
     * @param value The value to fill
     */
    fun fillRow(y: Int, value: T?) {
        require(y in 0 until height) { "Row index out of bounds: $y" }

        @Suppress("UNCHECKED_CAST")
        val oldValues = getRowCopy(y)
        val start = y * width
        for (x in 0 until width) {
            data[start + x] = value
        }
        val newValues = List(width) { value }
        fireOnRowChanged(y, RowChangeType.MODIFIED, oldValues, newValues)
    }

    /**
     * Fills a column with the specified value.
     *
     * @param x The zero-based column index
     * @param value The value to fill
     */
    fun fillColumn(x: Int, value: T?) {
        require(x in 0 until width) { "Column index out of bounds: $x" }

        @Suppress("UNCHECKED_CAST")
        val oldValues = getColumnCopy(x)
        for (y in 0 until height) {
            data[y * width + x] = value
        }
        val newValues = List(height) { value }
        fireOnColumnChanged(x, ColumnChangeType.MODIFIED, oldValues, newValues)
    }

    /**
     * Fills a column by letter with the specified value.
     *
     * @param column The column letter(s) (A, B, ..., Z, AA, ...)
     * @param value The value to fill
     */
    fun fillColumn(column: String, value: T?) =
        fillColumn(Coordinate.columnToIndex(column), value)

    /**
     * Clears all cells (sets them to null).
     */
    fun clear() {
        for (i in data.indices) {
            data[i] = null
        }
        fireOnBulkChange(BulkChangeType.CLEAR, null, "Grid cleared")
    }

    /**
     * Counts cells matching the predicate.
     */
    inline fun count(predicate: (T?) -> Boolean): Int {
        var count = 0
        for (i in data.indices) {
            @Suppress("UNCHECKED_CAST")
            if (predicate(data[i] as T?)) count++
        }
        return count
    }

    /**
     * Returns true if any cell matches the predicate.
     */
    inline fun any(predicate: (T?) -> Boolean): Boolean {
        for (i in data.indices) {
            @Suppress("UNCHECKED_CAST")
            if (predicate(data[i] as T?)) return true
        }
        return false
    }

    /**
     * Returns true if all cells match the predicate.
     */
    inline fun all(predicate: (T?) -> Boolean): Boolean {
        for (i in data.indices) {
            @Suppress("UNCHECKED_CAST")
            if (!predicate(data[i] as T?)) return false
        }
        return true
    }

    /**
     * Returns true if no cells match the predicate.
     */
    inline fun none(predicate: (T?) -> Boolean): Boolean = !any(predicate)

    /**
     * Creates a deep copy of this grid.
     */
    fun copy(): Grid<T> = Grid(width, height, data.copyOf(), elementType)

    /**
     * Returns all values as a flat list (row-major order).
     */
    @Suppress("UNCHECKED_CAST")
    fun toList(): List<T?> = data.map { it as T? }

    /**
     * Returns values as a list of rows.
     */
    fun toRowList(): List<List<T?>> = (0 until height).map { getRowCopy(it) }

    // ===== Listener Management =====

    /**
     * Adds a change listener to receive notifications about grid modifications.
     *
     * @param listener The listener to add
     */
    fun addChangeListener(listener: GridChangeListener<T>) {
        if (listeners == null) {
            listeners = mutableListOf()
        }
        listeners!!.add(listener)
    }

    /**
     * Removes a previously added change listener.
     *
     * @param listener The listener to remove
     * @return true if the listener was found and removed
     */
    fun removeChangeListener(listener: GridChangeListener<T>): Boolean {
        return listeners?.remove(listener) ?: false
    }

    /**
     * Returns true if this grid has any change listeners.
     */
    fun hasListeners(): Boolean = listeners?.isNotEmpty() ?: false

    // ===== Private Helper Methods =====

    private fun index(x: Int, y: Int): Int = y * width + x

    private fun checkBounds(x: Int, y: Int) {
        if (x < 0 || x >= width) {
            throw IndexOutOfBoundsException("Column $x out of bounds (0..${width - 1})")
        }
        if (y < 0 || y >= height) {
            throw IndexOutOfBoundsException("Row $y out of bounds (0..${height - 1})")
        }
    }

    private fun fireOnCellChanged(coord: Coordinate, oldValue: T?, newValue: T?) {
        listeners?.forEach { it.onCellChanged(CellChangeEvent(coord, oldValue, newValue)) }
    }

    private fun fireOnRowChanged(rowIndex: Int, type: RowChangeType, oldValues: List<T?>?, newValues: List<T?>?) {
        listeners?.forEach { it.onRowChanged(RowChangeEvent(rowIndex, type, oldValues, newValues)) }
    }

    private fun fireOnColumnChanged(colIndex: Int, type: ColumnChangeType, oldValues: List<T?>?, newValues: List<T?>?) {
        listeners?.forEach { it.onColumnChanged(ColumnChangeEvent(colIndex, type, oldValues, newValues)) }
    }

    private fun fireOnBulkChange(type: BulkChangeType, region: CoordinateRange?, description: String?) {
        listeners?.forEach { it.onBulkChange(BulkChangeEvent(type, region, description)) }
    }

    // ===== toString and Formatting =====

    /**
     * Returns the Ki literal representation of this grid.
     */
    override fun toString(): String = toKiLiteral()

    /**
     * Returns the Ki literal representation with optional type annotation.
     */
    fun toKiLiteral(includeType: Boolean = false): String {
        val builder = StringBuilder()

        if (includeType && elementType != null) {
            val typeName = when (elementType) {
                Int::class.java, java.lang.Integer::class.java -> "Int"
                Long::class.java, java.lang.Long::class.java -> "Long"
                Double::class.java, java.lang.Double::class.java -> "Double"
                Float::class.java, java.lang.Float::class.java -> "Float"
                String::class.java -> "String"
                Boolean::class.java, java.lang.Boolean::class.java -> "Bool"
                else -> elementType.simpleName
            }
            builder.append(".grid<$typeName>(\n")
        } else {
            builder.append(".grid(\n")
        }

        // Calculate column widths for alignment
        val colWidths = IntArray(width)
        for (y in 0 until height) {
            for (x in 0 until width) {
                @Suppress("UNCHECKED_CAST")
                val value = data[y * width + x] as T?
                val str = formatValue(value)
                colWidths[x] = maxOf(colWidths[x], str.length)
            }
        }

        // Output rows
        for (y in 0 until height) {
            builder.append("    ")
            for (x in 0 until width) {
                @Suppress("UNCHECKED_CAST")
                val value = data[y * width + x] as T?
                val str = formatValue(value)
                builder.append(str.padStart(colWidths[x]))
                if (x < width - 1) builder.append("  ")
            }
            builder.append("\n")
        }

        builder.append(")")
        return builder.toString()
    }

    private fun formatValue(value: T?): String = when (value) {
        null -> "nil"
        is String -> Ki.format(value)
        else -> value.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Grid<*>) return false
        if (width != other.width || height != other.height) return false
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + data.contentHashCode()
        return result
    }

    // ===== Companion Object: Factory Methods =====

    companion object : Parseable<Grid<*>> {

        /**
         * Creates a grid with the specified dimensions, initialized with the default value.
         *
         * @param width Number of columns
         * @param height Number of rows
         * @param defaultValue Value to initialize all cells with
         */
        @JvmStatic
        fun <T> of(width: Int, height: Int, defaultValue: T?): Grid<T> {
            val data = Array<Any?>(width * height) { defaultValue }
            val elementType: Class<*>? = defaultValue?.let { it::class.java }
            return Grid(width, height, data, elementType)
        }

        /**
         * Creates a grid with the specified dimensions, initialized with nulls.
         *
         * @param width Number of columns
         * @param height Number of rows
         */
        @JvmStatic
        fun <T> ofNulls(width: Int, height: Int): Grid<T> {
            return Grid(width, height, Array(width * height) { null }, null)
        }

        /**
         * Creates a grid from a list of rows.
         *
         * @param rows List of rows, where each row is a list of values
         * @throws WrongRowLengthException if rows have inconsistent lengths
         */
        @JvmStatic
        fun <T> fromRows(rows: List<List<T?>>): Grid<T> {
            require(rows.isNotEmpty()) { "Cannot create grid from empty row list" }

            val height = rows.size
            val width = rows[0].size

            require(width > 0) { "Row width must be positive" }

            // Validate all rows have the same width
            rows.forEachIndexed { index, row ->
                if (row.size != width) {
                    throw WrongRowLengthException(width, row.size, index)
                }
            }

            val data = Array<Any?>(width * height) { null }
            var elementType: Class<*>? = null

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val value = rows[y][x]
                    data[y * width + x] = value
                    if (elementType == null && value != null) {
                        elementType = value!!::class.java
                    }
                }
            }

            return Grid(width, height, data, elementType)
        }

        /**
         * Creates a grid from a vararg of rows.
         */
        @JvmStatic
        fun <T> fromRows(vararg rows: List<T?>): Grid<T> = fromRows(rows.toList())

        /**
         * Creates a single-row grid from a list of values.
         */
        @JvmStatic
        fun <T> fromSingleRow(values: List<T?>): Grid<T> = fromRows(listOf(values))

        /**
         * Creates a single-column grid from a list of values.
         */
        @JvmStatic
        fun <T> fromSingleColumn(values: List<T?>): Grid<T> {
            val rows = values.map { listOf(it) }
            return fromRows(rows)
        }

        /**
         * Creates a grid using a builder function.
         *
         * @param width Number of columns
         * @param height Number of rows
         * @param init Function called for each cell to compute its initial value
         */
        @JvmStatic
        inline fun <T> build(width: Int, height: Int, init: (x: Int, y: Int) -> T?): Grid<T> {
            val data = Array<Any?>(width * height) { null }
            var elementType: Class<*>? = null

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val value = init(x, y)
                    data[y * width + x] = value
                    if (elementType == null && value != null) {
                        elementType = value!!::class.java
                    }
                }
            }

            return Grid(width, height, data, elementType)
        }

        /**
         * Parses a Ki grid literal.
         *
         * **Note:** Full parsing requires the KD parser in Ki.KD.
         * This method provides basic parsing for simple grid literals.
         *
         * @param text The Ki grid literal string
         * @return The parsed Grid
         * @throws ParseException if the literal is malformed
         */
        override fun parseLiteral(text: String): Grid<*> {
            throw ParseException(
                "Grid literal parsing requires the KD parser. " +
                        "Use io.kixi.kd.KD.read() for full parsing support."
            )
        }

        /**
         * Checks if a string appears to be a Ki grid literal.
         */
        @JvmStatic
        fun isLiteral(text: String): Boolean {
            val trimmed = text.trim()
            return (trimmed.startsWith(".grid(") || trimmed.startsWith(".grid<")) &&
                    trimmed.endsWith(")")
        }
    }

    // ===== Row Accessor =====

    /**
     * Provides indexed access to rows as lightweight views.
     */
    class RowAccessor<T>(private val grid: Grid<T>) {
        /**
         * Gets a view of the specified row.
         * The view is lightweight and reflects live grid data.
         */
        operator fun get(y: Int): RowView<T> {
            require(y in 0 until grid.height) { "Row index out of bounds: $y" }
            return RowView(grid, y)
        }

        /** The number of rows. */
        val size: Int get() = grid.height

        /** Returns all rows as views. */
        fun toList(): List<RowView<T>> = (0 until grid.height).map { RowView(grid, it) }
    }

    /**
     * A lightweight view of a single row.
     * Changes to the grid are reflected in the view, and vice versa.
     */
    class RowView<T>(private val grid: Grid<T>, val index: Int) : AbstractList<T?>() {
        override val size: Int get() = grid.width

        @Suppress("UNCHECKED_CAST")
        override fun get(index: Int): T? {
            require(index in 0 until size) { "Column index out of bounds: $index" }
            return grid[index, this.index]
        }

        /** Sets a value in this row. */
        operator fun set(x: Int, value: T?) {
            grid[x, index] = value
        }

        /** Returns a copy of this row's data. */
        fun toCopy(): List<T?> = grid.getRowCopy(index)

        /** The row number (one-based, for sheet notation). */
        val rowNumber: Int get() = index + 1
    }

    // ===== Column Accessor =====

    /**
     * Provides indexed access to columns as lightweight views.
     */
    class ColumnAccessor<T>(private val grid: Grid<T>) {
        /**
         * Gets a view of the specified column by index.
         */
        operator fun get(x: Int): ColumnView<T> {
            require(x in 0 until grid.width) { "Column index out of bounds: $x" }
            return ColumnView(grid, x)
        }

        /**
         * Gets a view of the specified column by letter.
         */
        operator fun get(column: String): ColumnView<T> {
            val x = Coordinate.columnToIndex(column)
            return get(x)
        }

        /** The number of columns. */
        val size: Int get() = grid.width

        /** Returns all columns as views. */
        fun toList(): List<ColumnView<T>> = (0 until grid.width).map { ColumnView(grid, it) }
    }

    /**
     * A lightweight view of a single column.
     * Changes to the grid are reflected in the view, and vice versa.
     */
    class ColumnView<T>(private val grid: Grid<T>, val index: Int) : AbstractList<T?>() {
        override val size: Int get() = grid.height

        @Suppress("UNCHECKED_CAST")
        override fun get(index: Int): T? {
            require(index in 0 until size) { "Row index out of bounds: $index" }
            return grid[this.index, index]
        }

        /** Sets a value in this column. */
        operator fun set(y: Int, value: T?) {
            grid[index, y] = value
        }

        /** Returns a copy of this column's data. */
        fun toCopy(): List<T?> = grid.getColumnCopy(index)

        /** The column letter (for sheet notation). */
        val columnLetter: String get() = Coordinate.indexToColumn(index)
    }
}