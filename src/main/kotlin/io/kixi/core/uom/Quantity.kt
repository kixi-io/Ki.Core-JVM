package io.kixi.core.uom

import io.kixi.core.text.isKiIDStart
import io.kixi.core.whole
import java.math.BigDecimal as Dec

/**
 * A quantity is an amount of a given unit. The default value types are Int for integer
 * types and Dec for decimal numbers. These can be overridden with `:L`, `:d`
 * and `:f`. The Quantity class provides compile-time safety for quantities of units.
 *
 * ```
 *   5cm
 *   23.5kg
 *   235cm3:L // 235 cubic centimeters represented as a Long
 *
 *   // Using Quantities in Kotlin expressions
 *
 *   println(Quantity(2, Unit.cm) - Quantity(5, Unit.mm))
 *   value->  15mm
 * ````
 */
@Suppress("UNCHECKED_CAST", "unused")
class Quantity<T: Unit> : Comparable<Quantity<T>> {

    val value: Number
    val unit: T

    constructor(value:Number, unit:T) {
        this.value = value
        this.unit = unit
    }

    /**
     *  A convenience method for creating Decimal value quantities from a String.
     *
     * @param value Number
     * @param unit T
     * @constructor
     */
    constructor(value:String, unit:T) : this(Dec(value), unit)

    /**
     * Create a quantity from a string formatted as an integer or decimal number
     * followed by a unit symbol suffix and, optionally, a number type indicator
     * (d, L, f or I). Note: The default type for a Quantity is Dec.
     *
     * @param text String
     * @throws NumberFormatException If the quantity is malformed
     */
    constructor(text:String) {
        // TODO: fix - doesn't work for scientific notation

        if(text.isBlank())
            throw NumberFormatException("Quantity string cannot be empty.")

        var symbolIndex = 0
        for(i in 0..text.length) {
            if ((text[i].isKiIDStart() && text[i]!='_') || text[i] == 'ℓ') {
                symbolIndex = i
                break
            }
        }

        var symbol = text.substring(symbolIndex)
        val numTypeIndex = symbol.indexOf(':')
        var numTypeChar = '\u0000'

        if(numTypeIndex!=-1) {
            numTypeChar = symbol.last()
            symbol = symbol.substring(0, numTypeIndex)
        }

        val unit = Unit.getUnit(symbol) as T

        val numText = text.substring(0, symbolIndex).replace("_", "")

        val numValue: Number = when(numTypeChar) {
            'd','D' -> numText.toDouble()
            'L' -> numText.toLong()
            'f', 'F' -> numText.toFloat()
            'i' -> numText.toInt()

            // Default (no num type specified)
            '\u0000' -> Dec(numText)
            else -> throw NumberFormatException("'$numTypeChar' is not a valid number type specifier in a Quantity")
        }

        this.value = numValue
        this.unit = unit
    }

    override fun toString(): String {
        // Do nothing with Dec types. They are the default.
        val numType = when(value) {
            is Long -> ":L"
            is Double -> ":d"
            is Float -> ":f"
            is Int -> ":i"
            else -> ""
        }

        val valueText = if(value is Dec) value.stripTrailingZeros().toPlainString()
            else value.toString()

        return "$valueText$unit$numType"
    }

    infix fun convertTo(otherUnit: T): Quantity<T> {
        val multiplier = unit.factorTo(otherUnit)
        return when (value) {
            is Dec -> Quantity<T>(value * multiplier, otherUnit)
            is Int -> {
                if (multiplier.whole) Quantity<T>(value * multiplier.toInt(), otherUnit)
                else Quantity<T>(Dec(value.toString()) * multiplier, otherUnit)
            }
            is Long -> {
                if (multiplier.whole) Quantity<T>(value * multiplier.toLong(), otherUnit)
                else Quantity<T>(Dec(value.toString()) * multiplier, otherUnit)
            }
            is Double -> Quantity<T>(value * multiplier.toDouble(), otherUnit)
            is Float -> Quantity<T>(value * multiplier.toFloat(), otherUnit)
            else -> throw Error("Unknown Number type in Quantity: ${value.javaClass.simpleName}")
        }
    }

