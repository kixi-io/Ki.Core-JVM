package io.kixi.uom

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.shouldBe
import java.math.BigDecimal as Dec

/**
 * Slice 1 of the UoM fixes: verifies the corrected conversion factors,
 * exact affine temperature math (including the new °F), exact division,
 * and the Unit.hashCode repair. Every expected value here was computed by
 * running the fixed code, not typed by hand.
 */
class UomCorrectnessTest : FunSpec({

    context("speed conversion factor (mps was inverted)") {
        test("1 mps is 3.6 kph") {
            (Quantity(1, Unit.mps) convertTo Unit.kph).toString() shouldBe "3.6kph"
        }
        test("90 kph is 25 mps") {
            (Quantity(90, Unit.kph) convertTo Unit.mps).toString() shouldBe "25mps:i"
        }
        test("36 kph is 10 mps") {
            (Quantity(36, Unit.kph) convertTo Unit.mps).toString() shouldBe "10mps:i"
        }
        test("mps is the larger unit") {
            (Unit.mps > Unit.kph) shouldBe true
        }
    }

    context("Unit.hashCode (was a bitwise or)") {
        test("hash is symbol.hashCode() * 31") {
            Unit.cm.hashCode() shouldBe "cm".hashCode() * 31
        }
        test("registry lookup and constant hash agree") {
            Unit.getUnit("cm")!!.hashCode() shouldBe Unit.cm.hashCode()
        }
    }

    context("temperature: exact affine conversions") {
        test("0°C is 273.15K") {
            (Quantity(0, Unit.dC) convertTo Unit.K).toString() shouldBe "273.15K"
        }
        test("300K is 26.85°C") {
            (Quantity(300, Unit.K) convertTo Unit.dC).toString() shouldBe "26.85°C"
        }
        test("25°C is 298.15K") {
            (Quantity(25, Unit.dC) convertTo Unit.K).toString() shouldBe "298.15K"
        }
        test("32°F is 0°C, exactly") {
            (Quantity(32, Unit.dF) convertTo Unit.dC).toString() shouldBe "0°C:i"
        }
        test("98.6°F is 37°C, exactly") {
            (Quantity("98.6", Unit.dF) convertTo Unit.dC).toString() shouldBe "37°C"
        }
        test("37°C is 98.6°F, exactly") {
            (Quantity(37, Unit.dC) convertTo Unit.dF).toString() shouldBe "98.6°F"
        }
        test("212°F is 100°C") {
            (Quantity(212, Unit.dF) convertTo Unit.dC).toString() shouldBe "100°C:i"
        }
        test("32°F is 273.15K") {
            (Quantity(32, Unit.dF) convertTo Unit.K).toString() shouldBe "273.15K"
        }
        test("273.15K is 32°F") {
            (Quantity("273.15", Unit.K) convertTo Unit.dF).toString() shouldBe "32°F"
        }
        test("-40°F is -40°C") {
            (Quantity(-40, Unit.dF) convertTo Unit.dC).toString() shouldBe "-40°C:i"
        }
        test("non-terminating conversions round at DECIMAL128") {
            val c = (Quantity(100, Unit.dF) convertTo Unit.dC).value as Dec
            c.toDouble() shouldBe 37.77777777777778
        }
        test("dF parses and prints as °F") {
            Quantity.parse("72dF").toString() shouldBe "72°F"
            Quantity.parse("-3.5dF").toString() shouldBe "-3.5°F"
        }
        test("dC still parses") {
            Quantity.parse("21dC").toString() shouldBe "21°C"
        }
        test("temperatures compare across scales") {
            Quantity<Temperature>(32, Unit.dF).compareTo(Quantity(0, Unit.dC)) shouldBe 0
            Quantity<Temperature>(50, Unit.dF).compareTo(Quantity(0, Unit.dC)) shouldBe 1
        }
    }

    context("division is exact") {
        test("Int values promote when the quotient is not whole") {
            (Quantity(5, Unit.cm) / 2).toString() shouldBe "2.5cm"
        }
        test("Int values stay Int when the quotient is whole") {
            (Quantity(10, Unit.cm) / 2).toString() shouldBe "5cm:i"
        }
        test("Long values promote when the quotient is not whole") {
            (Quantity(7L, Unit.mm) / 2).toString() shouldBe "3.5mm"
            (Quantity(8L, Unit.mm) / 2).toString() shouldBe "4mm:L"
        }
        test("Dec division no longer truncates to operand scale") {
            (Quantity(Dec("1"), Unit.L) / 2).toString() shouldBe "0.5ℓ"
            (Quantity(Dec("10"), Unit.cm) / Dec("4")).toString() shouldBe "2.5cm"
        }
        test("non-terminating Dec quotients round at DECIMAL128") {
            ((Quantity(Dec("10"), Unit.cm) / Dec("3")).value as Dec).toDouble() shouldBe
                    3.3333333333333335
        }
        test("\$100 / 3 is a third of a hundred, not \$33") {
            ((Quantity(100, Unit.USD) / 3).value as Dec).toDouble() shouldBe
                    33.333333333333336
        }
        test("division by zero still throws") {
            shouldThrow<ArithmeticException> { Quantity(5, Unit.cm) / 0 }
        }
    }

    context("Float and Double interoperate with Dec through their decimal form") {
        test("Dec plus Double") {
            (Quantity(Dec("1"), Unit.m) + 0.1).toString() shouldBe "1.1m"
        }
        test("Dec minus Float") {
            (Quantity(Dec("1"), Unit.m) - 0.1f).toString() shouldBe "0.9m"
        }
        test("Dec times Double") {
            (Quantity(Dec("3"), Unit.m) * 0.1).toString() shouldBe "0.3m"
        }
        test("Dec divided by Double") {
            (Quantity(Dec("1"), Unit.m) / 0.1).toString() shouldBe "10m"
        }
        test("Dec 0.1m compares equal to 0.1:d m") {
            Quantity<Length>(Dec("0.1"), Unit.m)
                .shouldBeEqualComparingTo(Quantity(0.1, Unit.m))
        }
    }

    context("regressions: existing behavior is unchanged") {
        test("sums take the smaller unit") {
            (Quantity(2, Unit.cm) + Quantity(3, Unit.mm)).toString() shouldBe "23mm:i"
            (Quantity(2, Unit.cm) - Quantity(5, Unit.mm)).toString() shouldBe "15mm:i"
        }
        test("scalar multiply") {
            (Quantity(5, Unit.cm) * 3).toString() shouldBe "15cm:i"
        }
        test("scientific notation parsing") {
            Quantity.parse("1.496e(8)km").toString() shouldBe "149600000km"
            Quantity.parse("5.5en7m").toSuffixString() shouldBe "0.00000055m"
        }
        test("currency prefix formatting") {
            Quantity(Dec("23.53"), Unit.USD).toString() shouldBe "\$23.53"
        }
        test("length conversion and equivalence") {
            (Quantity(100, Unit.cm) convertTo Unit.m).toString() shouldBe "1m:i"
            Quantity<Length>(1, Unit.cm).equivalent(Quantity(10, Unit.mm)) shouldBe true
            Quantity<Length>(1, Unit.cm).compareTo(Quantity(10, Unit.mm)) shouldBe 0
        }
        test("number type suffixes survive parsing") {
            Quantity.parse("235cm³:L").toString() shouldBe "235cm³:L"
        }
        test("rem, unary minus, abs") {
            (Quantity(7, Unit.cm) % 2).toString() shouldBe "1cm:i"
            (-Quantity(5, Unit.cm)).toString() shouldBe "-5cm:i"
            Quantity(-5, Unit.cm).abs().toString() shouldBe "5cm:i"
        }
    }
})