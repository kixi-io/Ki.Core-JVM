@file:Suppress("unused")

package io.kixi.uom

import io.kixi.whole
import java.math.MathContext
import java.util.concurrent.CopyOnWriteArrayList
import java.math.BigDecimal as Dec

/**
 * Thrown when two quantities are multiplied or divided and no product rule
 * connects their dimensions (e.g. `Concentration × Concentration`, or
 * anything involving `Temperature`).
 */
class UndefinedUnitArithmeticException(
    val left: Unit,
    val right: Unit,
    operation: String
) : RuntimeException(
    "No defined result for ${left.dimensionName} $operation ${right.dimensionName} " +
            "($left $operation $right)"
)

/**
 * Cross-dimension arithmetic for quantities.
 *
 * The algebra is a small table of product rules, each a triple of dimensions
 * with the pairing units that make the numbers coherent:
 *
 * | Product (canonical units)             | Gives                 |
 * |---------------------------------------|-----------------------|
 * | Concentration (M) × Volume (ℓ)        | SubstanceAmount (mol) |
 * | SubstanceAmount (mol) × MolarMass (Da)| Mass (g)              |
 * | Density (kgpm³) × Volume (m³)         | Mass (kg)             |
 * | Length (m) × Length (m)               | Area (m²)             |
 * | Length (m) × Area (m²)                | Volume (m³)           |
 * | Speed (kph) × Time (h)                | Length (km)           |
 *
 * Everything else is derived mechanically: products commute, and each rule
 * yields its two quotients (C÷A→B, C÷B→A). So `mass ÷ molar mass → moles`,
 * `moles ÷ volume → concentration`, `volume ÷ area → length`, and
 * `150mM × 1ℓ → 150mmol` all come from the six rows.
 *
 * Dividing quantities of the same dimension returns a dimensionless ratio in
 * [Unit.ratio], computed on the base-unit amounts (so `20°C ÷ 10°C` is the
 * ratio of the absolute temperatures, and `$100 ÷ $50` is `2`).
 *
 * ## Result units
 *
 * Operands are converted to the rule's pairing units, the math runs in Dec
 * ([MathContext.DECIMAL128], division last), and the result is then rescaled
 * along its dimension's display ladder so the leading number lands at 1 or
 * above in the largest unit that fits: `150mM × 1ℓ` comes back as `150mmol`,
 * not `0.15mol`. `convertTo` remains the way to demand a specific unit.
 *
 * ## Numeric types
 *
 * Follows [Quantity.convertTo]'s precedent: math runs in Dec; if both operand
 * values were Int/Long and the result is whole it demotes back (Int only when
 * both were Int), and if either operand was Double/Float the result is Double.
 *
 * ## Host dimensions
 *
 * A host that registers its own [Unit] subclass can extend the algebra with
 * [defineProduct]. Host dimensions without a display ladder keep the rule's
 * canonical unit.
 */
object UnitAlgebra {

    private class ProductRule(val leftUnit: Unit, val rightUnit: Unit, val resultUnit: Unit) {
        val leftClass: Class<*> = leftUnit.javaClass
        val rightClass: Class<*> = rightUnit.javaClass
        val resultClass: Class<*> = resultUnit.javaClass
    }

    private val rules = CopyOnWriteArrayList(
        listOf(
            ProductRule(Unit.M, Unit.L, Unit.mol),
            ProductRule(Unit.mol, Unit.Da, Unit.g),
            ProductRule(Unit.kgpm3, Unit.m3, Unit.kg),
            ProductRule(Unit.m, Unit.m, Unit.m2),
            ProductRule(Unit.m, Unit.m2, Unit.m3),
            ProductRule(Unit.kph, Unit.h, Unit.km)
        )
    )

