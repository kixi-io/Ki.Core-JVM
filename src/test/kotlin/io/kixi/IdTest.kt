package io.kixi

import io.kixi.text.ParseException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength
import io.kotest.property.Arb
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll

class IdTest : StringSpec({

    val sample = "01J9A0X5K2M7N3P8Q1R6T4V9AB"

    // ========== Generation ==========

    "generated ids are strictly increasing" {
        val ids = List(1000) { Id.generate() }
        ids.zipWithNext().all { (a, b) -> a < b } shouldBe true
    }

    "ids generated in the same millisecond are strictly increasing" {
        val t = 1_700_000_000_000L
        val ids = List(1000) { Id.generate(t) }
        ids.zipWithNext().all { (a, b) -> a < b } shouldBe true
        ids.all { it.timestamp == t } shouldBe true
    }

    "generate honors an explicit timestamp" {
        Id.generate(1_000L).timestamp shouldBe 1_000L
        Id.generate(0L).timestamp shouldBe 0L
        Id.generate((1L shl 48) - 1).timestamp shouldBe (1L shl 48) - 1
    }

    "generate rejects a timestamp outside 48 bits" {
        shouldThrow<IllegalArgumentException> { Id.generate(-1L) }
        shouldThrow<IllegalArgumentException> { Id.generate(1L shl 48) }
    }

    "later timestamp sorts after earlier timestamp" {
        Id.generate(2_000L) shouldBeGreaterThan Id.generate(1_000L)
        Id.generate(1_000L) shouldBeLessThan Id.generate(3_000L)
    }

    // ========== Text form ==========

    "toBase32 is 26 characters and toString wraps it" {
        val id = Id.generate()
        id.toBase32() shouldHaveLength 26
        id.toString() shouldBe ".id(${id.toBase32()})"
    }

    "parse round trips the bare form" {
        val id = Id.generate()
        Id.parse(id.toBase32()) shouldBe id
    }

    "parseLiteral round trips the literal form" {
        val id = Id.generate()
        Id.parseLiteral(id.toString()) shouldBe id
        Id.parseLiteral("  .id($sample)  ") shouldBe Id.parse(sample)
    }

    "parse is case-insensitive and accepts Crockford aliases" {
        Id.parse(sample.lowercase()) shouldBe Id.parse(sample)
        Id.parse("O" + sample.drop(1)) shouldBe Id.parse(sample)   // O reads as 0
        Id.parse("0" + "L" + sample.drop(2)) shouldBe Id.parse(sample) // L reads as 1
    }

    "extreme values round trip through text" {
        val max = Id.of(-1L, -1L)
        max.toBase32() shouldBe "7ZZZZZZZZZZZZZZZZZZZZZZZZZ"
        Id.parse(max.toBase32()) shouldBe max
        val zero = Id.of(0L, 0L)
        zero.toBase32() shouldBe "00000000000000000000000000"
        Id.parse(zero.toBase32()) shouldBe zero
    }

    "parse rejects wrong length, invalid characters and overflow" {
        shouldThrow<ParseException> { Id.parse("01J9") }
        shouldThrow<ParseException> { Id.parse("U" + sample.drop(1)) }   // U is not in the alphabet
        shouldThrow<ParseException> { Id.parse("8" + sample.drop(1)) }   // first char must be 0-7
        shouldThrow<ParseException> { Id.parse("") }
    }

    "parseLiteral rejects a missing wrapper" {
        shouldThrow<ParseException> { Id.parseLiteral(sample) }
        shouldThrow<ParseException> { Id.parseLiteral(".id($sample") }
        shouldThrow<ParseException> { Id.parseLiteral("id($sample)") }
    }

    "parseOrNull returns null on failure and accepts both forms" {
        Id.parseOrNull("nope").shouldBeNull()
        Id.parseOrNull(sample) shouldBe Id.parse(sample)
        Id.parseOrNull(".id($sample)") shouldBe Id.parse(sample)
    }

    "isLiteral is a structural check" {
        Id.isLiteral(".id($sample)") shouldBe true
        Id.isLiteral(".id(anything)") shouldBe true
        Id.isLiteral(sample) shouldBe false
    }

    // ========== Binary form and ordering ==========

    "toBytes is 16 bytes and fromBytes round trips" {
        val id = Id.generate()
        id.toBytes().size shouldBe 16
        Id.fromBytes(id.toBytes()) shouldBe id
    }

    "fromBytes rejects the wrong length" {
        shouldThrow<IllegalArgumentException> { Id.fromBytes(ByteArray(15)) }
    }

    "byte order, text order and compareTo agree" {
        checkAll(Arb.long(), Arb.long(), Arb.long(), Arb.long()) { h1, l1, h2, l2 ->
            val a = Id.of(h1, l1)
            val b = Id.of(h2, l2)
            val expected = Integer.signum(a.compareTo(b))
            Integer.signum(java.util.Arrays.compareUnsigned(a.toBytes(), b.toBytes())) shouldBe expected
            Integer.signum(a.toBase32().compareTo(b.toBase32())) shouldBe expected
            Id.parse(a.toBase32()) shouldBe a
            Id.fromBytes(a.toBytes()) shouldBe a
        }
    }

    "equality and hashCode are structural" {
        val a = Id.of(1L, 2L)
        val b = Id.of(1L, 2L)
        a shouldBe b
        a.hashCode() shouldBe b.hashCode()
        a shouldNotBe Id.of(1L, 3L)
    }

    // ========== Ki integration ==========

    "Ki.parse and Ki.format handle Id literals" {
        val id = Id.generate()
        Ki.parse(id.toString()) shouldBe id
        Ki.format(id) shouldBe id.toString()
    }

    "Type and TypeDef know Id" {
        Type.typeOf(Id.generate()) shouldBe Type.Id
        TypeDef.Id.matches(Id.generate()) shouldBe true
        TypeDef.Id.matches(null) shouldBe false
        TypeDef.Id_N.matches(null) shouldBe true
        TypeDef.Id.matches("not an id") shouldBe false
        TypeDef.forName("Id") shouldBe TypeDef.Id
        TypeDef.forName("Id_N") shouldBe TypeDef.Id_N
    }
})
