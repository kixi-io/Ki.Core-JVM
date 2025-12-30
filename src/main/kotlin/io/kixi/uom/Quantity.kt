package io.kixi.uom

import io.kixi.Parseable
import io.kixi.text.ParseException
import io.kixi.whole
import java.math.BigDecimal as Dec

/**
 * A Quantity is an amount of a given unit. The default value types are Int for integer
 * types and Dec for decimal numbers. These can be overridden with `:L`, `:d`
 * and `:f`. The Quantity class provides compile-time safety for quantities of units.
 *
 * ## Basic Syntax
 * ```
 *   5cm
 *   23.5kg
 *   235cmÃ‚Â³:L    // 235 cubic centimeters represented as a Long
 *   1_000_000m  // underscores for readability
 * ```
 *
 * ## Scientific Notation
 *
 * Ki supports two forms of scientific notation for quantities. Standard `+` and `-`
 * operators cannot be used in exponents because they conflict with arithmetic operators
 * in Ki Script (KS) and other expression contexts.
 *
 * ### Option 1: Parentheses Style
 * Use parentheses around the exponent to include a sign:
 * ```
 *   5.5e(8)km       // 5.5 Ãƒâ€” 10Ã¢ÂÂ¸ km (550 million km)
 *   5.5e(-7)m       // 5.5 Ãƒâ€” 10Ã¢ÂÂ»Ã¢ÂÂ· m (550 nanometers)
 *   9.109e(-31)kg   // 9.109 Ãƒâ€” 10Ã¢ÂÂ»Ã‚Â³Ã‚Â¹ kg (electron mass)
 *   6.022e(23)mol   // 6.022 Ãƒâ€” 10Ã‚Â²Ã‚Â³ mol (Avogadro's number)
 * ```
 *
 * ### Option 2: Letter Style (n/p)
 * Use `n` for negative exponent, `p` for positive (p is optional for positive):
 * ```
 *   5.5e8km         // 5.5 Ãƒâ€” 10Ã¢ÂÂ¸ km (positive, no letter needed)
 *   5.5ep8km        // 5.5 Ãƒâ€” 10Ã¢ÂÂ¸ km (explicit positive with 'p')
 *   5.5en7m         // 5.5 Ãƒâ€” 10Ã¢ÂÂ»Ã¢ÂÂ· m (negative with 'n')
 *   9.109en31kg     // 9.109 Ãƒâ€” 10Ã¢ÂÂ»Ã‚Â³Ã‚Â¹ kg
 * ```
 *
 * Both styles can be combined with type specifiers:
 * ```
 *   1.5e(8)km:d     // Double
 *   5.5en7m:f       // Float
 * ```
 *
 * ## Using Quantities in Kotlin Expressions
 * ```kotlin
 *   println(Quantity(2, Unit.cm) - Quantity(5, Unit.mm))
 *   // Output: 15mm
 *
 *   val distance = Quantity.parse("1.496e(8)km")  // Earth to Sun
 *   val inMeters = distance convertTo Unit.m
 * ```
 *
 * @author Daniel Leuck
 * @property value The numeric value of the quantity
 * @property unit The unit of measure
 */
@Suppress("UNCHECKED_CAST", "unused")
class Quantity<T : Unit> : Comparable<Quantity<T>> {

    val value: Number
    val unit: T

    constructor(value: Number, unit: T) {
        this.value = value
        this.unit = unit
    }

    /**
     * A convenience method for creating Decimal value quantities from a String.
     *
     * @param value The numeric value as a string
     * @param unit The unit of measure
     */
    constructor(value: String, unit: T) : this(Dec(value), unit)

    /**
     * Create a quantity from a string formatted as an integer or decimal number
     * (optionally in scientific notation) followed by a unit symbol suffix and,
     * optionally, a number type indicator (d, L, f or i).
     *
     * Scientific notation supports two forms:
     * - Parentheses: `5.5e(-7)m`, `1.5e(8)km`
     * - Letter style: `5.5en7m` (n=negative), `5.5ep8km` or `5.5e8km` (p=positive)
     *
     * @param text The quantity literal string
     * @throws NumberFormatException If the quantity is malformed
     * @throws NoSuchUnitException If the unit symbol is not recognized
     */
    constructor(text: String) {
        if (text.isBlank())
            throw NumberFormatException("Quantity string cannot be empty.")

        val result = parseQuantityText(text)

        this.value = result.first
        this.unit = result.second as T
    }

