package io.kixi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.assertions.throwables.shouldThrow

class RangeTest : FunSpec({

    context("inclusive ranges") {
        test("integer range contains endpoints") {
            val range = Range(0, 5)
            (0 in range) shouldBe true
            (5 in range) shouldBe true
            (3 in range) shouldBe true
        }

        test("integer range excludes outside values") {
            val range = Range(0, 5)
            (-1 in range) shouldBe false
            (6 in range) shouldBe false
        }

        test("factory method creates inclusive range") {
            val range = Range.inclusive(1, 10)
            range.bound shouldBe Range.Bound.Inclusive
        }
    }

    context("exclusive ranges") {
        test("exclusive range excludes both endpoints") {
            val range = Range(0, 5, Range.Bound.Exclusive)
            (0 in range) shouldBe false
            (5 in range) shouldBe false
            (3 in range) shouldBe true
        }

        test("exclusive start includes end, excludes start") {
            val range = Range(0, 5, Range.Bound.ExclusiveStart)
            (0 in range) shouldBe false
            (5 in range) shouldBe true
            (3 in range) shouldBe true
        }

        test("exclusive end includes start, excludes end") {
            val range = Range(0, 5, Range.Bound.ExclusiveEnd)
            (0 in range) shouldBe true
            (5 in range) shouldBe false
            (3 in range) shouldBe true
        }

        test("factory method creates exclusive range") {
            val range = Range.exclusive(1, 10)
            range.bound shouldBe Range.Bound.Exclusive
        }
    }

    context("reversed ranges") {
        test("reversed range detected correctly") {
            val range = Range(5, 1)
            range.reversed shouldBe true
        }

        test("normal range not reversed") {
            val range = Range(1, 5)
            range.reversed shouldBe false
        }

        test("reversed inclusive range contains values") {
            val range = Range(5, 1)
            (3 in range) shouldBe true
            (1 in range) shouldBe true
            (5 in range) shouldBe true
        }

        test("reversed exclusive start") {
            val range = Range(5, 1, Range.Bound.ExclusiveStart)
            (5 in range) shouldBe false  // start is excluded
            (1 in range) shouldBe true   // end is included
            (3 in range) shouldBe true
        }

        test("reversed exclusive end") {
            val range = Range(5, 1, Range.Bound.ExclusiveEnd)
            (5 in range) shouldBe true   // start is included
            (1 in range) shouldBe false  // end is excluded
            (3 in range) shouldBe true
        }
    }

    context("open ranges") {
        test("open end (>= start)") {
            val range = Range.openEnd(5)
            range.isOpenEnd shouldBe true
            range.isOpen shouldBe true
            range.isClosed shouldBe false
            (5 in range) shouldBe true
            (10 in range) shouldBe true
            (4 in range) shouldBe false
        }

        test("open start (<= end)") {
            val range = Range.openStart(5)
            range.isOpenStart shouldBe true
            (5 in range) shouldBe true
            (0 in range) shouldBe true
            (-100 in range) shouldBe true
            (6 in range) shouldBe false
        }

        test("open end with exclusive start (> start)") {
            val range = Range<Int>(5, null, Range.Bound.ExclusiveStart)
            (5 in range) shouldBe false  // excluded
            (6 in range) shouldBe true
        }

        test("open start with exclusive end (< end)") {
            val range = Range<Int>(null, 5, Range.Bound.ExclusiveEnd)
            (5 in range) shouldBe false  // excluded
            (4 in range) shouldBe true
        }

        test("open start has null start") {
            val range = Range.openStart(10)
            range.start shouldBe null
            range.end shouldBe 10
        }

        test("open end has null end") {
            val range = Range.openEnd(10)
            range.start shouldBe 10
            range.end shouldBe null
        }

        test("open ranges have null min and max") {
            Range.openEnd(5).min shouldBe null
            Range.openEnd(5).max shouldBe null
            Range.openStart(5).min shouldBe null
            Range.openStart(5).max shouldBe null
        }

        test("open range is not reversed") {
            Range.openEnd(5).reversed shouldBe false
            Range.openStart(5).reversed shouldBe false
        }

        test("both null rejected") {
            shouldThrow<IllegalArgumentException> {
                Range<Int>(null, null)
            }
        }

        test("invalid exclusivity for open start rejected") {
            // Can't exclude a start that doesn't exist
            shouldThrow<IllegalArgumentException> {
                Range<Int>(null, 5, Range.Bound.ExclusiveStart)
            }
            shouldThrow<IllegalArgumentException> {
                Range<Int>(null, 5, Range.Bound.Exclusive)
            }
        }

        test("invalid exclusivity for open end rejected") {
            // Can't exclude an end that doesn't exist
            shouldThrow<IllegalArgumentException> {
                Range<Int>(5, null, Range.Bound.ExclusiveEnd)
            }
            shouldThrow<IllegalArgumentException> {
                Range<Int>(5, null, Range.Bound.Exclusive)
            }
        }
    }

    context("min and max") {
        test("min and max for normal range") {
            val range = Range(3, 7)
            range.min shouldBe 3
            range.max shouldBe 7
        }

        test("min and max for reversed range") {
            val range = Range(7, 3)
            range.min shouldBe 3
            range.max shouldBe 7
        }
    }

    context("toString formatting") {
        test("inclusive range") {
            Range(0, 5).toString() shouldBe "0..5"
        }

        test("exclusive range") {
            Range(0, 5, Range.Bound.Exclusive).toString() shouldBe "0<..<5"
        }

        test("exclusive start") {
            Range(0, 5, Range.Bound.ExclusiveStart).toString() shouldBe "0<..5"
        }

        test("exclusive end") {
            Range(0, 5, Range.Bound.ExclusiveEnd).toString() shouldBe "0..<5"
        }

        test("open end") {
            Range.openEnd(5).toString() shouldBe "5.._"
        }

        test("open start") {
            Range.openStart(5).toString() shouldBe "_..5"
        }
    }

    context("different comparable types") {
        test("string range") {
            val range = Range("apple", "mango")
            ("banana" in range) shouldBe true
            ("zebra" in range) shouldBe false
        }

        test("double range") {
            val range = Range(1.5, 3.5)
            (2.0 in range) shouldBe true
            (4.0 in range) shouldBe false
        }

        test("version range") {
            val range = Range(Version(1, 0, 0), Version(2, 0, 0))
            (Version(1, 5, 0) in range) shouldBe true
            (Version(2, 1, 0) in range) shouldBe false
        }
    }

    context("overlaps") {
        test("overlapping ranges") {
            val range1 = Range(0, 10)
            val range2 = Range(5, 15)
            range1.overlaps(range2) shouldBe true
        }

        test("non-overlapping ranges") {
            val range1 = Range(0, 5)
            val range2 = Range(10, 15)
            range1.overlaps(range2) shouldBe false
        }

        test("adjacent ranges overlap at endpoint") {
            val range1 = Range(0, 5)
            val range2 = Range(5, 10)
            range1.overlaps(range2) shouldBe true
        }

        test("throws for open ranges") {
            val range1 = Range.openEnd(5)
            val range2 = Range(0, 10)
            shouldThrow<IllegalArgumentException> {
                range1.overlaps(range2)
            }
        }
    }

    context("intersection") {
        test("intersecting ranges") {
            val range1 = Range(0, 10)
            val range2 = Range(5, 15)
            val intersection = range1.intersect(range2)
            intersection shouldNotBe null
            intersection!!.start shouldBe 5
            intersection.end shouldBe 10
        }

        test("non-overlapping ranges return null") {
            val range1 = Range(0, 5)
            val range2 = Range(10, 15)
            range1.intersect(range2) shouldBe null
        }

        test("throws for non-inclusive ranges") {
            val range1 = Range(0, 10, Range.Bound.Exclusive)
            val range2 = Range(5, 15)
            shouldThrow<IllegalArgumentException> {
                range1.intersect(range2)
            }
        }
    }

    context("clamp") {
        test("clamps below min") {
            val range = Range(5, 10)
            range.clamp(3) shouldBe 5
        }

        test("clamps above max") {
            val range = Range(5, 10)
            range.clamp(15) shouldBe 10
        }

        test("returns value within range unchanged") {
            val range = Range(5, 10)
            range.clamp(7) shouldBe 7
        }

        test("throws for open ranges") {
            val range = Range.openEnd(5)
            shouldThrow<IllegalArgumentException> {
                range.clamp(10)
            }
        }

        test("throws for exclusive ranges") {
            val range = Range(5, 10, Range.Bound.Exclusive)
            shouldThrow<IllegalArgumentException> {
                range.clamp(3)
            }
        }
    }

    context("equality") {
        test("equal ranges") {
            val range1 = Range(0, 5, Range.Bound.Exclusive)
            val range2 = Range(0, 5, Range.Bound.Exclusive)
            range1 shouldBe range2
        }

        test("different bounds not equal") {
            val range1 = Range(0, 5)
            val range2 = Range(0, 10)
            range1 shouldNotBe range2
        }

        test("different types not equal") {
            val range1 = Range(0, 5, Range.Bound.Inclusive)
            val range2 = Range(0, 5, Range.Bound.Exclusive)
            range1 shouldNotBe range2
        }

        test("open vs closed not equal") {
            val range1 = Range<Int>(null, 5)
            val range2 = Range(0, 5)
            range1 shouldNotBe range2
        }
    }
})