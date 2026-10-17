package io.kixi

import io.kixi.text.ParseException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class RefTest : StringSpec({

    val idText = "01J9A0X5K2M7N3P8Q1R6T4V9AB"
    val id = Id.parse(idText)

    // ========== Construction ==========

    "constructs with and without a collection" {
        Ref("compounds", id).collection shouldBe "compounds"
        Ref("compounds", id).hasCollection shouldBe true
        Ref(id).collection.shouldBeNull()
        Ref(id).hasCollection shouldBe false
    }

    "collection must be a Ki identifier" {
        shouldThrow<IllegalArgumentException> { Ref("9bad", id) }
        shouldThrow<IllegalArgumentException> { Ref("", id) }
        shouldThrow<IllegalArgumentException> { Ref("with space", id) }
    }

    "withCollection keeps the id" {
        val r = Ref(id).withCollection("assays")
        r.collection shouldBe "assays"
        r.id shouldBe id
        r.withCollection(null).collection.shouldBeNull()
    }

    // ========== Equality and ordering ==========

    "equality and hashCode consider the id only" {
        Ref("compounds", id) shouldBe Ref(id)
        Ref("compounds", id) shouldBe Ref("other", id)
        Ref("compounds", id).hashCode() shouldBe Ref("other", id).hashCode()
        Ref("compounds", id) shouldNotBe Ref("compounds", Id.generate())
    }

    "compareTo orders by id and ignores the collection" {
        val earlier = Id.generate(1_000L)
        val later = Id.generate(2_000L)
        Ref("z", earlier) shouldBeLessThan Ref("a", later)
    }

    // ========== Text form ==========

    "toString produces the literal" {
        Ref("compounds", id).toString() shouldBe ".ref(compounds/$idText)"
        Ref(id).toString() shouldBe ".ref($idText)"
    }

    "parse accepts collection/id and bare id" {
        val r = Ref.parse("compounds/$idText")
        r.collection shouldBe "compounds"
        r.id shouldBe id
        Ref.parse(idText).collection.shouldBeNull()
        Ref.parse("  compounds / $idText ").collection shouldBe "compounds"
    }

    "parseLiteral round trips both forms" {
        val a = Ref("compounds", id)
        val b = Ref(id)
        Ref.parseLiteral(a.toString()) shouldBe a
        Ref.parseLiteral(a.toString()).collection shouldBe "compounds"
        Ref.parseLiteral(b.toString()).collection.shouldBeNull()
    }

    "parse rejects bad input" {
        shouldThrow<ParseException> { Ref.parse("") }
        shouldThrow<ParseException> { Ref.parse("9bad/$idText") }
        shouldThrow<ParseException> { Ref.parse("a/b/$idText") }
        shouldThrow<ParseException> { Ref.parse("compounds/01J9") }
        shouldThrow<ParseException> { Ref.parseLiteral("ref(compounds/$idText)") }
        shouldThrow<ParseException> { Ref.parseLiteral(".ref(compounds/$idText") }
    }

    "parseOrNull and isLiteral" {
        Ref.parseOrNull("nope").shouldBeNull()
        Ref.parseOrNull("compounds/$idText") shouldBe Ref("compounds", id)
        Ref.parseOrNull(".ref($idText)") shouldBe Ref(id)
        Ref.isLiteral(".ref($idText)") shouldBe true
        Ref.isLiteral(idText) shouldBe false
    }

    // ========== Ki integration ==========

    "Ki.parse and Ki.format handle Ref literals" {
        val r = Ref("compounds", id)
        Ki.parse(r.toString()) shouldBe r
        Ki.format(r) shouldBe r.toString()
    }

    "Type and TypeDef know Ref" {
        Type.typeOf(Ref(id)) shouldBe Type.Ref
        TypeDef.Ref.matches(Ref(id)) shouldBe true
        TypeDef.Ref.matches(id) shouldBe false
        TypeDef.Ref_N.matches(null) shouldBe true
        TypeDef.forName("Ref") shouldBe TypeDef.Ref
        TypeDef.forName("Ref_N") shouldBe TypeDef.Ref_N
    }
})
