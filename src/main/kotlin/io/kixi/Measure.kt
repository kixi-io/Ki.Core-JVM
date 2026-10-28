package io.kixi

import io.kixi.text.ParseException
import io.kixi.uom.Currency
import io.kixi.uom.Quantity
import io.kixi.uom.UnitAlgebra
import java.math.MathContext
import java.math.BigDecimal as Dec
import io.kixi.uom.Unit as KiUnit

/**
 * A value with its standard uncertainty (one standard deviation), as a measurement
 * is reported: `0.0042±0.0006µM`, an absorbance `0.512±0.008`, a count `120±11`
 * (Ki type bags design, TB7; RevDB design 4.5).
 *
 * [value] and [uncertainty] are numbers of one numeric type, or quantities of one
 * unit (any unit but a currency). The literal writes the unit once, after the
 * uncertainty; `+-` is the ASCII spelling of `±` in KD (KS and KQ, where `5+-3`
 * is arithmetic, read only `±`).
 *
 * **Equality and order.** Two measures are equal when both their values and their
 * uncertainties are ([equals], keeping units and number types as [Quantity.equals]
 * does). [compareTo] orders by the central value, then by the uncertainty, so it is 0
 * exactly when the two are the same amount with the same uncertainty (`1±0.1cm` and
 * `10±1mm`). A measure does not compare with a plain number or quantity: compare its
 * [value].
 *
 * **Arithmetic** treats the uncertainties as independent and propagates them to first
 * order: `+` and `-` add them in quadrature, `*` and `/` combine `|∂f/∂x|·σx` terms in
 * quadrature, and an exact number or quantity has no uncertainty. Results are Dec when
 * a Dec takes part, else Double (Float when every number is a Float); a count's Int or
 * Long becomes a Double, since an uncertainty in quadrature is rarely whole.
 */
