package io.kixi.uom

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.assertions.throwables.shouldThrow
import java.math.BigDecimal as Dec

/**
 * Helper function to compare BigDecimal values by numeric value (ignoring scale differences).
 * BigDecimal.equals() considers scale, so Dec("5.5e8") != Dec("550000000") even though
 * they represent the same value. This uses compareTo which only compares numeric value.
 */
infix fun Dec.shouldBeNumericallyEqualTo(expected: Dec) {
    if (this.compareTo(expected) != 0) {
        throw AssertionError("expected: $expected but was: $this")
    }
}

/**
 * Helper to check if a Number is numerically equal to an expected BigDecimal.
 */
infix fun Number.shouldBeNumericallyEqualTo(expected: Dec) {
    when (this) {
        is Dec -> this shouldBeNumericallyEqualTo expected
        else -> {
            val asDec = Dec(this.toString())
            if (asDec.compareTo(expected) != 0) {
                throw AssertionError("expected: $expected but was: $this")
            }
        }
    }
}

class UnitTest : FunSpec({

    context("unit lookup") {
        test("getUnit finds length units") {
            Unit.getUnit("m") shouldBe Unit.m
            Unit.getUnit("cm") shouldBe Unit.cm
            Unit.getUnit("mm") shouldBe Unit.mm
            Unit.getUnit("km") shouldBe Unit.km
        }

        test("getUnit finds mass units") {
            Unit.getUnit("kg") shouldBe Unit.kg
            Unit.getUnit("g") shouldBe Unit.g
            Unit.getUnit("mg") shouldBe Unit.mg
        }

        test("getUnit handles alternate symbols") {
            Unit.getUnit("LT") shouldBe Unit.L  // Liter alternate
            Unit.getUnit("mL") shouldBe Unit.mL
            Unit.getUnit("dC") shouldBe Unit.dC  // Celsius alternate
        }

        test("getUnit handles exponent conversion") {
            Unit.getUnit("m2") shouldBe Unit.m2
            Unit.getUnit("m3") shouldBe Unit.m3
            Unit.getUnit("cm2") shouldBe Unit.cm2
            Unit.getUnit("cm3") shouldBe Unit.cm3
        }

        test("getUnit returns null for unknown") {
            Unit.getUnit("unknown") shouldBe null
        }
    }

    context("unit properties") {
        test("symbol returns correct value") {
            Unit.m.symbol shouldBe "m"
            Unit.kg.symbol shouldBe "kg"
        }

        test("baseUnit returns correct base") {
            Unit.cm.baseUnit shouldBe Unit.m
            Unit.mm.baseUnit shouldBe Unit.m
            Unit.g.baseUnit shouldBe Unit.kg
        }

        test("factor relative to base") {
            Unit.cm.factor shouldBeNumericallyEqualTo Dec("0.01")
            Unit.mm.factor shouldBeNumericallyEqualTo Dec("0.001")
            Unit.km.factor shouldBeNumericallyEqualTo Dec("1000")
        }

        test("dimensionName extracts class name") {
            Unit.m.dimensionName shouldBe "Length"
            Unit.kg.dimensionName shouldBe "Mass"
            Unit.K.dimensionName shouldBe "Temperature"
        }
    }

    context("factorTo conversion") {
        test("cm to mm factor is 10") {
            Unit.cm.factorTo(Unit.mm) shouldBeNumericallyEqualTo Dec("10")
        }

        test("m to cm factor is 100") {
            Unit.m.factorTo(Unit.cm) shouldBeNumericallyEqualTo Dec("100")
        }

        test("km to m factor is 1000") {
            Unit.km.factorTo(Unit.m) shouldBeNumericallyEqualTo Dec("1000")
        }

        test("throws for incompatible units") {
            shouldThrow<IncompatibleUnitsException> {
                Unit.m.factorTo(Unit.kg)
            }
        }
    }

    context("isCompatibleWith") {
        test("same dimension is compatible") {
            Unit.m.isCompatibleWith(Unit.cm) shouldBe true
            Unit.kg.isCompatibleWith(Unit.g) shouldBe true
        }

        test("different dimensions not compatible") {
            Unit.m.isCompatibleWith(Unit.kg) shouldBe false
        }
    }

    context("unit comparison") {
        test("larger units are greater") {
            Unit.km shouldBeGreaterThan Unit.m
            Unit.m shouldBeGreaterThan Unit.cm
            Unit.kg shouldBeGreaterThan Unit.g
        }

        test("smaller units are less") {
            Unit.mm shouldBeLessThan Unit.cm
            Unit.mg shouldBeLessThan Unit.g
        }

        test("comparing incompatible throws") {
            shouldThrow<IncompatibleUnitsException> {
                Unit.m.compareTo(Unit.kg)
            }
        }
    }

    context("unit equality") {
        test("same unit is equal") {
            Unit.m shouldBe Unit.m
        }

        test("different units not equal") {
            Unit.m shouldNotBe Unit.cm
        }
    }

    context("area units") {
        test("area unit properties") {
            Unit.m2.symbol shouldBe "m²"
            Unit.cm2.symbol shouldBe "cm²"
        }

        test("area conversion factors") {
            Unit.m2.factorTo(Unit.cm2) shouldBeNumericallyEqualTo Dec("10000")
        }
    }

    context("volume units") {
        test("volume unit properties") {
            Unit.m3.symbol shouldBe "m³"
            Unit.L.symbol shouldBe "ℓ"
        }
    }

    context("allUnits") {
        test("returns all registered units") {
            val units = Unit.allUnits()
            units.size shouldBeGreaterThan 20  // At least this many defined
            units.contains(Unit.m) shouldBe true
            units.contains(Unit.kg) shouldBe true
        }
    }

    context("combineUnits") {
        test("Length × Length = Area") {
            combineUnits(Unit.m, Unit.m) shouldBe Unit.m2
            combineUnits(Unit.cm, Unit.cm) shouldBe Unit.cm2
        }

        test("Length × Area = Volume") {
            combineUnits(Unit.m, Unit.m2) shouldBe Unit.m3
        }

        test("Area × Length = Volume") {
            combineUnits(Unit.m2, Unit.m) shouldBe Unit.m3
        }

        test("incompatible returns null") {
            combineUnits(Unit.m, Unit.kg) shouldBe null
        }
    }

    context("canCombineUnits") {
        test("combinable units return true") {
            canCombineUnits(Unit.m, Unit.m) shouldBe true
        }

        test("non-combinable return false") {
            canCombineUnits(Unit.m, Unit.kg) shouldBe false
        }
    }
})

