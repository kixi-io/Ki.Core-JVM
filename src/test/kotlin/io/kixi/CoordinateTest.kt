package io.kixi

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kixi.text.ParseException

class CoordinateTest : StringSpec({

    // ===== Standard Notation =====

    "standard notation creates correct coordinates" {
        val coord = Coordinate.standard(4, 7)
        coord.x shouldBe 4
        coord.y shouldBe 7
        coord.column shouldBe "E"
        coord.row shouldBe 8
    }

    "standard notation with z coordinate" {
        val coord = Coordinate.standard(0, 0, 5)
        coord.x shouldBe 0
        coord.y shouldBe 0
        coord.z shouldBe 5
        coord.hasZ shouldBe true
    }

    "standard notation rejects negative x" {
        shouldThrow<IllegalArgumentException> {
            Coordinate.standard(-1, 0)
        }
    }

    "standard notation rejects negative y" {
        shouldThrow<IllegalArgumentException> {
            Coordinate.standard(0, -1)
        }
    }

    // ===== Sheet Notation =====

    "sheet notation creates correct coordinates" {
        val coord = Coordinate.sheet("E", 8)
        coord.x shouldBe 4
        coord.y shouldBe 7
        coord.column shouldBe "E"
        coord.row shouldBe 8
    }

    "sheet notation with z coordinate" {
        val coord = Coordinate.sheet("A", 1, 10)
        coord.x shouldBe 0
        coord.y shouldBe 0
        coord.z shouldBe 10
    }

    "sheet notation A1 equals standard 0,0" {
        val sheet = Coordinate.sheet("A", 1)
        val standard = Coordinate.standard(0, 0)
        sheet shouldBe standard
    }

    "sheet notation handles multi-letter columns" {
        val coord = Coordinate.sheet("AA", 1)
        coord.x shouldBe 26
        coord.column shouldBe "AA"

        val coord2 = Coordinate.sheet("AZ", 1)
        coord2.x shouldBe 51

        val coord3 = Coordinate.sheet("BA", 1)
        coord3.x shouldBe 52
    }

    "sheet notation rejects invalid column" {
        shouldThrow<IllegalArgumentException> {
            Coordinate.sheet("", 1)
        }
        shouldThrow<IllegalArgumentException> {
            Coordinate.sheet("A1", 1)  // Numbers not allowed in column
        }
    }

    "sheet notation rejects row less than 1" {
        shouldThrow<IllegalArgumentException> {
            Coordinate.sheet("A", 0)
        }
    }

    // ===== Column Index Conversion =====

    "indexToColumn converts correctly" {
        Coordinate.indexToColumn(0) shouldBe "A"
        Coordinate.indexToColumn(25) shouldBe "Z"
        Coordinate.indexToColumn(26) shouldBe "AA"
        Coordinate.indexToColumn(27) shouldBe "AB"
        Coordinate.indexToColumn(51) shouldBe "AZ"
        Coordinate.indexToColumn(52) shouldBe "BA"
        Coordinate.indexToColumn(701) shouldBe "ZZ"
        Coordinate.indexToColumn(702) shouldBe "AAA"
    }

    "columnToIndex converts correctly" {
        Coordinate.columnToIndex("A") shouldBe 0
        Coordinate.columnToIndex("Z") shouldBe 25
        Coordinate.columnToIndex("AA") shouldBe 26
        Coordinate.columnToIndex("AB") shouldBe 27
        Coordinate.columnToIndex("AZ") shouldBe 51
        Coordinate.columnToIndex("BA") shouldBe 52
        Coordinate.columnToIndex("ZZ") shouldBe 701
        Coordinate.columnToIndex("AAA") shouldBe 702
    }

    "column conversion is case insensitive" {
        Coordinate.columnToIndex("a") shouldBe 0
        Coordinate.columnToIndex("aa") shouldBe 26
    }

    "column conversion roundtrips" {
        for (i in 0..1000) {
            val column = Coordinate.indexToColumn(i)
            Coordinate.columnToIndex(column) shouldBe i
        }
    }

    // ===== String Parsing =====

    "parse sheet notation string" {
        val coord = Coordinate.parse("A1")
        coord.x shouldBe 0
        coord.y shouldBe 0

        val coord2 = Coordinate.parse("E8")
        coord2.x shouldBe 4
        coord2.y shouldBe 7

        val coord3 = Coordinate.parse("AA100")
        coord3.x shouldBe 26
        coord3.y shouldBe 99
    }

    "parse standard notation string" {
        val coord = Coordinate.parse("0,0")
        coord.x shouldBe 0
        coord.y shouldBe 0

        val coord2 = Coordinate.parse("4, 7")
        coord2.x shouldBe 4
        coord2.y shouldBe 7

        val coord3 = Coordinate.parse("100,200")
        coord3.x shouldBe 100
        coord3.y shouldBe 200
    }

    "parse standard notation with z" {
        val coord = Coordinate.parse("1,2,3")
        coord.x shouldBe 1
        coord.y shouldBe 2
        coord.z shouldBe 3
    }

    "parse rejects empty string" {
        shouldThrow<ParseException> {
            Coordinate.parse("")
        }
    }

    "parse rejects invalid format" {
        shouldThrow<ParseException> {
            Coordinate.parse("not a coordinate")
        }
    }

    "parseOrNull returns null for invalid input" {
        Coordinate.parseOrNull("invalid").shouldBeNull()
        Coordinate.parseOrNull("A1").shouldNotBeNull()
    }

    // ===== Ki Literal Parsing =====

    "parseLiteral standard notation" {
        val coord = Coordinate.parseLiteral(".coordinate(x=4, y=7)")
        coord.x shouldBe 4
        coord.y shouldBe 7
    }

    "parseLiteral standard notation with z" {
        val coord = Coordinate.parseLiteral(".coordinate(x=1, y=2, z=3)")
        coord.x shouldBe 1
        coord.y shouldBe 2
        coord.z shouldBe 3
    }

    "parseLiteral sheet notation" {
        val coord = Coordinate.parseLiteral(".coordinate(c=\"E\", r=8)")
        coord.x shouldBe 4
        coord.y shouldBe 7
    }

    "parseLiteral sheet notation with z" {
        val coord = Coordinate.parseLiteral(".coordinate(c=\"AA\", r=100, z=5)")
        coord.x shouldBe 26
        coord.y shouldBe 99
        coord.z shouldBe 5
    }

    "parseLiteral rejects invalid prefix" {
        shouldThrow<ParseException> {
            Coordinate.parseLiteral("coordinate(x=0, y=0)")
        }
    }

    "parseLiteral rejects missing parameters" {
        shouldThrow<ParseException> {
            Coordinate.parseLiteral(".coordinate(x=0)")
        }
    }

    "isLiteral detects coordinate literals" {
        Coordinate.isLiteral(".coordinate(x=0, y=0)") shouldBe true
        Coordinate.isLiteral(".coordinate(c=\"A\", r=1)") shouldBe true
        Coordinate.isLiteral("A1") shouldBe false
        Coordinate.isLiteral(".geo(0, 0)") shouldBe false
    }

    // ===== Navigation Methods =====

    "offset creates new coordinate" {
        val coord = Coordinate.standard(5, 5)
        val offset = coord.offset(dx = 2, dy = 3)
        offset.x shouldBe 7
        offset.y shouldBe 8
    }

    "offset with z" {
        val coord = Coordinate.standard(5, 5, 5)
        val offset = coord.offset(dx = 1, dy = 1, dz = 1)
        offset.x shouldBe 6
        offset.y shouldBe 6
        offset.z shouldBe 6
    }

    "offset rejects negative result" {
        val coord = Coordinate.standard(1, 1)
        shouldThrow<IllegalArgumentException> {
            coord.offset(dx = -2)
        }
    }

    "directional navigation" {
        val coord = Coordinate.standard(5, 5)

        coord.right().x shouldBe 6
        coord.right(3).x shouldBe 8
        coord.left().x shouldBe 4
        coord.down().y shouldBe 6
        coord.up().y shouldBe 4
    }

    // ===== Coordinate Modifiers =====

    "withZ adds z coordinate" {
        val coord = Coordinate.standard(0, 0)
        coord.hasZ shouldBe false

        val withZ = coord.withZ(5)
        withZ.z shouldBe 5
        withZ.hasZ shouldBe true
    }

    "withoutZ removes z coordinate" {
        val coord = Coordinate.standard(0, 0, 5)
        val without = coord.withoutZ()
        without.z shouldBe null
        without.hasZ shouldBe false
    }

    // ===== String Representations =====

    "toSheetNotation returns correct format" {
        Coordinate.standard(0, 0).toSheetNotation() shouldBe "A1"
        Coordinate.standard(4, 7).toSheetNotation() shouldBe "E8"
        Coordinate.standard(26, 99).toSheetNotation() shouldBe "AA100"
    }

    "toStandardNotation returns correct format" {
        Coordinate.standard(0, 0).toStandardNotation() shouldBe "0,0"
        Coordinate.standard(4, 7).toStandardNotation() shouldBe "4,7"
        Coordinate.standard(1, 2, 3).toStandardNotation() shouldBe "1,2,3"
    }

    "toKiLiteral returns correct format" {
        Coordinate.standard(4, 7).toKiLiteral() shouldBe ".coordinate(x=4, y=7)"
        Coordinate.standard(1, 2, 3).toKiLiteral() shouldBe ".coordinate(x=1, y=2, z=3)"
    }

    "toString returns Ki literal" {
        Coordinate.standard(4, 7).toString() shouldBe ".coordinate(x=4, y=7)"
    }

    // ===== Equality and Comparison =====

    "equals compares all components" {
        val a = Coordinate.standard(1, 2)
        val b = Coordinate.standard(1, 2)
        val c = Coordinate.standard(1, 3)
        val d = Coordinate.standard(1, 2, 5)

        a shouldBe b
        a shouldNotBe c
        a shouldNotBe d
    }

    "compareTo orders by y then x" {
        val coords = listOf(
            Coordinate.standard(2, 1),
            Coordinate.standard(0, 2),
            Coordinate.standard(1, 0),
            Coordinate.standard(0, 0)
        ).sorted()

        coords[0] shouldBe Coordinate.standard(0, 0)
        coords[1] shouldBe Coordinate.standard(1, 0)
        coords[2] shouldBe Coordinate.standard(2, 1)
        coords[3] shouldBe Coordinate.standard(0, 2)
    }

    // ===== Coordinate Range =====

    "rangeTo creates coordinate range" {
        val start = Coordinate.standard(0, 0)
        val end = Coordinate.standard(2, 2)
        val range = start..end

        range.width shouldBe 3
        range.height shouldBe 3
        range.size shouldBe 9
    }

    "coordinate range contains" {
        val range = Coordinate.standard(1, 1)..Coordinate.standard(3, 3)

        (Coordinate.standard(2, 2) in range) shouldBe true
        (Coordinate.standard(0, 0) in range) shouldBe false
        (Coordinate.standard(4, 4) in range) shouldBe false
    }

    "coordinate range iteration" {
        val range = Coordinate.standard(0, 0)..Coordinate.standard(1, 1)
        val coords = range.toList()

        coords.size shouldBe 4
        coords[0] shouldBe Coordinate.standard(0, 0)
        coords[1] shouldBe Coordinate.standard(1, 0)
        coords[2] shouldBe Coordinate.standard(0, 1)
        coords[3] shouldBe Coordinate.standard(1, 1)
    }

    "coordinate range with reversed corners" {
        val range = Coordinate.standard(2, 2)..Coordinate.standard(0, 0)

        range.minX shouldBe 0
        range.maxX shouldBe 2
        range.minY shouldBe 0
        range.maxY shouldBe 2
        range.topLeft shouldBe Coordinate.standard(0, 0)
        range.bottomRight shouldBe Coordinate.standard(2, 2)
    }

    // ===== Special Cases =====

    "ORIGIN is (0, 0)" {
        Coordinate.ORIGIN.x shouldBe 0
        Coordinate.ORIGIN.y shouldBe 0
        Coordinate.ORIGIN.isOrigin shouldBe true
    }

    "isOrigin returns true only for origin" {
        Coordinate.standard(0, 0).isOrigin shouldBe true
        Coordinate.standard(1, 0).isOrigin shouldBe false
        Coordinate.standard(0, 1).isOrigin shouldBe false
    }
})