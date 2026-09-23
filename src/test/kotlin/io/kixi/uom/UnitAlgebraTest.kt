package io.kixi.uom

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal as Dec

/**
 * Slice 2 of the UoM work: cross-dimension Quantity arithmetic through
 * [UnitAlgebra]. Every expected value here was computed by running the code,
 * not typed by hand.
 */
class UnitAlgebraTest : FunSpec({

    context("concentration × volume (the headline)") {
        test("150mM × 1ℓ is 150mmol") {
            (Quantity.parse("150mM") * Quantity.parse("1LT")).toString() shouldBe "150mmol"
        }
        test("products commute") {
            (Quantity.parse("1LT") * Quantity.parse("150mM")).toString() shouldBe "150mmol"
        }
        test("2M × 50µℓ is 100µmol") {
            (Quantity.parse("2M") * Quantity.parse("50uL")).toString() shouldBe "100µmol"
        }
        test("0.5M × 2ℓ is 1mol") {
            (Quantity.parse("0.5M") * Quantity.parse("2LT")).toString() shouldBe "1mol"
        }
    }

    context("moles, mass, and molar mass") {
        test("2mol × 58.44Da is 116.88g") {
            (Quantity.parse("2mol") * Quantity.parse("58.44Da")).toString() shouldBe "116.88g"
            (Quantity.parse("58.44Da") * Quantity.parse("2mol")).toString() shouldBe "116.88g"
        }
        test("5g ÷ 58.44Da is moles (85.56mmol)") {
            val moles = Quantity.parse("5g") / Quantity.parse("58.44Da")
            moles.unit.toString() shouldBe "mmol"
            (moles.value as Dec).toDouble() shouldBe 85.55783709787816
        }
        test("mass ÷ moles is molar mass") {
            (Quantity.parse("116.88g") / Quantity.parse("2mol")).toString() shouldBe "58.44Da"
        }
        test("mass ÷ molar mass is moles") {
            (Quantity.parse("116.88g") / Quantity.parse("58.44Da")).toString() shouldBe "2mol"
        }
    }

    context("moles, concentration, and volume quotients") {
        test("150mmol ÷ 1ℓ is 150mM") {
            (Quantity.parse("150mmol") / Quantity.parse("1LT")).toString() shouldBe "150mM"
        }
        test("150mmol ÷ 150mM is 1ℓ") {
            (Quantity.parse("150mmol") / Quantity.parse("150mM")).toString() shouldBe "1ℓ"
        }
        test("1mol ÷ 4ℓ is 250mM") {
            (Quantity.parse("1mol") / Quantity.parse("4LT")).toString() shouldBe "250mM"
        }
    }

    context("geometry") {
        test("length × length is area") {
            (Quantity(2, Unit.cm) * Quantity(3, Unit.cm)).toString() shouldBe "6cm²:i"
            (Quantity(5, Unit.mm) * Quantity(3, Unit.mm)).toString() shouldBe "15mm²:i"
        }
        test("length × area is volume") {
            (Quantity(2, Unit.m) * Quantity(3, Unit.m2)).toString() shouldBe "6m³:i"
        }
        test("mixed prefixes get factor-corrected (the combineUnits bug)") {
            (Quantity(1, Unit.km) * Quantity(1, Unit.m)).toString() shouldBe "1000m²:i"
        }
        test("area ÷ length and volume ÷ area are lengths") {
            (Quantity(6, Unit.m2) / Quantity(2, Unit.m)).toString() shouldBe "3m:i"
            (Quantity(6, Unit.m3) / Quantity(2, Unit.m2)).toString() shouldBe "3m:i"
        }
        test("volume ÷ length is area, rescaled") {
            (Quantity(1, Unit.m3) / Quantity(2, Unit.m)).toString() shouldBe "5000cm²:i"
        }
    }

    context("speed and time") {
        test("60kph × 30min is 30km") {
            (Quantity(60, Unit.kph) * Quantity(30, Unit.min)).toString() shouldBe "30km:i"
        }
        test("100m ÷ 10s is 10mps") {
            (Quantity(100, Unit.m) / Quantity(10, Unit.s)).toString() shouldBe "10mps:i"
        }
        test("100km ÷ 50kph is 2h") {
            (Quantity(100, Unit.km) / Quantity(50, Unit.kph)).toString() shouldBe "2h:i"
        }
        test("90km ÷ 1h is 25mps") {
            (Quantity(90, Unit.km) / Quantity(1, Unit.h)).toString() shouldBe "25mps:i"
        }
    }

    context("density") {
        test("mass ÷ volume reads as mass concentration") {
            // Density results display on the lab ladder (g/mℓ family);
            // kgpm³ stays the base and is reachable through convertTo.
            (Quantity(1, Unit.kg) / Quantity(1, Unit.L)).toString() shouldBe "1gpmℓ:i"
            (Quantity(5, Unit.g) / Quantity(2, Unit.mL)).toString() shouldBe "2.5gpmℓ"
        }
        test("density × volume is mass") {
            (Quantity(1000, Unit.kgpm3) * Quantity(2, Unit.L)).toString() shouldBe "2kg:i"
        }
        test("mass ÷ density is volume") {
            (Quantity(1, Unit.kg) / Quantity(1000, Unit.kgpm3)).toString() shouldBe "1ℓ:i"
        }
    }

    context("same-dimension division gives a bare ratio") {
        test("300mm ÷ 1m prints as 0.3") {
            val r = Quantity.parse("300mm") / Quantity.parse("1m")
            r.toString() shouldBe "0.3"
            r.unit shouldBe Unit.ratio
        }
        test("6mol ÷ 2mol is 3") {
            (Quantity(6, Unit.mol) / Quantity(2, Unit.mol)).toString() shouldBe "3:i"
        }
        test("same-currency division works") {
            (Quantity(100, Unit.USD) / Quantity(50, Unit.USD)).toString() shouldBe "2:i"
        }
        test("temperature ratios use absolute temperatures") {
            val r = Quantity(20, Unit.dC) / Quantity(10, Unit.dC)
            (r.value as Dec).toDouble() shouldBe 1.0353169698039908 // 293.15K / 283.15K
        }
        test("ratio text reads as a Ki number") {
            (Quantity.parse("1m") / Quantity.parse("4m")).toString() shouldBe "0.25"
            (Quantity.parse("1m") / Quantity.parse("4m")).toSuffixString() shouldBe "0.25"
        }
    }

    context("numeric typing follows convertTo's precedent") {
        test("Double operands give Double results") {
            (Quantity(2.0, Unit.cm) * Quantity(3, Unit.cm)).toString() shouldBe "6.0cm²:d"
        }
        test("Dec stays Dec") {
            (Quantity(Dec("2"), Unit.cm) * Quantity(3, Unit.cm)).toString() shouldBe "6cm²"
        }
        test("integral operands with a non-whole result promote to Dec") {
            val conc = Quantity(1, Unit.mol) / Quantity(3, Unit.L)
            conc.unit.toString() shouldBe "mM"
            (conc.value as Dec).toDouble() shouldBe 333.3333333333333
        }
    }

    context("zero and errors") {
        test("zero results keep the natural unit") {
            (Quantity.parse("0mM") * Quantity.parse("1LT")).toString() shouldBe "0mol"
        }
        test("undefined products throw with a dimensional message") {
            val e = shouldThrow<UndefinedUnitArithmeticException> {
                Quantity.parse("1mM") * Quantity.parse("1mM")
            }
            e.message shouldBe
                    "No defined result for Concentration × Concentration (mM × mM)"
        }
        test("temperature never multiplies") {
            shouldThrow<UndefinedUnitArithmeticException> {
                Quantity(20, Unit.dC) * Quantity(2, Unit.cm)
            }
        }
        test("cross-currency division still refuses") {
            shouldThrow<IncompatibleUnitsException> {
                Quantity(100, Unit.USD) / Quantity(50, Unit.EUR)
            }
        }
        test("division by a zero quantity throws") {
            shouldThrow<ArithmeticException> {
                Quantity(1, Unit.mol) / Quantity(0, Unit.L)
            }
        }
    }

    context("capability helpers") {
        test("productUnit and quotientUnit report natural units") {
            UnitAlgebra.productUnit(Unit.M, Unit.L) shouldBe Unit.mol
            UnitAlgebra.productUnit(Unit.L, Unit.M) shouldBe Unit.mol
            UnitAlgebra.quotientUnit(Unit.g, Unit.Da) shouldBe Unit.mol
            UnitAlgebra.quotientUnit(Unit.cm, Unit.mm) shouldBe Unit.ratio
            UnitAlgebra.productUnit(Unit.mM, Unit.mM) shouldBe null
        }
        test("canMultiply and canDivide") {
            UnitAlgebra.canMultiply(Unit.M, Unit.L) shouldBe true
            UnitAlgebra.canDivide(Unit.K, Unit.cm) shouldBe false
        }
    }
})