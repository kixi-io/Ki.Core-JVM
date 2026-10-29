package io.kixi

import io.kixi.text.ParseException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.math.BigDecimal as Dec

class VectorTest : StringSpec({

    fun v(text: String) = Vector.parse(text)

    // ========== Literal ==========

    "literals read back as written" {
        for (text in listOf(
            ".vec<Float> { 1.0 2.5 3.1 }", ".vec<Int> { 3 1 4 1 5 }", ".vec<Long> { 9000000000 -1 }",
            ".vec<Double> { 0.1 1.0E-5 -2.5 }", ".vec<Dec> { 0.1 2 -3.25 }", ".vec<Int> { }",
        )) {
            v(text).toString() shouldBe text
            v(v(text).toString()) shouldBe v(text)
        }
    }

    "numbers take the declared type, with or without their own suffix" {
        v(".vec<Float> { 1.0 2.5 }")[1] shouldBe 2.5f
        v(".vec<Long> { 1 2L }").toList() shouldBe listOf(1L, 2L)
        v(".vec<Double> { 1 2d }").toList() shouldBe listOf(1.0, 2.0)
        v(".vec<Dec> { 1.50 2bd }").toList() shouldBe listOf(Dec("1.50"), Dec("2"))
        v(".vec<Int> { 1_000 2 }")[0] shouldBe 1000
    }

    "lines and comments" {
        v(
            """.vec<Double> {
                0.1 0.2   # first pair
                0.3       // the last
            }"""
        ) shouldBe Vector.ofDoubles(0.1, 0.2, 0.3)
    }

    "bad literals are refused" {
        for (bad in listOf(
            ".vec<Int> { 1.5 }", ".vec<Int> { 1f }", ".vec<String> { }", ".vec<Quantity> { }", ".vec<Int> 1 2",
            ".vec<Int> { 3000000000 }", ".vec<Double> { NaN }", ".vec<Int> { 1 two }", ".vec { 1 }",
        )) {
            shouldThrow<ParseException> { v(bad) }
        }
        shouldThrow<IllegalArgumentException> { Vector.of(Type.Int, listOf(1.5)) }
        shouldThrow<IllegalArgumentException> { Vector.of(Type.Double, listOf(Double.NaN)) }
        shouldThrow<IllegalArgumentException> { Vector.ofDoubles(1.0, Double.POSITIVE_INFINITY) }
        shouldThrow<IllegalArgumentException> { Vector.of(Type.String, listOf()) }
    }

    "of converts exactly" {
        Vector.of(Type.Int, listOf(1L, 2.0, Dec("3"))) shouldBe Vector.ofInts(1, 2, 3)
        Vector.of(Type.Dec, listOf(0.1, 1)).toList() shouldBe listOf(Dec("0.1"), Dec(1))
        Vector.of("Float", listOf(1, 2)) shouldBe Vector.ofFloats(1f, 2f)
    }

    // ========== Equality and order ==========

    "equality is the element type and the numbers, a Dec by value" {
        v(".vec<Dec> { 1.5 2 }") shouldBe Vector.ofDecs(Dec("1.50"), Dec("2.0"))
        v(".vec<Dec> { 1.5 2 }").hashCode() shouldBe Vector.ofDecs(Dec("1.50"), Dec("2.0")).hashCode()
        Vector.ofFloats(1f) shouldNotBe Vector.ofDoubles(1.0)
        Vector.ofInts(1, 2) shouldNotBe Vector.ofInts(1, 2, 3)
        Vector.ofInts(1, 2) shouldNotBe listOf(1, 2)
    }

    "order is element by element, then length" {
        Vector.ofInts(1, 2) shouldBeLessThan Vector.ofInts(1, 3)
        Vector.ofInts(1, 2) shouldBeLessThan Vector.ofInts(1, 2, 0)
        Vector.ofInts() shouldBeLessThan Vector.ofInts(-5)
        Vector.ofInts(1, 2).compareTo(Vector.ofDoubles(1.0, 2.0)) shouldBe 0
        Vector.ofDoubles(0.1) shouldBeLessThan Vector.ofDecs(Dec("0.10000001"))
    }

    // ========== Numbers ==========

    "statistics" {
        val x = Vector.ofInts(3, 1, 4, 1, 5)
        x.size shouldBe 5
        x.sum() shouldBe 14L
        x.mean() shouldBe 2.8
        x.min() shouldBe 1
        x.max() shouldBe 5
        Vector.ofDecs(Dec("0.1"), Dec("0.2")).sum() shouldBe Dec("0.3")
        Vector.ofDecs(Dec("1"), Dec("2")).mean() shouldBe Dec("1.5")
        Vector.ofDoubles(0.1, 0.2).sum() shouldBe 0.30000000000000004  // as Kotlin's sum() and KQ's sum (RevDB design 4.5)
        Vector.ofDoubles(0.1, 0.2).mean() shouldBe doubleArrayOf(0.1, 0.2).average()
        Vector.ofLongs(Long.MAX_VALUE, 1L).sum() shouldBe Long.MIN_VALUE        // wraps, as Kotlin's and KQ's sums do
        Vector.ofFloats(0.5f, 0.25f).sum().shouldBeInstanceOf<Float>()
        Vector.ofInts().sum().shouldBeNull()
        Vector.ofInts().mean().shouldBeNull()
        Vector.ofInts().min().shouldBeNull()
        Vector.ofDoubles(3.0, 4.0).norm() shouldBe 5.0
        Vector.ofInts(Int.MAX_VALUE, Int.MAX_VALUE).sum() shouldBe 2L * Int.MAX_VALUE
    }

    "element-wise and scalar arithmetic" {
        (Vector.ofInts(1, 2) + Vector.ofInts(10, 20)) shouldBe Vector.ofInts(11, 22)
        (Vector.ofInts(1, 2) + Vector.ofDoubles(0.5, 0.5)) shouldBe Vector.ofDoubles(1.5, 2.5)
        (Vector.ofInts(1, 2) - Vector.ofInts(1, 3)) shouldBe Vector.ofInts(0, -1)
        (Vector.ofInts(1, 2) * 3) shouldBe Vector.ofInts(3, 6)
        (Vector.ofInts(1, 2) * 0.5) shouldBe Vector.ofDoubles(0.5, 1.0)
        (Vector.ofInts(1, 2) / 2) shouldBe Vector.ofDoubles(0.5, 1.0)
        (Vector.ofFloats(1f) / 2f) shouldBe Vector.ofFloats(0.5f)
        (Vector.ofDecs(Dec(1)) / 3).toList().single().shouldBeInstanceOf<Dec>()
        (-Vector.ofInts(1, -2)) shouldBe Vector.ofInts(-1, 2)
        Vector.ofInts(1, 2, 3).dot(Vector.ofInts(4, 5, 6)) shouldBe 32L
        Vector.ofDoubles(0.5).dot(Vector.ofInts(2)) shouldBe 1.0
        shouldThrow<IllegalArgumentException> { Vector.ofInts(1) + Vector.ofInts(1, 2) }
        shouldThrow<IllegalArgumentException> { Vector.ofInts(1).dot(Vector.ofInts()) }
        shouldThrow<ArithmeticException> { Vector.ofInts(Int.MAX_VALUE) + Vector.ofInts(1) }
    }

    "a vector is immutable" {
        val xs = intArrayOf(1, 2)
        val x = Vector.ofInts(*xs)
        xs[0] = 9
        x[0] shouldBe 1
    }

    // ========== The review of 2026-10-28 ==========

    "-0.0 stays -0.0; commas and comments read as KD reads them" {
        Vector.parse(".vec<Double> { -0.0 }") shouldBe Vector.ofDoubles(-0.0)
        Vector.of(Type.Double, listOf(-0.0)) shouldBe Vector.ofDoubles(-0.0)
        Ki.parse(".vec<Int> { 1, 2 /* a /* nested */ note */ 3 }") shouldBe Vector.ofInts(1, 2, 3)
        Vector.parse(Vector.ofDecs(Dec("1E+19")).toString()) shouldBe Vector.ofDecs(Dec("1E+19"))
    }

    // ========== Ki ==========

    "Ki reads, writes and types a vector" {
        val x = Ki.parse(".vec<Int> { 1 2 }")
        x shouldBe Vector.ofInts(1, 2)
        Ki.format(x) shouldBe ".vec<Int> { 1 2 }"
        Ki.typeOf(x) shouldBe Type.Vector
        Type.forName("Vector") shouldBe Type.Vector
        Type.of(Vector::class.java) shouldBe Type.Vector
        TypeDef.forName("Vector")!!.matches(x) shouldBe true
        TypeDef(Type.List, false).matches(x) shouldBe false
        Vector.isLiteral("  .vec<Int> { }") shouldBe true
    }
})