    override fun toString(): String {
        val numType = when (value) {
            is Long -> ":L"
            is Double -> ":d"
            is Float -> ":f"
            is Int -> ":i"
            else -> ""
        }

        val valueText = when (value) {
            is Dec -> {
                // Use toPlainString() to avoid scientific notation (e.g., 1E+1 for 10)
                // stripTrailingZeros() removes unnecessary decimal places (e.g., 10.00 -> 10)
                val stripped = value.stripTrailingZeros()
                stripped.toPlainString()
            }
            else -> value.toString()
        }

        return "$valueText$unit$numType"
    }

    /**
     * Converts this quantity to an equivalent quantity in the target unit.
     *
     * The numeric type is preserved when possible. If the conversion produces
     * a non-whole number and the value is Int or Long, the result will be promoted to Dec.
     *
     * For temperature conversions, this properly handles the offset between
     * Celsius and Kelvin (e.g., 0Ã‚Â°C = 273.15K).
     *
     * @param otherUnit The target unit (must be of the same dimension)
     * @return A new Quantity with the converted value and target unit
     * @throws IncompatibleUnitsException if the units have different dimensions
     */
    infix fun convertTo(otherUnit: T): Quantity<T> {
        val decValue = when (value) {
            is Dec -> value
            else -> Dec(value.toString())
        }

        val converted = unit.convertValue(decValue, otherUnit)

        return when (value) {
            is Dec -> Quantity(converted, otherUnit)
            is Int -> {
                if (converted.whole) Quantity(converted.toInt(), otherUnit)
                else Quantity(converted, otherUnit)
            }
            is Long -> {
                if (converted.whole) Quantity(converted.toLong(), otherUnit)
                else Quantity(converted, otherUnit)
            }
            is Double -> Quantity(converted.toDouble(), otherUnit)
            is Float -> Quantity(converted.toFloat(), otherUnit)
            else -> throw Error("Unknown Number type in Quantity: ${value.javaClass.simpleName}")
        }
    }

    /**
     * This does not consider 1cm and 10mm to be equal, because quantities use units
     * rather than unit axes. To check equivalence of the quantity use
     * [equivalent].
     */
    override fun equals(other: Any?): Boolean {
        if (other !is Quantity<*> || other.unit != unit || value::class != other.value::class)
            return false

        return toString() == other.toString()
    }

    /**
     * Returns true if this quantity represents the same physical amount as the other,
     * even if they use different units (e.g., 1cm equivalent to 10mm).
     */
    fun equivalent(other: Quantity<T>): Boolean {
        return this convertTo this.unit.baseUnit as T ==
                other convertTo other.unit.baseUnit as T
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + unit.hashCode()
        return result
    }

    override fun compareTo(other: Quantity<T>): Int {
        var quantity1 = this
        var quantity2 = other

        if (unit == other.unit) {
            // no unit conversion needed
        } else if (this.unit.factor < other.unit.factor) {
            quantity2 = other convertTo this.unit
        } else if (this.unit.factor > other.unit.factor) {
            quantity1 = this convertTo other.unit
        }

        val value1 = quantity1.value
        val value2 = quantity2.value

        return compareNumbers(value1, value2)
    }

    /**
     * Returns the absolute value of this quantity.
     */
    fun abs(): Quantity<T> = when (value) {
        is Int -> Quantity(kotlin.math.abs(value), unit)
        is Long -> Quantity(kotlin.math.abs(value), unit)
        is Float -> Quantity(kotlin.math.abs(value), unit)
        is Double -> Quantity(kotlin.math.abs(value), unit)
        is Dec -> Quantity(value.abs(), unit)
        else -> this
    }

    /**
     * Returns true if this quantity's value is zero.
     */
    val isZero: Boolean
        get() = when (value) {
            is Int -> value == 0
            is Long -> value == 0L
            is Float -> value == 0.0f
            is Double -> value == 0.0
            is Dec -> value.signum() == 0
            else -> false
        }

