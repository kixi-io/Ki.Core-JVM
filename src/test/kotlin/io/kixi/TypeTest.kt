package io.kixi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kixi.uom.Quantity
import io.kixi.uom.Unit
import io.kixi.uom.Length
import java.math.BigDecimal as Dec
import java.net.URL
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

class TypeTest : FunSpec({

    context("typeOf detection") {
        test("null returns nil") {
            Type.typeOf(null) shouldBe Type.nil
        }

        test("String detected") {
            Type.typeOf("hello") shouldBe Type.String
        }

        test("Char detected") {
            Type.typeOf('a') shouldBe Type.Char
        }

        test("Int detected") {
            Type.typeOf(42) shouldBe Type.Int
        }

        test("Long detected") {
            Type.typeOf(42L) shouldBe Type.Long
        }

        test("Float detected") {
            Type.typeOf(3.14f) shouldBe Type.Float
        }

        test("Double detected") {
            Type.typeOf(3.14) shouldBe Type.Double
        }

        test("BigDecimal detected") {
            Type.typeOf(Dec("3.14")) shouldBe Type.Dec
        }

        test("Boolean detected") {
            Type.typeOf(true) shouldBe Type.Bool
        }

        test("URL detected") {
            Type.typeOf(URL("https://example.com")) shouldBe Type.URL
        }

        test("LocalDate detected") {
            Type.typeOf(LocalDate.now()) shouldBe Type.Date
        }

        test("LocalDateTime detected") {
            Type.typeOf(LocalDateTime.now()) shouldBe Type.LocalDateTime
        }

        test("ZonedDateTime detected") {
            Type.typeOf(ZonedDateTime.now()) shouldBe Type.ZonedDateTime
        }

        test("Duration detected") {
            Type.typeOf(Duration.ofHours(1)) shouldBe Type.Duration
        }

        test("Version detected") {
            Type.typeOf(Version(1, 2, 3)) shouldBe Type.Version
        }

        test("Blob detected") {
            Type.typeOf(Blob.of("test")) shouldBe Type.Blob
        }

        test("Quantity detected") {
            Type.typeOf(Quantity(5, Unit.cm)) shouldBe Type.Quantity
        }

        test("Range detected") {
            Type.typeOf(Range(1, 10)) shouldBe Type.Range
        }

        test("List detected") {
            Type.typeOf(listOf(1, 2, 3)) shouldBe Type.List
        }

        test("Map detected") {
            Type.typeOf(mapOf("a" to 1)) shouldBe Type.Map
        }

        test("unknown type returns null") {
            Type.typeOf(object {}) shouldBe null
        }
    }

    context("isAssignableFrom") {
        test("same type is assignable") {
            Type.Int.isAssignableFrom(Type.Int) shouldBe true
        }

        test("Any accepts all types") {
            Type.Any.isAssignableFrom(Type.String) shouldBe true
            Type.Any.isAssignableFrom(Type.Int) shouldBe true
            Type.Any.isAssignableFrom(Type.List) shouldBe true
        }

        test("Number accepts numeric types") {
            Type.Number.isAssignableFrom(Type.Int) shouldBe true
            Type.Number.isAssignableFrom(Type.Long) shouldBe true
            Type.Number.isAssignableFrom(Type.Float) shouldBe true
            Type.Number.isAssignableFrom(Type.Double) shouldBe true
            Type.Number.isAssignableFrom(Type.Dec) shouldBe true
        }

        test("Number does not accept non-numeric") {
            Type.Number.isAssignableFrom(Type.String) shouldBe false
        }

        test("specific type does not accept supertype") {
            Type.Int.isAssignableFrom(Type.Number) shouldBe false
        }
    }

    context("isNumber") {
        test("Number type isNumber") {
            Type.Number.isNumber() shouldBe true
        }

        test("numeric types isNumber") {
            Type.Int.isNumber() shouldBe true
            Type.Long.isNumber() shouldBe true
            Type.Float.isNumber() shouldBe true
            Type.Double.isNumber() shouldBe true
            Type.Dec.isNumber() shouldBe true
        }

        test("non-numeric types not isNumber") {
            Type.String.isNumber() shouldBe false
            Type.Bool.isNumber() shouldBe false
            Type.List.isNumber() shouldBe false
        }
    }
})

