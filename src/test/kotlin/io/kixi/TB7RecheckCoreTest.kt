package io.kixi

import io.kixi.uom.Currency
import io.kixi.uom.Quantity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal as Dec
import java.util.Random
import kotlin.math.sign
import io.kixi.uom.Unit as KiUnit

/**
 * TB7 re-check (2026-10-29), Ki.Core: Measure and Vector properties over random values in
 * every registered unit and number type. Each test collects every disagreement and fails
 * with a count and the first few, so one run shows the whole picture.
 */
class TB7RecheckCoreTest : FunSpec({

    val units: List<KiUnit> = KiUnit.allUnits().filter { it !is Currency }.distinct()
    val byDimension: Map<String, List<KiUnit>> = units.groupBy { it.dimensionName }

    fun report(name: String, problems: List<String>) {
        if (problems.isNotEmpty()) println("[$name] ${problems.size} problems; first: ${problems.take(8).joinToString(" | ")}")
        withClue(problems.take(8).joinToString("\n")) { problems.size shouldBe 0 }
    }

    // A number with few digits, as an Int, Long, Float, Double or Dec.
    fun number(rnd: Random, type: Int, nonNegative: Boolean): Number {
        val mant = rnd.nextInt(2000) - if (nonNegative) 0 else 1000
        return when (type) {
            0 -> mant
            1 -> mant.toLong() * (if (rnd.nextBoolean()) 1L else 1_000_000_000L)
            2 -> Dec.valueOf(mant.toLong(), rnd.nextInt(4)).toFloat()
            3 -> Dec.valueOf(mant.toLong(), rnd.nextInt(6) - 1).toDouble()
            else -> Dec.valueOf(mant.toLong(), rnd.nextInt(8) - 2)
        }
    }

    fun randomMeasure(rnd: Random, unit: KiUnit?): Measure {
        val type = rnd.nextInt(5)
        val v = number(rnd, type, false)
        val u = number(rnd, type, true)
        return if (unit == null) Measure.of(v, u) else Measure.of(Quantity(v, unit), Quantity(u, unit))
    }

    // A second measure near the first: often the same amount in another unit or number type.
    fun near(rnd: Random, a: Measure): Measure {
        val q = a.value as? Quantity<*>
        return when (rnd.nextInt(4)) {
            0 -> if (q != null) {
                val others = byDimension[(q.unit as KiUnit).dimensionName]!!
                try { a.to(others[rnd.nextInt(others.size)]) } catch (e: Exception) { a }
            } else a
            1 -> {   // the same numbers as Dec
                val v = Dec(((q?.value ?: a.value) as Number).toString())
                val u = Dec((((a.uncertainty as? Quantity<*>)?.value ?: a.uncertainty) as Number).toString())
                if (q != null) Measure.of(Quantity(v, q.unit as KiUnit), Quantity(u, q.unit as KiUnit)) else Measure.of(v, u)
            }
            2 -> randomMeasure(rnd, q?.unit as KiUnit?)
            else -> a
        }
    }

    test("R1 compareTo is antisymmetric, transitive and 0 for equal measures, in every unit and number type") {
        val rnd = Random(29)
        val problems = ArrayList<String>()
        var pairs = 0
        for (dim in byDimension.keys + listOf("<number>")) {
            val pool = ArrayList<Measure>()
            repeat(120) {
                val unit = if (dim == "<number>") null else byDimension[dim]!!.let { it[rnd.nextInt(it.size)] }
                val a = randomMeasure(rnd, unit)
                pool += a
                pool += near(rnd, a)
            }
            for (i in pool.indices) for (j in pool.indices) {
                if (rnd.nextInt(6) != 0) continue
                val a = pool[i]; val b = pool[j]
                pairs++
                val ab = a.compareTo(b).sign
                val ba = b.compareTo(a).sign
                if (ab != -ba) problems += "antisymmetry $a vs $b: $ab, $ba"
                if (a == b && (ab != 0 || a.hashCode() != b.hashCode())) problems += "equals but cmp=$ab or hash differs: $a vs $b"
            }
            try {
                val sorted = pool.sorted()
                for (k in 0 until sorted.size - 1) if (sorted[k].compareTo(sorted[k + 1]) > 0) problems += "sort order broken at $k: ${sorted[k]} > ${sorted[k + 1]}"
            } catch (e: IllegalArgumentException) {
                problems += "sort failed in $dim: ${e.message}"
            }
        }
        println("[R1] $pairs pairs")
        report("R1", problems)
    }

    test("R2 toString reads back equal through Measure.parse, Ki.parse and the +- spelling") {
        val rnd = Random(30)
        val problems = ArrayList<String>()
        var n = 0
        for (unit in units + listOf<KiUnit?>(null)) repeat(40) {
            val m = randomMeasure(rnd, unit)
            for (x in listOf(m, -m)) {
                n++
                val text = x.toString()
                try { if (Measure.parse(text) != x) problems += "Measure.parse($text) = ${Measure.parse(text)} != $x" } catch (e: Exception) { problems += "Measure.parse($text): ${e.message}" }
                try { if (Ki.parse(text) != x) problems += "Ki.parse($text) != $x" } catch (e: Exception) { problems += "Ki.parse($text): ${e.message}" }
                try { if (Measure.parse(text.replace("±", "+-")) != x) problems += "+- $text" } catch (e: Exception) { problems += "+- $text: ${e.message}" }
                if (Ki.format(x) != text) problems += "Ki.format($x) != toString"
            }
        }
        println("[R2] $n measures")
        report("R2", problems)
    }

    test("R3 edge values read back: -0.0, extremes, exponents, big whole values") {
        val cm = KiUnit.getUnit("cm")!!
        val cases = listOf(
            Measure.of(-0.0, 1.0), Measure.of(Quantity(-0.0, cm), Quantity(1.0, cm)), Measure.of(-0.0f, 0.5f),
            Measure.of(1.0E-5, 1.0E-6), Measure.of(Quantity(1.0E-5, cm), Quantity(1.0E-6, cm)), Measure.of(Quantity(1.0E20, cm), Quantity(1.0, cm)),
            Measure.of(Quantity(1.0E-7f, cm), Quantity(1.0E-8f, cm)), Measure.of(Quantity(3.0E38f, cm), Quantity(1.0f, cm)),
            Measure.of(Int.MIN_VALUE, 0), Measure.of(Long.MIN_VALUE, 1L), Measure.of(Quantity(Long.MIN_VALUE, cm), Quantity(1L, cm)),
            -Measure.of(Long.MIN_VALUE, 1L), -Measure.of(Quantity(Long.MIN_VALUE, cm), Quantity(1L, cm)),
            Measure.of(Dec("-1E+30"), Dec("1E+29")), Measure.of(Quantity(Dec("-1E+30"), cm), Quantity(Dec("1E+29"), cm)),
            Measure.of(Double.MAX_VALUE, Double.MAX_VALUE), Measure.of(Double.MIN_VALUE, 0.0),
            Measure.of(5.0, -0.0),                                              // an uncertainty of -0.0 is "not negative"
        )
        val problems = ArrayList<String>()
        for (m in cases) {
            val text = m.toString()
            try { if (Measure.parse(text) != m) problems += "$text reads as ${Measure.parse(text)}" } catch (e: Exception) { problems += "$text: ${e.message}" }
        }
        report("R3", problems)
    }

    test("R4 arithmetic that overflows a Double or Float is an ArithmeticException, never a non-finite measure") {
        val problems = ArrayList<String>()
        val cm = KiUnit.getUnit("cm")!!
        val products: List<Pair<String, () -> Measure?>> = listOf(
            "(1e308±1) * 10" to { Measure.of(1e308, 1.0) * 10 },
            "(1e308±1) + (1e308±1)" to { Measure.of(1e308, 1.0) + Measure.of(1e308, 1.0) },
            "(3e38f±1f) * 10f" to { Measure.of(3e38f, 1f) * 10f },
            "(1e308±1e308 cm:d) * 10" to { Measure.of(Quantity(1e308, cm), Quantity(1e308, cm)) * 10 },
            "sum(1e308±1, 1e308±1)" to { Measure.sum(listOf(Measure.of(1e308, 1.0), Measure.of(1e308, 1.0))) },
        )
        for ((what, f) in products) {
            try {
                problems += "$what = ${f()}, expected an ArithmeticException"
            } catch (e: ArithmeticException) {
                // as expected
            }
        }
        report("R4", problems)
    }

    test("R5 negation and subtraction never wrap; -(-m) compares equal to m") {
        val problems = ArrayList<String>()
        val cm = KiUnit.getUnit("cm")!!
        for (m in listOf(Measure.of(Int.MIN_VALUE, 1), Measure.of(Int.MAX_VALUE, 1), Measure.of(Long.MIN_VALUE, 1L), Measure.of(Long.MAX_VALUE, 0L),
            Measure.of(Quantity(Int.MIN_VALUE, cm), Quantity(1, cm)), Measure.of(Quantity(Long.MIN_VALUE, cm), Quantity(1L, cm)))) {
            val n = -m
            if (n.compareTo(m) != -m.compareTo(n) || (-n).compareTo(m) != 0) problems += "-($m) = $n, -(-m) = ${-n}"
            val amount = ((n.value as? Quantity<*>)?.value ?: n.value) as Number
            val orig = ((m.value as? Quantity<*>)?.value ?: m.value) as Number
            if (Dec(amount.toString()).compareTo(Dec(orig.toString()).negate()) != 0) problems += "-($m) has value $amount"
        }
        report("R5", problems)
    }

    test("R6 number types after to(unit) and of(two units), as design 4.5 states them") {
        val problems = ArrayList<String>()
        fun u(s: String) = KiUnit.getUnit(s)!!
        fun type(m: Measure) = listOf((m.value as Quantity<*>).value, (m.uncertainty as Quantity<*>).value).map { it::class.simpleName }
        val expect = listOf(
            Measure.parse("5±1cm:i").to(u("mm")) to listOf("Int", "Int"),
            Measure.parse("5±1cm:L").to(u("mm")) to listOf("Long", "Long"),
            Measure.parse("5±1cm:i").to(u("m")) to listOf("BigDecimal", "BigDecimal"),
            Measure.parse("0±1m:i").to(u("cm")) to listOf("Int", "Int"),
            Measure.parse("20±1°C:i").to(u("°F")) to listOf("BigDecimal", "BigDecimal"),   // 68 is whole, 1.8 is not
            Measure.parse("5.0±0.1cm:d").to(u("mm")) to listOf("Double", "Double"),
            Measure.parse("5.0±0.1cm:f").to(u("mm")) to listOf("Float", "Float"),
            Measure.of(Quantity(5.0, u("cm")), Quantity(1.0, u("mm"))) to listOf("Double", "Double"),
            Measure.of(Quantity(5, u("cm")), Quantity(1, u("mm"))) to listOf("BigDecimal", "BigDecimal"),
            Measure.of(Quantity(5, u("cm")), Quantity(10, u("mm"))) to listOf("Int", "Int"),
            Measure.of(Quantity(5L, u("cm")), Quantity(10L, u("mm"))) to listOf("Long", "Long"),
        )
        for ((m, types) in expect) if (type(m) != types) problems += "$m is ${type(m)}, expected $types"
        // A conversion whose Int result does not fit: ArithmeticException (KQ and KS must turn it into their own error)
        try { Measure.parse("3000000±1km:i").to(u("mm")); problems += "no overflow for 3000000km:i in mm" } catch (e: ArithmeticException) { } catch (e: Exception) { problems += "overflow is ${e::class.simpleName}" }
        report("R6", problems)
    }

    test("R7 equal measures hash alike (Dec scales, quantities, mixed scales)") {
        val rnd = Random(31)
        val problems = ArrayList<String>()
        val cm = KiUnit.getUnit("cm")!!
        repeat(2000) {
            val v = Dec.valueOf(rnd.nextInt(20000) - 10000L, rnd.nextInt(5))
            val s = Dec.valueOf(rnd.nextInt(2000).toLong(), rnd.nextInt(5))
            val a = Measure.of(v, s); val b = Measure.of(v.setScale(v.scale() + 3), s.setScale(s.scale() + 2))
            if (a != b || a.hashCode() != b.hashCode()) problems += "$a / $b"
            val qa = Measure.of(Quantity(v, cm), Quantity(s, cm)); val qb = Measure.of(Quantity(v.setScale(v.scale() + 1), cm), Quantity(s.setScale(s.scale() + 4), cm))
            if (qa != qb || qa.hashCode() != qb.hashCode()) problems += "$qa / $qb"
        }
        report("R7", problems)
    }

    test("R8 sum, mean and weightedMean match the formulas, in a common unit else the base unit") {
        val problems = ArrayList<String>()
        fun close(a: Double, b: Double) = Math.abs(a - b) <= 1e-12 * maxOf(1.0, Math.abs(b))
        fun amt(x: Any) = ((x as? Quantity<*>)?.value ?: x) as Number
        val xs = listOf(Measure.parse("10±3"), Measure.parse("20±4"), Measure.parse("30±12"))
        val sum = Measure.sum(xs)!!; val mean = Measure.mean(xs)!!; val wm = Measure.weightedMean(xs)!!
        if (!close(amt(sum.value).toDouble(), 60.0) || !close(amt(sum.uncertainty).toDouble(), 13.0)) problems += "sum $sum"
        if (!close(amt(mean.value).toDouble(), 20.0) || !close(amt(mean.uncertainty).toDouble(), 13.0 / 3)) problems += "mean $mean"
        val w = listOf(1 / 9.0, 1 / 16.0, 1 / 144.0)
        val wmv = (10 * w[0] + 20 * w[1] + 30 * w[2]) / w.sum()
        if (!close(amt(wm.value).toDouble(), wmv) || !close(amt(wm.uncertainty).toDouble(), 1 / Math.sqrt(w.sum()))) problems += "weightedMean $wm (expected $wmv)"
        // mixed units: base unit, values as readings, uncertainties as differences
        val t = Measure.mean(listOf(Measure.parse("20±1°C"), Measure.parse("68±1.8°F")))!!
        if ((t.unit as KiUnit).symbol != "K" || !close(amt(t.value).toDouble(), 293.15) || !close(amt(t.uncertainty).toDouble(), Math.sqrt(2.0) / 2)) problems += "mean of °C and °F: $t"
        val c = Measure.weightedMean(listOf(Measure.parse("1±0.1µM"), Measure.parse("1000±100nM")))!!
        if (!close(amt(c.value).toDouble(), 1e-6) || !close(amt(c.uncertainty).toDouble(), 1e-7 / Math.sqrt(2.0))) problems += "weightedMean µM and nM: $c"
        report("R8", problems)
    }

    test("R9 Vector: toString reads back equal; order antisymmetric; equal vectors hash alike; -0.0 kept") {
        val rnd = Random(32)
        val problems = ArrayList<String>()
        val types = listOf(Type.Int, Type.Long, Type.Float, Type.Double, Type.Dec)
        val pool = ArrayList<Vector<*>>()
        repeat(600) {
            val t = types[rnd.nextInt(5)]
            val n = rnd.nextInt(5)
            val xs: List<Number> = List(n) {
                val mant = rnd.nextInt(2000) - 1000
                when (t) {
                    Type.Int -> mant
                    Type.Long -> mant * 10_000_000_000L
                    Type.Float -> if (rnd.nextInt(20) == 0) -0.0f else Dec.valueOf(mant.toLong(), rnd.nextInt(8) - 2).toFloat()
                    Type.Double -> if (rnd.nextInt(20) == 0) -0.0 else Dec.valueOf(mant.toLong(), rnd.nextInt(12) - 3).toDouble()
                    else -> Dec.valueOf(mant.toLong(), rnd.nextInt(30) - 25)
                }
            }
            val v = Vector.of(t, xs)
            pool += v
            val text = v.toString()
            try { if (Vector.parse(text) != v) problems += "parse($text) = ${Vector.parse(text)}" } catch (e: Exception) { problems += "parse($text): ${e.message}" }
            try { if (Ki.parse(text) != v) problems += "Ki.parse($text)" } catch (e: Exception) { problems += "Ki.parse($text): ${e.message}" }
        }
        for (i in pool.indices) for (j in pool.indices) {
            if (rnd.nextInt(8) != 0) continue
            val a = pool[i]; val b = pool[j]
            if (a.compareTo(b).sign != -b.compareTo(a).sign) problems += "antisymmetry $a $b"
            if (a == b && (a.compareTo(b) != 0 || a.hashCode() != b.hashCode())) problems += "equal $a $b"
        }
        // -0.0: kept when it is already of the vector's type, and through a mixed list?
        if ((Vector.of(Type.Double, listOf(-0.0))[0] as Double).let { 1 / it } != Double.NEGATIVE_INFINITY) problems += "of(Double, [-0.0])"
        if ((Vector.of(Type.Float, listOf<Number>(-0.0))[0] as Float).let { 1 / it } != Float.NEGATIVE_INFINITY) problems += "of(Float, [-0.0 as Double]) loses the sign"
        if ((Vector.of(Type.Double, listOf<Number>(1, -0.0))[1] as Double).let { 1 / it } != Double.NEGATIVE_INFINITY) problems += "of(Double, [1, -0.0]) loses the sign"
        if (((-Vector.ofDoubles(0.0))[0] as Double).let { 1 / it } != Double.NEGATIVE_INFINITY) problems += "-(.vec<Double> { 0.0 }) is +0.0 (Kotlin's -0.0 is -0.0)"
        report("R9", problems)
    }

    test("R11 subtraction of temperatures in two units gives the difference Quantity gives (offset units)") {
        val problems = ArrayList<String>()
        fun u(s: String) = KiUnit.getUnit(s)!!
        fun q(n: Int, s: String) = Quantity(n, u(s))
        @Suppress("UNCHECKED_CAST")
        fun qMinus(a: Quantity<*>, b: Quantity<*>) = (a as Quantity<KiUnit>) - (b as Quantity<KiUnit>)
        for ((a, b) in listOf(q(50, "°F") to q(20, "°C"), q(50, "°F") to q(283, "K"), q(20, "°C") to q(50, "°F"), q(300, "K") to q(20, "°C"), q(20, "°C") to q(293, "K"))) {
            val quantity = qMinus(a, b)
            val measure = Measure.of(a, Quantity(1, a.unit as KiUnit)) - Measure.of(b, Quantity(1, b.unit as KiUnit))
            val exact = Measure.of(a, Quantity(1, a.unit as KiUnit)) - b
            @Suppress("UNCHECKED_CAST")
            if ((measure.value as Quantity<KiUnit>).compareTo(quantity as Quantity<KiUnit>) != 0 || (exact.value as Quantity<KiUnit>).compareTo(quantity) != 0)
                problems += "($a±1) - ($b±1) = $measure and ($a±1) - $b = $exact, but $a - $b = $quantity"
        }
        report("R11", problems)
    }

    test("R12 the uncertainty of a quotient of temperatures is a relative one (not the readings' ratio)") {
        val problems = ArrayList<String>()
        fun amt(x: Any) = (((x as? Quantity<*>)?.value ?: x) as Number).toDouble()
        for ((a, b) in listOf("20±1°C" to "10±1°C", "300±1K" to "150±1K", "68±1.8°F" to "20±1°C")) {
            val r = Measure.parse(a) / Measure.parse(b)
            val ka = Measure.parse(a).to(KiUnit.getUnit("K")!!); val kb = Measure.parse(b).to(KiUnit.getUnit("K")!!)
            val q = amt(ka.value) / amt(kb.value)
            val sigma = q * Math.sqrt(Math.pow(amt(ka.uncertainty) / amt(ka.value), 2.0) + Math.pow(amt(kb.uncertainty) / amt(kb.value), 2.0))
            if (Math.abs(amt(r.value) - q) > 1e-9 * q || Math.abs(amt(r.uncertainty) - sigma) > 1e-6 * sigma) problems += "($a) / ($b) = $r, expected $q±$sigma"
        }
        report("R12", problems)
    }

    test("R10 Vector.parse reads commas and block comments as KD does (F17)") {
        val problems = ArrayList<String>()
        val cases = mapOf(
            ".vec<Int> { 1, 2,3 }" to Vector.ofInts(1, 2, 3),
            ".vec<Int> { 1 /* two */ 3 }" to Vector.ofInts(1, 3),
            ".vec<Int> { 1 /* a /* nested */ b */ 3 }" to Vector.ofInts(1, 3),
            ".vec<Int> {\n 1 # one\n 2 // two\n}" to Vector.ofInts(1, 2),
            ".vec<Double> { 1.5,\n -2.5 }" to Vector.ofDoubles(1.5, -2.5),
            ".vec<Int> { 1 /* } */ 2 }" to Vector.ofInts(1, 2),
        )
        for ((text, v) in cases) try { if (Vector.parse(text) != v) problems += "$text -> ${Vector.parse(text)}" } catch (e: Exception) { problems += "$text: ${e.message}" }
        report("R10", problems)
    }
})

private inline fun <T> withClue(clue: String, block: () -> T): T = io.kotest.assertions.withClue(clue, block)