    /**
     * Returns true if this quantity's value is positive (> 0).
     */
    val isPositive: Boolean
        get() = when (value) {
            is Int -> value > 0
            is Long -> value > 0L
            is Float -> value > 0.0f
            is Double -> value > 0.0
            is Dec -> value.signum() > 0
            else -> false
        }

    /**
     * Returns true if this quantity's value is negative (< 0).
     */
    val isNegative: Boolean
        get() = when (value) {
            is Int -> value < 0
            is Long -> value < 0L
            is Float -> value < 0.0f
            is Double -> value < 0.0
            is Dec -> value.signum() < 0
            else -> false
        }

    // Operators ////

    operator fun unaryMinus(): Quantity<T> = when (value) {
        is Int -> Quantity(-value, unit)
        is Dec -> Quantity(-value, unit)
        is Long -> Quantity(-value, unit)
        is Double -> Quantity(-value, unit)
        is Float -> Quantity(-value, unit)
        else -> Quantity(-value.toInt(), unit)
    }

    // Int operand
    operator fun plus(operand: Int): Quantity<T> = when (value) {
        is Int -> Quantity(value + operand, unit)
        is Dec -> Quantity(value + operand.toBigDecimal(), unit)
        is Long -> Quantity(value + operand, unit)
        is Double -> Quantity(value + operand, unit)
        is Float -> Quantity(value + operand, unit)
        else -> Quantity(value.toInt() + operand, unit)
    }

    operator fun minus(operand: Int): Quantity<T> = when (value) {
        is Int -> Quantity(value - operand, unit)
        is Dec -> Quantity(value - operand.toBigDecimal(), unit)
        is Long -> Quantity(value - operand, unit)
        is Double -> Quantity(value - operand, unit)
        is Float -> Quantity(value - operand, unit)
        else -> Quantity(value.toInt() - operand, unit)
    }

    operator fun times(operand: Int): Quantity<T> = when (value) {
        is Int -> Quantity(value * operand, unit)
        is Dec -> Quantity(value * operand.toBigDecimal(), unit)
        is Long -> Quantity(value * operand, unit)
        is Double -> Quantity(value * operand, unit)
        is Float -> Quantity(value * operand, unit)
        else -> Quantity(value.toInt() * operand, unit)
    }

    operator fun div(operand: Int): Quantity<T> = when (value) {
        is Int -> Quantity(value / operand, unit)
        is Dec -> Quantity(value / operand.toBigDecimal(), unit)
        is Long -> Quantity(value / operand, unit)
        is Double -> Quantity(value / operand, unit)
        is Float -> Quantity(value / operand, unit)
        else -> Quantity(value.toInt() / operand, unit)
    }

    operator fun rem(operand: Int): Quantity<T> = when (value) {
        is Int -> Quantity(value % operand, unit)
        is Dec -> Quantity(value % operand.toBigDecimal(), unit)
        is Long -> Quantity(value % operand, unit)
        is Double -> Quantity(value % operand, unit)
        is Float -> Quantity(value % operand, unit)
        else -> Quantity(value.toInt() % operand, unit)
    }

    // Long operand
    operator fun plus(operand: Long): Quantity<T> = when (value) {
        is Int -> Quantity(value + operand, unit)
        is Dec -> Quantity(value + operand.toBigDecimal(), unit)
        is Long -> Quantity(value + operand, unit)
        is Double -> Quantity(value + operand, unit)
        is Float -> Quantity(value + operand, unit)
        else -> Quantity(value.toInt() + operand, unit)
    }

    operator fun minus(operand: Long): Quantity<T> = when (value) {
        is Int -> Quantity(value - operand, unit)
        is Dec -> Quantity(value - operand.toBigDecimal(), unit)
        is Long -> Quantity(value - operand, unit)
        is Double -> Quantity(value - operand, unit)
        is Float -> Quantity(value - operand, unit)
        else -> Quantity(value.toInt() - operand, unit)
    }

    operator fun times(operand: Long): Quantity<T> = when (value) {
        is Int -> Quantity(value * operand, unit)
        is Dec -> Quantity(value * operand.toBigDecimal(), unit)
        is Long -> Quantity(value * operand, unit)
        is Double -> Quantity(value * operand, unit)
        is Float -> Quantity(value * operand, unit)
        else -> Quantity(value.toInt() * operand, unit)
    }

