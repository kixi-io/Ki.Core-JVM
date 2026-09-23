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
 *                  (how many base units one of this unit represents)
 * @property offset The offset added to a value, in this unit's own scale, before
 *                  the factor is applied when converting to the base unit
 *                  (default: 0; only temperature units use it)
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
    override fun hashCode(): Int = symbol.hashCode() * 31

    /**
     * Formats a quantity value with this unit for display.
     *
     * The default implementation produces suffix notation: `"23cm:i"`, `"3.14kg"`.
     * Subclasses may override to provide alternate formatting (e.g., prefix notation
     * for currencies).
     *
     * @param valueText The formatted numeric value (e.g., "23.53", "100")
     * @param numType The number type suffix (e.g., ":L", ":d", ":i", or "")
     * @return The formatted quantity string
     */
    open fun formatQuantity(valueText: String, numType: String): String =
        "$valueText$symbol$numType"

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
     * For most units, this is equivalent to multiplying by [factorTo]. Offset
     * units (temperatures) override this with exact affine math.
     *
     * Formula: result = (value + sourceOffset) × sourceFactor ÷ targetFactor − targetOffset
     *
     * The division happens last, in one step, so conversions that terminate in
     * decimal come out exact.
     *
     * @param value The value in this unit
     * @param target The target unit
     * @return The converted value in the target unit
     * @throws IncompatibleUnitsException If the units have different dimensions
     */
    open fun convertValue(value: Dec, target: Unit): Dec {
        if (this::class.java != target::class.java)
            throw IncompatibleUnitsException(this, target)

        val base = (value + this.offset).multiply(this.factor)
        return base.divide(target.factor, MathContext.DECIMAL128) - target.offset
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

        /** The ångström (0.1nm): bond lengths, protein structures. */
        val Å = addUnit(Length("Å", Dec(".0000000001")))

        // Mass ////
        val fg = addUnit(Mass("fg", Dec(".000000000000001")))
        val pg = addUnit(Mass("pg", Dec(".000000000001")))
        val ng = addUnit(Mass("ng", Dec(".000000001")))
        val µg = addUnit(Mass("µg", Dec(".000001")))
        val mg = addUnit(Mass("mg", Dec(".001")))
        val cg = addUnit(Mass("cg", Dec(".01")))
        val g = addUnit(Mass("g", Dec("1")))
        val kg = addUnit(Mass("kg", Dec("1000")))

        // Temperature ////
        val K = addUnit(Temperature("K"))

        /**
         * Celsius (°C) has the same magnitude as Kelvin, just a different zero point:
         * K = °C + 273.15. dC is also accepted when parsing, but °C is always used
         * for output.
         */
        val dC = addUnit(Temperature("°C", Dec.ONE, Dec("273.15")))

        /**
         * Fahrenheit (°F) has 1.8 degrees per kelvin: K = (°F + 459.67) ÷ 1.8.
         * dF is also accepted when parsing, but °F is always used for output.
         */
        val dF = addUnit(Temperature("°F", Dec("1.8"), Dec("459.67")))

        // Substance Amount
        val fmol = addUnit(SubstanceAmount("fmol", Dec(".000000000000001")))
        val pmol = addUnit(SubstanceAmount("pmol", Dec(".000000000001")))
        val nmol = addUnit(SubstanceAmount("nmol", Dec(".000000001")))
        val µmol = addUnit(SubstanceAmount("µmol", Dec(".000001")))
        val mmol = addUnit(SubstanceAmount("mmol", Dec(".001")))
        val mol = addUnit(SubstanceAmount("mol", Dec("1")))

        // Molar Concentration (mol per litre) ////
        val pM = addUnit(Concentration("pM", Dec(".000000000001")))
        val nM = addUnit(Concentration("nM", Dec(".000000001")))
        val µM = addUnit(Concentration("µM", Dec(".000001")))
        val mM = addUnit(Concentration("mM", Dec(".001")))
        val M = addUnit(Concentration("M", Dec("1")))

        // Molar Mass ////

        /**
         * The dalton, numerically equal to g/mol — spelled the way chemists
         * read a molar mass, since a unit symbol cannot contain `/`. kDa is
         * the protein scale.
         */
        val Da = addUnit(MolarMass("Da", Dec("1")))
        val kDa = addUnit(MolarMass("kDa", Dec("1000")))

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
        val ns = addUnit(Time("ns", Dec(".000000001")))
        val µs = addUnit(Time("µs", Dec(".000001")))
        val ms = addUnit(Time("ms", Dec(".001")))
        val s = addUnit(Time("s", Dec("1")))
        val min = addUnit(Time("min", Dec("60")))
        val h = addUnit(Time("h", Dec("3600")))
        val day = addUnit(Time("day", Dec("86400")))

        // pH as a dimensionless unit
        val pH = addUnit(Dimensionless("pH", Dec("1")))

        /**
         * The unitless ratio produced by dividing quantities of the same
         * dimension (`300mm / 1m`, `6mol / 2mol`, `$100 / $50`). Its symbol is
         * empty, so a ratio quantity prints as a bare number ("0.3"), which is
         * also how it reads as a Ki literal. Deliberately NOT registered in
         * the unit registry: no literal parses to it.
         */
        val ratio = Dimensionless("", Dec.ONE)

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

        /** µL / uL / ul are accepted when parsing; µℓ is the output form. */
        val µL = addUnit(Volume("µℓ", Dec(".000000001")))

        /** nL is accepted when parsing; nℓ is the output form. */
        val nL = addUnit(Volume("nℓ", Dec(".000000000001")))

        /** pL is accepted when parsing; pℓ is the output form (acoustic dispensing). */
        val pL = addUnit(Volume("pℓ", Dec(".000000000000001")))

        // Speed / Velocity ////

        /**
         * KTS breaks with the SI standard here for practical purposes. kph is our
         * base speed unit. It is far more common and useful for most purposes than mps.
         */
        val kph = addUnit(Speed("kph", Dec("1")))

        /** 1 m/s is exactly 3.6 km/h. */
        val mps = addUnit(Speed("mps", Dec("3.6")))

        // Density (Volumetric Mass) ////

        /**
         * kgpm3 is also accepted when parsing, but kgpm³ is always used for output
         */
        val kgpm3 = addUnit(Density("kgpm³", Dec("1")))

        /*
         * Mass concentration reads as density dimensionally (1 ng/µℓ = 1 g/ℓ
         * = 1 kg/m³), so the lab spellings live on the Density dimension. The
         * `p` in each symbol reads "per", following kgpm³. ASCII forms
         * (mgpmL, ngpuL, ...) are accepted when parsing.
         */

        /** g/mℓ — how chemists write the density of liquids (water is 1gpmℓ). */
        val gpmL = addUnit(Density("gpmℓ", Dec("1000")))

        /** g/ℓ, numerically kg/m³. */
        val gpL = addUnit(Density("gpℓ", Dec("1")))

        /** mg/mℓ — protein concentrations. */
        val mgpmL = addUnit(Density("mgpmℓ", Dec("1")))

        /** µg/mℓ — antibody and drug concentrations. */
        val µgpmL = addUnit(Density("µgpmℓ", Dec(".001")))

        /** ng/µℓ — nucleic acid concentrations (numerically µg/mℓ). */
        val ngpµL = addUnit(Density("ngpµℓ", Dec(".001")))

        /** ng/mℓ — serum analytes. */
        val ngpmL = addUnit(Density("ngpmℓ", Dec(".000001")))

        /** pg/mℓ — cytokines, ELISA territory. */
        val pgpmL = addUnit(Density("pgpmℓ", Dec(".000000001")))

        // Pressure ////

        /** The pascal, SI. */
        val Pa = addUnit(Pressure("Pa"))
        val kPa = addUnit(Pressure("kPa", Dec("1000")))
        val bar = addUnit(Pressure("bar", Dec("100000")))
        val mbar = addUnit(Pressure("mbar", Dec("100")))

        /** Standard atmosphere, exactly 101325 Pa. */
        val atm = addUnit(Pressure("atm", Dec("101325")))

        /**
         * The millimetre of mercury, defined here as the torr (exactly
         * 101325/760 Pa) so that 760mmHg is exactly 1atm — vacuum-line and
         * rotovap arithmetic comes out clean. Torr is accepted when parsing.
         * (The BIPM "conventional" mmHg differs by 2 parts in 10⁷; treated
         * as the same unit.)
         */
        val mmHg = addUnit(Pressure("mmHg", Dec("101325"), Dec("760")))

        /**
         * Pound-force per square inch, exactly 0.45359237 × 9.80665 ÷
         * 0.0254² Pa, stored as the exact ratio 44482216152605 / 6451600000.
         */
        val psi = addUnit(Pressure("psi", Dec("44482216152605"), Dec("6451600000")))

        // TODO: Derived acceleration, force, energy, power, charge,
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

                // ASCII spellings of the micro-prefixed units (µ is
                // awkward to type) and the litre's ASCII forms.
                "uL", "µL", "ul" -> "µℓ"
                "nL" -> "nℓ"
                "pL" -> "pℓ"
                "ug" -> "µg"
                "uM" -> "µM"
                "umol" -> "µmol"
                "us" -> "µs"

                // Molar-mass spellings chemists write: the unified atomic
                // mass unit and g/mol are both numerically the dalton.
                "u" -> "Da"
                "gpmol" -> "Da"

                // Torr is the mmHg definition this library uses.
                "Torr", "torr" -> "mmHg"

                // Mass-concentration ASCII forms.
                "gpmL" -> "gpmℓ"
                "gpL", "gpLT" -> "gpℓ"
                "mgpmL" -> "mgpmℓ"
                "ugpmL", "µgpmL" -> "µgpmℓ"
                "ngpuL", "ngpµL", "ngpul" -> "ngpµℓ"
                "ngpmL" -> "ngpmℓ"
                "pgpmL" -> "pgpmℓ"

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
         * - `dC` → `°C` (Celsius), `dF` → `°F` (Fahrenheit)
         * - `um` → `µm` (micrometer)
         * - `uL` / `µL` → `µℓ`, `nL` → `nℓ`, `ug` → `µg`, `uM` → `µM`,
         *   `umol` → `µmol`
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

/**
 * Temperature units (K, °C, °F). Base unit: Kelvin (K).
 *
 * Temperature scales are affine: kelvin = (value + [offset]) ÷ [degreesPerKelvin],
 * with both constants expressed in the unit's own degrees. Kelvin and Celsius have
 * one degree per kelvin; Fahrenheit has 1.8.
 *
 * The inherited [factor] (1 ÷ degreesPerKelvin, the size of one degree in kelvins)
 * exists for ordering and unit-selection only. It is rounded for Fahrenheit, so
 * temperature value conversion never uses it: [convertValue] is overridden with
 * exact affine math that multiplies first and divides once, keeping every
 * terminating conversion exact (98.6°F → 37°C, 32°F → 273.15K).
 */
@Suppress("unused", "UNUSED_PARAMETER")
class Temperature(
    symbol: String,
    val degreesPerKelvin: Dec = Dec.ONE,
    offset: Dec = Dec.ZERO,
    unicode: String = symbol
) : Unit(symbol, Dec.ONE.divide(degreesPerKelvin, MathContext.DECIMAL128), offset, unicode) {

    override val baseUnit get() = getUnit("K")!! as Temperature

    override fun convertValue(value: Dec, target: Unit): Dec {
        if (target !is Temperature)
            throw IncompatibleUnitsException(this, target)

        // result = (value + offset) × targetDegreesPerKelvin ÷ degreesPerKelvin − targetOffset
        // Multiply before the single division so terminating conversions stay exact.
        val scaled = (value + offset).multiply(target.degreesPerKelvin)
        return scaled.divide(degreesPerKelvin, MathContext.DECIMAL128) - target.offset
    }
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

/**
 * Molar concentration units (M, mM, µM, nM, pM) — moles of solute per
 * litre of solution. Base unit: molar (M).
 */
@Suppress("unused", "UNUSED_PARAMETER")
class Concentration(symbol: String, factor: Dec, unicode: String = symbol) :
    Unit(symbol, factor, unicode = unicode) {
    override val baseUnit get() = getUnit("M")!! as Concentration
}

/**
 * Molar mass units (Da, kDa). The dalton is numerically g/mol, so a
 * molecule's molar mass in Da is the number a balance needs per mole.
 * Base unit: dalton (Da).
 */
@Suppress("unused", "UNUSED_PARAMETER")
class MolarMass(symbol: String, factor: Dec, unicode: String = symbol) :
    Unit(symbol, factor, unicode = unicode) {
    override val baseUnit get() = getUnit("Da")!! as MolarMass
}

/** Volumetric mass density units. Base unit: kilograms per cubic meter (kgpm³). */
@Suppress("unused", "UNUSED_PARAMETER")
class Density(symbol: String, factor: Dec, unicode: String = symbol) :
    Unit(symbol, factor, unicode = unicode) {
    override val baseUnit get() = getUnit("kgpm³")!! as Density
}

/**
 * Pressure units (Pa, kPa, bar, mbar, atm, mmHg, psi). Base unit: pascal (Pa).
 *
 * Some pressure units are exact ratios with no finite decimal form (mmHg is
 * 101325/760 Pa), so a Pressure unit stores its size as the exact rational
 * [pascals] ÷ [per] and overrides [convertValue] to multiply first and divide
 * once, keeping every terminating conversion exact: 760mmHg is exactly 1atm.
 * The inherited [factor] is the rounded ratio, for ordering and
 * unit-selection only.
 */
@Suppress("unused", "UNUSED_PARAMETER")
class Pressure(
    symbol: String,
    val pascals: Dec = Dec.ONE,
    val per: Dec = Dec.ONE,
    unicode: String = symbol
) : Unit(symbol, pascals.divide(per, MathContext.DECIMAL128), unicode = unicode) {

    override val baseUnit get() = getUnit("Pa")!! as Pressure

    override fun convertValue(value: Dec, target: Unit): Dec {
        if (target !is Pressure)
            throw IncompatibleUnitsException(this, target)

        // result = value × (pascals/per) ÷ (targetPascals/targetPer),
        // multiplied out before the single division so terminating
        // conversions stay exact.
        val numerator = value.multiply(pascals).multiply(target.per)
        val denominator = per.multiply(target.pascals)
        return numerator.divide(denominator, MathContext.DECIMAL128)
    }
}

// combineUnits/canCombineUnits were removed: they returned a unit without any
// factor correction, so mixed-prefix products (km × m) produced wrong
// magnitudes. Quantity × Quantity and Quantity ÷ Quantity now go through
// [UnitAlgebra], which converts operands to coherent pairing units first.