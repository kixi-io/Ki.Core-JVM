package io.kixi

import io.kixi.text.ParseException
import java.math.MathContext
import java.math.BigDecimal as Dec

/**
 * One row of numbers of one type, stored packed: a spectrum's absorbances, a
 * time course, a fingerprint's counts (Ki type bags design, TB7). The literal
 * uses the grid's brace syntax, its numbers unsuffixed because the type is
 * declared: `.vec<Float> { 1.0 2.5 3.1 }`, `.vec<Int> { 3 1 4 1 5 }`, `.vec<Dec> { }`.
 *
 * The element type is `Int`, `Long`, `Float`, `Double` or `Dec`, and every element
 * is finite. A vector is a value, not a list: it is immutable, two vectors are
 * equal when their element types and numbers are (Dec numbers by value, so `1.5`
 * and `1.50` are the same element), and the order ([compareTo]) is element by
 * element on the numbers' decimal values, a shorter vector before a longer one
 * that starts with it. [toList] gives the numbers as a list.
 */
class Vector<T : Number> private constructor(
    /** The element type: [Type.Int], [Type.Long], [Type.Float], [Type.Double] or [Type.Dec]. */
    val elementType: Type,
    private val ints: IntArray?,
    private val longs: LongArray?,
    private val floats: FloatArray?,
    private val doubles: DoubleArray?,
    private val decs: Array<Dec>?,
) : Comparable<Vector<*>> {

    val size: Int = ints?.size ?: longs?.size ?: floats?.size ?: doubles?.size ?: decs!!.size

    fun isEmpty(): Boolean = size == 0

    @Suppress("UNCHECKED_CAST")
    operator fun get(index: Int): T = when {
        ints != null -> ints[index]
        longs != null -> longs[index]
        floats != null -> floats[index]
        doubles != null -> doubles[index]
        else -> decs!![index]
    } as T

    /** The numbers, as a new list. */
    fun toList(): List<T> = List(size) { get(it) }

    /** A copy as doubles (Dec rounded to the nearest double). */
    fun toDoubleArray(): DoubleArray = DoubleArray(size) { get(it).toDouble() }

    /** The decimal view of element [index] (a Double as it prints: 0.1, not its binary expansion). */
    fun decAt(index: Int): Dec = dec(get(index))

    /** The sum: an Int vector's is a Long (as KQ sums Ints, design 7.5), else the element type; null when empty. */
    fun sum(): Number? {
        if (isEmpty()) return null
        return when {
            ints != null -> ints.fold(0L) { a, x -> Math.addExact(a, x.toLong()) }
            longs != null -> longs.fold(0L) { a, x -> Math.addExact(a, x) }
            floats != null -> decSum().toFloat()
            doubles != null -> decSum().toDouble()
            else -> decSum()
        }
    }

    /** The mean: Dec for a Dec vector, else a Double; null when empty. */
    fun mean(): Number? {
        if (isEmpty()) return null
        val m = decSum().divide(Dec.valueOf(size.toLong()), MathContext.DECIMAL128)
        return if (decs != null) m else m.toDouble()
    }

    /** The smallest number, or null when empty. */
    fun min(): T? = if (isEmpty()) null else (0 until size).minWith { a, b -> decAt(a).compareTo(decAt(b)) }.let { get(it) }

    /** The largest number, or null when empty. */
    fun max(): T? = if (isEmpty()) null else (0 until size).maxWith { a, b -> decAt(a).compareTo(decAt(b)) }.let { get(it) }

    /**
     * The dot product, in the wider element type of the two (an Int vector's as a Long).
     *
     * @throws IllegalArgumentException when the sizes differ
     */
    fun dot(other: Vector<*>): Number {
        requireSameSize(other, "dot")
        var s = Dec.ZERO
        for (i in 0 until size) s = s.add(decAt(i).multiply(other.decAt(i)))
        return typed(s, wider(elementType, other.elementType), sum = true)
    }

    /** The Euclidean length, √(Σ x²), as a Double (Dec for a Dec vector). */
    fun norm(): Number {
        var s = Dec.ZERO
        for (i in 0 until size) s = s.add(decAt(i).pow(2))
        val r = s.sqrt(MathContext.DECIMAL128)
        return if (decs != null) r else r.toDouble()
    }

    /** Element by element, in the wider element type. @throws IllegalArgumentException when the sizes differ */
    operator fun plus(other: Vector<*>): Vector<Number> = zip(other, "+") { a, b -> a.add(b) }

    /** Element by element, in the wider element type. @throws IllegalArgumentException when the sizes differ */
    operator fun minus(other: Vector<*>): Vector<Number> = zip(other, "-") { a, b -> a.subtract(b) }

    /** Each element times [k], in the wider of the element type and [k]'s type. */
    operator fun times(k: Number): Vector<Number> = scale(k) { it.multiply(dec(k)) }

    /** Each element divided by [k]: Dec for Dec, else Double (a Float vector by a Float stays Float). */
    operator fun div(k: Number): Vector<Number> {
        val t = when (val w = wider(elementType, typeOfNumber(k))) {
            Type.Dec, Type.Float -> w
            else -> Type.Double
        }
        return build(t, size) { decAt(it).divide(dec(k), MathContext.DECIMAL128) }
    }

    operator fun unaryMinus(): Vector<Number> = build(elementType, size) { decAt(it).negate() }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vector<*> || other.elementType != elementType || other.size != size) return false
        return when {
            ints != null -> ints.contentEquals(other.ints)
            longs != null -> longs.contentEquals(other.longs)
            floats != null -> floats.contentEquals(other.floats)
            doubles != null -> doubles.contentEquals(other.doubles)
            else -> (0 until size).all { decs!![it].compareTo(other.decs!![it]) == 0 }
        }
    }

    override fun hashCode(): Int = elementType.hashCode() * 31 + when {
        ints != null -> ints.contentHashCode()
        longs != null -> longs.contentHashCode()
        floats != null -> floats.contentHashCode()
        doubles != null -> doubles.contentHashCode()
        else -> decs!!.fold(1) { h, d -> h * 31 + d.stripTrailingZeros().hashCode() }
    }

    /** Element by element on decimal values, then by size (a prefix first). Element types are not compared. */
    override fun compareTo(other: Vector<*>): Int {
        for (i in 0 until minOf(size, other.size)) {
            val c = decAt(i).compareTo(other.decAt(i))
            if (c != 0) return c
        }
        return size.compareTo(other.size)
    }

    /** The KD literal: `.vec<Float> { 1.0 2.5 3.1 }`, `.vec<Int> { }`. */
    override fun toString(): String {
        val b = StringBuilder(".vec<").append(elementType.name).append("> {")
        for (i in 0 until size) b.append(' ').append(text(get(i)))
        return b.append(" }").toString()
    }

    // ------------------------------------------------------------------ internals

    private fun decSum(): Dec {
        var s = Dec.ZERO
        for (i in 0 until size) s = s.add(decAt(i))
        return s
    }

    private fun requireSameSize(other: Vector<*>, op: String) =
        require(other.size == size) { "Vectors of $size and ${other.size} numbers cannot be combined ($op)" }

    private inline fun zip(other: Vector<*>, op: String, f: (Dec, Dec) -> Dec): Vector<Number> {
        requireSameSize(other, op)
        return build(wider(elementType, other.elementType), size) { f(decAt(it), other.decAt(it)) }
    }

    private inline fun scale(k: Number, f: (Dec) -> Dec): Vector<Number> =
        build(wider(elementType, typeOfNumber(k)), size) { f(decAt(it)) }

    companion object : Parseable<Vector<*>> {

        /** The element types a vector can have. */
        @JvmField
        val ELEMENT_TYPES: Set<Type> = setOf(Type.Int, Type.Long, Type.Float, Type.Double, Type.Dec)

        @JvmStatic fun ofInts(vararg xs: Int): Vector<Int> = Vector(Type.Int, xs.copyOf(), null, null, null, null)
        @JvmStatic fun ofLongs(vararg xs: Long): Vector<Long> = Vector(Type.Long, null, xs.copyOf(), null, null, null)
        @JvmStatic fun ofFloats(vararg xs: Float): Vector<Float> = checkFinite(Vector(Type.Float, null, null, xs.copyOf(), null, null))
        @JvmStatic fun ofDoubles(vararg xs: Double): Vector<Double> = checkFinite(Vector(Type.Double, null, null, null, xs.copyOf(), null))
        @JvmStatic fun ofDecs(vararg xs: Dec): Vector<Dec> = Vector(Type.Dec, null, null, null, null, arrayOf(*xs))

        /**
         * A vector of [type] holding [numbers], each converted to the type exactly: a
         * whole number for Int and Long (in range), any number for Float, Double and Dec
         * (a Float or Double as the decimal it prints as).
         *
         * @throws IllegalArgumentException for another element type, a number that does
         *   not fit, or a non-finite number
         */
        @JvmStatic
        fun of(type: Type, numbers: List<Number>): Vector<Number> {
            require(type in ELEMENT_TYPES) { "A vector holds Int, Long, Float, Double or Dec numbers, not $type" }
            return try {
                build(type, numbers.size) { dec(numbers[it]) }
            } catch (e: ArithmeticException) {
                throw IllegalArgumentException("A number does not fit in .vec<${type.name}>: ${e.message}", e)
            }
        }

        /** The vector of Ki type [type] (by name: `"Float"`) holding [numbers]; see [of]. */
        @JvmStatic
        fun of(type: String, numbers: List<Number>): Vector<Number> =
            of(Type.forName(type) ?: throw IllegalArgumentException("Unknown type $type"), numbers)

        /** True for text that starts a vector literal (`.vec<`). */
        @JvmStatic
        fun isLiteral(text: String): Boolean = text.trimStart().startsWith(".vec<")

        /**
         * Reads `.vec<T> { n n n }`: the numbers separated by whitespace (newlines and
         * `#` / `//` comments to the end of a line are allowed), each unsuffixed or with
         * its type's own suffix (`L`, `f`, `d`, `bd`), `_` digit separators allowed.
         */
        @JvmStatic
        fun parse(text: String): Vector<*> {
            val t = text.trim()
            if (!t.startsWith(".vec<")) throw ParseException("A vector literal starts with .vec<T>: $text")
            val close = t.indexOf('>')
            if (close < 0) throw ParseException("Missing > in $text")
            val typeName = t.substring(5, close).trim()
            val type = Type.forName(typeName)?.takeIf { it in ELEMENT_TYPES }
                ?: throw ParseException("A vector's type is Int, Long, Float, Double or Dec, not '$typeName'")
            val rest = t.substring(close + 1).trim()
            if (!rest.startsWith("{") || !rest.endsWith("}")) throw ParseException("A vector's numbers are in braces: $text")
            val body = rest.substring(1, rest.length - 1)
                .lines().joinToString("\n") { line -> stripComment(line) }
            val words = body.split(Regex("\\s+")).filter { it.isNotEmpty() }
            val numbers = words.map { w -> element(w, type) ?: throw ParseException("'$w' is not a ${type.name} in $text") }
            return try {
                of(type, numbers)
            } catch (e: IllegalArgumentException) {
                throw ParseException(e.message ?: "Invalid vector $text", cause = e)
            }
        }

        override fun parseLiteral(text: String): Vector<*> = parse(text)

        // ------------------------------------------------------------------ internals

        private fun stripComment(line: String): String {
            val i = listOf(line.indexOf('#'), line.indexOf("//")).filter { it >= 0 }.minOrNull() ?: return line
            return line.substring(0, i)
        }

        /** One element, read as [type]: unsuffixed, or with that type's suffix. */
        private fun element(word: String, type: Type): Number? {
            val w = word.replace("_", "")
            val (body, suffixType) = when {
                w.endsWith("bd") || w.endsWith("BD") -> w.dropLast(2) to Type.Dec
                w.endsWith("L") -> w.dropLast(1) to Type.Long
                w.endsWith("f") || w.endsWith("F") -> w.dropLast(1) to Type.Float
                w.endsWith("d") || w.endsWith("D") -> w.dropLast(1) to Type.Double
                else -> w to null
            }
            if (suffixType != null && suffixType != type) return null
            if (body.isEmpty() || !body.all { it.isDigit() || it in "+-.eE" }) return null
            return try {
                when (type) {
                    Type.Int -> body.toInt()
                    Type.Long -> body.toLong()
                    Type.Float -> body.toFloat()
                    Type.Double -> body.toDouble()
                    else -> Dec(body)
                }
            } catch (e: NumberFormatException) {
                null
            }
        }

        private fun <T : Number> checkFinite(v: Vector<T>): Vector<T> {
            for (i in 0 until v.size) {
                val x = v[i]
                require(!(x is Double && !x.isFinite()) && !(x is Float && !x.isFinite())) { "A vector's numbers are finite, not $x" }
            }
            return v
        }

        private fun typeOfNumber(n: Number): Type = when (n) {
            is Int, is Short, is Byte -> Type.Int
            is Long -> Type.Long
            is Float -> Type.Float
            is Dec -> Type.Dec
            else -> Type.Double
        }

        private val ORDER = listOf(Type.Int, Type.Long, Type.Float, Type.Double, Type.Dec)

        private fun wider(a: Type, b: Type): Type = if (ORDER.indexOf(a) >= ORDER.indexOf(b)) a else b

        private fun dec(n: Number): Dec = when (n) {
            is Dec -> n
            is Int, is Long, is Short, is Byte -> Dec.valueOf(n.toLong())
            is Double -> {
                require(n.isFinite()) { "A vector's numbers are finite, not $n" }
                Dec(n.toString())
            }
            is Float -> {
                require(n.isFinite()) { "A vector's numbers are finite, not $n" }
                Dec(n.toString())
            }
            else -> Dec(n.toString())
        }

        /** A sum or dot product in [t]: Int widens to Long, as KQ sums Ints. */
        private fun typed(d: Dec, t: Type, sum: Boolean): Number = when (t) {
            Type.Int -> if (sum) d.longValueExact() else d.intValueExact()
            Type.Long -> d.longValueExact()
            Type.Float -> d.toFloat()
            Type.Double -> d.toDouble()
            else -> d
        }

        /** A vector of [type] from decimal values; Int and Long must be whole and in range. */
        private inline fun build(type: Type, n: Int, f: (Int) -> Dec): Vector<Number> {
            @Suppress("UNCHECKED_CAST")
            val v = when (type) {
                Type.Int -> Vector<Int>(type, IntArray(n) { f(it).intValueExact() }, null, null, null, null)
                Type.Long -> Vector<Long>(type, null, LongArray(n) { f(it).longValueExact() }, null, null, null)
                Type.Float -> checkFinite(Vector<Float>(type, null, null, FloatArray(n) { f(it).toFloat() }, null, null))
                Type.Double -> checkFinite(Vector<Double>(type, null, null, null, DoubleArray(n) { f(it).toDouble() }, null))
                Type.Dec -> Vector<Dec>(type, null, null, null, null, Array(n) { f(it) })
                else -> throw IllegalArgumentException("A vector holds Int, Long, Float, Double or Dec numbers, not $type")
            }
            return v as Vector<Number>
        }

        private fun text(n: Number): String = when (n) {
            is Dec -> n.stripTrailingZeros().toPlainString()
            else -> n.toString()
        }
    }
}