    /**
     * This does not consider 1cm and 10mm to be equal, because quantities use units
     * rather than unit axes. To check equivalence of the quantity use
     * equivalent(Quantity)
     */
    override fun equals(other: Any?): Boolean {
        if(other !is Quantity<*> || other.unit!=unit || value::class != other.value::class)
            return false

        return toString().equals(other.toString())
    }

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

        if(unit == other.unit) {
            // no unit conversion needed
        } else if (this.unit.factor < other.unit.factor) {
            quantity2 = other convertTo this.unit
        } else if (this.unit.factor > other.unit.factor) {
            quantity1 = this convertTo other.unit
        }

        val value1 = quantity1.value
        val value2 = quantity2.value

        return when (value1) {
            // This looks odd, but the "is" check performs an implicit cast
            is Int -> when(value2) {
                is Int -> value1.compareTo(value2)
                is Dec -> value1.toBigDecimal().compareTo(value2)
                is Long -> value1.compareTo(value2)
                is Double -> value1.compareTo(value2)
                is Float -> value1.compareTo(value2)
                else -> value1.compareTo(value2.toInt())
            }
            is Dec -> when(value2) {
                is Int -> value1.compareTo(value2.toBigDecimal())
                is Dec -> value1.compareTo(value2)
                is Long -> value1.compareTo(value2.toBigDecimal())
                is Double -> value1.compareTo(value2.toBigDecimal())
                is Float -> value1.compareTo(value2.toBigDecimal())
                else -> value1.compareTo(value2.toInt().toBigDecimal())
            }
            is Long -> when(value2) {
                is Int -> value1.compareTo(value2)
                is Dec -> value1.toBigDecimal().compareTo(value2)
                is Long -> value1.compareTo(value2)
                is Double -> value1.compareTo(value2)
                is Float -> value1.compareTo(value2)
                else -> value1.compareTo(value2.toInt())
            }
            is Double -> when(value2) {
                is Int -> value1.compareTo(value2)
                is Dec -> value1.toBigDecimal().compareTo(value2)
                is Long -> value1.compareTo(value2)
                is Double -> value1.compareTo(value2)
                is Float -> value1.compareTo(value2)
                else -> value1.compareTo(value2.toInt())
            }
            is Float -> when(value2) {
                is Int -> value1.compareTo(value2)
                is Dec -> value1.toBigDecimal().compareTo(value2)
                is Long -> value1.compareTo(value2)
                is Double -> value1.compareTo(value2)
                is Float -> value1.compareTo(value2)
                else -> value1.compareTo(value2.toInt())
            }
            else -> when(value2) {
                is Int -> value1.toInt().compareTo(value2)
                is Dec -> value1.toInt().toBigDecimal().compareTo(value2)
                is Long -> value1.toInt().compareTo(value2)
                is Double -> value1.toInt().compareTo(value2)
                is Float -> value1.toInt().compareTo(value2)
                else -> value1.toInt().compareTo(value2.toInt())
            }
        }
    }

    // Operators ////

    // Int operand
    operator fun plus(operand: Int): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value + operand, unit)
        is Dec -> Quantity(value + operand.toBigDecimal(), unit)
        is Long -> Quantity(value + operand, unit)
        is Double -> Quantity(value + operand, unit)
        is Float -> Quantity(value + operand, unit)
        else -> Quantity(value.toInt() + operand, unit) // Byte & Short
    }

    operator fun minus(operand: Int): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value - operand, unit)
        is Dec -> Quantity(value - operand.toBigDecimal(), unit)
        is Long -> Quantity(value - operand, unit)
        is Double -> Quantity(value - operand, unit)
        is Float -> Quantity(value - operand, unit)
        else -> Quantity(value.toInt() - operand, unit)
    }

    operator fun times(operand: Int): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value * operand, unit)
        is Dec -> Quantity(value * operand.toBigDecimal(), unit)
        is Long -> Quantity(value * operand, unit)
        is Double -> Quantity(value * operand, unit)
        is Float -> Quantity(value * operand, unit)
        else -> Quantity(value.toInt() * operand, unit)
    }

    operator fun div(operand: Int): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value / operand, unit)
        is Dec -> Quantity(value / operand.toBigDecimal(), unit)
        is Long -> Quantity(value / operand, unit)
        is Double -> Quantity(value / operand, unit)
        is Float -> Quantity(value / operand, unit)
        else -> Quantity(value.toInt() / operand, unit)
    }

    operator fun rem(operand: Int): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value % operand, unit)
        is Dec -> Quantity(value % operand.toBigDecimal(), unit)
        is Long -> Quantity(value % operand, unit)
        is Double -> Quantity(value % operand, unit)
        is Float -> Quantity(value % operand, unit)
        else -> Quantity(value.toInt() % operand, unit)
    }

    // Long operand
    operator fun plus(operand: Long): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value + operand, unit)
        is Dec -> Quantity(value + operand.toBigDecimal(), unit)
        is Long -> Quantity(value + operand, unit)
        is Double -> Quantity(value + operand, unit)
        is Float -> Quantity(value + operand, unit)
        else -> Quantity(value.toInt() + operand, unit) // Byte & Short
    }

    operator fun minus(operand: Long): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value - operand, unit)
        is Dec -> Quantity(value - operand.toBigDecimal(), unit)
        is Long -> Quantity(value - operand, unit)
        is Double -> Quantity(value - operand, unit)
        is Float -> Quantity(value - operand, unit)
        else -> Quantity(value.toInt() - operand, unit)
    }

    operator fun times(operand: Long): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value * operand, unit)
        is Dec -> Quantity(value * operand.toBigDecimal(), unit)
        is Long -> Quantity(value * operand, unit)
        is Double -> Quantity(value * operand, unit)
        is Float -> Quantity(value * operand, unit)
        else -> Quantity(value.toInt() * operand, unit)
    }

    operator fun div(operand: Long): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value / operand, unit)
        is Dec -> Quantity(value / operand.toBigDecimal(), unit)
        is Long -> Quantity(value / operand, unit)
        is Double -> Quantity(value / operand, unit)
        is Float -> Quantity(value / operand, unit)
        else -> Quantity(value.toInt() / operand, unit)
    }

    operator fun rem(operand: Long): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value % operand, unit)
        is Dec -> Quantity(value % operand.toBigDecimal(), unit)
        is Long -> Quantity(value % operand, unit)
        is Double -> Quantity(value % operand, unit)
        is Float -> Quantity(value % operand, unit)
        else -> Quantity(value.toInt() % operand, unit)
    }

    // Float operand
    operator fun plus(operand: Float): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value + operand, unit)
        is Dec -> Quantity(value + operand.toBigDecimal(), unit)
        is Long -> Quantity(value + operand, unit)
        is Double -> Quantity(value + operand, unit)
        is Float -> Quantity(value + operand, unit)
        else -> Quantity(value.toInt() + operand, unit) // Byte & Short
    }

    operator fun minus(operand: Float): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value - operand, unit)
        is Dec -> Quantity(value - operand.toBigDecimal(), unit)
        is Long -> Quantity(value - operand, unit)
        is Double -> Quantity(value - operand, unit)
        is Float -> Quantity(value - operand, unit)
        else -> Quantity(value.toInt() - operand, unit)
    }

    operator fun times(operand: Float): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value * operand, unit)
        is Dec -> Quantity(value * operand.toBigDecimal(), unit)
        is Long -> Quantity(value * operand, unit)
        is Double -> Quantity(value * operand, unit)
        is Float -> Quantity(value * operand, unit)
        else -> Quantity(value.toInt() * operand, unit)
    }

    operator fun div(operand: Float): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value / operand, unit)
        is Dec -> Quantity(value / operand.toBigDecimal(), unit)
        is Long -> Quantity(value / operand, unit)
        is Double -> Quantity(value / operand, unit)
        is Float -> Quantity(value / operand, unit)
        else -> Quantity(value.toInt() / operand, unit)
    }

    operator fun rem(operand: Float): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value % operand, unit)
        is Dec -> Quantity(value % operand.toBigDecimal(), unit)
        is Long -> Quantity(value % operand, unit)
        is Double -> Quantity(value % operand, unit)
        is Float -> Quantity(value % operand, unit)
        else -> Quantity(value.toInt() % operand, unit)
    }

    // Double operand
    operator fun plus(operand: Double): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value + operand, unit)
        is Dec -> Quantity(value + operand.toBigDecimal(), unit)
        is Long -> Quantity(value + operand, unit)
        is Double -> Quantity(value + operand, unit)
        is Float -> Quantity(value + operand, unit)
        else -> Quantity(value.toInt() + operand, unit) // Byte & Short
    }

    operator fun minus(operand: Double): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value - operand, unit)
        is Dec -> Quantity(value - operand.toBigDecimal(), unit)
        is Long -> Quantity(value - operand, unit)
        is Double -> Quantity(value - operand, unit)
        is Float -> Quantity(value - operand, unit)
        else -> Quantity(value.toInt() - operand, unit)
    }

    operator fun times(operand: Double): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value * operand, unit)
        is Dec -> Quantity(value * operand.toBigDecimal(), unit)
        is Long -> Quantity(value * operand, unit)
        is Double -> Quantity(value * operand, unit)
        is Float -> Quantity(value * operand, unit)
        else -> Quantity(value.toInt() * operand, unit)
    }

    operator fun div(operand: Double): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value / operand, unit)
        is Dec -> Quantity(value / operand.toBigDecimal(), unit)
        is Long -> Quantity(value / operand, unit)
        is Double -> Quantity(value / operand, unit)
        is Float -> Quantity(value / operand, unit)
        else -> Quantity(value.toInt() / operand, unit)
    }

    operator fun rem(operand: Double): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value % operand, unit)
        is Dec -> Quantity(value % operand.toBigDecimal(), unit)
        is Long -> Quantity(value % operand, unit)
        is Double -> Quantity(value % operand, unit)
        is Float -> Quantity(value % operand, unit)
        else -> Quantity(value.toInt() % operand, unit)
    }

    // Dec operand
    operator fun plus(operand: Dec): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value.toBigDecimal() + operand, unit)
        is Dec -> Quantity(value + operand, unit)
        is Long -> Quantity(value.toBigDecimal() + operand, unit)
        is Double -> Quantity(value.toBigDecimal() + operand, unit)
        is Float -> Quantity(value.toBigDecimal() + operand, unit)
        else -> Quantity(value.toInt().toBigDecimal() + operand, unit) // Byte & Short
    }

    operator fun minus(operand: Dec): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value.toBigDecimal() - operand, unit)
        is Dec -> Quantity(value - operand, unit)
        is Long -> Quantity(value.toBigDecimal() - operand, unit)
        is Double -> Quantity(value.toBigDecimal() - operand, unit)
        is Float -> Quantity(value.toBigDecimal() - operand, unit)
        else -> Quantity(value.toInt().toBigDecimal() - operand, unit)
    }

    operator fun times(operand: Dec): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value.toBigDecimal() * operand, unit)
        is Dec -> Quantity(value * operand, unit)
        is Long -> Quantity(value.toBigDecimal() * operand, unit)
        is Double -> Quantity(value.toBigDecimal() * operand, unit)
        is Float -> Quantity(value.toBigDecimal() * operand, unit)
        else -> Quantity(value.toInt().toBigDecimal() * operand, unit)
    }

    operator fun div(operand: Dec): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value.toBigDecimal() / operand, unit)
        is Dec -> Quantity(value / operand, unit)
        is Long -> Quantity(value.toBigDecimal() / operand, unit)
        is Double -> Quantity(value.toBigDecimal() / operand, unit)
        is Float -> Quantity(value.toBigDecimal() / operand, unit)
        else -> Quantity(value.toInt().toBigDecimal() / operand, unit)
    }

    operator fun rem(operand: Dec): Quantity<T> = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
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
     * units (e.g. 2cm + 3mm returns 23mm)
     *
     * @throws IncompatibleUnitsException if the units don't have the same Unit.Type
     */
    operator fun plus(operand: Quantity<T>): Quantity<T> {
        //if(operand.unit.type!= unit.type)
        //    throw IncompatibleUnitsException(unit, operand.unit)

        var quantity1 = this
        var quantity2 = operand

        if(unit == operand.unit) {
            // no unit conversion needed
        } else if (this.unit.factor < operand.unit.factor) {
            quantity2 = operand convertTo this.unit
        } else {
            quantity1 = this convertTo operand.unit
        }

        val value2 = quantity2.value

        return when (value2) {
            // This looks odd, but the "is" check performs an implicit cast
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
     * units (e.g. 2cm - 3mm returns 17mm)
     *
     * @throws IncompatibleUnitsException if the units don't have the same Unit.Type
     */
    operator fun minus(operand: Quantity<T>): Quantity<T> {
        //if(operand.unit.type!= unit.type)
        //    throw IncompatibleUnitsException(unit, operand.unit)

        var quantity1 = this
        var quantity2 = operand

        if(unit == operand.unit) {
            // no unit conversion needed
        } else if (this.unit.factor < operand.unit.factor) {
            quantity2 = operand convertTo this.unit
        } else {
            quantity1 = this convertTo operand.unit
        }

        val value2 = quantity2.value
        return when (value2) {
            // This looks odd, but the "is" check performs an implicit cast
            is Int -> quantity1 - value2
            is Dec -> quantity1 - value2
            is Long -> quantity1 - value2
            is Double -> quantity1 - value2
            is Float -> quantity1 - value2
            else -> quantity1 - value2.toInt()
        }
    }

    companion object {
        fun length(text:String) = Quantity<Length>(text)
        fun area(text:String) = Quantity<Area>(text)
        fun volume(text:String) = Quantity<Volume>(text)
        fun mass(text:String) = Quantity<Mass>(text)
        fun temperature(text:String) = Quantity<Temperature>(text)
        fun speed(text:String) = Quantity<Speed>(text)

        /**
         * @param text String Quantity type to parse
         * @return Quantity<*>
         * @throws NumberFormatException if the number portion is malformed
         * @throws NoSuchUnitException if the unit specified does not exist
         */
        fun parse(text:String): Quantity<*> {
            if(text.isBlank())
                throw NumberFormatException("Quantity string cannot be empty.")

            var symbolIndex = 0
            for(i in 0..text.length) {
                if ((text[i].isKiIDStart() && text[i]!='_') || text[i] == 'ℓ' || text[i] == '°') {
                    symbolIndex = i
                    break
                }
            }

            var symbol = text.substring(symbolIndex)
            val numTypeIndex = symbol.indexOf(':')
            var numTypeChar = '\u0000'

            if(numTypeIndex!=-1) {
                numTypeChar = symbol.last()
                symbol = symbol.substring(0, numTypeIndex)
            }

            val unit = Unit.getUnit(symbol)
            if(unit==null)
                throw NoSuchUnitException("Unit $symbol in $text does not exist.")

            val numText = text.substring(0, symbolIndex).replace("_", "")

            val numValue: Number = when(numTypeChar) {
                'd','D' -> numText.toDouble()
                'L' -> numText.toLong()
                'f', 'F' -> numText.toFloat()
                'i' -> numText.toInt()
                // Default (no num type specified)
                '\u0000' -> Dec(numText)
                else -> throw NumberFormatException("'$numTypeChar' is not a valid number type specifier in a Quantity")
            }

            return when(unit) {

                // base units
                is Length -> Quantity<Length>(numValue, unit)
                is Mass -> Quantity<Mass>(numValue, unit)
                is SubstanceAmount -> Quantity<SubstanceAmount>(numValue, unit)
                is Luminosity -> Quantity<Luminosity>(numValue, unit)
                is Temperature -> Quantity<Temperature>(numValue, unit)
                is Current -> Quantity<Current>(numValue, unit)

                // common derived units
                is Area -> Quantity<Area>(numValue, unit)
                is Volume -> Quantity<Volume>(numValue, unit)
                is Speed -> Quantity<Speed>(numValue, unit)
                is Density -> Quantity<Density>(numValue, unit)

                else -> throw NoSuchUnitException(unit.symbol)
            }
        }
    }
}
