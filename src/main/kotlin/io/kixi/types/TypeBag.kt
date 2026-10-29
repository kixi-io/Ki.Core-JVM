package io.kixi.types

import kotlin.reflect.KClass

/**
 * A library of value types, functions and constants for one domain (chemistry,
 * biology, ...), which KD, KS, KQ and RevDB pick up without knowing the domain.
 *
 * Bags are found by [TypeBags]: registered with [TypeBags.register], or discovered
 * from `META-INF/services/io.kixi.types.TypeBag`, which lists the bag's class, and
 * its manifest, `META-INF/ki/bags/<bag class>.properties` ([TypeBags.manifestOf]
 * writes it). Discovery reads only the manifest; the bag is created the first time
 * something refers to it (its literal, a type or function name, one of its values),
 * unless the manifest says `preload=true`. Preloading is generally a bad idea: it
 * costs every host memory for a bag it may never use, so keep it for a small bag that
 * is used frequently (Ki.Math, Ki.Sci, Ki.Bio). A discovered bag needs a public
 * no-argument constructor; a Kotlin `object` is registered with [TypeBags.register],
 * or listed through a small class that delegates to it.
 *
 * Names are global: two bags may not share a bag name, a type name, a literal
 * name, or a function or constant name (a bag's functions and constants share one
 * namespace, as `use ki.<bag>.*` binds both). [TypeBags.register] refuses a
 * conflict with both bags' names in the message.
 */
interface TypeBag {

    /** `[a-z][a-z0-9]*`: the name in `use ki.<name>` and in RevDB's stored values and index keys. */
    val name: String

    /**
     * The codec version, at least 1, written with every encoded value. Bump it when
     * an encoding changes; [ValueType.decode] must accept every version the bag
     * ever wrote.
     */
    val version: Int

    /** Names of the bags this one extends; they are registered first. */
    val requires: List<String> get() = emptyList()

    /** The bag's value types. Return the same instances every time. */
    val types: List<ValueType<*>>

    /** Functions for KS, Boa and KQ. */
    val functions: List<BagFunction> get() = emptyList()

    /** Named constants for KS, Boa and KQ (`avogadro`). Values are never nil. */
    val constants: Map<String, Any> get() = emptyMap()

    /**
     * Members this bag adds to types of other bags it [requires], by type name
     * (Bio adding `residues` to Sci's `Molecule`). They are looked up after the
     * type's own members, in registration order, and may not reuse their names. Each
     * member must take values of the type it is listed under.
     */
    val extensions: Map<String, List<Member<*>>> get() = emptyMap()
}

/**
 * One value type: how it is written ([parse], [format]), stored ([encode],
 * [decode]), ordered ([sortKey]) and navigated ([members]).
 *
 * Equality and hashing are the Kotlin class's (`equals`, `hashCode`), as for every
 * Ki.Core type.
 */
interface ValueType<T : Any> {

    /** `[A-Z][A-Za-z0-9]*`: the KDS type name, what KS's `is` checks, the type KQ reports. */
    val name: String

    /** The class of the values. A value of a subclass is a value of this type. */
    val kotlinClass: KClass<T>

    /**
     * The dot-literal names (`dna` for `.dna("ATG")`): letters only, starting with a
     * lowercase letter, matched without regard to case (as KD matches `.id(`). [format]
     * writes the first. Empty for a type that has no literal, whose values cannot be
     * stored in KD or RevDB.
     */
    val literals: List<String>

    /**
     * Reads a literal, given whole as written (`.dna("ATG")`), as Ki.Core's
     * `Parseable.parseLiteral` does. [TypeBags.inside] and [TypeBags.stringOrToken]
     * read the usual forms. Throws [io.kixi.text.ParseException] for bad text.
     */
    fun parse(literal: String): T

    /** The literal for [value], whole: `parse(format(v)) == v`. */
    fun format(value: T): String

    /** The stored form of [value]: self-contained, with no references to the bag's tables. */
    fun encode(value: T): ByteArray

    /** Reads bytes [encode] wrote at bag [version] (any version the bag ever wrote). */
    fun decode(bytes: ByteArray, version: Int): T

    /** True when the type orders: then [sortKey] gives each value its key. */
    val ordered: Boolean get() = false

    /**
     * Bytes whose unsigned order is the type's order, equal exactly when the values
     * are equal, and fixed for the life of the type (indexes store them; a bag that
     * needs another order adds another type). Called only when [ordered].
     */
    fun sortKey(value: T): ByteArray = throw UnsupportedOperationException("$name does not order")

    /** The type's members for KS, Boa and KQ. */
    val members: List<Member<T>> get() = emptyList()

    /**
     * Values that exercise the type (the empty and largest cases, one per kind), for
     * [TypeBags.check] and RevDB's storage round-trip test. Every bag should list some.
     */
    val samples: List<T> get() = emptyList()
}

/**
 * A member of a value type: a property when [arity] is null (`seq.gc`), else a
 * method taking that many arguments (`seq.slice(1, 4)`). [invoke] gets the value
 * and the arguments (none for a property); its result may be nil. [resultType] is a
 * [ValueType] or Ki.Core `Type` name when the result's type is known.
 */
class Member<in T : Any>(
    val name: String,
    val arity: IntRange?,
    val resultType: String? = null,
    val invoke: (T, List<Any?>) -> Any?,
) {
    val isProperty: Boolean get() = arity == null

    override fun toString(): String = if (arity == null) name else "$name(${describeArity(arity)})"
}

/**
 * A function a bag contributes to KS, Boa and KQ. [resultType] is a [ValueType] or
 * Ki.Core `Type` name when the result's type is known.
 */
class BagFunction(
    val name: String,
    val arity: IntRange,
    val resultType: String? = null,
    val invoke: (List<Any?>) -> Any?,
) {
    override fun toString(): String = "$name(${describeArity(arity)})"
}

internal fun describeArity(r: IntRange): String = when {
    r.first == r.last -> "${r.first}"
    r.last == Int.MAX_VALUE -> "${r.first}+"
    else -> "${r.first}..${r.last}"
}