class TypeDefTest : FunSpec({

    context("basic TypeDefs") {
        test("non-nullable String") {
            TypeDef.String.type shouldBe Type.String
            TypeDef.String.nullable shouldBe false
            TypeDef.String.toString() shouldBe "String"
        }

        test("nullable String") {
            TypeDef.String_N.type shouldBe Type.String
            TypeDef.String_N.nullable shouldBe true
            TypeDef.String_N.toString() shouldBe "String?"
        }
    }

    context("forName lookup") {
        test("finds basic types") {
            TypeDef.forName("String") shouldBe TypeDef.String
            TypeDef.forName("Int") shouldBe TypeDef.Int
            TypeDef.forName("Bool") shouldBe TypeDef.Bool
        }

        test("finds nullable types") {
            TypeDef.forName("String_N") shouldBe TypeDef.String_N
            TypeDef.forName("Int_N") shouldBe TypeDef.Int_N
        }

        test("finds nil") {
            TypeDef.forName("nil") shouldBe TypeDef.nil
            TypeDef.forName("null") shouldBe TypeDef.nil
        }

        test("returns null for unknown") {
            TypeDef.forName("Unknown").shouldBeNull()
        }
    }

    context("matches") {
        test("non-nullable matches value") {
            TypeDef.String.matches("hello") shouldBe true
            TypeDef.Int.matches(42) shouldBe true
        }

        test("non-nullable does not match null") {
            TypeDef.String.matches(null) shouldBe false
        }

        test("nullable matches null") {
            TypeDef.String_N.matches(null) shouldBe true
        }

        test("nullable matches value") {
            TypeDef.String_N.matches("hello") shouldBe true
        }

        test("nil matches null") {
            TypeDef.nil.matches(null) shouldBe true
        }

        test("nil does not match value") {
            TypeDef.nil.matches("hello") shouldBe false
        }

        test("supertype matches subtypes") {
            TypeDef.Number.matches(42) shouldBe true
            TypeDef.Number.matches(3.14) shouldBe true
            TypeDef.Any.matches("anything") shouldBe true
        }
    }

    context("inferCollectionType") {
        test("infers from homogeneous list") {
            val def = TypeDef.inferCollectionType(listOf(1, 2, 3))
            def.type shouldBe Type.Int
            def.nullable shouldBe false
        }

        test("infers nullable when contains null") {
            val def = TypeDef.inferCollectionType(listOf(1, null, 3))
            def.type shouldBe Type.Int
            def.nullable shouldBe true
        }

        test("widens to Number for mixed numeric") {
            val def = TypeDef.inferCollectionType(listOf(1, 2L, 3.0))
            def.type shouldBe Type.Number
        }

        test("widens to Any for mixed types") {
            val def = TypeDef.inferCollectionType(listOf(1, "two", true))
            def.type shouldBe Type.Any
        }

        test("empty collection returns nil type") {
            val def = TypeDef.inferCollectionType(emptyList())
            def.type shouldBe Type.nil
        }

        test("all nulls returns nil type") {
            val def = TypeDef.inferCollectionType(listOf(null, null))
            def.type shouldBe Type.nil
            def.nullable shouldBe true
        }
    }
})

class ListDefTest : FunSpec({

    context("ListDef matching") {
        test("matches list of correct type") {
            val listDef = ListDef(false, TypeDef.Int)
            listDef.matches(listOf(1, 2, 3)) shouldBe true
        }

        test("does not match list of wrong type") {
            val listDef = ListDef(false, TypeDef.Int)
            listDef.matches(listOf("a", "b")) shouldBe false
        }

        test("empty list matches any element type") {
            val listDef = ListDef(false, TypeDef.Int)
            listDef.matches(emptyList<Int>()) shouldBe true
        }

        test("null list matches nullable def") {
            val listDef = ListDef(true, TypeDef.Int)
            listDef.matches(null) shouldBe true
        }

        test("null list does not match non-nullable def") {
            val listDef = ListDef(false, TypeDef.Int)
            listDef.matches(null) shouldBe false
        }

        test("toString format") {
            ListDef(false, TypeDef.String).toString() shouldBe "List<String>"
            ListDef(true, TypeDef.Int).toString() shouldBe "List<Int>?"
        }

        test("is generic") {
            ListDef(false, TypeDef.String).generic shouldBe true
        }
    }
})

class MapDefTest : FunSpec({

    context("MapDef matching") {
        test("matches map of correct types") {
            val mapDef = MapDef(false, TypeDef.String, TypeDef.Int)
            mapDef.matches(mapOf("a" to 1, "b" to 2)) shouldBe true
        }

        test("does not match wrong key type") {
            val mapDef = MapDef(false, TypeDef.String, TypeDef.Int)
            mapDef.matches(mapOf(1 to 1, 2 to 2)) shouldBe false
        }

        test("does not match wrong value type") {
            val mapDef = MapDef(false, TypeDef.String, TypeDef.Int)
            mapDef.matches(mapOf("a" to "x")) shouldBe false
        }

        test("empty map matches any types") {
            val mapDef = MapDef(false, TypeDef.String, TypeDef.Int)
            mapDef.matches(emptyMap<String, Int>()) shouldBe true
        }

        test("toString format") {
            MapDef(false, TypeDef.String, TypeDef.Int).toString() shouldBe "Map<String, Int>"
        }

        test("is generic") {
            MapDef(false, TypeDef.String, TypeDef.Int).generic shouldBe true
        }
    }
})

class RangeDefTest : FunSpec({

    context("RangeDef matching") {
        test("matches range of correct type") {
            val rangeDef = RangeDef(false, TypeDef.Int)
            rangeDef.matches(Range(1, 10)) shouldBe true
        }

        test("toString format") {
            RangeDef(false, TypeDef.Int).toString() shouldBe "Range<Int>"
            RangeDef(true, TypeDef.Double).toString() shouldBe "Range<Double>?"
        }

        test("is generic") {
            RangeDef(false, TypeDef.Int).generic shouldBe true
        }
    }
})

class QuantityDefTest : FunSpec({

    context("QuantityDef matching") {
        test("matches quantity of correct unit and number type") {
            val qtyDef = QuantityDef(false, Length::class, Type.Dec)
            qtyDef.matches(Quantity(Dec("5"), Unit.cm)) shouldBe true
        }

        test("does not match wrong unit type") {
            val qtyDef = QuantityDef(false, Length::class, Type.Dec)
            qtyDef.matches(Quantity(Dec("5"), Unit.kg)) shouldBe false
        }

        test("toString format") {
            val qtyDef = QuantityDef(false, Length::class, Type.Dec)
            qtyDef.toString() shouldBe "Quantity<Length>"
        }

        test("toString with Double") {
            val qtyDef = QuantityDef(false, Length::class, Type.Double)
            qtyDef.toString() shouldBe "Quantity<Length:d>"
        }

        test("is generic") {
            QuantityDef(false, Length::class, Type.Dec).generic shouldBe true
        }
    }
})