    /**
     * Display ladders, largest unit first. A result is expressed in the first
     * ladder unit where its magnitude reaches 1; if none fits, the smallest.
     * Curated: display-friendly units only (no dm, no cm³ next to mℓ).
     */
    private val ladders: Map<Class<*>, List<Unit>> = mapOf(
        SubstanceAmount::class.java to
                listOf(Unit.mol, Unit.mmol, Unit.µmol, Unit.nmol, Unit.pmol, Unit.fmol),
        Concentration::class.java to
                listOf(Unit.M, Unit.mM, Unit.µM, Unit.nM, Unit.pM),
        Mass::class.java to
                listOf(Unit.kg, Unit.g, Unit.mg, Unit.µg, Unit.ng, Unit.pg, Unit.fg),
        Volume::class.java to
                listOf(Unit.m3, Unit.L, Unit.mL, Unit.µL, Unit.nL, Unit.pL),
        Area::class.java to listOf(Unit.km2, Unit.m2, Unit.cm2, Unit.mm2, Unit.nm2),
        Length::class.java to
                listOf(Unit.km, Unit.m, Unit.cm, Unit.mm, Unit.µm, Unit.nm),
        MolarMass::class.java to listOf(Unit.kDa, Unit.Da),
        Time::class.java to
                listOf(Unit.day, Unit.h, Unit.min, Unit.s, Unit.ms, Unit.µs, Unit.ns),
        Speed::class.java to listOf(Unit.mps, Unit.kph),
        // Density results read as mass concentration, the lab's spelling
        // (water is 1gpmℓ; 50ng ÷ 25µℓ is 2µgpmℓ). kgpm³ stays the base and
        // is always reachable through convertTo.
        Density::class.java to
                listOf(Unit.gpmL, Unit.mgpmL, Unit.µgpmL, Unit.ngpmL, Unit.pgpmL)
    )

    /**
     * Registers an additional product rule, `left × right = result`, stated
     * in the pairing units that make the numbers coherent. Commuted products
     * and both quotients are derived automatically, exactly like the
     * built-in rules.
     */
    @JvmStatic
    fun defineProduct(left: Unit, right: Unit, result: Unit) {
        rules.add(ProductRule(left, right, result))
    }

    /**
     * The natural (pre-scaling) unit of `left × right`, or null when no rule
     * connects the dimensions.
     */
    @JvmStatic
    fun productUnit(left: Unit, right: Unit): Unit? = productRuleFor(left, right)?.resultUnit

    /**
     * The natural (pre-scaling) unit of `left ÷ right`: [Unit.ratio] for the
     * same dimension, the derived quotient unit when a rule connects them,
     * or null.
     */
    @JvmStatic
    fun quotientUnit(left: Unit, right: Unit): Unit? {
        if (left.javaClass == right.javaClass) return Unit.ratio
        for (rule in rules) {
            if (rule.resultClass == left.javaClass && rule.leftClass == right.javaClass)
                return rule.rightUnit
            if (rule.resultClass == left.javaClass && rule.rightClass == right.javaClass)
                return rule.leftUnit
        }
        return null
    }

    /** True when `left × right` has a defined result. */
    @JvmStatic
    fun canMultiply(left: Unit, right: Unit): Boolean = productUnit(left, right) != null

    /** True when `left ÷ right` has a defined result. */
    @JvmStatic
    fun canDivide(left: Unit, right: Unit): Boolean = quotientUnit(left, right) != null

    /**
     * Multiplies two quantities across dimensions.
     *
     * @throws UndefinedUnitArithmeticException when no rule connects the dimensions
     */
    @JvmStatic
    fun multiply(a: Quantity<*>, b: Quantity<*>): Quantity<*> {
        val rule = productRuleFor(a.unit, b.unit)
            ?: throw UndefinedUnitArithmeticException(a.unit, b.unit, "×")

        // Orient the operands to the rule's sides (products commute)
        val first: Quantity<*>
        val second: Quantity<*>
        if (rule.leftClass == a.unit.javaClass) {
            first = a; second = b
        } else {
            first = b; second = a
        }

        val x = first.unit.convertValue(dec(first.value), rule.leftUnit)
        val y = second.unit.convertValue(dec(second.value), rule.rightUnit)
        val raw = x.multiply(y, MathContext.DECIMAL128)

        return scaledQuantity(raw, rule.resultUnit, a.value, b.value)
    }