    operator fun div(operand: Long): Quantity<T> = when (value) {
        is Int -> Quantity(value / operand, unit)
        is Dec -> Quantity(value / operand.toBigDecimal(), unit)
        is Long -> Quantity(value / operand, unit)
        is Double -> Quantity(value / operand, unit)
        is Float -> Quantity(value / operand, unit)
        else -> Quantity(value.toInt() / operand, unit)
    }

    operator fun rem(operand: Long): Quantity<T> = when (value) {
        is Int -> Quantity(value % operand, unit)
        is Dec -> Quantity(value % operand.toBigDecimal(), unit)
        is Long -> Quantity(value % operand, unit)
        is Double -> Quantity(value % operand, unit)
        is Float -> Quantity(value % operand, unit)
        else -> Quantity(value.toInt() % operand, unit)
    }

    // Float operand
    operator fun plus(operand: Float): Quantity<T> = when (value) {
        is Int -> Quantity(value + operand, unit)
        is Dec -> Quantity(value + operand.toBigDecimal(), unit)
        is Long -> Quantity(value + operand, unit)
        is Double -> Quantity(value + operand, unit)
        is Float -> Quantity(value + operand, unit)
        else -> Quantity(value.toInt() + operand, unit)
    }

    operator fun minus(operand: Float): Quantity<T> = when (value) {
        is Int -> Quantity(value - operand, unit)
        is Dec -> Quantity(value - operand.toBigDecimal(), unit)
        is Long -> Quantity(value - operand, unit)
        is Double -> Quantity(value - operand, unit)
        is Float -> Quantity(value - operand, unit)
        else -> Quantity(value.toInt() - operand, unit)
    }

    operator fun times(operand: Float): Quantity<T> = when (value) {
        is Int -> Quantity(value * operand, unit)
        is Dec -> Quantity(value * operand.toBigDecimal(), unit)
        is Long -> Quantity(value * operand, unit)
        is Double -> Quantity(value * operand, unit)
        is Float -> Quantity(value * operand, unit)
        else -> Quantity(value.toInt() * operand, unit)
    }

    operator fun div(operand: Float): Quantity<T> = when (value) {
        is Int -> Quantity(value / operand, unit)
        is Dec -> Quantity(value / operand.toBigDecimal(), unit)
        is Long -> Quantity(value / operand, unit)
        is Double -> Quantity(value / operand, unit)
        is Float -> Quantity(value / operand, unit)
        else -> Quantity(value.toInt() / operand, unit)
    }

    operator fun rem(operand: Float): Quantity<T> = when (value) {
        is Int -> Quantity(value % operand, unit)
        is Dec -> Quantity(value % operand.toBigDecimal(), unit)
        is Long -> Quantity(value % operand, unit)
        is Double -> Quantity(value % operand, unit)
        is Float -> Quantity(value % operand, unit)
        else -> Quantity(value.toInt() % operand, unit)
    }

    // Double operand
    operator fun plus(operand: Double): Quantity<T> = when (value) {
        is Int -> Quantity(value + operand, unit)
        is Dec -> Quantity(value + operand.toBigDecimal(), unit)
        is Long -> Quantity(value + operand, unit)
        is Double -> Quantity(value + operand, unit)
        is Float -> Quantity(value + operand, unit)
        else -> Quantity(value.toInt() + operand, unit)
    }

    operator fun minus(operand: Double): Quantity<T> = when (value) {
        is Int -> Quantity(value - operand, unit)
        is Dec -> Quantity(value - operand.toBigDecimal(), unit)
        is Long -> Quantity(value - operand, unit)
        is Double -> Quantity(value - operand, unit)
        is Float -> Quantity(value - operand, unit)
        else -> Quantity(value.toInt() - operand, unit)
    }

    operator fun times(operand: Double): Quantity<T> = when (value) {
        is Int -> Quantity(value * operand, unit)
        is Dec -> Quantity(value * operand.toBigDecimal(), unit)
        is Long -> Quantity(value * operand, unit)
        is Double -> Quantity(value * operand, unit)
        is Float -> Quantity(value * operand, unit)
        else -> Quantity(value.toInt() * operand, unit)
    }

