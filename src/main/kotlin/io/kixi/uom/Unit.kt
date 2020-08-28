package io.kixi.uom

import java.math.BigDecimal
import java.math.MathContext
import java.util.TreeMap

/**
 * An SI unit of measure
 */
class Unit(val symbol: String, val type: Type, val factor: BigDecimal,
                val unicode: String = symbol)  {

    /**
     * An SI unit of measure type.
     */
    enum class Type(var baseSymbol:String) {
        // Base Unit Types
        LENGTH("m"),
        MASS("kg"),
        TIME("s"),
        TEMPERATURE("K"),
        SUBSTANCE_AMOUNT("mol"),
        CURRENT("A"), // electric current
        LUMINOSITY("cd"), // luminous intensity

        // Common Derived Unit Types
        AREA("m2"),
        VOLUME("m3"),
        DENSITY("kgpm3"), // volumetric mass density
        SPEED("mps"),
        ACCELERATION("mps2"),
        FORCE("N"),
        PRESSURE("Pa"),
        ENERGY("J"),
        POWER("W"),
        CHARGE("C"),
        POTENTIAL_DELTA("V"),
        RESISTANCE("G"),
        CONDUCTANCE("S"),
        CAPACITANCE("F");

        val baseUnit: Unit get() = getUnit(this.baseSymbol)!!
    }

    val description get() = "$symbol ${type.name.toLowerCase()} ${factor.toPlainString()}"

    override fun toString(): String = symbol
    override fun equals(other: Any?): Boolean = other != null && other is Unit &&
            other.symbol == symbol
    override fun hashCode(): Int = symbol.hashCode() or 31


    /**
     * The number by which you multiply to get a conversion to the target unit.
     * For example, `Unit.cm.getFactor(Unit.mm)` is `10`.
     *
     * @param target The unit to which we are converting
     * @throws IncompatibleUnitsException If the units do not have the same type.
     */
    fun factorTo(target:Unit) : BigDecimal {
        if(type != target.type)
            throw IncompatibleUnitsException(this, target)

        return factor.divide(target.factor, MathContext.DECIMAL128)
    }

    companion object {

        private val UNITS = TreeMap<String, Unit>()

        /* Base Units -------- */

        // Length ////
        val nm = addUnit(Unit("nm", Type.LENGTH, BigDecimal(".000000001")))
        val mm = addUnit(Unit("mm", Type.LENGTH, BigDecimal(".001")))
        val cm = addUnit(Unit("cm", Type.LENGTH, BigDecimal(".01")))
        val m = addUnit(Unit("m", Type.LENGTH, BigDecimal("1")))
        val km = addUnit(Unit("km", Type.LENGTH, BigDecimal("1000")))

        // Mass ////
        val ng = addUnit(Unit("ng", Type.MASS, BigDecimal(".000000001")))
        val mg = addUnit(Unit("mg", Type.MASS, BigDecimal(".001")))
        val cg = addUnit(Unit("cg", Type.MASS, BigDecimal(".01")))
        val g = addUnit(Unit("g", Type.MASS, BigDecimal("1")))
        val kg = addUnit(Unit("kg", Type.MASS, BigDecimal("1000")))

        // Time ////
        // This is handled by the Duration type. TODO: bridge SI time units w/ Durations

        // TODO: Base temperature, substance amount, current & luminosity units

        /* Common Derived Units -------- */

        // Area ////
        val nm2 = addUnit(Unit("nm²", Type.AREA, BigDecimal(".000000000000000001")))
        val mm2 = addUnit(Unit("mm²", Type.AREA, BigDecimal(".000001")))
        val cm2 = addUnit(Unit("cm²", Type.AREA, BigDecimal(".0001")))
        val m2 = addUnit(Unit("m²", Type.AREA, BigDecimal("1")))
        val km2 = addUnit(Unit("km²", Type.AREA, BigDecimal("1000000")))

        // Volume ////
        val nm3 = addUnit(Unit("nm³", Type.VOLUME, BigDecimal(".000000000000000000000000001")))
        val mm3 = addUnit(Unit("mm³", Type.VOLUME, BigDecimal(".000000001")))
        val cm3 = addUnit(Unit("cm³", Type.VOLUME, BigDecimal(".000001")))
        val m3 = addUnit(Unit("m³", Type.VOLUME, BigDecimal("1")))
        val km3 = addUnit(Unit("km³", Type.VOLUME, BigDecimal("1000000000")))

        /**
         * We have to use ℓ to avoid a conflict with L for Long integer literals. LT is accepted when parsing, but ℓ is
         * always used for output.
         */
        val L = addUnit(Unit("ℓ", Type.VOLUME, BigDecimal(".001")))
        /**
         * mL is also accepted when parsing, but ℓ is always used for output to be consistent with liter (ℓ)
         */
        val mL = addUnit(Unit("mℓ", Type.VOLUME, BigDecimal(".000001")))

        // TODO: Derived density, speed, acceleration, force, pressure, energy, power, charge, potential delta,
        //   resistance, conductance and capacitance

        @JvmStatic
        fun getUnit(symbol:String) : Unit? {
            synchronized(Unit::class.java) {
                val key = when(symbol) {
                    "LT" -> "ℓ"
                    "mL" -> "mℓ"
                    else -> convertExponent(symbol)
                }

                return UNITS.get(key)
            }
        }

        @JvmStatic
        fun addUnit(unit: Unit) : Unit {
            synchronized(Unit::class.java) {
                UNITS.put(unit.symbol, unit)
                return unit
            }
        }

        /**
         * Converts symbols ending with "2" or "3" to exponents
         */
        private fun convertExponent(text: String) = when {
            text.last()=='2' -> text.dropLast(1) + '²'
            text.last()=='3' -> text.dropLast(1) + '³'
            else -> text
        }
    }
}