class QuantityTest : FunSpec({

    context("creation") {
        test("create with Number and Unit") {
            val q = Quantity(5, Unit.cm)
            q.value shouldBe 5
            q.unit shouldBe Unit.cm
        }

        test("create with String value") {
            val q = Quantity("3.14", Unit.m)
            (q.value as Dec) shouldBeNumericallyEqualTo Dec("3.14")
        }

        test("create from text literal") {
            val q = Quantity<Length>("5cm")
            q.value shouldBeNumericallyEqualTo Dec("5")
            q.unit shouldBe Unit.cm
        }

        test("parse with underscore separators") {
            val q = Quantity.parse("1_000_000m")
            q.value shouldBeNumericallyEqualTo Dec("1000000")
        }
    }

    context("type specifiers") {
        test("Long specifier :L") {
            val q = Quantity.parse("100m:L")
            q.value shouldBe 100L
            q.value::class shouldBe Long::class
        }

        test("Double specifier :d") {
            val q = Quantity.parse("3.14m:d")
            q.value shouldBe 3.14
            q.value::class shouldBe Double::class
        }

        test("Float specifier :f") {
            val q = Quantity.parse("3.14m:f")
            q.value shouldBe 3.14f
            q.value::class shouldBe Float::class
        }

        test("Int specifier :i") {
            val q = Quantity.parse("100m:i")
            q.value shouldBe 100
            q.value::class shouldBe Int::class
        }

        test("default is BigDecimal") {
            val q = Quantity.parse("5cm")
            q.value::class shouldBe Dec::class
        }
    }

    context("scientific notation - parentheses style") {
        test("positive exponent") {
            val q = Quantity.parse("5.5e(8)km")
            q.value shouldBeNumericallyEqualTo Dec("550000000")
            q.unit shouldBe Unit.km
        }

        test("negative exponent") {
            val q = Quantity.parse("5.5e(-7)m")
            q.value shouldBeNumericallyEqualTo Dec("0.00000055")
        }

        test("explicit positive with +") {
            val q = Quantity.parse("1.5e(+8)m")
            q.value shouldBeNumericallyEqualTo Dec("150000000")
        }
    }

    context("scientific notation - letter style") {
        test("negative with n") {
            val q = Quantity.parse("5.5en7m")
            q.value shouldBeNumericallyEqualTo Dec("0.00000055")
        }

        test("explicit positive with p") {
            val q = Quantity.parse("5.5ep8km")
            q.value shouldBeNumericallyEqualTo Dec("550000000")
        }

        test("implicit positive (no letter)") {
            val q = Quantity.parse("5.5e8km")
            q.value shouldBeNumericallyEqualTo Dec("550000000")
        }
    }

    context("toString formatting") {
        test("basic quantity") {
            Quantity(5, Unit.cm).toString() shouldBe "5cm:i"
        }

        test("decimal quantity omits type suffix") {
            Quantity(Dec("3.5"), Unit.m).toString() shouldBe "3.5m"
        }

        test("Long quantity") {
            Quantity(100L, Unit.km).toString() shouldBe "100km:L"
        }

        test("Double quantity") {
            Quantity(3.14, Unit.m).toString() shouldBe "3.14m:d"
        }

        test("strips trailing zeros from BigDecimal") {
            Quantity(Dec("5.00"), Unit.cm).toString() shouldBe "5cm"
        }

        test("large values use plain notation not scientific") {
            val q = Quantity.parse("5.5e(8)km")
            q.toString() shouldBe "550000000km"
        }
    }

    context("unit conversion") {
        test("cm to mm") {
            val q = Quantity(1, Unit.cm)
            val converted = q convertTo Unit.mm
            converted.value shouldBe 10
            converted.unit shouldBe Unit.mm
        }

        test("km to m") {
            val q = Quantity(Dec("1.5"), Unit.km)
            val converted = q convertTo Unit.m
            converted.value shouldBeNumericallyEqualTo Dec("1500")
        }

        test("m to km with Int becomes Dec") {
            val q = Quantity(500, Unit.m)
            val converted = q convertTo Unit.km
            converted.value shouldBeNumericallyEqualTo Dec("0.5")
        }
    }

    context("comparison") {
        test("same unit comparison") {
            val q1 = Quantity(5, Unit.cm)
            val q2 = Quantity(3, Unit.cm)
            (q1 > q2) shouldBe true
        }

        test("different unit comparison (converts)") {
            val q1 = Quantity(10, Unit.mm)
            val q2 = Quantity(1, Unit.cm)
            (q1.compareTo(q2)) shouldBe 0  // 10mm == 1cm
        }
    }

    context("equality") {
        test("equal quantities") {
            Quantity(5, Unit.cm) shouldBe Quantity(5, Unit.cm)
        }

        test("different values not equal") {
            Quantity(5, Unit.cm) shouldNotBe Quantity(10, Unit.cm)
        }

        test("different units not equal (even if equivalent)") {
            Quantity(10, Unit.mm) shouldNotBe Quantity(1, Unit.cm)
        }

        test("equivalent checks physical equality") {
            val q1 = Quantity(10, Unit.mm)
            val q2 = Quantity(1, Unit.cm)
            q1.equivalent(q2) shouldBe true
        }
    }

    context("utility methods") {
        test("abs of positive") {
            Quantity(-5, Unit.cm).abs().value shouldBe 5
        }

        test("isZero") {
            Quantity(0, Unit.m).isZero shouldBe true
            Quantity(1, Unit.m).isZero shouldBe false
        }
    }

    context("factory methods") {
        test("length factory") {
            val q = Quantity.length("5cm")
            q.unit shouldBe Unit.cm
        }

        test("mass factory") {
            val q = Quantity.mass("100kg")
            q.unit shouldBe Unit.kg
        }

        test("parse with wildcard type") {
            val q = Quantity.parse("5m")
            q.unit shouldBe Unit.m
        }

        test("parseOrNull returns null on failure") {
            Quantity.parseOrNull("invalid") shouldBe null
        }

        test("parseOrNull returns Quantity on success") {
            Quantity.parseOrNull("5cm")?.unit shouldBe Unit.cm
        }
    }

    context("error handling") {
        test("throws on empty string") {
            shouldThrow<NumberFormatException> {
                Quantity.parse("")
            }
        }

        test("throws on unknown unit") {
            shouldThrow<NoSuchUnitException> {
                Quantity.parse("5xyz")
            }
        }

        test("throws on malformed number") {
            shouldThrow<NumberFormatException> {
                Quantity.parse("abcm")
            }
        }
    }
})