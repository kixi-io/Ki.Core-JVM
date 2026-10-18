package io.kixi.uom

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.checkAll
import java.math.MathContext
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

    // combineUnits/canCombineUnits were replaced by UnitAlgebra, which
    // converts operands to coherent pairing units instead of ignoring
    // prefixes. Full coverage lives in UnitAlgebraTest; these check the
    // unit-capability surface that replaced the old functions.
    context("UnitAlgebra unit capabilities") {
        test("Length × Length gives Area") {
            UnitAlgebra.productUnit(Unit.m, Unit.m) shouldBe Unit.m2
            (Quantity(2, Unit.cm) * Quantity(3, Unit.cm)).toString() shouldBe "6cm²:i"
        }

        test("Length × Area gives Volume, commuting") {
            UnitAlgebra.productUnit(Unit.m, Unit.m2) shouldBe Unit.m3
            UnitAlgebra.productUnit(Unit.m2, Unit.m) shouldBe Unit.m3
        }

        test("undefined combinations return null") {
            UnitAlgebra.productUnit(Unit.m, Unit.kg) shouldBe null
        }

        test("canMultiply and canDivide") {
            UnitAlgebra.canMultiply(Unit.m, Unit.m) shouldBe true
            UnitAlgebra.canMultiply(Unit.m, Unit.kg) shouldBe false
            UnitAlgebra.canDivide(Unit.kg, Unit.L) shouldBe true
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

    context("Int and Long results are exact, never wrapped") {
        test("convertTo throws when a whole Int result does not fit in Int") {
            val e = shouldThrow<ArithmeticException> { Quantity(781, Unit.day) convertTo Unit.ms }
            e.message shouldBe "781day:i in ms is 67478400000, which does not fit in Int; " +
                "convert a Long (:L) or Dec quantity instead"
            shouldThrow<ArithmeticException> { Quantity(-781, Unit.day) convertTo Unit.ms }
        }

        test("the same conversion from a Long or Dec quantity succeeds") {
            (Quantity(781L, Unit.day) convertTo Unit.ms).toString() shouldBe "67478400000ms:L"
            (Quantity(Dec("781"), Unit.day) convertTo Unit.ms).value shouldBeNumericallyEqualTo Dec("67478400000")
        }

        test("convertTo throws when a whole Long result does not fit in Long") {
            val e = shouldThrow<ArithmeticException> { Quantity(2689L, Unit.kg) convertTo Unit.fg }
            e.message shouldBe "2689kg:L in fg is 2689000000000000000000, which does not fit in Long; " +
                "convert a Dec quantity instead"
        }

        test("results at the Int and Long limits still fit") {
            (Quantity(Int.MAX_VALUE, Unit.mm) convertTo Unit.mm).value shouldBe Int.MAX_VALUE
            (Quantity(Int.MIN_VALUE, Unit.mm) convertTo Unit.mm).value shouldBe Int.MIN_VALUE
            // 2147483 m is 2147483000 mm, which fits; 2147484 m does not
            (Quantity(2_147_483, Unit.m) convertTo Unit.mm).value shouldBe 2_147_483_000
            shouldThrow<ArithmeticException> { Quantity(2_147_484, Unit.m) convertTo Unit.mm }
            (Quantity(Long.MAX_VALUE, Unit.mm) convertTo Unit.mm).value shouldBe Long.MAX_VALUE
            shouldThrow<ArithmeticException> { Quantity(Long.MAX_VALUE / 100, Unit.m) convertTo Unit.mm }
        }

        test("a fractional Int result still becomes Dec") {
            val q = Quantity(1500, Unit.mm) convertTo Unit.m
            q.value shouldBeNumericallyEqualTo Dec("1.5")
            q.value::class shouldBe Dec::class
        }

        test("Double results follow IEEE rules") {
            (Quantity(1e300, Unit.kg) convertTo Unit.fg).value shouldBe Double.POSITIVE_INFINITY
        }

        test("an :i or :L literal must be a whole number in range") {
            Quantity.parse("2147483647mm:i").value shouldBe Int.MAX_VALUE
            Quantity.parse("-2147483648mm:i").value shouldBe Int.MIN_VALUE
            Quantity.parse("1.5e3mm:i").value shouldBe 1500
            Quantity.parse("9223372036854775807mm:L").value shouldBe Long.MAX_VALUE

            shouldThrow<NumberFormatException> { Quantity.parse("99999999999mm:i") }.message shouldBe
                "'99999999999mm:i': 99999999999 does not fit in Int; use :L, or omit :i for a Dec"
            shouldThrow<NumberFormatException> { Quantity.parse("2147483648mm:i") }
            shouldThrow<NumberFormatException> { Quantity.parse("9223372036854775808mm:L") }.message shouldBe
                "'9223372036854775808mm:L': 9223372036854775808 does not fit in Long; omit :L for a Dec"
            shouldThrow<NumberFormatException> { Quantity.parse("1.5mm:i") }.message shouldBe
                "'1.5mm:i': 1.5 is not a whole number, so it cannot be Int (:i); omit :i for a Dec"
            shouldThrow<NumberFormatException> { Quantity<Length>("1en3mm:L") }
        }
    }

    context("compareTo compares exact physical amounts") {
        test("units with the same factor are still converted (°C and K both have factor 1)") {
            Quantity(10, Unit.dC) shouldBeGreaterThan Quantity(20, Unit.K)   // 10°C is 283.15K
            Quantity(Dec("0"), Unit.dC).compareTo(Quantity(Dec("273.15"), Unit.K)) shouldBe 0
            Quantity(Dec("-273.15"), Unit.dC).compareTo(Quantity(0, Unit.K)) shouldBe 0
            Quantity(32, Unit.dF).compareTo(Quantity(Dec("273.15"), Unit.K)) shouldBe 0
            Quantity(Dec("98.6"), Unit.dF).compareTo(Quantity(37, Unit.dC)) shouldBe 0
        }

        test("no Int or Long conversion, so no overflow") {
            Quantity(781, Unit.day) shouldBeGreaterThan Quantity(1, Unit.ms)
            Quantity(781, Unit.day).compareTo(Quantity(67_478_400_000L, Unit.ms)) shouldBe 0
            Quantity(Long.MAX_VALUE, Unit.km) shouldBeGreaterThan Quantity(Long.MAX_VALUE, Unit.mm)
        }

        test("exact beyond 34 significant digits and for non-terminating unit sizes") {
            val a = Quantity(Dec.ONE, Unit.m)
            val b = Quantity(Dec("1000.000000000000000000000000000000000001"), Unit.mm)
            a shouldBeLessThan b
            Quantity(760, Unit.mmHg).compareTo(Quantity(1, Unit.atm)) shouldBe 0
            Quantity(750, Unit.mmHg) shouldBeLessThan Quantity(1, Unit.atm)
            // 1mmHg is 133.3223684210526315789…Pa, repeating
            Quantity(1, Unit.mmHg) shouldBeGreaterThan Quantity(Dec("133.3223684210526315789473684210526315"), Unit.Pa)
            Quantity(1, Unit.mmHg) shouldBeLessThan Quantity(Dec("133.3223684210526315789473684210526316"), Unit.Pa)
        }

        test("different currencies throw; the same currency compares amounts") {
            shouldThrow<IncompatibleUnitsException> { Quantity(100, Unit.USD).compareTo(Quantity(5, Unit.EUR)) }
            Quantity(100, Unit.USD) shouldBeGreaterThan Quantity(5, Unit.USD)
            Quantity(Dec("5.00"), Unit.USD).compareTo(Quantity(5, Unit.USD)) shouldBe 0
        }

        test("different dimensions throw") {
            @Suppress("UNCHECKED_CAST")
            val metre = Quantity(1, Unit.m) as Quantity<Unit>
            @Suppress("UNCHECKED_CAST")
            val kilo = Quantity(1, Unit.kg) as Quantity<Unit>
            shouldThrow<IncompatibleUnitsException> { metre.compareTo(kilo) }
        }

        test("mixed number types across units") {
            Quantity(1.5, Unit.m).compareTo(Quantity(150, Unit.cm)) shouldBe 0
            Quantity(0.1, Unit.m).compareTo(Quantity(Dec("100"), Unit.mm)) shouldBe 0   // a Double counts as the decimal it prints as
            Quantity(1.5f, Unit.km).compareTo(Quantity(1500L, Unit.m)) shouldBe 0
        }

        test("infinite and NaN values are the same in every unit") {
            Quantity(Double.POSITIVE_INFINITY, Unit.mm) shouldBeGreaterThan Quantity(Long.MAX_VALUE, Unit.km)
            Quantity(Double.NEGATIVE_INFINITY, Unit.km) shouldBeLessThan Quantity(Long.MIN_VALUE, Unit.mm)
            Quantity(Double.NaN, Unit.mm) shouldBeGreaterThan Quantity(Double.POSITIVE_INFINITY, Unit.km)
            Quantity(Double.NaN, Unit.mm).compareTo(Quantity(Float.NaN, Unit.km)) shouldBe 0
            Quantity(Double.NaN, Unit.m) shouldBeGreaterThan Quantity(Dec.ONE, Unit.m)   // threw NumberFormatException before
        }

        test("sorting mixed units orders by amount") {
            listOf(Quantity(1, Unit.km), Quantity(10, Unit.m), Quantity(500, Unit.cm), Quantity(2000, Unit.mm))
                .sorted().map { it.toString() } shouldBe listOf("2000mm:i", "500cm:i", "10m:i", "1km:i")
        }

        test("agrees with a 300-digit reference and is antisymmetric") {
            val temps = listOf(Unit.K, Unit.dC, Unit.dF)
            val pressures = listOf(Unit.Pa, Unit.atm, Unit.mmHg, Unit.psi)
            val lengths = listOf(Unit.nm, Unit.mm, Unit.cm, Unit.m, Unit.km)
            val mc = MathContext(300)
            fun kelvin(q: Quantity<*>): Dec {
                val t = q.unit as Temperature
                return (Dec(q.value.toString()) + t.offset).divide(t.degreesPerKelvin, mc)
            }
            fun pascals(q: Quantity<*>): Dec {
                val p = q.unit as Pressure
                return Dec(q.value.toString()).multiply(p.pascals).divide(p.per, mc)
            }
            fun metres(q: Quantity<*>): Dec = Dec(q.value.toString()).multiply(q.unit.factor)
            val numbers = Arb.bind(Arb.int(0, 3), Arb.long(-2_000_000L, 2_000_000L), Arb.int(0, 6)) { kind, n, scale ->
                when (kind) {
                    0 -> n.toInt()
                    1 -> n
                    2 -> Dec.valueOf(n, scale)
                    else -> Dec.valueOf(n, scale).toDouble()
                }
            }
            suspend fun <U : Unit> check(units: List<U>, toBase: (Quantity<*>) -> Dec) {
                checkAll(2_000, numbers, Arb.element(units), numbers, Arb.element(units)) { v1, u1, v2, u2 ->
                    val a = Quantity<U>(v1, u1)
                    val b = Quantity<U>(v2, u2)
                    a.compareTo(b) shouldBe toBase(a).compareTo(toBase(b))
                    a.compareTo(b) shouldBe -b.compareTo(a)
                }
            }
            check(temps, ::kelvin)
            check(pressures, ::pascals)
            check(lengths, ::metres)
        }
    }

    context("equivalent means the same amount, whatever the units and number types") {
        test("across number types") {
            Quantity<Length>(100, Unit.cm).equivalent(Quantity(1000L, Unit.mm)) shouldBe true
            Quantity<Length>(1.5, Unit.m).equivalent(Quantity(150, Unit.cm)) shouldBe true
            Quantity<Length>(100, Unit.cm).equivalent(Quantity(Dec.ONE, Unit.m)) shouldBe true
            Quantity<Length>(1, Unit.cm).equivalent(Quantity(10, Unit.mm)) shouldBe true
            Quantity<Time>(781, Unit.day).equivalent(Quantity(67_478_400_000L, Unit.ms)) shouldBe true
        }

        test("exact, including offsets and non-terminating unit sizes") {
            Quantity<Temperature>(Dec("0"), Unit.dC).equivalent(Quantity(Dec("273.15"), Unit.K)) shouldBe true
            Quantity<Temperature>(32, Unit.dF).equivalent(Quantity(0, Unit.dC)) shouldBe true
            Quantity<Pressure>(760, Unit.mmHg).equivalent(Quantity(1, Unit.atm)) shouldBe true
            Quantity<Length>(1, Unit.m).equivalent(Quantity(101, Unit.cm)) shouldBe false
            Quantity<Length>(Dec.ONE, Unit.m).equivalent(Quantity(Dec("1000.000000000000000000000000000000000001"), Unit.mm)) shouldBe false
        }

        test("different dimensions or currencies are not equivalent, and do not throw") {
            @Suppress("UNCHECKED_CAST")
            val metre = Quantity(1, Unit.m) as Quantity<Unit>
            @Suppress("UNCHECKED_CAST")
            val kilo = Quantity(1, Unit.kg) as Quantity<Unit>
            metre.equivalent(kilo) shouldBe false
            Quantity<Currency>(100, Unit.USD).equivalent(Quantity(100, Unit.EUR)) shouldBe false
            Quantity<Currency>(Dec("5.00"), Unit.USD).equivalent(Quantity(5, Unit.USD)) shouldBe true
        }

        test("zero signs and NaN") {
            Quantity<Length>(-0.0, Unit.m).equivalent(Quantity(0.0, Unit.m)) shouldBe true
            Quantity<Length>(-0.0, Unit.m).compareTo(Quantity(0.0, Unit.m)) shouldBe 0
            Quantity<Length>(-0.0, Unit.m).equivalent(Quantity(0, Unit.cm)) shouldBe true
            Quantity<Length>(Double.NaN, Unit.m).equivalent(Quantity(Double.NaN, Unit.km)) shouldBe true
            Quantity<Length>(Double.NaN, Unit.m).equivalent(Quantity(1, Unit.m)) shouldBe false
        }

        test("equals still keeps the unit and number type") {
            (Quantity<Length>(1, Unit.cm) == Quantity(10, Unit.mm)) shouldBe false
            (Quantity<Length>(1, Unit.m) == Quantity(1L, Unit.m)) shouldBe false
        }
    }
})
