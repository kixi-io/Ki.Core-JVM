@file:Suppress("unused")

package io.kixi.uom

import io.kixi.Parseable
import io.kixi.text.ParseException
import java.math.BigDecimal as Dec
import java.math.MathContext
import java.util.concurrent.ConcurrentHashMap

/**
 * An SI unit of measure.
 *
 * Units are organized by dimension (Length, Mass, Temperature, etc.) and include
 * a conversion factor relative to a base unit within that dimension.
 *
 * For most units, conversion is purely multiplicative (factor-based). Temperature
 * units also require an offset since Celsius and Fahrenheit have different zero points
 * than Kelvin.
 *
 * @property symbol The unit's symbol (e.g., "km", "kg", "°C")
 * @property factor The conversion factor relative to the dimension's base unit
 * @property offset The offset to add when converting to the base unit (default: 0)
 * @property unicode The Unicode representation of the symbol (defaults to [symbol])
 */
@Suppress("unused")
abstract class Unit(
    val symbol: String,
    val factor: Dec,
    val offset: Dec = Dec.ZERO,
    val unicode: String = symbol
) : Comparable<Unit> {

    /** The base unit for this unit's dimension (e.g., meters for Length). */
    abstract val baseUnit: Unit

    /** A human-readable description including symbol, dimension, and factor. */
    val description get() = "$symbol ${this::class.java.simpleName} ${factor.toPlainString()}"

    /** The dimension name (e.g., "Length", "Mass"). Alias for the class simple name. */
    val axis get() = this::class.simpleName

    override fun toString(): String = symbol
    override fun equals(other: Any?): Boolean = other != null && other is Unit &&
            other.symbol == symbol
    override fun hashCode(): Int = symbol.hashCode() or 31

    /**
     * The number by which you multiply to get a conversion to the target unit.
     * For example, `Unit.cm.factorTo(Unit.mm)` is `10`.
     *
     * Note: For temperature units, use [convertValue] instead, which also handles offsets.
     *
     * @param target The unit to which we are converting
     * @throws IncompatibleUnitsException If the units do not have the same type.
     */
    open fun factorTo(target: Unit): Dec {
        if (this::class.java != target::class.java)
            throw IncompatibleUnitsException(this, target)

        return factor.divide(target.factor, MathContext.DECIMAL128)
    }

    /**
     * Converts a value from this unit to the target unit, handling both factor
     * and offset conversions.
     *
     * For most units, this is equivalent to multiplying by [factorTo]. For temperature
     * units, this also applies the necessary offset adjustment (e.g., Celsius to Kelvin).
     *
     * Formula: result = (value + sourceOffset - targetOffset) × (sourceFactor / targetFactor)
     *
     * @param value The value in this unit
     * @param target The target unit
     * @return The converted value in the target unit
     * @throws IncompatibleUnitsException If the units have different dimensions
     */
    open fun convertValue(value: Dec, target: Unit): Dec {
        if (this::class.java != target::class.java)
            throw IncompatibleUnitsException(this, target)

        // Apply offset adjustment (for temperature), then factor
        val adjusted = value + this.offset - target.offset
        return adjusted.multiply(factorTo(target), MathContext.DECIMAL128)
    }

    /**
     * Property to access a unit's dimension.
     * This uses the unit's class as a simple dimension identifier.
     */
    val dimension: Class<*>
        get() = this.javaClass

    /**
     * Check if two units are compatible (have the same dimension).
     */
    fun isCompatibleWith(other: Unit): Boolean = this.javaClass == other.javaClass

    /**
     * Get the dimension name of a unit.
     */
    val dimensionName: String
        get() {
            val className = this.javaClass.simpleName
            return if (className.endsWith("Unit")) {
                className.substring(0, className.length - 4)
            } else {
                className
            }
        }

    companion object : Parseable<Unit> {
        // Using ConcurrentHashMap for better concurrent read performance
        private val UNITS = ConcurrentHashMap<String, Unit>()

        /* Base Units -------- */

        // Length ////
        val nm = addUnit(Length("nm", Dec(".000000001")))
        val mm = addUnit(Length("mm", Dec(".001")))
        val cm = addUnit(Length("cm", Dec(".01")))
        val m = addUnit(Length("m", Dec("1")))
        val km = addUnit(Length("km", Dec("1000")))
        val µm = addUnit(Length("µm", Dec(".000001")))
        val dm = addUnit(Length("dm", Dec(".1")))

        // Mass ////
        val ng = addUnit(Mass("ng", Dec(".000000001")))
        val mg = addUnit(Mass("mg", Dec(".001")))
        val cg = addUnit(Mass("cg", Dec(".01")))
        val g = addUnit(Mass("g", Dec("1")))
        val kg = addUnit(Mass("kg", Dec("1000")))

        // Temperature ////
        val K = addUnit(Temperature("K", Dec("1")))

        /**
         * Celsius (°C) has the same magnitude as Kelvin, just a different zero point.
         * The offset of 273.15 means: K = °C + 273.15
         * dC is also accepted when parsing, but °C is always used for output.
         */
        val dC = addUnit(Temperature("°C", Dec("1"), Dec("273.15")))

        // Substance Amount
        val mol = addUnit(SubstanceAmount("mol", Dec("1")))

        // Electric Current
        val A = addUnit(Current("A", Dec("1")))

        // Luminosity
        val cd = addUnit(Luminosity("cd", Dec("1")))

        // Time ////
        // This is handled by the Duration type.
        // TODO: bridge SI time units w/ Durations

        /* Common Derived Units -------- */

        // Area ////
        val nm2 = addUnit(Area("nm²", Dec(".000000000000000001")))
        val mm2 = addUnit(Area("mm²", Dec(".000001")))
        val cm2 = addUnit(Area("cm²", Dec(".0001")))
        val m2 = addUnit(Area("m²", Dec("1")))
        val km2 = addUnit(Area("km²", Dec("1000000")))

        // Volume ////
        val nm3 = addUnit(Volume("nm³", Dec(".000000000000000000000000001")))
        val mm3 = addUnit(Volume("mm³", Dec(".000000001")))
        val cm3 = addUnit(Volume("cm³", Dec(".000001")))
        val m3 = addUnit(Volume("m³", Dec("1")))
        val km3 = addUnit(Volume("km³", Dec("1000000000")))

        // Duration
        val s = addUnit(Time("s", Dec("1")))
        val min = addUnit(Time("min", Dec("60")))
        val h = addUnit(Time("h", Dec("3600")))
        val day = addUnit(Time("day", Dec("86400")))

        // pH as a dimensionless unit
        val pH = addUnit(Dimensionless("pH", Dec("1")))

        /**
         * We have to use ℓ to avoid a conflict with L for Long integer literals.
         * LT is accepted when parsing, but ℓ is always used for output.
         */
        val L = addUnit(Volume("ℓ", Dec(".001")))

        /**
         * mL is also accepted when parsing, but mℓ is always used for output
         * to be consistent with liter (ℓ)
         */
        val mL = addUnit(Volume("mℓ", Dec(".000001")))

        // Speed / Velocity ////

        /**
         * KTS breaks with the SI standard here for practical purposes. kph is our
         * base speed unit. It is far more common and useful for most purposes than mps.
         */
        val kph = addUnit(Speed("kph", Dec("1")))
        val mps = addUnit(Speed("mps", Dec("0.277778")))

        // Density (Volumetric Mass) ////

        /**
         * kgpm3 is also accepted when parsing, but kgpm³ is always used for output
         */
        val kgpm3 = addUnit(Density("kgpm³", Dec("1")))

        // TODO: Derived acceleration, force, pressure, energy, power, charge,
        //       potential delta, resistance, conductance and capacitance

        /* Currencies -------- */

        /**
         * Helper method to register a currency.
         *
         * Note: This method does NOT call Currency.registerPrefix() to avoid
         * circular initialization issues. The prefix map is populated lazily
         * in Currency.ensurePrefixesInitialized() when first needed.
         */
        private fun addCurrency(currency: Currency): Currency {
            addUnit(currency)
            // Do NOT call Currency.registerPrefix here - it causes circular init issues
            // Prefix registration is handled lazily in Currency.ensurePrefixesInitialized()
            return currency
        }

        // Fiat Currencies (Top 12 by usage)
        /** US Dollar */
        val USD = addCurrency(Currency("USD", '$', "US Dollar"))
        /** Euro */
        val EUR = addCurrency(Currency("EUR", '€', "Euro"))
        /** Japanese Yen */
        val JPY = addCurrency(Currency("JPY", '¥', "Japanese Yen"))
        /** British Pound */
        val GBP = addCurrency(Currency("GBP", '£', "British Pound"))
        /** Chinese Yuan */
        val CNY = addCurrency(Currency("CNY", currencyName = "Chinese Yuan"))
        /** Australian Dollar */
        val AUD = addCurrency(Currency("AUD", currencyName = "Australian Dollar"))
        /** Canadian Dollar */
        val CAD = addCurrency(Currency("CAD", currencyName = "Canadian Dollar"))
        /** Swiss Franc */
        val CHF = addCurrency(Currency("CHF", currencyName = "Swiss Franc"))
        /** Hong Kong Dollar */
        val HKD = addCurrency(Currency("HKD", currencyName = "Hong Kong Dollar"))
        /** Singapore Dollar */
        val SGD = addCurrency(Currency("SGD", currencyName = "Singapore Dollar"))
        /** Indian Rupee */
        val INR = addCurrency(Currency("INR", currencyName = "Indian Rupee"))
        /** South Korean Won */
        val KRW = addCurrency(Currency("KRW", currencyName = "South Korean Won"))

        // Cryptocurrencies
        /** Bitcoin */
        val BTC = addCurrency(Currency("BTC", '₿', "Bitcoin"))
        /** Ether (Ethereum) */
        val ETH = addCurrency(Currency("ETH", 'Ξ', "Ether"))

        /**
         * Retrieves a unit by its symbol, handling common aliases and ASCII alternatives.
         *
         * @param symbol The unit symbol to look up
         * @return The matching Unit, or null if not found
         */
        @JvmStatic
        fun getUnit(symbol: String): Unit? {
            val key = when (symbol) {
                "LT" -> "ℓ"
                "mL" -> "mℓ"
                "dC" -> "°C"
                "dF" -> "°F"
                "um", "µm" -> "µm"

                // Handle ASCII alternatives for superscripts
                "mm2" -> "mm²"
                "cm2" -> "cm²"
                "m2" -> "m²"
                "km2" -> "km²"
                "nm2" -> "nm²"
                "mm3" -> "mm³"
                "cm3" -> "cm³"
                "m3" -> "m³"
                "km3" -> "km³"
                "nm3" -> "nm³"
                "kgpm3" -> "kgpm³"
                else -> convertExponent(symbol)
            }

            return UNITS[key]
        }

        /**
         * Retrieves a currency unit by its prefix symbol.
         *
         * @param prefix The currency prefix symbol (e.g., '$', '€', '¥')
         * @return The matching Currency, or null if not found
         */
        @JvmStatic
        fun getCurrencyByPrefix(prefix: Char): Currency? = Currency.fromPrefix(prefix)

        /**
         * Checks if a character is a valid currency prefix symbol.
         *
         * @param ch The character to check
         * @return true if the character is a currency prefix symbol
         */
        @JvmStatic
        fun isCurrencyPrefix(ch: Char): Boolean = Currency.isPrefixSymbol(ch)

        /**
         * Registers a new unit in the unit registry.
         *
         * @param unit The unit to register
         * @return The registered unit (for chaining)
         */
        @JvmStatic
        fun <T : Unit> addUnit(unit: T): T {
            UNITS[unit.symbol] = unit
            return unit
        }

        /**
         * Converts symbols ending with "2" or "3" to exponents
         */
        private fun convertExponent(text: String): String = when {
            text.isEmpty() -> text
            text.last() == '2' -> text.dropLast(1) + '²'
            text.last() == '3' -> text.dropLast(1) + '³'
            else -> text
        }

        /**
         * Returns all registered units
         */
        @JvmStatic
        fun allUnits(): Collection<Unit> = UNITS.values

        /**
         * Parse a unit symbol string into a Unit instance.
         *
         * Handles common aliases and ASCII alternatives:
         * - `LT` → `ℓ` (liter)
         * - `mL` → `mℓ` (milliliter)
         * - `dC` → `°C` (Celsius)
         * - `um` → `µm` (micrometer)
         * - `m2` → `m²` (square meter)
         * - `m3` → `m³` (cubic meter)
         *
         * ```kotlin
         * val meter = Unit.parse("m")
         * val celsius = Unit.parse("dC")  // or "°C"
         * val sqMeter = Unit.parse("m2")  // or "m²"
         * ```
         *
         * @param symbol The unit symbol to parse
         * @return The parsed Unit
         * @throws ParseException if the symbol does not match any known unit
         */
        @JvmStatic
        fun parse(symbol: String): Unit {
            val trimmed = symbol.trim()

            if (trimmed.isEmpty())
                throw ParseException("Unit symbol cannot be empty.", index = 0)

            return getUnit(trimmed)
                ?: throw ParseException("Unknown unit symbol: $trimmed")
        }

        /**
         * Parses a Ki unit symbol string into a Unit instance.
         *
         * @param text The unit symbol string to parse
         * @return The parsed Unit
         * @throws ParseException if the text cannot be parsed as a valid Unit
         */
        override fun parseLiteral(text: String): Unit = parse(text)

        /**
         * Parse a unit symbol, returning null on failure instead of throwing.
         *
         * @param symbol The unit symbol to parse
         * @return The parsed Unit, or null if parsing fails
         */
        @JvmStatic
        fun parseOrNull(symbol: String): Unit? = try {
            parse(symbol)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Compares this unit to another unit of the same dimension by their conversion factors.
     *
     * @throws IncompatibleUnitsException if the units have different dimensions
     */
    override fun compareTo(other: Unit): Int {
        if (this::class.java != other::class.java)
            throw IncompatibleUnitsException(this, other)

        val factor = this.factorTo(other)
        return when {
            factor == Dec.ONE -> 0
            factor > Dec.ONE -> 1
            factor < Dec.ONE -> -1
            else -> throw Error("Internal error in Unit.compareTo(Unit)")
        }
    }
}

/** Length units (nm, mm, cm, m, km, etc.). Base unit: meter (m). */
@Suppress("unused", "UNUSED_PARAMETER")
class Length(symbol: String, factor: Dec, unicode: String = symbol) :
    Unit(symbol, factor, unicode = unicode) {
    override val baseUnit get() = getUnit("m")!! as Length
}

/** Area units (mm², cm², m², km², etc.). Base unit: square meter (m²). */
@Suppress("unused", "UNUSED_PARAMETER")
class Area(symbol: String, factor: Dec, unicode: String = symbol) :
    Unit(symbol, factor, unicode = unicode) {
    override val baseUnit get() = getUnit("m²")!! as Area
}

/** Volume units (mm³, cm³, m³, ℓ, mℓ, etc.). Base unit: cubic meter (m³). */
@Suppress("unused", "UNUSED_PARAMETER")
class Volume(symbol: String, factor: Dec, unicode: String = symbol) :
    Unit(symbol, factor, unicode = unicode) {
    override val baseUnit get() = getUnit("m³")!! as Volume
}

/** Mass units (ng, mg, g, kg, etc.). Base unit: kilogram (kg). */
@Suppress("unused", "UNUSED_PARAMETER")
class Mass(symbol: String, factor: Dec, unicode: String = symbol) :
    Unit(symbol, factor, unicode = unicode) {
    override val baseUnit get() = getUnit("kg")!! as Mass
}

/** Time duration units (s, min, h, day). Base unit: second (s). */
@Suppress("unused", "UNUSED_PARAMETER")
class Time(symbol: String, factor: Dec, unicode: String = symbol) :
    Unit(symbol, factor, unicode = unicode) {
    override val baseUnit get() = getUnit("s")!! as Time
}

/** Dimensionless units (e.g., pH). */
@Suppress("unused", "UNUSED_PARAMETER")
class Dimensionless(symbol: String, factor: Dec, unicode: String = symbol) :
    Unit(symbol, factor, unicode = unicode) {
    override val baseUnit get() = this
}

/** Temperature units (K, °C). Base unit: Kelvin (K). */
@Suppress("unused", "UNUSED_PARAMETER")
class Temperature(symbol: String, factor: Dec, offset: Dec = Dec.ZERO, unicode: String = symbol) :
    Unit(symbol, factor, offset, unicode) {
    override val baseUnit get() = getUnit("K")!! as Temperature
}

/** Speed/velocity units (kph, mps). Base unit: kilometers per hour (kph). */
@Suppress("unused", "UNUSED_PARAMETER")
class Speed(symbol: String, factor: Dec, unicode: String = symbol) :
    Unit(symbol, factor, unicode = unicode) {
    override val baseUnit get() = getUnit("kph")!! as Speed
}

/** Substance amount units. Base unit: mole (mol). */
@Suppress("unused", "UNUSED_PARAMETER")
class SubstanceAmount(symbol: String, factor: Dec, unicode: String = symbol) :
    Unit(symbol, factor, unicode = unicode) {
    override val baseUnit get() = getUnit("mol")!! as SubstanceAmount
}

/** Electric current units. Base unit: ampere (A). */
@Suppress("unused", "UNUSED_PARAMETER")
class Current(symbol: String, factor: Dec, unicode: String = symbol) :
    Unit(symbol, factor, unicode = unicode) {
    override val baseUnit get() = getUnit("A")!! as Current
}

/** Luminous intensity units. Base unit: candela (cd). */
@Suppress("unused", "UNUSED_PARAMETER")
class Luminosity(symbol: String, factor: Dec, unicode: String = symbol) :
    Unit(symbol, factor, unicode = unicode) {
    override val baseUnit get() = getUnit("cd")!! as Luminosity
}

/** Volumetric mass density units. Base unit: kilograms per cubic meter (kgpm³). */
@Suppress("unused", "UNUSED_PARAMETER")
class Density(symbol: String, factor: Dec, unicode: String = symbol) :
    Unit(symbol, factor, unicode = unicode) {
    override val baseUnit get() = getUnit("kgpm³")!! as Density
}

/**
 * Creates a compound unit by combining two units.
 * For example: m × m = m²
 *
 * @param left The first unit
 * @param right The second unit
 * @return The resulting compound unit, or null if the combination is not supported
 */
fun combineUnits(left: Unit, right: Unit): Unit? {
    return when {
        // Length × Length = Area
        left is Length && right is Length -> {
            when {
                left == Unit.nm && right == Unit.nm -> Unit.nm2
                left == Unit.mm && right == Unit.mm -> Unit.mm2
                left == Unit.cm && right == Unit.cm -> Unit.cm2
                left == Unit.m && right == Unit.m -> Unit.m2
                left == Unit.km && right == Unit.km -> Unit.km2
                else -> Unit.m2
            }
        }

        // Length × Area = Volume
        left is Length && right is Area -> {
            when {
                left == Unit.nm && right == Unit.nm2 -> Unit.nm3
                left == Unit.mm && right == Unit.mm2 -> Unit.mm3
                left == Unit.cm && right == Unit.cm2 -> Unit.cm3
                left == Unit.m && right == Unit.m2 -> Unit.m3
                left == Unit.km && right == Unit.km2 -> Unit.km3
                else -> Unit.m3
            }
        }

        // Area × Length = Volume
        left is Area && right is Length -> combineUnits(right, left)

        else -> null
    }
}

/**
 * Checks if two units can be combined using the combine operator.
 *
 * @param left The first unit
 * @param right The second unit
 * @return true if the units can be combined into a compound unit
 */
fun canCombineUnits(left: Unit, right: Unit): Boolean = combineUnits(left, right) != null