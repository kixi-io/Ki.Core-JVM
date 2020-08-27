package io.kixi.uom

import io.kixi.isWhole
import io.kixi.text.isKiIDStart
import java.lang.NumberFormatException
import java.math.BigDecimal
import kotlin.reflect.typeOf

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
data class Quantity(val value:Number, val unit:Unit) {

    /**
     * TODO: override compareTo, convert to normal (non-data) class
     */

    // TODO: operators for basic arithmetic

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

    override fun equals(other: Any?): Boolean {
        if(other !is Quantity || other.unit!=unit || value::class != other.value::class)
            return false

        return toString().equals(other.toString())
    }

    companion object {
        /**
         * Create a quantity from a string formatted as an integer or decimal number
         * followed by a unit symbol suffix.
         *
         * @param text String
         * @throws NumberFormatException If the quantity is malformed
         */
        @JvmStatic
        fun parse(text:String): Quantity {
            // TODO: doesn't work for scientific notation
            // TODO: doesn't parse number type specifier after unit (e.g. 5.2cm:f)

            if(text.isBlank())
                throw NumberFormatException("Quantity string cannot be empty.")

            var symbolIndex = 0
            for(i in 0..text.length) {
                if (text[i].isKiIDStart()) {
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

            val numText = text.substring(0, symbolIndex)

            val numValue: Number = when(numTypeChar) {
                'd','D' -> numText.toDouble()
                'L' -> numText.toLong()
                'f', 'F' -> numText.toFloat()
                // Default (no num type specified)
                '\u0000' -> if(numText.contains('.')) BigDecimal(numText) else
                    Integer.parseInt(numText)
                else -> throw NumberFormatException("'$numTypeChar' is not a valid number type specifier in a Quantity")
            }

            return Quantity(numValue, unit)
        }
    }
}