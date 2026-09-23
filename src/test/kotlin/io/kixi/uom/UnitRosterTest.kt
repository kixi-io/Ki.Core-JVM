package io.kixi.uom

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal as Dec

/**
 * Slice 3 of the UoM work: the expanded unit roster (Å, pg/fg, fmol, pℓ,
 * ms/µs/ns, the u and g/mol dalton aliases, mass concentration on the
 * Density dimension, and the Pressure dimension with exact rational
 * conversion). Every expected value here was computed by running the code.
 */
class UnitRosterTest : FunSpec({

    context("ångström") {
        test("parses and converts") {
            Quantity.parse("1.5Å").toString() shouldBe "1.5Å"
            (Quantity<Length>("1.5Å") convertTo Unit.nm).toString() shouldBe "0.15nm"
        }
        test("participates in the algebra") {
            (Quantity(10, Unit.Å) * Quantity(10, Unit.Å)).toString() shouldBe "1nm²:i"
        }
    }

    context("small masses, amounts, volumes, times") {
        test("pg and fg") {
            Quantity.parse("50pg").toString() shouldBe "50pg"
            (Quantity(1000, Unit.fg) convertTo Unit.pg).toString() shouldBe "1pg:i"
        }
        test("fmol") {
            Quantity.parse("5fmol").toString() shouldBe "5fmol"
            (Quantity(1000, Unit.fmol) convertTo Unit.pmol).toString() shouldBe "1pmol:i"
        }
        test("pℓ") {
            Quantity.parse("2.5pL").toString() shouldBe "2.5pℓ"
            (Quantity(1000, Unit.pL) convertTo Unit.nL).toString() shouldBe "1nℓ:i"
        }
        test("ms, µs, ns") {
            Quantity.parse("250ms").toString() shouldBe "250ms"
            Quantity.parse("5us").toString() shouldBe "5µs"
            (Quantity(1000, Unit.ns) convertTo Unit.µs).toString() shouldBe "1µs:i"
            (Quantity<Time>("1.5s") convertTo Unit.ms).toString() shouldBe "1500ms"
        }
    }

    context("dalton aliases") {
        test("u and gpmol parse to Da") {
            Quantity.parse("12u").toString() shouldBe "12Da"
            Quantity.parse("58.44gpmol").toString() shouldBe "58.44Da"
            Unit.getUnit("u") shouldBe Unit.Da
            Unit.getUnit("gpmol") shouldBe Unit.Da
        }
    }

    context("mass concentration (on the Density dimension)") {
        test("lab spellings parse, ASCII forms accepted") {
            Quantity.parse("50ngpuL").toString() shouldBe "50ngpµℓ"
        }
        test("numerically consistent with each other and with kgpm³") {
            (Quantity(1, Unit.ngpµL) convertTo Unit.µgpmL).toString() shouldBe "1µgpmℓ:i"
            (Quantity(1, Unit.gpmL) convertTo Unit.kgpm3).toString() shouldBe "1000kgpm³:i"
            (Quantity("2.5", Unit.gpmL) convertTo Unit.kgpm3).toString() shouldBe "2500kgpm³"
        }
        test("mass ÷ volume lands on the lab ladder") {
            (Quantity(1, Unit.kg) / Quantity(1, Unit.L)).toString() shouldBe "1gpmℓ:i"
            (Quantity(50, Unit.ng) / Quantity(25, Unit.µL)).toString() shouldBe "2µgpmℓ:i"
            (Quantity(100, Unit.pg) / Quantity(1, Unit.mL)).toString() shouldBe "100pgpmℓ:i"
        }
        test("round trips through the algebra") {
            (Quantity(2, Unit.µgpmL) * Quantity(500, Unit.mL)).toString() shouldBe "1mg:i"
            (Quantity(1, Unit.mg) / Quantity(1, Unit.mgpmL)).toString() shouldBe "1mℓ:i"
        }
    }

    context("pressure") {
        test("decimal conversions are exact") {
            (Quantity<Pressure>("1atm") convertTo Unit.kPa).toString() shouldBe "101.325kPa"
            (Quantity<Pressure>("1atm") convertTo Unit.Pa).toString() shouldBe "101325Pa"
            (Quantity<Pressure>("1bar") convertTo Unit.kPa).toString() shouldBe "100kPa"
            (Quantity<Pressure>("1013.25mbar") convertTo Unit.atm).toString() shouldBe "1atm"
        }
        test("mmHg is the torr: 760mmHg is exactly 1atm") {
            (Quantity(760, Unit.mmHg) convertTo Unit.atm).toString() shouldBe "1atm:i"
            (Quantity(1, Unit.atm) convertTo Unit.mmHg).toString() shouldBe "760mmHg:i"
            Quantity.parse("25Torr").toString() shouldBe "25mmHg"
        }
        test("non-terminating conversions round at DECIMAL128") {
            ((Quantity<Pressure>("1mmHg") convertTo Unit.Pa).value as Dec).toDouble() shouldBe
                    133.32236842105263
            ((Quantity<Pressure>("1psi") convertTo Unit.Pa).value as Dec).toDouble() shouldBe
                    6894.757293168362
            ((Quantity<Pressure>("14.7psi") convertTo Unit.atm).value as Dec).toDouble() shouldBe
                    1.0002756694752026
        }
        test("pressures compare across units") {
            (Quantity<Pressure>(1, Unit.atm) > Quantity(750, Unit.mmHg)) shouldBe true
        }
        test("no product rules involve pressure yet") {
            shouldThrow<UndefinedUnitArithmeticException> {
                Quantity(1, Unit.atm) * Quantity(1, Unit.L)
            }
        }
    }
})