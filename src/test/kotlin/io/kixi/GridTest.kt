package io.kixi

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

class GridTest : StringSpec({

    // ===== Grid Creation =====

    "of creates grid with default value" {
        val grid = Grid.of(3, 2, 0)

        grid.width shouldBe 3
        grid.height shouldBe 2
        grid.size shouldBe 6

        for (y in 0 until 2) {
            for (x in 0 until 3) {
                grid[x, y] shouldBe 0
            }
        }
    }

    "ofNulls creates grid with null values" {
        val grid = Grid.ofNulls<Int>(3, 3)

        grid.width shouldBe 3
        grid.height shouldBe 3
        grid.isEmpty shouldBe true

        for (y in 0 until 3) {
            for (x in 0 until 3) {
                grid[x, y].shouldBeNull()
            }
        }
    }

    "fromRows creates grid from row lists" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        grid.width shouldBe 3
        grid.height shouldBe 2
        grid[0, 0] shouldBe 1
        grid[1, 0] shouldBe 2
        grid[2, 0] shouldBe 3
        grid[0, 1] shouldBe 4
        grid[1, 1] shouldBe 5
        grid[2, 1] shouldBe 6
    }

    "fromRows rejects ragged rows" {
        shouldThrow<WrongRowLengthException> {
            Grid.fromRows(
                listOf(1, 2, 3),
                listOf(4, 5)  // Wrong length
            )
        }.also {
            it.expectedLength shouldBe 3
            it.actualLength shouldBe 2
            it.rowIndex shouldBe 1
        }
    }

    "fromSingleRow creates 1-row grid" {
        val grid = Grid.fromSingleRow(listOf(1, 2, 3, 4, 5))
        grid.width shouldBe 5
        grid.height shouldBe 1
    }

    "fromSingleColumn creates 1-column grid" {
        val grid = Grid.fromSingleColumn(listOf(1, 2, 3, 4, 5))
        grid.width shouldBe 1
        grid.height shouldBe 5
    }

    "build creates grid with initializer" {
        val grid = Grid.build(3, 3) { x, y -> x + y * 3 }

        grid[0, 0] shouldBe 0
        grid[1, 0] shouldBe 1
        grid[2, 0] shouldBe 2
        grid[0, 1] shouldBe 3
        grid[1, 1] shouldBe 4
        grid[2, 2] shouldBe 8
    }

    // ===== Cell Access =====

    "get/set with x,y coordinates" {
        val grid = Grid.of(3, 3, 0)

        grid[1, 2] = 42
        grid[1, 2] shouldBe 42
    }

    "get/set with Coordinate" {
        val grid = Grid.of(3, 3, 0)
        val coord = Coordinate.standard(1, 2)

        grid[coord] = 42
        grid[coord] shouldBe 42
    }

    "get/set with sheet column and row" {
        val grid = Grid.of(10, 10, 0)

        grid["E", 8] = 42
        grid["E", 8] shouldBe 42

        // E is column 4 (index), row 8 is index 7
        grid[4, 7] shouldBe 42
    }

    "get/set with sheet notation string" {
        val grid = Grid.of(10, 10, 0)

        grid["E8"] = 42
        grid["E8"] shouldBe 42
        grid[4, 7] shouldBe 42
    }

    "get with coordinate range returns subgrid" {
        val grid = Grid.build(5, 5) { x, y -> x + y * 5 }
        val range = Coordinate.standard(1, 1)..Coordinate.standard(2, 2)

        val sub = grid[range]
        sub.width shouldBe 2
        sub.height shouldBe 2
        sub[0, 0] shouldBe 6   // (1,1) in original
        sub[1, 1] shouldBe 12  // (2,2) in original
    }

    "access throws for out of bounds" {
        val grid = Grid.of(3, 3, 0)

        shouldThrow<IndexOutOfBoundsException> { grid[-1, 0] }
        shouldThrow<IndexOutOfBoundsException> { grid[0, -1] }
        shouldThrow<IndexOutOfBoundsException> { grid[3, 0] }
        shouldThrow<IndexOutOfBoundsException> { grid[0, 3] }
    }

    // ===== Row and Column Access =====

    "rows accessor provides row views" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        )

        val row0 = grid.rows[0]
        row0.size shouldBe 3
        row0[0] shouldBe 1
        row0[1] shouldBe 2
        row0[2] shouldBe 3

        val row2 = grid.rows[2]
        row2.shouldContainExactly(7, 8, 9)
    }

    "row view reflects grid changes" {
        val grid = Grid.of(3, 3, 0)
        val row = grid.rows[0]

        grid[1, 0] = 42
        row[1] shouldBe 42
    }

    "row view modifications update grid" {
        val grid = Grid.of(3, 3, 0)
        val row = grid.rows[1]

        row[0] = 100
        grid[0, 1] shouldBe 100
    }

    "columns accessor provides column views" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        )

        val col0 = grid.columns[0]
        col0.size shouldBe 3
        col0.shouldContainExactly(1, 4, 7)

        val col2 = grid.columns[2]
        col2.shouldContainExactly(3, 6, 9)
    }

    "columns accessor by letter" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        grid.columns["A"].shouldContainExactly(1, 4)
        grid.columns["C"].shouldContainExactly(3, 6)
    }

    "getRowCopy returns copy" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        val copy = grid.getRowCopy(0)
        copy.shouldContainExactly(1, 2, 3)

        // Modifying copy doesn't affect grid
        (copy as MutableList)[0] = 999
        grid[0, 0] shouldBe 1
    }

    "getColumnCopy returns copy" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        val copy = grid.getColumnCopy(1)
        copy.shouldContainExactly(2, 5)
    }

    "getColumnCopy by letter" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        grid.getColumnCopy("B").shouldContainExactly(2, 5)
    }

    "setRow replaces entire row" {
        val grid = Grid.of(3, 2, 0)
        grid.setRow(1, listOf(7, 8, 9))

        grid[0, 1] shouldBe 7
        grid[1, 1] shouldBe 8
        grid[2, 1] shouldBe 9
    }

    "setColumn replaces entire column" {
        val grid = Grid.of(3, 3, 0)
        grid.setColumn(1, listOf(10, 20, 30))

        grid[1, 0] shouldBe 10
        grid[1, 1] shouldBe 20
        grid[1, 2] shouldBe 30
    }

    // ===== Common Operations =====

    "transpose swaps rows and columns" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        val transposed = grid.transpose()

        transposed.width shouldBe 2
        transposed.height shouldBe 3
        transposed[0, 0] shouldBe 1
        transposed[1, 0] shouldBe 4
        transposed[0, 1] shouldBe 2
        transposed[1, 1] shouldBe 5
        transposed[0, 2] shouldBe 3
        transposed[1, 2] shouldBe 6
    }

    "subgrid extracts region" {
        val grid = Grid.build(5, 5) { x, y -> x + y * 5 }
        val sub = grid.subgrid(1, 1, 3, 2)

        sub.width shouldBe 3
        sub.height shouldBe 2
        sub[0, 0] shouldBe 6   // (1,1) -> 1 + 1*5 = 6
        sub[2, 1] shouldBe 13  // (3,2) -> 3 + 2*5 = 13
    }

    "map transforms values" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        val doubled = grid.map { it?.times(2) }

        doubled[0, 0] shouldBe 2
        doubled[2, 1] shouldBe 12
    }

    "mapIndexed provides coordinates" {
        val grid = Grid.of(3, 3, 0)
        val mapped = grid.mapIndexed { x, y, _ -> "$x,$y" }

        mapped[0, 0] shouldBe "0,0"
        mapped[2, 2] shouldBe "2,2"
    }

    "forEach iterates all cells" {
        val grid = Grid.fromRows(
            listOf(1, 2),
            listOf(3, 4)
        )

        var sum = 0
        grid.forEach { sum += it ?: 0 }
        sum shouldBe 10
    }

    "forEachIndexed provides coordinates" {
        val grid = Grid.of(2, 2, 1)
        val coords = mutableListOf<String>()

        grid.forEachIndexed { x, y, _ -> coords.add("$x,$y") }

        coords.shouldContainExactly("0,0", "1,0", "0,1", "1,1")
    }

    "find locates first match" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        val found = grid.find { it == 5 }
        found.shouldNotBeNull()
        found.first shouldBe Coordinate.standard(1, 1)
        found.second shouldBe 5
    }

    "find returns null when not found" {
        val grid = Grid.of(3, 3, 0)
        grid.find { it == 99 }.shouldBeNull()
    }

    "findAll locates all matches" {
        val grid = Grid.fromRows(
            listOf(1, 2, 1),
            listOf(1, 3, 1)
        )

        val found = grid.findAll { it == 1 }
        found.size shouldBe 4
    }

    "fill sets all cells" {
        val grid = Grid.of(3, 3, 0)
        grid.fill(42)

        grid.all { it == 42 } shouldBe true
    }

    "fillRow fills one row" {
        val grid = Grid.of(3, 3, 0)
        grid.fillRow(1, 99)

        grid[0, 0] shouldBe 0
        grid[0, 1] shouldBe 99
        grid[1, 1] shouldBe 99
        grid[2, 1] shouldBe 99
        grid[0, 2] shouldBe 0
    }

    "fillColumn fills one column" {
        val grid = Grid.of(3, 3, 0)
        grid.fillColumn(1, 99)

        grid[0, 0] shouldBe 0
        grid[1, 0] shouldBe 99
        grid[1, 1] shouldBe 99
        grid[1, 2] shouldBe 99
        grid[2, 0] shouldBe 0
    }

    "clear sets all cells to null" {
        val grid = Grid.of(3, 3, 42)
        grid.clear()

        grid.isEmpty shouldBe true
        grid.all { it == null } shouldBe true
    }

    "count counts matching cells" {
        val grid = Grid.fromRows(
            listOf(1, 2, 1),
            listOf(1, 3, 1)
        )

        grid.count { it == 1 } shouldBe 4
        grid.count { it == 2 } shouldBe 1
        grid.count { it == 99 } shouldBe 0
    }

    "any returns true if any match" {
        val grid = Grid.of(3, 3, 0)
        grid[1, 1] = 42

        grid.any { it == 42 } shouldBe true
        grid.any { it == 99 } shouldBe false
    }

    "all returns true if all match" {
        val grid = Grid.of(3, 3, 42)
        grid.all { it == 42 } shouldBe true

        grid[0, 0] = 0
        grid.all { it == 42 } shouldBe false
    }

    "none returns true if none match" {
        val grid = Grid.of(3, 3, 0)
        grid.none { it == 42 } shouldBe true

        grid[0, 0] = 42
        grid.none { it == 42 } shouldBe false
    }

    "copy creates independent copy" {
        val grid = Grid.of(3, 3, 0)
        val copy = grid.copy()

        copy[1, 1] = 99
        grid[1, 1] shouldBe 0
    }

    "toList returns flat list" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        grid.toList().shouldContainExactly(1, 2, 3, 4, 5, 6)
    }

    "toRowList returns list of rows" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        val rows = grid.toRowList()
        rows.size shouldBe 2
        rows[0].shouldContainExactly(1, 2, 3)
        rows[1].shouldContainExactly(4, 5, 6)
    }

    // ===== Change Listeners =====

    "change listener receives cell changes" {
        val grid = Grid.of(3, 3, 0)
        val events = mutableListOf<CellChangeEvent<Int>>()

        grid.addChangeListener(object : GridChangeListener<Int> {
            override fun onCellChanged(event: CellChangeEvent<Int>) {
                events.add(event)
            }
        })

        grid[1, 1] = 42

        events.size shouldBe 1
        events[0].coordinate shouldBe Coordinate.standard(1, 1)
        events[0].oldValue shouldBe 0
        events[0].newValue shouldBe 42
    }

    "change listener receives row changes" {
        val grid = Grid.of(3, 3, 0)
        val events = mutableListOf<RowChangeEvent<Int>>()

        grid.addChangeListener(object : GridChangeListener<Int> {
            override fun onRowChanged(event: RowChangeEvent<Int>) {
                events.add(event)
            }
        })

        grid.setRow(1, listOf(7, 8, 9))

        events.size shouldBe 1
        events[0].rowIndex shouldBe 1
        events[0].type shouldBe RowChangeType.MODIFIED
    }

    "change listener receives column changes" {
        val grid = Grid.of(3, 3, 0)
        val events = mutableListOf<ColumnChangeEvent<Int>>()

        grid.addChangeListener(object : GridChangeListener<Int> {
            override fun onColumnChanged(event: ColumnChangeEvent<Int>) {
                events.add(event)
            }
        })

        grid.fillColumn(1, 99)

        events.size shouldBe 1
        events[0].columnIndex shouldBe 1
        events[0].type shouldBe ColumnChangeType.MODIFIED
    }

    "change listener receives bulk changes" {
        val grid = Grid.of(3, 3, 0)
        val events = mutableListOf<BulkChangeEvent<Int>>()

        grid.addChangeListener(object : GridChangeListener<Int> {
            override fun onBulkChange(event: BulkChangeEvent<Int>) {
                events.add(event)
            }
        })

        grid.fill(42)
        grid.clear()

        events.size shouldBe 2
        events[0].type shouldBe BulkChangeType.FILL
        events[1].type shouldBe BulkChangeType.CLEAR
    }

    "removeChangeListener stops notifications" {
        val grid = Grid.of(3, 3, 0)
        var count = 0

        val listener = object : GridChangeListener<Int> {
            override fun onCellChanged(event: CellChangeEvent<Int>) {
                count++
            }
        }

        grid.addChangeListener(listener)
        grid[0, 0] = 1
        count shouldBe 1

        grid.removeChangeListener(listener)
        grid[0, 0] = 2
        count shouldBe 1
    }

    "hasListeners returns correct state" {
        val grid = Grid.of(3, 3, 0)
        grid.hasListeners() shouldBe false

        val listener = object : GridChangeListener<Int> {}
        grid.addChangeListener(listener)
        grid.hasListeners() shouldBe true

        grid.removeChangeListener(listener)
        grid.hasListeners() shouldBe false
    }

    // ===== String Representations =====

    "toString returns Ki literal" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        val str = grid.toString()
        // Grid.fromRows infers elementType, so type parameter is included
        str.contains(".grid<Int>") shouldBe true
        str.contains("1") shouldBe true
        str.contains("6") shouldBe true
    }

    "toKiLiteral with type" {
        val grid = Grid.of(2, 2, 42)
        val str = grid.toKiLiteral(includeType = true)
        str.contains(".grid<") shouldBe true
    }

    "isLiteral detects grid literals" {
        Grid.isLiteral(".grid(1 2 3)") shouldBe true
        Grid.isLiteral(".grid<Int>(1 2 3)") shouldBe true
        Grid.isLiteral(".blob(abc)") shouldBe false
    }

    // ===== Equality =====

    "equals compares content" {
        val grid1 = Grid.fromRows(
            listOf(1, 2),
            listOf(3, 4)
        )

        val grid2 = Grid.fromRows(
            listOf(1, 2),
            listOf(3, 4)
        )

        val grid3 = Grid.fromRows(
            listOf(1, 2),
            listOf(3, 5)
        )

        grid1 shouldBe grid2
        grid1 shouldNotBe grid3
    }

    "equals considers dimensions" {
        val grid1 = Grid.fromRows(
            listOf(1, 2, 3, 4)
        )

        val grid2 = Grid.fromRows(
            listOf(1, 2),
            listOf(3, 4)
        )

        grid1 shouldNotBe grid2
    }

    // ===== Mixed Type Grid =====

    "Grid<Any> holds mixed types" {
        val grid = Grid.fromRows(
            listOf<Any?>(1, "hello", 3.14),
            listOf<Any?>(true, null, 'x')
        )

        grid[0, 0] shouldBe 1
        grid[1, 0] shouldBe "hello"
        grid[2, 0] shouldBe 3.14
        grid[0, 1] shouldBe true
        grid[1, 1].shouldBeNull()
        grid[2, 1] shouldBe 'x'
    }

    // ===== Row/Column View Properties =====

    "row view has correct properties" {
        val grid = Grid.of(5, 3, 0)
        val row = grid.rows[1]

        row.index shouldBe 1
        row.rowNumber shouldBe 2
        row.size shouldBe 5
    }

    "column view has correct properties" {
        val grid = Grid.of(5, 3, 0)
        val col = grid.columns[2]

        col.index shouldBe 2
        col.columnLetter shouldBe "C"
        col.size shouldBe 3
    }

    // ===== Nullability Tests =====

    "of with non-null default has elementNullable=false" {
        val grid = Grid.of(3, 3, 42)
        grid.elementNullable shouldBe false
        grid.elementType shouldBe Int::class.javaObjectType
    }

    "of with null default has elementNullable=true" {
        val grid = Grid.of<Int>(3, 3, null)
        grid.elementNullable shouldBe true
    }

    "ofNulls has elementNullable=true" {
        val grid = Grid.ofNulls<Int>(3, 3)
        grid.elementNullable shouldBe true
    }

    "fromRows with no nulls has elementNullable=false" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )
        grid.elementNullable shouldBe false
    }

    "fromRows with nulls has elementNullable=true" {
        val grid = Grid.fromRows(
            listOf<Int?>(1, null, 3),
            listOf<Int?>(4, 5, 6)
        )
        grid.elementNullable shouldBe true
    }

    "build with no nulls has elementNullable=false" {
        val grid = Grid.build(3, 3) { x, y -> x + y }
        grid.elementNullable shouldBe false
    }

    "build with nulls has elementNullable=true" {
        val grid = Grid.build(3, 3) { x, y ->
            if (x == 1 && y == 1) null else x + y
        }
        grid.elementNullable shouldBe true
    }

    "copy preserves elementNullable" {
        val grid1 = Grid.fromRows(
            listOf<Int?>(1, null, 3),
            listOf<Int?>(4, 5, 6)
        )
        val grid2 = grid1.copy()
        grid2.elementNullable shouldBe true
    }

    "transpose preserves elementNullable" {
        val grid = Grid.fromRows(
            listOf<Int?>(1, null, 3),
            listOf<Int?>(4, 5, 6)
        )
        val transposed = grid.transpose()
        transposed.elementNullable shouldBe true
    }

    "subgrid detects nullability in region" {
        val grid = Grid.fromRows(
            listOf<Int?>(1, 2, 3),
            listOf<Int?>(4, null, 6),
            listOf<Int?>(7, 8, 9)
        )

        // Subgrid with null
        val sub1 = grid.subgrid(1, 1, 2, 2)
        sub1.elementNullable shouldBe true

        // Subgrid without null
        val sub2 = grid.subgrid(0, 0, 2, 1)
        sub2.elementNullable shouldBe false
    }

    "map detects nullability from transformed values" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )

        // Map with nulls
        val mapped1 = grid.map { if (it == 2) null else it }
        mapped1.elementNullable shouldBe true

        // Map without nulls
        val mapped2 = grid.map { (it ?: 0) * 2 }
        mapped2.elementNullable shouldBe false
    }

    "toKiLiteral includes nullable suffix when elementNullable=true" {
        val grid = Grid.fromRows(
            listOf<Int?>(1, null, 3),
            listOf<Int?>(4, 5, 6)
        )
        val str = grid.toKiLiteral()
        str.contains(".grid<Int?>") shouldBe true
    }

    "toKiLiteral excludes nullable suffix when elementNullable=false" {
        val grid = Grid.fromRows(
            listOf(1, 2, 3),
            listOf(4, 5, 6)
        )
        val str = grid.toKiLiteral()
        str.contains(".grid<Int>") shouldBe true
        str.contains(".grid<Int?>") shouldBe false
    }
})