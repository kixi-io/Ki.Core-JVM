package io.kixi

import io.kixi.text.ParseException
import io.kixi.uom.Quantity
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import java.math.BigDecimal as Dec
import kotlin.math.sign
import io.kixi.uom.Unit as KiUnit

class MeasureTest : StringSpec({

    fun m(text: String) = Measure.parse(text)
    fun sigma(x: Measure): Double = when (val u = x.uncertainty) {
        is Quantity<*> -> u.value.toDouble()
        else -> (u as Number).toDouble()
    }
    fun central(x: Measure): Double = when (val v = x.value) {
        is Quantity<*> -> v.value.toDouble()
        else -> (v as Number).toDouble()
    }

    // ========== Literal ==========

    "literals read back as written" {
        for (text in listOf(
            "0.0042±0.0006µM", "0.512±0.008", "120±11", "5±1cm:i", "5.0±0.1cm:d", "1.5bd±0.1bd",
            "2L±1L", "0.5f±0.1f", "-3.25±0.5", "20±0.5°C", "-5±0.1cm", "0±0",
        )) {
            m(text).toString() shouldBe text
            m(m(text).toString()) shouldBe m(text)
        }
    }

    "the numbers take one type" {
        m("0.512±0.008").value shouldBe 0.512
        m("120±11").value shouldBe 120
        m("120±11").uncertainty shouldBe 11
        m("120±0.5").value shouldBe 120.0                  // Int and Double: both Double
        m("1.5bd±1").uncertainty shouldBe Dec.ONE           // Dec and Int: both Dec
        m("0.0042±0.0006µM").value shouldBe Quantity(Dec("0.0042"), KiUnit.µM)
        m("5±1cm:i").value shouldBe Quantity(5, KiUnit.cm)
        m("5±1cm").value shouldBe Quantity(Dec(5), KiUnit.cm)   // a quantity's number is Dec unless suffixed
    }

    "+- is the ASCII spelling" {
        m("0.512+-0.008") shouldBe m("0.512±0.008")
        m("0.0042+-0.0006µM") shouldBe m("0.0042±0.0006µM")
        m("-3+-1") shouldBe m("-3±1")
    }

    "each side may have its own unit; the uncertainty takes the value's" {
        val x = m("5cm±1mm")
        x.value shouldBe Quantity(Dec(5), KiUnit.cm)
        x.uncertainty shouldBe Quantity(Dec("0.1"), KiUnit.cm)
        x.toString() shouldBe "5±0.1cm"
        x.unit shouldBe KiUnit.cm
    }

    "a temperature's uncertainty converts without the offset" {
        val x = Measure.of(Quantity(Dec(20), KiUnit.dC), Quantity(Dec("0.5"), KiUnit.K))
        x.uncertainty shouldBe Quantity(Dec("0.5"), KiUnit.dC)
        val f = Measure.of(Quantity(Dec(20), KiUnit.dC), Quantity(Dec("1.8"), KiUnit.dF))
        (f.uncertainty as Quantity<*>).value.let { (it as Dec).compareTo(Dec.ONE) } shouldBe 0
        m("68±0.9°F").toString() shouldBe "68±0.9°F"
    }

    "a small Double quantity is written in plain notation" {
        val x = Measure.of(Quantity(1.0e-5, KiUnit.µM), Quantity(2.0e-6, KiUnit.µM))
        x.toString() shouldBe "0.00001±0.000002µM:d"
        m(x.toString()) shouldBe x
    }

    "bad literals and values are refused" {
        for (bad in listOf("5±", "±5", "5", "5±abc", "5±1xyz", "5±-1", "\$5±\$1")) {
            shouldThrow<ParseException> { m(bad) }
        }
        shouldThrow<IllegalArgumentException> { Measure.of(5, -1) }
        shouldThrow<IllegalArgumentException> { Measure.of(5.0, Double.NaN) }
        shouldThrow<IllegalArgumentException> { Measure.of(Double.POSITIVE_INFINITY, 1.0) }
        shouldThrow<IllegalArgumentException> { Measure.of(Quantity(5, KiUnit.cm), Quantity(1, KiUnit.g)) }
        shouldThrow<IllegalArgumentException> { Measure.of(Quantity(5, KiUnit.USD), Quantity(1, KiUnit.USD)) }
        shouldThrow<IllegalArgumentException> { Measure.of("5", 1) }
    }

    // ========== Equality and order ==========

    "equality keeps units and number types, and reads a Dec by value" {
        m("1.50bd±0.1bd") shouldBe m("1.5bd±0.10bd")
        m("1.50bd±0.1bd").hashCode() shouldBe m("1.5bd±0.10bd").hashCode()
        m("1±0.1cm") shouldNotBe m("10±1mm")
        m("5±1") shouldNotBe m("5L±1L")
        m("5±1") shouldNotBe m("5±2")
    }

    "order is the central value, then the uncertainty, across units" {
        m("1±0.1cm").compareTo(m("10±1mm")) shouldBe 0
        m("5±1") shouldBeLessThan m("5±2")
        m("4±9") shouldBeLessThan m("5±0")
        m("2±0.1cm") shouldBeGreaterThan m("19±5mm")
        // uncertainties compare as differences: 0.5°C and 0.9°F are the same, though 0.5°C reads warmer than 0.9°F
        Measure.of(Quantity(Dec(20), KiUnit.dC), Quantity(Dec("0.5"), KiUnit.dC))
            .compareTo(Measure.of(Quantity(Dec(68), KiUnit.dF), Quantity(Dec("0.9"), KiUnit.dF))) shouldBe 0
        m("5±1").compareTo(m("5.0±1.0")) shouldBe 0
        shouldThrow<IllegalArgumentException> { m("5±1").compareTo(m("5±1cm")) }
    }

    // ========== Arithmetic ==========

    "sums and differences add uncertainties in quadrature" {
        val s = m("10±3") + m("20±4")
        s shouldBe m("30.0±5.0")
        (m("10±3") - m("20±4")) shouldBe m("-10.0±5.0")
        (m("10±3") + 5) shouldBe m("15.0±3.0")
        (-m("10±3")) shouldBe m("-10±3")
    }

    "products and quotients combine relative uncertainties" {
        (m("4±0.3") * 2) shouldBe m("8.0±0.6")
        (m("8±0.6") / 2) shouldBe m("4.0±0.3")
        val p = m("10±1") * m("20±2")
        central(p) shouldBe 200.0
        sigma(p) shouldBe (Math.sqrt(800.0) plusOrMinus 1e-12)
        val q = m("10±1") / m("20±2")
        central(q) shouldBe 0.5
        sigma(q) shouldBe (Math.sqrt(0.005) plusOrMinus 1e-15)
        val z = m("0±1") * m("3±0")                  // a zero central value: absolute terms
        sigma(z) shouldBe 3.0
    }

    "a Dec measure stays Dec, a count becomes a Double" {
        (m("1.5bd±0.1bd") + m("2.5bd±0.1bd")).value.shouldBeInstanceOf<Dec>()
        (m("120±11") + m("80±9")).value shouldBe 200.0
        (m("0.5f±0.1f") + m("0.25f±0.1f")).value.shouldBeInstanceOf<Float>()
    }

    "quantities keep their units" {
        val s = m("5±0.1cm:d") + m("2±0.2mm:d")
        s.unit shouldBe KiUnit.mm
        central(s) shouldBe 52.0
        sigma(s) shouldBe (Math.sqrt(1.04) plusOrMinus 1e-12)
        val area = m("2±0.1cm:d") * m("3±0.1cm:d")
        area.unit shouldNotBe null
        val conc = m("10±1µM:d") / 2
        conc.unit shouldBe KiUnit.µM
        central(conc) shouldBe 5.0
        val ratio = m("10±1mm:d") / m("2±0.1cm:d")       // one dimension: a plain number
        ratio.unit.shouldBeNull()
        central(ratio) shouldBe 0.5
        shouldThrow<Exception> { m("5±1cm") + m("5±1g") }
    }

    "sum and mean propagate under independence" {
        val xs = listOf(m("10±3"), m("20±4"))
        Measure.sum(xs) shouldBe m("30.0±5.0")
        Measure.mean(xs) shouldBe m("15.0±2.5")
        Measure.sum(emptyList()).shouldBeNull()
        Measure.mean(listOf(m("1±0.1bd"), Dec(3))).toString() shouldBe "2bd±0.05bd"
    }

    "conversion: the value as a reading, the uncertainty as a difference" {
        m("20±0.5°C").to(KiUnit.dF).let { central(it) shouldBe 68.0; sigma(it) shouldBe 0.9 }
        m("5±0.1cm:d").to(KiUnit.mm) shouldBe m("50.0±1.0mm:d")
        shouldThrow<IllegalArgumentException> { m("5±1").to(KiUnit.mm) }
        shouldThrow<IllegalArgumentException> { m("5±1cm").to(KiUnit.g) }
    }

    "sums in mixed units use the base unit, as KQ's aggregates do" {
        val s = Measure.sum(listOf(m("1±0.1cm:d"), m("10±1mm:d")))!!
        s.unit shouldBe KiUnit.m
        central(s) shouldBe (0.02 plusOrMinus 1e-15)
        Measure.sum(listOf(m("1±0.1cm:d"), m("2±0.1cm:d")))!!.unit shouldBe KiUnit.cm
        shouldThrow<IllegalArgumentException> { Measure.sum(listOf(m("1±0.1cm"), m("1±0.1"))) }
    }

    "weighted mean: inverse-variance weights" {
        val w = Measure.weightedMean(listOf(m("10.0±1.0"), m("12.0±2.0")))!!
        central(w) shouldBe (10.4 plusOrMinus 1e-12)                   // (10/1 + 12/4) / (1 + 1/4)
        sigma(w) shouldBe (Math.sqrt(0.8) plusOrMinus 1e-12)          // 1/sqrt(1.25)
        Measure.weightedMean(listOf(m("5±1"))) shouldBe m("5±1")
        Measure.weightedMean(emptyList()).shouldBeNull()
        shouldThrow<IllegalArgumentException> { Measure.weightedMean(listOf(m("5±0"), m("6±1"))) }
    }

    "relative uncertainty" {
        m("10±0.5").relative shouldBe 0.05
        m("0±1").relative.shouldBeNull()
        m("10±0.5mM").relative shouldBe 0.05
    }

    // ========== The review of 2026-10-28 ==========

    "uncertainties in different units compare exactly, both ways, as a key orders them" {
        val a = m("20°C±1°F")                        // 20±0.555…6°C: the conversion to °C rounds
        val b = m("68±1°F")
        a.compareTo(b) shouldBe -b.compareTo(a)
        (a.compareTo(b) == 0) shouldBe (b.compareTo(a) == 0)
        m("20±0.5°C").compareTo(m("68±0.9°F")) shouldBe 0
        m("68±0.9°F").compareTo(m("20±0.5°C")) shouldBe 0
        m("760±1mmHg").compareTo(m("1atm±1mmHg")).let { it shouldBe -m("1atm±1mmHg").compareTo(m("760±1mmHg")) }
        // A sweep of natural pairs: antisymmetric, and 0 exactly both ways.
        for (c in 0..30) for (u in listOf("0.1", "0.5", "1", "2")) {
            val x = m("$c°C±${u}°F")
            val y = m("${Dec(c).multiply(Dec("1.8")).add(Dec(32)).toPlainString()}±${u}°F")
            x.compareTo(y).sign shouldBe -y.compareTo(x).sign
        }
    }

    "dF is a unit, not a Float suffix; a long whole number reads" {
        m("20±1dF") shouldBe m("20±1°F")
        (m("20±1dF").value as Quantity<*>).unit.symbol shouldBe "°F"
        m("5±1f") shouldBe Measure.of(5f, 1f)                              // a suffix after a number still counts
        val big = Measure.of(Quantity(Dec("1E+20"), KiUnit.cm), Quantity(Dec.ONE, KiUnit.cm))
        Measure.parse(big.toString()) shouldBe big
        Measure.parse("100000000000000000000±1").value shouldBe Dec("100000000000000000000")
    }

    "negation widens rather than wraps" {
        (-Measure.of(Int.MIN_VALUE, 1)).value shouldBe 2147483648L
        (Measure.of(0, 0) - Measure.of(Int.MIN_VALUE, 0)).value.let { (it as Number).toDouble() shouldBe 2.147483648E9 }
        (-Measure.of(Long.MIN_VALUE, 1L)).value shouldBe Dec(Long.MIN_VALUE).negate()
        (-m("-2147483648±1cm:i")).value shouldBe Quantity(2147483648L, KiUnit.cm)
    }

    "number types follow the inputs: Int stays Int when the conversion is whole, Double stays Double" {
        val i = m("5±1cm:i").to(KiUnit.mm)
        i.value shouldBe Quantity(50, KiUnit.mm)
        i.uncertainty shouldBe Quantity(10, KiUnit.mm)
        m("5±1cm:i").to(KiUnit.m).value.let { (it as Quantity<*>).value.shouldBeInstanceOf<Dec>() }
        Measure.of(Quantity(5.0, KiUnit.cm), Quantity(1.0, KiUnit.mm)).uncertainty.let { (it as Quantity<*>).value.shouldBeInstanceOf<Double>() }
        Measure.of(Quantity(5, KiUnit.cm), Quantity(1, KiUnit.mm)).uncertainty shouldBe Quantity(Dec("0.1"), KiUnit.cm)
        Measure.of(Quantity(5, KiUnit.cm), Quantity(10, KiUnit.mm)).uncertainty shouldBe Quantity(1, KiUnit.cm)
    }

    "equal measures hash alike, Dec quantities included" {
        m("1.5±0.1cm") shouldBe m("1.50±0.1cm")
        m("1.5±0.1cm").hashCode() shouldBe m("1.50±0.1cm").hashCode()
        Quantity(Dec("1.5"), KiUnit.cm).hashCode() shouldBe Quantity(Dec("1.50"), KiUnit.cm).hashCode()
    }

    // ========== Ki ==========

    "Ki reads, writes and types a measure" {
        val x = Ki.parse("0.0042±0.0006µM")
        x shouldBe m("0.0042±0.0006µM")
        Ki.parse("0.512+-0.008") shouldBe m("0.512±0.008")
        Ki.format(x) shouldBe "0.0042±0.0006µM"
        Ki.typeOf(x) shouldBe Type.Measure
        Type.forName("Measure") shouldBe Type.Measure
        Type.of(Measure::class.java) shouldBe Type.Measure
        TypeDef.forName("Measure_N")!!.matches(null) shouldBe true
        TypeDef.forName("Measure")!!.matches(x) shouldBe true
        TypeDef.Number.matches(x) shouldBe false
        Ki.parse("-5") shouldBe -5
        Ki.parse("1.2.3") shouldBe Version.parse("1.2.3")
    }
})