    /**
     * Divides two quantities. Same dimension gives a dimensionless ratio in
     * [Unit.ratio] (computed on base-unit amounts); otherwise a rule's
     * derived quotient applies.
     *
     * @throws UndefinedUnitArithmeticException when no rule connects the dimensions
     * @throws ArithmeticException on division by a zero quantity
     */
    @JvmStatic
    fun divide(a: Quantity<*>, b: Quantity<*>): Quantity<*> {
        if (a.unit.javaClass == b.unit.javaClass) {
            val base = a.unit.baseUnit
            val x = a.unit.convertValue(dec(a.value), base)
            val y = b.unit.convertValue(dec(b.value), base)
            val raw = x.divide(y, MathContext.DECIMAL128)
            return typedQuantity(raw, Unit.ratio, a.value, b.value)
        }

        for (rule in rules) {
            if (rule.resultClass == a.unit.javaClass && rule.leftClass == b.unit.javaClass) {
                val num = a.unit.convertValue(dec(a.value), rule.resultUnit)
                val den = b.unit.convertValue(dec(b.value), rule.leftUnit)
                val raw = num.divide(den, MathContext.DECIMAL128)
                return scaledQuantity(raw, rule.rightUnit, a.value, b.value)
            }
            if (rule.resultClass == a.unit.javaClass && rule.rightClass == b.unit.javaClass) {
                val num = a.unit.convertValue(dec(a.value), rule.resultUnit)
                val den = b.unit.convertValue(dec(b.value), rule.rightUnit)
                val raw = num.divide(den, MathContext.DECIMAL128)
                return scaledQuantity(raw, rule.leftUnit, a.value, b.value)
            }
        }

        throw UndefinedUnitArithmeticException(a.unit, b.unit, "÷")
    }

    // Internals ////

    private fun productRuleFor(u1: Unit, u2: Unit): ProductRule? =
        rules.firstOrNull {
            (it.leftClass == u1.javaClass && it.rightClass == u2.javaClass) ||
                    (it.leftClass == u2.javaClass && it.rightClass == u1.javaClass)
        }

    private fun dec(n: Number): Dec = when (n) {
        is Dec -> n
        else -> Dec(n.toString())
    }

    /** Rescales along the dimension's ladder, then applies the type rule. */
    private fun scaledQuantity(
        raw: Dec, naturalUnit: Unit, a: Number, b: Number
    ): Quantity<Unit> {
        val unit = scaleUnit(raw, naturalUnit)
        val value =
            if (unit == naturalUnit) raw
            else naturalUnit.convertValue(raw, unit)
        return typedQuantity(value, unit, a, b)
    }

    /**
     * Picks the largest ladder unit in which the magnitude reaches 1. Zero
     * keeps the natural unit; a value below 1 in every ladder unit takes the
     * smallest.
     */
    private fun scaleUnit(raw: Dec, naturalUnit: Unit): Unit {
        if (raw.signum() == 0) return naturalUnit
        val ladder = ladders[naturalUnit.javaClass] ?: return naturalUnit
        val abs = raw.abs()

        for (unit in ladder) {
            val magnitude = abs.multiply(naturalUnit.factor)
                .divide(unit.factor, MathContext.DECIMAL128)
            if (magnitude >= Dec.ONE) return unit
        }
        return ladder.last()
    }

    /**
     * Applies the numeric type rule (see class doc): Double when either
     * operand was floating point, integral demotion when both were integral
     * and the result is whole, Dec otherwise.
     */
    private fun typedQuantity(raw: Dec, unit: Unit, a: Number, b: Number): Quantity<Unit> {
        val value: Number = when {
            a is Double || a is Float || b is Double || b is Float -> raw.toDouble()
            integral(a) && integral(b) && raw.whole -> demote(raw, a, b)
            else -> raw
        }
        return Quantity(value, unit)
    }

    private fun integral(n: Number): Boolean = n is Int || n is Long

    private fun demote(raw: Dec, a: Number, b: Number): Number = try {
        val long = raw.stripTrailingZeros().longValueExact()
        if (a is Int && b is Int && long in Int.MIN_VALUE..Int.MAX_VALUE) long.toInt()
        else long
    } catch (e: ArithmeticException) {
        raw // whole but outside Long range
    }
}