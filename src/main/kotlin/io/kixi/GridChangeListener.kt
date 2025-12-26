package io.kixi

/**
 * Listener interface for grid change events.
 *
 * Implement this interface to receive notifications when a [Grid] is modified.
 * Register listeners using [Grid.addChangeListener].
 *
 * ## Usage
 * ```kotlin
 * val grid = Grid.of(10, 10, 0)
 *
 * grid.addChangeListener(object : GridChangeListener<Int> {
 *     override fun onCellChanged(event: CellChangeEvent<Int>) {
 *         println("Cell ${event.coordinate} changed from ${event.oldValue} to ${event.newValue}")
 *     }
 *
 *     override fun onRowChanged(event: RowChangeEvent<Int>) {
 *         println("Row ${event.rowIndex} was ${event.type}")
 *     }
 *
 *     override fun onColumnChanged(event: ColumnChangeEvent<Int>) {
 *         println("Column ${event.columnIndex} was ${event.type}")
 *     }
 *
 *     override fun onBulkChange(event: BulkChangeEvent<Int>) {
 *         println("Bulk operation: ${event.type}")
 *     }
 * })
 * ```
 *
 * @param T The type of values stored in the grid
 * @see Grid
 * @see CellChangeEvent
 * @see RowChangeEvent
 * @see ColumnChangeEvent
 * @see BulkChangeEvent
 */
interface GridChangeListener<T> {

    /**
     * Called when a single cell value changes.
     *
     * @param event The event describing the change
     */
    fun onCellChanged(event: CellChangeEvent<T>) {}

    /**
     * Called when a row is inserted, deleted, or modified.
     *
     * @param event The event describing the change
     */
    fun onRowChanged(event: RowChangeEvent<T>) {}

    /**
     * Called when a column is inserted, deleted, or modified.
     *
     * @param event The event describing the change
     */
    fun onColumnChanged(event: ColumnChangeEvent<T>) {}

    /**
     * Called when a bulk operation (clear, fill, paste) occurs.
     *
     * @param event The event describing the change
     */
    fun onBulkChange(event: BulkChangeEvent<T>) {}
}

/**
 * Event fired when a single cell value changes.
 *
 * @property coordinate The coordinate of the changed cell
 * @property oldValue The previous value (may be null)
 * @property newValue The new value (may be null)
 */
data class CellChangeEvent<T>(
    val coordinate: Coordinate,
    val oldValue: T?,
    val newValue: T?
) {
    /** The x (column) index of the changed cell. */
    val x: Int get() = coordinate.x

    /** The y (row) index of the changed cell. */
    val y: Int get() = coordinate.y
}

/**
 * Types of row change operations.
 */
enum class RowChangeType {
    /** A new row was inserted. */
    INSERTED,
    /** An existing row was deleted. */
    DELETED,
    /** Row values were modified (e.g., via fillRow). */
    MODIFIED
}

/**
 * Event fired when a row is inserted, deleted, or modified.
 *
 * @property rowIndex The zero-based row index
 * @property type The type of change
 * @property oldValues The previous row values (for DELETED or MODIFIED)
 * @property newValues The new row values (for INSERTED or MODIFIED)
 */
data class RowChangeEvent<T>(
    val rowIndex: Int,
    val type: RowChangeType,
    val oldValues: List<T?>? = null,
    val newValues: List<T?>? = null
)

/**
 * Types of column change operations.
 */
enum class ColumnChangeType {
    /** A new column was inserted. */
    INSERTED,
    /** An existing column was deleted. */
    DELETED,
    /** Column values were modified (e.g., via fillColumn). */
    MODIFIED
}

/**
 * Event fired when a column is inserted, deleted, or modified.
 *
 * @property columnIndex The zero-based column index
 * @property type The type of change
 * @property oldValues The previous column values (for DELETED or MODIFIED)
 * @property newValues The new column values (for INSERTED or MODIFIED)
 */
data class ColumnChangeEvent<T>(
    val columnIndex: Int,
    val type: ColumnChangeType,
    val oldValues: List<T?>? = null,
    val newValues: List<T?>? = null
)

/**
 * Types of bulk change operations.
 */
enum class BulkChangeType {
    /** Grid was cleared (all cells set to null or default). */
    CLEAR,
    /** Grid was filled with a value. */
    FILL,
    /** A region was pasted or copied into the grid. */
    PASTE,
    /** Grid was transposed (rows and columns swapped). */
    TRANSPOSE,
    /** Grid was resized. */
    RESIZE
}

/**
 * Event fired when a bulk operation occurs.
 *
 * @property type The type of bulk operation
 * @property affectedRegion The coordinate range affected by the operation (if applicable)
 * @property description Additional description of the operation
 */
data class BulkChangeEvent<T>(
    val type: BulkChangeType,
    val affectedRegion: CoordinateRange? = null,
    val description: String? = null
)