    operator fun div(operand: Double): Quantity<T> = when (value) {
        is Int -> Quantity(value / operand, unit)
        is Dec -> Quantity(value / operand.toBigDecimal(), unit)
        is Long -> Quantity(value / operand, unit)
        is Double -> Quantity(value / operand, unit)
        is Float -> Quantity(value / operand, unit)
        else -> Quantity(value.toInt() / operand, unit)
    }

    operator fun rem(operand: Double): Quantity<T> = when (value) {
        is Int -> Quantity(value % operand, unit)
        is Dec -> Quantity(value % operand.toBigDecimal(), unit)
        is Long -> Quantity(value % operand, unit)
        is Double -> Quantity(value % operand, unit)
        is Float -> Quantity(value % operand, unit)
        else -> Quantity(value.toInt() % operand, unit)
    }

    // Dec operand
    operator fun plus(operand: Dec): Quantity<T> = when (value) {
        is Int -> Quantity(value.toBigDecimal() + operand, unit)
        is Dec -> Quantity(value + operand, unit)
        is Long -> Quantity(value.toBigDecimal() + operand, unit)
        is Double -> Quantity(value.toBigDecimal() + operand, unit)
        is Float -> Quantity(value.toBigDecimal() + operand, unit)
        else -> Quantity(value.toInt().toBigDecimal() + operand, unit)
    }

    operator fun minus(operand: Dec): Quantity<T> = when (value) {
        is Int -> Quantity(value.toBigDecimal() - operand, unit)
        is Dec -> Quantity(value - operand, unit)
        is Long -> Quantity(value.toBigDecimal() - operand, unit)
        is Double -> Quantity(value.toBigDecimal() - operand, unit)
        is Float -> Quantity(value.toBigDecimal() - operand, unit)
        else -> Quantity(value.toInt().toBigDecimal() - operand, unit)
    }

    operator fun times(operand: Dec): Quantity<T> = when (value) {
        is Int -> Quantity(value.toBigDecimal() * operand, unit)
        is Dec -> Quantity(value * operand, unit)
        is Long -> Quantity(value.toBigDecimal() * operand, unit)
        is Double -> Quantity(value.toBigDecimal() * operand, unit)
        is Float -> Quantity(value.toBigDecimal() * operand, unit)
        else -> Quantity(value.toInt().toBigDecimal() * operand, unit)
    }

    operator fun div(operand: Dec): Quantity<T> = when (value) {
        is Int -> Quantity(value.toBigDecimal() / operand, unit)
        is Dec -> Quantity(value / operand, unit)
        is Long -> Quantity(value.toBigDecimal() / operand, unit)
        is Double -> Quantity(value.toBigDecimal() / operand, unit)
        is Float -> Quantity(value.toBigDecimal() / operand, unit)
        else -> Quantity(value.toInt().toBigDecimal() / operand, unit)
    }

    operator fun rem(operand: Dec): Quantity<T> = when (value) {
        is Int -> Quantity(value.toBigDecimal() % operand, unit)
        is Dec -> Quantity(value % operand, unit)
        is Long -> Quantity(value.toBigDecimal() % operand, unit)
        is Double -> Quantity(value.toBigDecimal() % operand, unit)
        is Float -> Quantity(value.toBigDecimal() % operand, unit)
        else -> Quantity(value.toInt().toBigDecimal() % operand, unit)
    }

    // Quantity operand

    /**
     * Adds the quantities. The returned quantity will use the smaller of the two
     * units (e.g. 2cm + 3mm returns 23mm).
     *
     * @throws IncompatibleUnitsException if the units don't have the same dimension
     */
    operator fun plus(operand: Quantity<T>): Quantity<T> {
        var quantity1 = this
        var quantity2 = operand

        if (unit == operand.unit) {
            // no unit conversion needed
        } else if (this.unit.factor < operand.unit.factor) {
            quantity2 = operand convertTo this.unit
        } else {
            quantity1 = this convertTo operand.unit
        }

        val value2 = quantity2.value

        return when (value2) {
            is Int -> quantity1 + value2
            is Dec -> quantity1 + value2
            is Long -> quantity1 + value2
            is Double -> quantity1 + value2
            is Float -> quantity1 + value2
            else -> quantity1 + value2.toInt()
        }
    }

