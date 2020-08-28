package io.kixi.uom

import io.kixi.isWhole
import io.kixi.text.isKiIDStart
import java.lang.NumberFormatException
import java.math.BigDecimal

/**
 * A quantity is an amount of a given unit. The default value types are Int for integer
 * types and BigDecimal for decimal numbers. These can be overridden with :L, :d
 * and :f.
 *
 * ```
 *   5cm
 *   23.5kg
 *   235cm3:L // 235 cubic centimeters represented as a Long
 * ````
 */
class Quantity : Comparable<Quantity> {

    val value: Number
    val unit: Unit

    constructor(value:Number, unit:Unit) {
        this.value = value
        this.unit = unit
    }

    /**
     * Create a quantity from a string formatted as an integer or decimal number
     * followed by a unit symbol suffix.
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

        val unit = Unit.getUnit(symbol)

        if(unit == null)
            throw NoSuchUnitException(symbol)

        val numText = text.substring(0, symbolIndex).replace("_", "")

        val numValue: Number = when(numTypeChar) {
            'd','D' -> numText.toDouble()
            'L' -> numText.toLong()
            'f', 'F' -> numText.toFloat()
            // Default (no num type specified)
            '\u0000' -> if(numText.contains('.')) BigDecimal(numText) else
                numText.toInt()
            else -> throw NumberFormatException("'$numTypeChar' is not a valid number type specifier in a Quantity")
        }

        this.value = numValue
        this.unit = unit
    }

    override fun toString(): String {

        // Do nothing with Int or Decimal types. They are the defaults.
        val numType = when(value) {
            is Long -> ":L"
            is Double -> ":d"
            is Float -> ":f"
            else -> ""
        }

        val valueText = if(value is BigDecimal) value.stripTrailingZeros().toPlainString()
            else value.toString()

        return "$valueText$unit$numType"
    }

    infix fun convertTo(otherUnit: Unit): Quantity {
        val multiplier = unit.factorTo(otherUnit)
        return when (value) {
            is BigDecimal -> Quantity(value * multiplier, otherUnit)
            is Int -> {
                if (multiplier.isWhole) Quantity(value * multiplier.toInt(), otherUnit)
                else Quantity(BigDecimal(value.toString()) * multiplier, otherUnit)
            }
            is Long -> {
                if (multiplier.isWhole) Quantity(value * multiplier.toLong(), otherUnit)
                else Quantity(BigDecimal(value.toString()) * multiplier, otherUnit)
            }
            is Double -> Quantity(value * multiplier.toDouble(), otherUnit)
            is Float -> Quantity(value * multiplier.toFloat(), otherUnit)
            else -> throw Error("Unknown Number type in Quantity: ${value.javaClass.simpleName}")
        }
    }

    /**
     * This does not consider 1cm and 10mm to be equal, because quantities use units
     * rather than unit types. To check equivalence of the quantity use
     * equivalent(Quantity)
     */
    override fun equals(other: Any?): Boolean {
        if(other !is Quantity || other.unit!=unit || value::class != other.value::class)
            return false

        return toString().equals(other.toString())
    }

