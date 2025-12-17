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
            range.type shouldBe Range.Type.Inclusive
        }
    }

    context("exclusive ranges") {
        test("exclusive range excludes both endpoints") {
            val range = Range(0, 5, Range.Type.Exclusive)
            (0 in range) shouldBe false
            (5 in range) shouldBe false
            (3 in range) shouldBe true
        }

        test("exclusive left includes right, excludes left") {
            val range = Range(0, 5, Range.Type.ExclusiveLeft)
            (0 in range) shouldBe false
            (5 in range) shouldBe true
            (3 in range) shouldBe true
        }

        test("exclusive right includes left, excludes right") {
            val range = Range(0, 5, Range.Type.ExclusiveRight)
            (0 in range) shouldBe true
            (5 in range) shouldBe false
            (3 in range) shouldBe true
        }

        test("factory method creates exclusive range") {
            val range = Range.exclusive(1, 10)
            range.type shouldBe Range.Type.Exclusive
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

        test("reversed exclusive left") {
            val range = Range(5, 1, Range.Type.ExclusiveLeft)
            (5 in range) shouldBe false  // left is excluded
            (1 in range) shouldBe true   // right is included
            (3 in range) shouldBe true
        }

        test("reversed exclusive right") {
            val range = Range(5, 1, Range.Type.ExclusiveRight)
            (5 in range) shouldBe true   // left is included
            (1 in range) shouldBe false  // right is excluded
            (3 in range) shouldBe true
        }
    }

    context("open ranges") {
        test("open right (>= left)") {
            val range = Range.openRight(5)
            range.openRight shouldBe true
            range.isOpen shouldBe true
            range.isClosed shouldBe false
            (5 in range) shouldBe true
            (10 in range) shouldBe true
            (4 in range) shouldBe false
        }

        test("open left (<= right)") {
            val range = Range.openLeft(5)
            range.openLeft shouldBe true
            (5 in range) shouldBe true
            (0 in range) shouldBe true
            (-100 in range) shouldBe true
            (6 in range) shouldBe false
        }

        test("open right with exclusive left (> left)") {
            val range = Range(5, 5, Range.Type.ExclusiveLeft, openLeft = false, openRight = true)
            (5 in range) shouldBe false  // excluded
            (6 in range) shouldBe true
        }

        test("open left with exclusive right (< right)") {
            val range = Range(5, 5, Range.Type.ExclusiveRight, openLeft = true, openRight = false)
            (5 in range) shouldBe false  // excluded
            (4 in range) shouldBe true
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
            Range(0, 5, Range.Type.Exclusive).toString() shouldBe "0<..<5"
        }

        test("exclusive left") {
            Range(0, 5, Range.Type.ExclusiveLeft).toString() shouldBe "0<..5"
        }

        test("exclusive right") {
            Range(0, 5, Range.Type.ExclusiveRight).toString() shouldBe "0..<5"
        }

        test("open right") {
            Range.openRight(5).toString() shouldBe "5.._"
        }

        test("open left") {
            Range.openLeft(5).toString() shouldBe "_.." + "5"
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
            val range1 = Range.openRight(5)
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
            intersection!!.left shouldBe 5
            intersection.right shouldBe 10
        }

        test("non-overlapping ranges return null") {
            val range1 = Range(0, 5)
            val range2 = Range(10, 15)
            range1.intersect(range2) shouldBe null
        }

        test("throws for non-inclusive ranges") {
            val range1 = Range(0, 10, Range.Type.Exclusive)
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
            val range = Range.openRight(5)
            shouldThrow<IllegalArgumentException> {
                range.clamp(10)
            }
        }

        test("throws for exclusive ranges") {
            val range = Range(5, 10, Range.Type.Exclusive)
            shouldThrow<IllegalArgumentException> {
                range.clamp(3)
            }
        }
    }

    context("equality") {
        test("equal ranges") {
            val range1 = Range(0, 5, Range.Type.Exclusive)
            val range2 = Range(0, 5, Range.Type.Exclusive)
            range1 shouldBe range2
        }

        test("different bounds not equal") {
            val range1 = Range(0, 5)
            val range2 = Range(0, 10)
            range1 shouldNotBe range2
        }

        test("different types not equal") {
            val range1 = Range(0, 5, Range.Type.Inclusive)
            val range2 = Range(0, 5, Range.Type.Exclusive)
            range1 shouldNotBe range2
        }
    }
})