class Measure private constructor(
    /** The central value: a number, or a quantity. */
    val value: Any,
    /** The standard uncertainty: the same numeric type as [value] and, for a quantity, the same unit; never negative. */
    val uncertainty: Any,
) : Comparable<Measure> {

    /** The unit of a measure of a quantity, or null for a measure of a plain number. */
    val unit: KiUnit? get() = (value as? Quantity<*>)?.unit

    /** [uncertainty] / |[value]|, a plain number (Double), or null when the value is 0. */
    val relative: Double?
        get() {
            val v = dec(amount(value)).abs()
            return if (v.signum() == 0) null else dec(amount(uncertainty)).divide(v, MC).toDouble()
        }

    override fun equals(other: Any?): Boolean =
        other is Measure && same(value, other.value) && same(uncertainty, other.uncertainty)

    override fun hashCode(): Int = hash(value) * 31 + hash(uncertainty)

    override fun compareTo(other: Measure): Int {
        val c = compareAmounts(value, other.value)
        return if (c != 0) c else compareDifferences(uncertainty, other.uncertainty)
    }

    /** The KD literal: `0.0042±0.0006µM`, `0.512±0.008`, `5bd±0.1bd`, `120±11`. */
    override fun toString(): String {
        val q = value as? Quantity<*> ?: return "${Ki.format(value)}±${Ki.format(uncertainty)}"
        val u = uncertainty as Quantity<*>
        return "${plain(q.value)}±${plain(u.value)}${q.unit.symbol}${typeSuffix(q.value)}"
    }

    operator fun unaryMinus(): Measure = Measure(negate(value), uncertainty)

    /**
     * This measure in [target], a unit of the same dimension: the value converts as a
     * reading (`20±0.5°C` is `68±0.9°F`), the uncertainty as a difference. A Double or
     * Float measure stays one; an Int or Long one becomes Dec when the conversion is
     * not whole, as [Quantity.convertTo] has it.
     *
     * @throws IllegalArgumentException for a measure of a plain number, a currency,
     *   or a unit of another dimension
     */
    fun to(target: KiUnit): Measure {
        val q = value as? Quantity<*> ?: throw IllegalArgumentException("A measure of a plain number has no unit to convert")
        val u = uncertainty as Quantity<*>
        val from: KiUnit = q.unit
        require(target !is Currency) { "A measure is not money" }
        require(from.dimension == target.dimension) { "Cannot convert a measure in ${from.dimensionName} to ${target.symbol}" }
        if (from == target) return this
        @Suppress("UNCHECKED_CAST")
        val v = (q as Quantity<KiUnit>).convertTo(target)
        val s = difference(dec(u.value), from, target)
        val sv: Number = when (v.value) { is Double -> s.toDouble(); is Float -> s.toFloat(); else -> s }
        return of(v, Quantity(sv, target))
    }

    operator fun plus(other: Any): Measure = combine('+', this, other)
    operator fun minus(other: Any): Measure = combine('-', this, other)
    operator fun times(other: Any): Measure = combine('*', this, other)
    operator fun div(other: Any): Measure = combine('/', this, other)

    companion object : Parseable<Measure> {

        /** The plus-minus sign the literal is written with. */
        const val PLUS_MINUS: Char = '±'

        private val MC = MathContext.DECIMAL128

        /**
         * A measure of [value] ± [uncertainty]: two numbers, two quantities of one
         * dimension (the uncertainty is expressed in the value's unit), or a number and
         * a quantity, the number taking the quantity's unit (`5±0.1cm` reads 5 as
         * centimeters). The numbers are brought to one numeric type, as Kotlin promotes
         * them (Dec, else Double, else Float, else Long, else Int).
         *
         * @throws IllegalArgumentException for a negative or non-finite uncertainty, a
         *   currency, units of different dimensions, or values that are not numbers
         */
        @JvmStatic
        fun of(value: Any, uncertainty: Any): Measure {
            var v = value
            var u = uncertainty
            if (v is Number && u is Quantity<*>) v = Quantity(v, u.unit)
            if (v is Quantity<*> && u is Number) u = Quantity(u, v.unit)
            when {
                v is Number && u is Number -> {
                    val t = common(v, u)
                    v = cast(v, t); u = cast(u, t)
                }
                v is Quantity<*> && u is Quantity<*> -> {
                    require(v.unit !is Currency && u.unit !is Currency) { "A measure is not money: $value±$uncertainty" }
                    val vu: KiUnit = v.unit
                    val uu: KiUnit = u.unit
                    require(vu.dimension == uu.dimension) {
                        "A measure's value and uncertainty have one dimension, not ${vu.dimensionName} and ${uu.dimensionName}"
                    }
                    val un: Number = if (uu == vu) u.value else difference(dec(u.value), uu, vu)
                    val t = common(v.value, un)
                    v = Quantity(cast(v.value, t), vu)
                    u = Quantity(cast(un, t), vu)
                }
                else -> throw IllegalArgumentException("A measure is a number or a quantity with an uncertainty, not $value±$uncertainty")
            }
            val ua = amount(u)
            require(finite(amount(v))) { "A measure's value is a finite number, not ${Ki.format(amount(v))}" }
            require(finite(ua)) { "An uncertainty is a finite number, not ${Ki.format(ua)}" }
            require(dec(ua).signum() >= 0) { "An uncertainty is not negative: ${Ki.format(ua)}" }
            return Measure(v, u)
        }

        /** Reads the literal `0.0042±0.0006µM`, `5cm±1mm`, `0.512±0.008` or `0.512+-0.008` (KD's form; see [Measure]). */
        @JvmStatic
        fun parse(text: String): Measure {
            val t = text.trim()
            var i = t.indexOf(PLUS_MINUS)
            var width = 1
            if (i < 0) {
                i = t.indexOf("+-", 1)
                width = 2
            }
            if (i <= 0 || i + width >= t.length) throw ParseException("A measure is written value±uncertainty: $text")
            return try {
                val left = t.substring(0, i).trim()
                val right = t.substring(i + width).trim()
                val u = side(right)
                // The unit is written once, after the uncertainty: `5±0.1cm`, `5±1cm:i` read 5 as the
                // quantity `5cm`, `5cm:i`, by the quantity's own number rules (Dec unless suffixed).
                val v: Any = if (u is Quantity<*> && plainNumber(left) != null) {
                    val suffix = Regex(":[A-Za-z]$").find(right)?.value ?: ""
                    Quantity<KiUnit>(left + u.unit.symbol + suffix)
                } else side(left)
                of(v, u)
            } catch (e: ParseException) {
                throw e
            } catch (e: Exception) {
                throw ParseException("Invalid measure $text: ${e.message}", cause = e)
            }
        }

        override fun parseLiteral(text: String): Measure = parse(text)

        /** One side of a literal: a number with KD's suffixes (`5`, `0.5`, `5L`, `0.5f`, `0.5d`, `0.5bd`) or a quantity. */
        private fun side(s: String): Any = plainNumber(s) ?: Quantity<KiUnit>(s)

        private fun plainNumber(s: String): Number? {
            val c = s.replace("_", "")
            if (c.isEmpty() || !(c[0].isDigit() || (c.length > 1 && c[0] in "+-" && (c[1].isDigit() || c[1] == '.')) || c[0] == '.')) return null
            return try {
                when {
                    c.endsWith("bd") || c.endsWith("BD") -> Dec(c.dropLast(2))
                    c.endsWith("L") -> c.dropLast(1).toLong()
                    c.endsWith("f") || c.endsWith("F") -> c.dropLast(1).toFloat()
                    c.endsWith("d") || c.endsWith("D") -> c.dropLast(1).toDouble()
                    c.all { it.isDigit() || it in "+-" } -> c.toLong().let { if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else it }
                    c.all { it.isDigit() || it in "+-.eE" } -> c.toDouble()
                    else -> null
                }
            } catch (e: NumberFormatException) {
                null
            }
        }

        /**
         * [a] op [b] for `+`, `-`, `*` or `/`, either side a [Measure] or an exact number
         * or quantity (at least one a measure), with first-order propagation of
         * independent uncertainties (see [Measure]).
         */
        @JvmStatic
        fun combine(op: Char, a: Any, b: Any): Measure {
            val x = a as? Measure ?: exact(a)
            val y = b as? Measure ?: exact(b)
            val target = resultType(listOf(x.value, x.uncertainty, y.value, y.uncertainty))
            val q: Any
            val terms: List<Any>
            when (op) {
                '+', '-' -> {
                    q = if (op == '+') add(x.value, y.value) else subtract(x.value, y.value)
                    terms = listOf(x.uncertainty, y.uncertainty)
                }
                '*' -> {
                    q = multiply(x.value, y.value)
                    terms = listOf(multiply(abs(y.value), x.uncertainty), multiply(abs(x.value), y.uncertainty))
                }
                '/' -> {
                    q = divide(x.value, y.value)
                    // σ² = (σx / y)² + (q · σy / y)²
                    terms = listOf(divide(x.uncertainty, abs(y.value)), multiply(abs(q), divide(y.uncertainty, abs(y.value))))
                }
                else -> throw IllegalArgumentException("A measure takes +, -, * or /, not $op")
            }
            return build(q, quadrature(terms, q), target)
        }

        /**
         * The sum of measures (exact numbers and quantities may be among them), its
         * uncertainty the root of the sum of the squares; null for an empty list.
         * Quantities are summed in their common unit when they share one, otherwise in
         * the dimension's base unit, as KQ aggregates quantities (RevDB design 7.5).
         *
         * @throws IllegalArgumentException for numbers mixed with quantities, or
         *   quantities of different dimensions
         */
        @JvmStatic
        fun sum(items: List<Any>): Measure? {
            if (items.isEmpty()) return null
            val xs = inOneUnit(items)
            var total: Any = xs[0].let { it as? Measure ?: exact(it) }
            for (i in 1 until xs.size) total = combine('+', total, xs[i])
            return total as Measure
        }

        /** The mean of measures: [sum] divided by their count, so its uncertainty is √(Σσ²)/n. Null for an empty list. */
        @JvmStatic
        fun mean(items: List<Any>): Measure? = sum(items)?.let { combine('/', it, items.size) }

        /**
         * The inverse-variance weighted mean of measures of one quantity:
         * `Σ(xᵢ/σᵢ²) / Σ(1/σᵢ²)` with uncertainty `1/√Σ(1/σᵢ²)`, the best estimate from
         * independent measurements of different precision. Null for an empty list; one
         * measure is returned as it is. Units as [sum] has them.
         *
         * @throws IllegalArgumentException when a measure has no uncertainty (its weight
         *   would be infinite), or for the mixes [sum] refuses
         */
        @JvmStatic
        fun weightedMean(items: List<Measure>): Measure? {
            if (items.isEmpty()) return null
            if (items.size == 1) return items[0]
            val xs = inOneUnit(items).map { it as Measure }
            var sw = Dec.ZERO
            var swx = Dec.ZERO
            for (m in xs) {
                val s = dec(amount(m.uncertainty))
                require(s.signum() > 0) { "A weighted mean needs every uncertainty above zero, not $m" }
                val w = Dec.ONE.divide(s.multiply(s), MC)
                sw = sw.add(w)
                swx = swx.add(w.multiply(dec(amount(m.value))))
            }
            val mean = swx.divide(sw, MC)
            val sigma = Dec.ONE.divide(sw.sqrt(MC), MC)
            val q: Any = (xs[0].value as? Quantity<*>)?.let { Quantity(mean, it.unit) } ?: mean
            return build(q, sigma, resultType(xs.flatMap { listOf(it.value, it.uncertainty) }))
        }

        /** [items] (measures, numbers, quantities) in one unit: their common unit, else the dimension's base unit. */
        private fun inOneUnit(items: List<Any>): List<Any> {
            val units: List<KiUnit?> = items.map { x ->
                when (x) {
                    is Measure -> x.unit
                    is Quantity<*> -> x.unit
                    is Number -> null
                    else -> throw IllegalArgumentException("A measure combines with numbers and quantities, not ${x::class.simpleName}")
                }
            }
            if (units.all { it == null }) return items
            require(units.none { it == null }) { "Cannot combine plain numbers with quantities" }
            val first = units[0]!!
            if (units.all { it == first }) return items
            require(units.all { it!!.dimension == first.dimension } && first !is Currency) {
                "Cannot combine quantities of different dimensions (${units.map { it!!.symbol }.distinct().joinToString()})"
            }
            val base = first.baseUnit
            @Suppress("UNCHECKED_CAST")
            return items.map { x -> if (x is Measure) x.to(base) else (x as Quantity<KiUnit>).convertTo(base) }
        }

        // ------------------------------------------------------------------ internals

        private fun exact(x: Any): Measure = when (x) {
            is Number -> Measure(x, zeroLike(x))
            is Quantity<*> -> Measure(x, Quantity(zeroLike(x.value), x.unit))
            else -> throw IllegalArgumentException("A measure combines with numbers and quantities, not ${x::class.simpleName}")
        }

        private enum class NumType { INT, LONG, FLOAT, DOUBLE, DEC }

        private fun typeOf(n: Number): NumType = when (n) {
            is Int, is Short, is Byte -> NumType.INT
            is Long -> NumType.LONG
            is Float -> NumType.FLOAT
            is Dec -> NumType.DEC
            else -> NumType.DOUBLE
        }

        private fun common(a: Number, b: Number): NumType = maxOf(typeOf(a), typeOf(b))

        /** Arithmetic results: Dec when a Dec takes part, Float when every number is a Float, else Double. */
        private fun resultType(xs: List<Any>): NumType {
            val types = xs.map { typeOf(amount(it)) }
            return when {
                NumType.DEC in types -> NumType.DEC
                types.all { it == NumType.FLOAT } -> NumType.FLOAT
                else -> NumType.DOUBLE
            }
        }

        private fun cast(n: Number, t: NumType): Number = when (t) {
            NumType.INT -> n.toInt()
            NumType.LONG -> n.toLong()
            NumType.FLOAT -> n.toFloat()
            NumType.DOUBLE -> if (n is Dec) n.toDouble() else n.toDouble()
            NumType.DEC -> dec(n)
        }

        private fun zeroLike(n: Number): Number = when (typeOf(n)) {
            NumType.INT -> 0
            NumType.LONG -> 0L
            NumType.FLOAT -> 0f
            NumType.DOUBLE -> 0.0
            NumType.DEC -> Dec.ZERO
        }

        /** The decimal view of a number: a Double as written (0.1, not its binary expansion). */
        private fun dec(n: Number): Dec = when (n) {
            is Dec -> n
            is Int, is Long, is Short, is Byte -> Dec.valueOf(n.toLong())
            else -> Dec(n.toString())
        }

        private fun finite(n: Number): Boolean = when (n) {
            is Double -> n.isFinite()
            is Float -> n.isFinite()
            else -> true
        }

        private fun amount(x: Any): Number = when (x) {
            is Number -> x
            is Quantity<*> -> x.value
            else -> throw IllegalArgumentException("Not a number or quantity: $x")
        }

        /**
         * An amount of difference in [from], expressed in [to]: a reading's conversion less
         * the conversion of zero, so a temperature's offset cancels (0.5°C of uncertainty is
         * 0.5K and 0.9°F, never 273.65K).
         */
        private fun difference(d: Dec, from: KiUnit, to: KiUnit): Dec =
            from.convertValue(d, to).subtract(from.convertValue(Dec.ZERO, to))

        private fun compareAmounts(a: Any, b: Any): Int = when {
            a is Number && b is Number -> {
                if (!finite(a) || !finite(b)) a.toDouble().compareTo(b.toDouble()) else dec(a).compareTo(dec(b))
            }
            a is Quantity<*> && b is Quantity<*> -> {
                @Suppress("UNCHECKED_CAST")
                (a as Quantity<KiUnit>).compareTo(b as Quantity<KiUnit>)
            }
            else -> throw IllegalArgumentException("Cannot compare a measure of ${kind(a)} with one of ${kind(b)}")
        }

        /**
         * Two uncertainties compared as amounts of difference: a temperature's offset
         * does not apply (0.5°C and 0.9°F are the same uncertainty), unlike [compareAmounts],
         * which compares readings.
         */
        private fun compareDifferences(a: Any, b: Any): Int = when {
            a is Quantity<*> && b is Quantity<*> -> {
                val au: KiUnit = a.unit
                val bu: KiUnit = b.unit
                require(au.dimension == bu.dimension) { "Cannot compare a measure of ${au.dimensionName} with one of ${bu.dimensionName}" }
                val bd = if (au == bu) dec(b.value) else difference(dec(b.value), bu, au)
                dec(a.value).compareTo(bd)
            }
            else -> compareAmounts(a, b)
        }

        private fun kind(x: Any): String = if (x is Quantity<*>) (x.unit as KiUnit).dimensionName else "a number"

        private fun negate(x: Any): Any = when (x) {
            is Quantity<*> -> -x
            is Dec -> x.negate()
            is Double -> -x
            is Float -> -x
            is Long -> -x
            is Int -> -x
            is Number -> -x.toDouble()
            else -> throw IllegalArgumentException("Not a number or quantity: $x")
        }

        private fun abs(x: Any): Any = when (x) {
            is Quantity<*> -> x.abs()
            is Number -> dec(x).abs()
            else -> throw IllegalArgumentException("Not a number or quantity: $x")
        }

        @Suppress("UNCHECKED_CAST")
        private fun add(a: Any, b: Any): Any = when {
            a is Number && b is Number -> dec(a).add(dec(b))
            a is Quantity<*> && b is Quantity<*> -> (a as Quantity<KiUnit>) + (b as Quantity<KiUnit>)
            else -> throw IllegalArgumentException("Cannot add ${kind(a)} and ${kind(b)}")
        }

        private fun subtract(a: Any, b: Any): Any = add(a, negate(b))

        private fun multiply(a: Any, b: Any): Any = when {
            a is Number && b is Number -> dec(a).multiply(dec(b))
            a is Quantity<*> && b is Number -> Quantity(dec(a.value).multiply(dec(b)), a.unit)
            a is Number && b is Quantity<*> -> Quantity(dec(a).multiply(dec(b.value)), b.unit)
            a is Quantity<*> && b is Quantity<*> -> ratioAsNumber(UnitAlgebra.multiply(a, b))
            else -> throw IllegalArgumentException("Cannot multiply ${kind(a)} and ${kind(b)}")
        }

        private fun divide(a: Any, b: Any): Any = when {
            a is Number && b is Number -> dec(a).divide(dec(b), MC)
            a is Quantity<*> && b is Number -> Quantity(dec(a.value).divide(dec(b), MC), a.unit)
            a is Number && b is Quantity<*> -> ratioAsNumber(UnitAlgebra.divide(Quantity(dec(a), KiUnit.ratio), b))
            a is Quantity<*> && b is Quantity<*> -> ratioAsNumber(UnitAlgebra.divide(a, b))
            else -> throw IllegalArgumentException("Cannot divide ${kind(a)} by ${kind(b)}")
        }

        /** A quotient of one dimension is a plain number (as KQ makes it, design 7.5), not a ratio quantity. */
        private fun ratioAsNumber(q: Quantity<*>): Any = if (q.unit == KiUnit.ratio) dec(q.value) else q

        /** √(Σ t²), each term expressed in the result's unit (a difference, so by factor only). */
        private fun quadrature(terms: List<Any>, result: Any): Dec {
            var sum = Dec.ZERO
            for (t in terms) {
                val d: Dec = when {
                    result is Quantity<*> && t is Quantity<*> -> {
                        val ru: KiUnit = result.unit
                        val tu: KiUnit = t.unit
                        require(ru.dimension == tu.dimension) { "Uncertainty in ${tu.dimensionName} for a value in ${ru.dimensionName}" }
                        if (tu == ru) dec(t.value) else difference(dec(t.value), tu, ru)
                    }
                    result is Quantity<*> && t is Number -> dec(t)        // a zero uncertainty of an exact operand
                    t is Number -> dec(t)
                    t is Quantity<*> -> dec(t.value)
                    else -> throw IllegalArgumentException("Not an uncertainty: $t")
                }
                sum = sum.add(d.multiply(d))
            }
            return sum.sqrt(MC)
        }

        private fun build(q: Any, sigma: Dec, t: NumType): Measure = when (q) {
            is Quantity<*> -> {
                require(q.unit !is Currency) { "A measure is not money: $q" }
                Measure(Quantity(cast(q.value, t), q.unit), Quantity(cast(sigma, t), q.unit))
            }
            is Number -> Measure(cast(q, t), cast(sigma, t))
            else -> throw IllegalArgumentException("Not a number or quantity: $q")
        }

        /**
         * A number as a quantity's amount is written: Dec plain with trailing zeros stripped,
         * a Double or Float in plain notation (`0.00001`, not `1.0E-5`, which a quantity
         * literal cannot hold; it reads back as the same Double or Float).
         */
        private fun plain(n: Number): String = when {
            n is Dec -> n.stripTrailingZeros().toPlainString()
            (n is Double || n is Float) && n.toString().contains('E') -> Dec(n.toString()).stripTrailingZeros().toPlainString()
            else -> n.toString()
        }

        /** Equal components: a quantity as [Quantity.equals] has it, a Dec by value (1.5 and 1.50), other numbers by type and value. */
        private fun same(a: Any, b: Any): Boolean =
            if (a is Dec && b is Dec) a.compareTo(b) == 0 else a == b

        private fun hash(x: Any): Int = if (x is Dec) x.stripTrailingZeros().hashCode() else x.hashCode()

        private fun typeSuffix(n: Number): String = when (n) {
            is Long -> ":L"
            is Double -> ":d"
            is Float -> ":f"
            is Int -> ":i"
            else -> ""
        }
    }
}