    fun equivalent(other: Quantity): Boolean {
        return this convertTo this.unit.type.baseUnit ==
                other convertTo other.unit.type.baseUnit
    }

    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + unit.hashCode()
        return result
    }

    override fun compareTo(other: Quantity): Int {
        if(other.unit.type != unit.type)
            throw IncompatibleUnitsException(unit, other.unit)

        var quantity1 = this
        var quantity2 = other

        if(unit == other.unit) {
            // no unit conversion needed
        } else if (this.unit.factor < other.unit.factor) {
            quantity2 = other convertTo this.unit
        } else {
            quantity1 = this convertTo other.unit
        }

        val value1 = quantity1.value
        val value2 = quantity2.value

        return when (value1) {
            // This looks odd, but the "is" check performs an implicit cast
            is Int -> when(value2) {
                is Int -> value1.compareTo(value2)
                is BigDecimal -> value1.toBigDecimal().compareTo(value2)
                is Long -> value1.compareTo(value2)
                is Double -> value1.compareTo(value2)
                is Float -> value1.compareTo(value2)
                else -> value1.compareTo(value2.toInt())
            }
            is BigDecimal -> when(value2) {
                is Int -> value1.compareTo(value2.toBigDecimal())
                is BigDecimal -> value1.compareTo(value2)
                is Long -> value1.compareTo(value2.toBigDecimal())
                is Double -> value1.compareTo(value2.toBigDecimal())
                is Float -> value1.compareTo(value2.toBigDecimal())
                else -> value1.compareTo(value2.toInt().toBigDecimal())
            }
            is Long -> when(value2) {
                is Int -> value1.compareTo(value2)
                is BigDecimal -> value1.toBigDecimal().compareTo(value2)
                is Long -> value1.compareTo(value2)
                is Double -> value1.compareTo(value2)
                is Float -> value1.compareTo(value2)
                else -> value1.compareTo(value2.toInt())
            }
            is Double -> when(value2) {
                is Int -> value1.compareTo(value2)
                is BigDecimal -> value1.toBigDecimal().compareTo(value2)
                is Long -> value1.compareTo(value2)
                is Double -> value1.compareTo(value2)
                is Float -> value1.compareTo(value2)
                else -> value1.compareTo(value2.toInt())
            }
            is Float -> when(value2) {
                is Int -> value1.compareTo(value2)
                is BigDecimal -> value1.toBigDecimal().compareTo(value2)
                is Long -> value1.compareTo(value2)
                is Double -> value1.compareTo(value2)
                is Float -> value1.compareTo(value2)
                else -> value1.compareTo(value2.toInt())
            }
            else -> when(value2) {
                is Int -> value1.toInt().compareTo(value2)
                is BigDecimal -> value1.toInt().toBigDecimal().compareTo(value2)
                is Long -> value1.toInt().compareTo(value2)
                is Double -> value1.toInt().compareTo(value2)
                is Float -> value1.toInt().compareTo(value2)
                else -> value1.toInt().compareTo(value2.toInt())
            }
        }
    }

    // Operators ////

    // Int operand
    operator fun plus(operand: Int): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value + operand, unit)
        is BigDecimal -> Quantity(value + operand.toBigDecimal(), unit)
        is Long -> Quantity(value + operand, unit)
        is Double -> Quantity(value + operand, unit)
        is Float -> Quantity(value + operand, unit)
        else -> Quantity(value.toInt() + operand, unit) // Byte & Short
    }

    operator fun minus(operand: Int): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value - operand, unit)
        is BigDecimal -> Quantity(value - operand.toBigDecimal(), unit)
        is Long -> Quantity(value - operand, unit)
        is Double -> Quantity(value - operand, unit)
        is Float -> Quantity(value - operand, unit)
        else -> Quantity(value.toInt() - operand, unit)
    }

    operator fun times(operand: Int): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value * operand, unit)
        is BigDecimal -> Quantity(value * operand.toBigDecimal(), unit)
        is Long -> Quantity(value * operand, unit)
        is Double -> Quantity(value * operand, unit)
        is Float -> Quantity(value * operand, unit)
        else -> Quantity(value.toInt() * operand, unit)
    }

    operator fun div(operand: Int): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value / operand, unit)
        is BigDecimal -> Quantity(value / operand.toBigDecimal(), unit)
        is Long -> Quantity(value / operand, unit)
        is Double -> Quantity(value / operand, unit)
        is Float -> Quantity(value / operand, unit)
        else -> Quantity(value.toInt() / operand, unit)
    }

    operator fun rem(operand: Int): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value % operand, unit)
        is BigDecimal -> Quantity(value % operand.toBigDecimal(), unit)
        is Long -> Quantity(value % operand, unit)
        is Double -> Quantity(value % operand, unit)
        is Float -> Quantity(value % operand, unit)
        else -> Quantity(value.toInt() % operand, unit)
    }

    // Long operand
    operator fun plus(operand: Long): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value + operand, unit)
        is BigDecimal -> Quantity(value + operand.toBigDecimal(), unit)
        is Long -> Quantity(value + operand, unit)
        is Double -> Quantity(value + operand, unit)
        is Float -> Quantity(value + operand, unit)
        else -> Quantity(value.toInt() + operand, unit) // Byte & Short
    }

    operator fun minus(operand: Long): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value - operand, unit)
        is BigDecimal -> Quantity(value - operand.toBigDecimal(), unit)
        is Long -> Quantity(value - operand, unit)
        is Double -> Quantity(value - operand, unit)
        is Float -> Quantity(value - operand, unit)
        else -> Quantity(value.toInt() - operand, unit)
    }

    operator fun times(operand: Long): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value * operand, unit)
        is BigDecimal -> Quantity(value * operand.toBigDecimal(), unit)
        is Long -> Quantity(value * operand, unit)
        is Double -> Quantity(value * operand, unit)
        is Float -> Quantity(value * operand, unit)
        else -> Quantity(value.toInt() * operand, unit)
    }

    operator fun div(operand: Long): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value / operand, unit)
        is BigDecimal -> Quantity(value / operand.toBigDecimal(), unit)
        is Long -> Quantity(value / operand, unit)
        is Double -> Quantity(value / operand, unit)
        is Float -> Quantity(value / operand, unit)
        else -> Quantity(value.toInt() / operand, unit)
    }

    operator fun rem(operand: Long): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value % operand, unit)
        is BigDecimal -> Quantity(value % operand.toBigDecimal(), unit)
        is Long -> Quantity(value % operand, unit)
        is Double -> Quantity(value % operand, unit)
        is Float -> Quantity(value % operand, unit)
        else -> Quantity(value.toInt() % operand, unit)
    }

    // Float operand
    operator fun plus(operand: Float): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value + operand, unit)
        is BigDecimal -> Quantity(value + operand.toBigDecimal(), unit)
        is Long -> Quantity(value + operand, unit)
        is Double -> Quantity(value + operand, unit)
        is Float -> Quantity(value + operand, unit)
        else -> Quantity(value.toInt() + operand, unit) // Byte & Short
    }

    operator fun minus(operand: Float): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value - operand, unit)
        is BigDecimal -> Quantity(value - operand.toBigDecimal(), unit)
        is Long -> Quantity(value - operand, unit)
        is Double -> Quantity(value - operand, unit)
        is Float -> Quantity(value - operand, unit)
        else -> Quantity(value.toInt() - operand, unit)
    }

    operator fun times(operand: Float): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value * operand, unit)
        is BigDecimal -> Quantity(value * operand.toBigDecimal(), unit)
        is Long -> Quantity(value * operand, unit)
        is Double -> Quantity(value * operand, unit)
        is Float -> Quantity(value * operand, unit)
        else -> Quantity(value.toInt() * operand, unit)
    }

    operator fun div(operand: Float): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value / operand, unit)
        is BigDecimal -> Quantity(value / operand.toBigDecimal(), unit)
        is Long -> Quantity(value / operand, unit)
        is Double -> Quantity(value / operand, unit)
        is Float -> Quantity(value / operand, unit)
        else -> Quantity(value.toInt() / operand, unit)
    }

    operator fun rem(operand: Float): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value % operand, unit)
        is BigDecimal -> Quantity(value % operand.toBigDecimal(), unit)
        is Long -> Quantity(value % operand, unit)
        is Double -> Quantity(value % operand, unit)
        is Float -> Quantity(value % operand, unit)
        else -> Quantity(value.toInt() % operand, unit)
    }

    // Double operand
    operator fun plus(operand: Double): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value + operand, unit)
        is BigDecimal -> Quantity(value + operand.toBigDecimal(), unit)
        is Long -> Quantity(value + operand, unit)
        is Double -> Quantity(value + operand, unit)
        is Float -> Quantity(value + operand, unit)
        else -> Quantity(value.toInt() + operand, unit) // Byte & Short
    }

    operator fun minus(operand: Double): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value - operand, unit)
        is BigDecimal -> Quantity(value - operand.toBigDecimal(), unit)
        is Long -> Quantity(value - operand, unit)
        is Double -> Quantity(value - operand, unit)
        is Float -> Quantity(value - operand, unit)
        else -> Quantity(value.toInt() - operand, unit)
    }

    operator fun times(operand: Double): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value * operand, unit)
        is BigDecimal -> Quantity(value * operand.toBigDecimal(), unit)
        is Long -> Quantity(value * operand, unit)
        is Double -> Quantity(value * operand, unit)
        is Float -> Quantity(value * operand, unit)
        else -> Quantity(value.toInt() * operand, unit)
    }

    operator fun div(operand: Double): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value / operand, unit)
        is BigDecimal -> Quantity(value / operand.toBigDecimal(), unit)
        is Long -> Quantity(value / operand, unit)
        is Double -> Quantity(value / operand, unit)
        is Float -> Quantity(value / operand, unit)
        else -> Quantity(value.toInt() / operand, unit)
    }

    operator fun rem(operand: Double): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value % operand, unit)
        is BigDecimal -> Quantity(value % operand.toBigDecimal(), unit)
        is Long -> Quantity(value % operand, unit)
        is Double -> Quantity(value % operand, unit)
        is Float -> Quantity(value % operand, unit)
        else -> Quantity(value.toInt() % operand, unit)
    }

    // BigDecimal operand
    operator fun plus(operand: BigDecimal): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value.toBigDecimal() + operand, unit)
        is BigDecimal -> Quantity(value + operand, unit)
        is Long -> Quantity(value.toBigDecimal() + operand, unit)
        is Double -> Quantity(value.toBigDecimal() + operand, unit)
        is Float -> Quantity(value.toBigDecimal() + operand, unit)
        else -> Quantity(value.toInt().toBigDecimal() + operand, unit) // Byte & Short
    }

    operator fun minus(operand: BigDecimal): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value.toBigDecimal() - operand, unit)
        is BigDecimal -> Quantity(value - operand, unit)
        is Long -> Quantity(value.toBigDecimal() - operand, unit)
        is Double -> Quantity(value.toBigDecimal() - operand, unit)
        is Float -> Quantity(value.toBigDecimal() - operand, unit)
        else -> Quantity(value.toInt().toBigDecimal() - operand, unit)
    }

    operator fun times(operand: BigDecimal): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value.toBigDecimal() * operand, unit)
        is BigDecimal -> Quantity(value * operand, unit)
        is Long -> Quantity(value.toBigDecimal() * operand, unit)
        is Double -> Quantity(value.toBigDecimal() * operand, unit)
        is Float -> Quantity(value.toBigDecimal() * operand, unit)
        else -> Quantity(value.toInt().toBigDecimal() * operand, unit)
    }

    operator fun div(operand: BigDecimal): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value.toBigDecimal() / operand, unit)
        is BigDecimal -> Quantity(value / operand, unit)
        is Long -> Quantity(value.toBigDecimal() / operand, unit)
        is Double -> Quantity(value.toBigDecimal() / operand, unit)
        is Float -> Quantity(value.toBigDecimal() / operand, unit)
        else -> Quantity(value.toInt().toBigDecimal() / operand, unit)
    }

    operator fun rem(operand: BigDecimal): Quantity = when(value) {
        // This looks odd, but the "is" check performs an implicit cast
        is Int -> Quantity(value.toBigDecimal() % operand, unit)
        is BigDecimal -> Quantity(value % operand, unit)
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
    operator fun plus(operand: Quantity): Quantity {
        if(operand.unit.type!= unit.type)
            throw IncompatibleUnitsException(unit, operand.unit)

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
            is BigDecimal -> quantity1 + value2
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
    operator fun minus(operand: Quantity): Quantity {
        if(operand.unit.type!= unit.type)
            throw IncompatibleUnitsException(unit, operand.unit)

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
            is BigDecimal -> quantity1 - value2
            is Long -> quantity1 - value2
            is Double -> quantity1 - value2
            is Float -> quantity1 - value2
            else -> quantity1 - value2.toInt()
        }
    }
}