    /**
     * Subtracts the operand. The returned quantity will use the smaller of the two
     * units (e.g. 2cm - 3mm returns 17mm).
     *
     * @throws IncompatibleUnitsException if the units don't have the same dimension
     */
    operator fun minus(operand: Quantity<T>): Quantity<T> {
        var quantity1 = this
        var quantity2 = operand

        if (unit == operand.unit) {
            // no unit conversion needed
        } else if (this.unit.factor < operand.unit.factor) {
            quantity2 = operand convertTo this.unit
        } else {
            quantity1 = this convertTo operand.unit
        }

        val value2 = quantity2.value
        return when (value2) {
            is Int -> quantity1 - value2
            is Dec -> quantity1 - value2
            is Long -> quantity1 - value2
            is Double -> quantity1 - value2
            is Float -> quantity1 - value2
            else -> quantity1 - value2.toInt()
        }
    }

    companion object : Parseable<Quantity<*>> {
        /**
         * Parses the numeric portion and unit from a quantity string.
         * Handles scientific notation in both parentheses and letter (n/p) styles.
         *
         * @return Pair of (Number, Unit)
         */
        private fun parseQuantityText(text: String): Pair<Number, Unit> {
            if (text.isBlank())
                throw NumberFormatException("Quantity string cannot be empty.")

            val cleaned = text.replace("_", "")
            val len = cleaned.length

            // Find where the unit symbol starts by parsing through the number portion
            var i = 0

            // Skip leading sign (for the mantissa)
            if (i < len && cleaned[i] == '-') i++

            // Skip digits before decimal
            while (i < len && cleaned[i].isDigit()) i++

            // Skip decimal point and digits after
            if (i < len && cleaned[i] == '.') {
                i++
                while (i < len && cleaned[i].isDigit()) i++
            }

            // Handle scientific notation
            if (i < len && (cleaned[i] == 'e' || cleaned[i] == 'E')) {
                val eIndex = i
                i++ // skip 'e' or 'E'

                if (i < len) {
                    when {
                        // Option 1: Parentheses style e(-7) or e(8)
                        cleaned[i] == '(' -> {
                            i++ // skip '('
                            if (i < len && (cleaned[i] == '-' || cleaned[i] == '+')) i++
                            while (i < len && cleaned[i].isDigit()) i++
                            if (i < len && cleaned[i] == ')') {
                                i++ // skip ')'
                            } else {
                                throw NumberFormatException(
                                    "Missing closing parenthesis in scientific notation: $text"
                                )
                            }
                        }
                        // Option 2: 'n' for negative exponent
                        cleaned[i] == 'n' -> {
                            i++ // skip 'n'
                            if (i >= len || !cleaned[i].isDigit()) {
                                // 'en' not followed by digit - backtrack, 'e' is part of unit
                                i = eIndex
                            } else {
                                while (i < len && cleaned[i].isDigit()) i++
                            }
                        }
                        // Option 2: 'p' for positive exponent
                        cleaned[i] == 'p' -> {
                            i++ // skip 'p'
                            if (i >= len || !cleaned[i].isDigit()) {
                                // 'ep' not followed by digit - backtrack
                                i = eIndex
                            } else {
                                while (i < len && cleaned[i].isDigit()) i++
                            }
                        }
                        // Standard: just digits (positive exponent)
                        cleaned[i].isDigit() -> {
                            while (i < len && cleaned[i].isDigit()) i++
                        }
                        // 'e' followed by something else - it's part of unit symbol
                        else -> {
                            i = eIndex // backtrack
                        }
                    }
                }
            }

            if (i == 0) {
                throw NumberFormatException("Invalid quantity format: $text")
            }

            val symbolIndex = i

            // Extract and parse symbol portion
            var symbol = cleaned.substring(symbolIndex)
            var numTypeChar = '\u0000'

            val numTypeIndex = symbol.indexOf(':')
            if (numTypeIndex != -1) {
                numTypeChar = symbol.last()
                symbol = symbol.substring(0, numTypeIndex)
            }

            // Get the unit
            val unit = Unit.getUnit(symbol)
                ?: throw NoSuchUnitException("Unit '$symbol' in '$text' does not exist.")

            // Extract and normalize the number portion
            val numText = normalizeScientificNotation(cleaned.substring(0, symbolIndex))

            // Parse the number with appropriate type
            val numValue: Number = when (numTypeChar) {
                'd', 'D' -> numText.toDouble()
                'L' -> {
                    // For scientific notation with Long, parse as Dec first then convert
                    Dec(numText).toLong()
                }
                'f', 'F' -> numText.toFloat()
                'i', 'I' -> {
                    // For scientific notation with Int, parse as Dec first then convert
                    Dec(numText).toInt()
                }
                '\u0000' -> Dec(numText) // Default
                else -> throw NumberFormatException(
                    "'$numTypeChar' is not a valid number type specifier in a Quantity"
                )
            }

            return Pair(numValue, unit)
        }

        /**
         * Normalizes Ki scientific notation to standard Java format.
         *
         * Converts:
         * - `5.5e(-7)` Ã¢â€ â€™ `5.5e-7`
         * - `5.5e(8)` Ã¢â€ â€™ `5.5e8`
         * - `5.5en7` Ã¢â€ â€™ `5.5e-7`
         * - `5.5ep8` Ã¢â€ â€™ `5.5e8`
         * - `5.5e8` Ã¢â€ â€™ `5.5e8` (unchanged)
         */
        private fun normalizeScientificNotation(text: String): String {
            // Handle parentheses style: e(-7) or e(8)
            val parenMatch = Regex("""[eE]\(([+-]?)(\d+)\)""").find(text)
            if (parenMatch != null) {
                val sign = if (parenMatch.groupValues[1] == "-") "-" else ""
                val exp = parenMatch.groupValues[2]
                return text.replace(parenMatch.value, "e$sign$exp")
            }

            // Handle letter style: en7 or ep8
            val letterMatch = Regex("""[eE]([np])(\d+)""").find(text)
            if (letterMatch != null) {
                val sign = if (letterMatch.groupValues[1] == "n") "-" else ""
                val exp = letterMatch.groupValues[2]
                return text.replace(letterMatch.value, "e$sign$exp")
            }

            // Already in standard form or not scientific notation
            return text
        }

        /**
         * Compare two Number values.
         */
        private fun compareNumbers(value1: Number, value2: Number): Int {
            return when (value1) {
                is Int -> when (value2) {
                    is Int -> value1.compareTo(value2)
                    is Dec -> value1.toBigDecimal().compareTo(value2)
                    is Long -> value1.toLong().compareTo(value2)
                    is Double -> value1.toDouble().compareTo(value2)
                    is Float -> value1.toFloat().compareTo(value2)
                    else -> value1.compareTo(value2.toInt())
                }
                is Dec -> when (value2) {
                    is Int -> value1.compareTo(value2.toBigDecimal())
                    is Dec -> value1.compareTo(value2)
                    is Long -> value1.compareTo(value2.toBigDecimal())
                    is Double -> value1.compareTo(value2.toBigDecimal())
                    is Float -> value1.compareTo(value2.toBigDecimal())
                    else -> value1.compareTo(value2.toInt().toBigDecimal())
                }
                is Long -> when (value2) {
                    is Int -> value1.compareTo(value2.toLong())
                    is Dec -> value1.toBigDecimal().compareTo(value2)
                    is Long -> value1.compareTo(value2)
                    is Double -> value1.toDouble().compareTo(value2)
                    is Float -> value1.toFloat().compareTo(value2)
                    else -> value1.compareTo(value2.toLong())
                }
                is Double -> when (value2) {
                    is Int -> value1.compareTo(value2.toDouble())
                    is Dec -> value1.toBigDecimal().compareTo(value2)
                    is Long -> value1.compareTo(value2.toDouble())
                    is Double -> value1.compareTo(value2)
                    is Float -> value1.compareTo(value2.toDouble())
                    else -> value1.compareTo(value2.toDouble())
                }
                is Float -> when (value2) {
                    is Int -> value1.compareTo(value2.toFloat())
                    is Dec -> value1.toBigDecimal().compareTo(value2)
                    is Long -> value1.toDouble().compareTo(value2.toDouble())
                    is Double -> value1.toDouble().compareTo(value2)
                    is Float -> value1.compareTo(value2)
                    else -> value1.compareTo(value2.toFloat())
                }
                else -> when (value2) {
                    is Int -> value1.toInt().compareTo(value2)
                    is Dec -> value1.toInt().toBigDecimal().compareTo(value2)
                    is Long -> value1.toLong().compareTo(value2)
                    is Double -> value1.toDouble().compareTo(value2)
                    is Float -> value1.toFloat().compareTo(value2)
                    else -> value1.toInt().compareTo(value2.toInt())
                }
            }
        }

        // Convenience factory methods for specific unit types

        /** Creates a Length quantity from a literal string (e.g., "5cm", "100m"). */
        fun length(text: String) = Quantity<Length>(text)

        /** Creates an Area quantity from a literal string (e.g., "25mÃ‚Â²", "100cm2"). */
        fun area(text: String) = Quantity<Area>(text)

        /** Creates a Volume quantity from a literal string (e.g., "500mÃ¢â€žâ€œ", "1mÃ‚Â³"). */
        fun volume(text: String) = Quantity<Volume>(text)

        /** Creates a Mass quantity from a literal string (e.g., "75kg", "500g"). */
        fun mass(text: String) = Quantity<Mass>(text)

        /** Creates a Temperature quantity from a literal string (e.g., "25Ã‚Â°C", "300K"). */
        fun temperature(text: String) = Quantity<Temperature>(text)

        /** Creates a Speed quantity from a literal string (e.g., "100kph", "30mps"). */
        fun speed(text: String) = Quantity<Speed>(text)

        /** Creates a Density quantity from a literal string (e.g., "1000kgpmÃ‚Â³"). */
        fun density(text: String) = Quantity<Density>(text)

        /** Creates a Time quantity from a literal string (e.g., "30s", "5min"). */
        fun time(text: String) = Quantity<Time>(text)

        /**
         * Parse a quantity string and return a Quantity with the appropriate unit type.
         *
         * Supports scientific notation:
         * - Parentheses style: `5.5e(-7)m`, `1.5e(8)km`
         * - Letter style: `5.5en7m`, `5.5ep8km`, `5.5e8km`
         *
         * @param text Quantity literal to parse
         * @return Quantity<*> with the parsed value and unit
         * @throws NumberFormatException if the number portion is malformed
         * @throws NoSuchUnitException if the unit specified does not exist
         */
        fun parse(text: String): Quantity<*> {
            val (numValue, unit) = parseQuantityText(text)

            return when (unit) {
                // Base units
                is Length -> Quantity<Length>(numValue, unit)
                is Mass -> Quantity<Mass>(numValue, unit)
                is SubstanceAmount -> Quantity<SubstanceAmount>(numValue, unit)
                is Luminosity -> Quantity<Luminosity>(numValue, unit)
                is Temperature -> Quantity<Temperature>(numValue, unit)
                is Current -> Quantity<Current>(numValue, unit)
                is Time -> Quantity<Time>(numValue, unit)
                is Dimensionless -> Quantity<Dimensionless>(numValue, unit)

                // Common derived units
                is Area -> Quantity<Area>(numValue, unit)
                is Volume -> Quantity<Volume>(numValue, unit)
                is Speed -> Quantity<Speed>(numValue, unit)
                is Density -> Quantity<Density>(numValue, unit)

                // Currency units
                is Currency -> Quantity<Currency>(numValue, unit)

                else -> throw NoSuchUnitException(unit.symbol)
            }
        }

        /**
         * Parses a Ki quantity literal string into a Quantity instance.
         *
         * This method wraps any parsing errors in a [ParseException] for
         * consistent error handling across the Ki ecosystem.
         *
         * @param text The Ki quantity literal string to parse
         * @return The parsed Quantity
         * @throws ParseException if the text cannot be parsed as a valid Quantity
         */
        override fun parseLiteral(text: String): Quantity<*> {
            return try {
                parse(text)
            } catch (e: NumberFormatException) {
                throw ParseException("Invalid quantity format: ${e.message}", cause = e)
            } catch (e: NoSuchUnitException) {
                throw ParseException("Unknown unit: ${e.message}", cause = e)
            }
        }

        /**
         * Try to parse a quantity string, returning null on failure.
         */
        fun parseOrNull(text: String): Quantity<*>? {
            return try {
                parse(text)
            } catch (e: Exception) {
                null
            }
        }
    }
}