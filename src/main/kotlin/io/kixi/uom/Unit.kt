package io.kixi.uom

import java.math.BigDecimal
import java.math.MathContext
import java.util.TreeMap

/**
 * An SI unit of measure
 */
@Suppress("unused")
abstract class Unit(val symbol: String, val factor: BigDecimal, val unicode: String = symbol)
    : Comparable<Unit> {

    abstract val baseUnit: Unit

    val description get() = "$symbol ${Length::class.java.simpleName} ${factor.toPlainString()}"

    val axis get() = this::class.simpleName

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
    open fun factorTo(target:Unit) : BigDecimal {

        // There should be a better way to do this with generics
        if(this::class.java != target::class.java)
            throw IncompatibleUnitsException(this, target)

        return factor.divide(target.factor, MathContext.DECIMAL128)
    }

    companion object {

        private val UNITS = TreeMap<String, Unit>()

        /* Base Units -------- */

        // Length ////
        val nm = addUnit(Length("nm", BigDecimal(".000000001")))
        val mm = addUnit(Length("mm", BigDecimal(".001")))
        val cm = addUnit(Length("cm", BigDecimal(".01")))
        val m = addUnit(Length("m", BigDecimal("1")))
        val km = addUnit(Length("km", BigDecimal("1000")))

        // Mass ////
        val ng = addUnit(Mass("ng", BigDecimal(".000000001")))
        val mg = addUnit(Mass("mg", BigDecimal(".001")))
        val cg = addUnit(Mass("cg", BigDecimal(".01")))
        val g = addUnit(Mass("g", BigDecimal("1")))
        val kg = addUnit(Mass("kg", BigDecimal("1000")))

        // Temperature ////
        val K = addUnit(Temperature("K", BigDecimal("1")))

        /**
         * Celsius (°C) has the same magnitude as Kelvin, just a different null point
         *
         * dC is also accepted when parsing, but °C is always used for output
         */
        val dC = addUnit(Temperature("°C", BigDecimal("1")))

        // Substance Amount
        val mol = addUnit(SubstanceAmount("mol", BigDecimal("1")))

        // Electric Current
        val A = addUnit(Current("A", BigDecimal("1")))

        // Luminosity
        val cd = addUnit(Current("cd", BigDecimal("1")))

        // Time ////
        // This is handled by the Duration type.
        // TODO: bridge SI time units w/ Durations

        /* Common Derived Units -------- */

        // Area ////
        val nm2 = addUnit(Area("nm²", BigDecimal(".000000000000000001")))
        val mm2 = addUnit(Area("mm²", BigDecimal(".000001")))
        val cm2 = addUnit(Area("cm²", BigDecimal(".0001")))
        val m2 = addUnit(Area("m²", BigDecimal("1")))
        val km2 = addUnit(Area("km²", BigDecimal("1000000")))

        // Volume ////
        val nm3 = addUnit(Volume("nm³", BigDecimal(".000000000000000000000000001")))
        val mm3 = addUnit(Volume("mm³", BigDecimal(".000000001")))
        val cm3 = addUnit(Volume("cm³", BigDecimal(".000001")))
        val m3 = addUnit(Volume("m³", BigDecimal("1")))
        val km3 = addUnit(Volume("km³", BigDecimal("1000000000")))

        /**
         * We have to use ℓ to avoid a conflict with L for Long integer literals. LT is accepted when parsing, but ℓ is
         * always used for output.
         */
        val L = addUnit(Volume("ℓ", BigDecimal(".001")))

        /**
         * mL is also accepted when parsing, but ℓ is always used for output to be consistent with liter (ℓ)
         */
        val mL = addUnit(Volume("mℓ", BigDecimal(".000001")))

        // Speed / Velocity ////

        /**
         * KTS breaks with the SI standard here for practical purposes. kph is our base speed unit. It is far more
         * common and useful for most purposes than mps. We define both.
         */
        val kph = addUnit(Speed("kph", BigDecimal("1")))

        val mps = addUnit(Speed("mps", BigDecimal("0.277778")))

        // Density (Volumetric Mass) ////

        /**
         * kgpm3 is also accepted when parsing, but kgpm³ is always used for output
         */
        val kgpm3 = addUnit(Density("kgpm³", BigDecimal("1")))

        // TODO: Derived acceleration, force, pressure, energy, power, charge, potential delta, resistance, conductance
        //     and capacitance

        @JvmStatic
        fun getUnit(symbol:String) : Unit? {
            synchronized(Unit::class.java) {
                val key = when(symbol) {
                    "LT" -> "ℓ"
                    "mL" -> "mℓ"
                    "dC" -> "°C"
                    else -> convertExponent(symbol)
                }

                return UNITS.get(key)
            }
        }

        @JvmStatic
        fun <T : Unit>addUnit(unit: T) : T {
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

    // TODO - This will compare incompatible Units. Make type safe!
    override fun compareTo(other: Unit): Int {
        val factor = this.factorTo(other)
        return when {
            factor == BigDecimal.ONE -> 0
            factor > BigDecimal.ONE -> 1
            factor < BigDecimal.ONE -> -1
            // Can't happen
            else -> throw Error("Internal error in Unit.compareTo(Unit)")
        }
    }
}

class Length(symbol: String, factor: BigDecimal, unicode: String = symbol) :
    Unit(symbol, factor, unicode= symbol) {

    override val baseUnit get() = getUnit("m")!! as Length
}

class Area(symbol: String, factor: BigDecimal, unicode: String = symbol) :
    Unit(symbol, factor, unicode= symbol) {

    override val baseUnit get() = getUnit("m2")!! as Area
}

class Volume(symbol: String, factor: BigDecimal, unicode: String = symbol) :
    Unit(symbol, factor, unicode= symbol) {

    override val baseUnit get() = getUnit("m3")!! as Volume
}

class Mass(symbol: String, factor: BigDecimal, unicode: String = symbol) :
    Unit(symbol, factor, unicode= symbol) {

    override val baseUnit get() = getUnit("kg")!! as Mass
}

class Temperature(symbol: String, factor: BigDecimal, unicode: String = symbol) :
    Unit(symbol, factor, unicode= symbol) {

    override val baseUnit get() = getUnit("K")!! as Temperature
}

class Speed(symbol: String, factor: BigDecimal, unicode: String = symbol) :
    Unit(symbol, factor, unicode= symbol) {

    override val baseUnit get() = getUnit("kph")!! as Speed
}

class SubstanceAmount(symbol: String, factor: BigDecimal, unicode: String = symbol) :
    Unit(symbol, factor, unicode= symbol) {

    override val baseUnit get() = getUnit("mol")!! as SubstanceAmount
}

class Current(symbol: String, factor: BigDecimal, unicode: String = symbol) :
    Unit(symbol, factor, unicode= symbol) {

    override val baseUnit get() = getUnit("A")!! as Current
}

class Luminosity(symbol: String, factor: BigDecimal, unicode: String = symbol) :
    Unit(symbol, factor, unicode= symbol) {

    override val baseUnit get() = getUnit("cd")!! as Current
}

/**
 * Volumetric mass density
 */
class Density(symbol: String, factor: BigDecimal, unicode: String = symbol) :
    Unit(symbol, factor, unicode= symbol) {

    override val baseUnit get() = getUnit("kgpm3")!! as Density
}

// TODO: Add these unit axes
/*
// Base Types
TIME("s") // TODO: Bridge with Duration

// Common Derived Unit Types
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
*/
