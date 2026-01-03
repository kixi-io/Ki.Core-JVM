package io.kixi

import io.kixi.text.ParseException
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * A Call is a [KTS](https://github.com/kixi-io/Ki.Docs/wiki/Ki-Types) type that
 * represents a function call, including indexed and named arguments, as data. Its core
 * components are the name (NSID), arguments (values) and named arguments (attributes).
 *
 * **Note:** Call parsing requires the full KD value parser and is implemented
 * in the Ki.KD library rather than KiCore. The [parseLiteral] method in KiCore
 * will throw a [ParseException] indicating this limitation.
 *
 * @property nsid NSID The name of the call, optionally with a namespace
 * @property values MutableList<Any?> The indexed arguments (values)
 * @property attributes MutableMap<NSID, Any?> The named arguments (attributes)
 * @property value Any? A calculated convenience property for the first argument (value)
 */
open class Call {

    var nsid: NSID

    /**
     * Convenience property that returns the name component of the NSID.
     * Equivalent to `nsid.name`.
     */
    val name: String get() = nsid.name

    /**
     * Convenience property that returns the namespace component of the NSID.
     * Equivalent to `nsid.namespace`.
     */
    val namespace: String get() = nsid.namespace

    // Backing fields for lazy initialization
    private var _values: MutableList<Any?>? = null
    private var _attributes: MutableMap<NSID, Any?>? = null

    /**
     * The indexed arguments (values). Lazily initialized on first access.
     */
    val values: MutableList<Any?>
        get() = _values ?: ArrayList<Any?>().also { _values = it }

    /**
     * The named arguments (attributes). Lazily initialized on first access.
     */
    val attributes: MutableMap<NSID, Any?>
        get() = _attributes ?: HashMap<NSID, Any?>().also { _attributes = it }

    // ===== NSID-based Constructors =====

    /**
     * Creates a Call with just an NSID (no values or attributes).
     *
     * @param nsid The namespaced identifier for this call
     */
    constructor(nsid: NSID) {
        this.nsid = nsid
    }

    /**
     * Creates a Call with an NSID and values (vararg).
     *
     * Example:
     * ```kotlin
     * Call(NSID("add"), 1, 2, 3)
     * Call(NSID("greet"), "Hello", "World")
     * ```
     *
     * @param nsid The namespaced identifier for this call
     * @param values The indexed arguments (values) for this call
     */
    constructor(nsid: NSID, vararg values: Any?) : this(nsid) {
        if (values.isNotEmpty()) this.values.addAll(values)
    }

    /**
     * Creates a Call with an NSID and attributes only.
     *
     * Example:
     * ```kotlin
     * Call(NSID("config"), mapOf(NSID("debug") to true, NSID("level") to 5))
     * ```
     *
     * @param nsid The namespaced identifier for this call
     * @param attributes The named arguments (attributes) for this call
     */
    constructor(nsid: NSID, attributes: Map<NSID, Any?>) : this(nsid) {
        if (attributes.isNotEmpty()) this.attributes.putAll(attributes)
    }

    /**
     * Creates a Call with an NSID, values list, and attributes map.
     *
     * Example:
     * ```kotlin
     * Call(NSID("create"), listOf("item", 5), mapOf(NSID("urgent") to true))
     * ```
     *
     * @param nsid The namespaced identifier for this call
     * @param values The indexed arguments (values) for this call
     * @param attributes The named arguments (attributes) for this call
     */
    constructor(nsid: NSID, values: List<Any?>, attributes: Map<NSID, Any?>) : this(nsid) {
        if (values.isNotEmpty()) this.values.addAll(values)
        if (attributes.isNotEmpty()) this.attributes.putAll(attributes)
    }

    // ===== String-based Constructors =====

    /**
     * Creates a Call with name, optional namespace, optional values, and optional attributes.
     *
     * Use named parameters for values and attributes to avoid ambiguity.
     *
     * Examples:
     * ```kotlin
     * Call("func")                                              // name only
     * Call("func", "ns")                                        // name and namespace
     * Call("func", values = listOf(1, 2))                       // name with values
     * Call("func", "ns", values = listOf(1, 2))                 // name, namespace, and values
     * Call("func", attributes = mapOf(NSID("key") to "val"))    // name with attributes
     * Call("func", values = listOf(1), attributes = mapOf(...)) // all parameters
     * ```
     *
     * @param name The name of the call
     * @param namespace The optional namespace (default: empty string)
     * @param values Optional list of indexed arguments
     * @param attributes Optional map of named arguments
     */
    constructor(
        name: String,
        namespace: String = "",
        values: List<Any?>? = null,
        attributes: Map<NSID, Any?>? = null
    ) : this(NSID(name, namespace)) {
        values?.let { if (it.isNotEmpty()) this.values.addAll(it) }
        attributes?.let { if (it.isNotEmpty()) this.attributes.putAll(it) }
    }

    /**
     * Returns true if this Call has any values.
     * Does not trigger lazy initialization.
     */
    fun hasValues(): Boolean = _values?.isNotEmpty() == true

    /**
     * Returns true if this Call has any attributes.
     * Does not trigger lazy initialization.
     */
    fun hasAttributes(): Boolean = _attributes?.isNotEmpty() == true

    /**
     * Returns the number of values, or 0 if none.
     * Does not trigger lazy initialization.
     */
    val valueCount: Int get() = _values?.size ?: 0

    /**
     * Returns the number of attributes, or 0 if none.
     * Does not trigger lazy initialization.
     */
    val attributeCount: Int get() = _attributes?.size ?: 0

    override fun toString(): String {
        if (!hasValues() && !hasAttributes())
            return "$nsid()"

        val builder = StringBuilder("$nsid(")

        // output values
        if (hasValues()) {
            val i: Iterator<*> = values.iterator()
            while (i.hasNext()) {
                builder.append(Ki.format(i.next()))
                if (i.hasNext()) {
                    builder.append(", ")
                }
            }
        }

        // output attributes
        if (hasAttributes()) {
            if (hasValues()) {
                builder.append(", ")
            }

            val i = attributes.entries.iterator()
            while (i.hasNext()) {
                val e = i.next()
                builder.append("${e.key}=${Ki.format(e.value)}")
                if (i.hasNext()) {
                    builder.append(", ")
                }
            }
        }

        return builder.append(')').toString()
    }

    // Values ////

    operator fun get(valueIndex: Int): Any? = values[valueIndex]
    operator fun set(valueIndex: Int, obj: Any?): Any? = values.set(valueIndex, obj)

    /**
     * Returns true if a value exists at the given index.
     */
    fun hasValue(index: Int): Boolean = index in 0..<valueCount

    /**
     * Gets a value at the given index, or returns the default if the index is out of bounds.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getValueOrDefault(index: Int, default: T): T =
        if (hasValue(index)) values[index] as T else default

    /**
     * Gets a value at the given index, or null if the index is out of bounds.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getValueOrNull(index: Int): T? =
        if (hasValue(index)) values[index] as T? else null

    // Attributes ////

    operator fun get(name: String, namespace: String = ""): Any? = getAttribute(name, namespace)
    operator fun set(name: String, namespace: String = "", value: Any?): Any? =
        setAttribute(name, namespace, value)

    operator fun get(nsid: NSID): Any? = _attributes?.get(nsid)
    operator fun set(nsid: NSID, value: Any?): Any? = setAttribute(nsid, value)

    fun setAttribute(name: String, namespace: String = "", value: Any?): Any? =
        attributes.put(NSID(name, namespace), value)

    @Suppress("UNCHECKED_CAST")
    fun <T> getAttribute(name: String, namespace: String = ""): T =
        attributes[NSID(name, namespace)] as T

    fun setAttribute(nsid: NSID, value: Any?): Any? = attributes.put(nsid, value)

    @Suppress("UNCHECKED_CAST")
    fun <T> getAttribute(nsid: NSID): T = attributes[nsid] as T

    /**
     * Returns true if an attribute exists with the given name (and optionally namespace).
     */
    fun hasAttribute(name: String, namespace: String = ""): Boolean =
        _attributes?.containsKey(NSID(name, namespace)) == true

    /**
     * Returns true if an attribute exists with the given NSID.
     */
    fun hasAttribute(nsid: NSID): Boolean = _attributes?.containsKey(nsid) == true

    /**
     * Gets an attribute value, or returns the default if not found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getAttributeOrDefault(name: String, default: T, namespace: String = ""): T {
        val nsid = NSID(name, namespace)
        return if (hasAttribute(nsid)) attributes[nsid] as T else default
    }

    /**
     * Gets an attribute value, or null if not found.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getAttributeOrNull(name: String, namespace: String = ""): T? {
        val nsid = NSID(name, namespace)
        return if (hasAttribute(nsid)) attributes[nsid] as T? else null
    }

    /**
     * Returns all attributes in the given namespace as a map of name to value.
     */
    fun <T> getAttributesInNamespace(namespace: String): Map<String, T> {
        if (_attributes == null) {
            // @Suppress("UNCHECKED_CAST")
            return emptyMap<String, T>()
        }

        val map = HashMap<String, Any?>()
        for (e in attributes.entries) {
            if (e.key.namespace == namespace)
                map[e.key.name] = e.value
        }
        @Suppress("UNCHECKED_CAST")
        return map as Map<String, T>
    }

    /**
     * Convenience method that gets or sets the first value in the value list. The getter
     * returns null if no values are present.
     */
    var value: Any?
        get() = if (valueCount == 0) null else values[0]
        set(_value) {
            if (valueCount == 0) {
                values.add(_value)
            } else {
                values[0] = _value
            }
        }

    // Fluent builders ////

    /**
     * Adds a value and returns this Call for chaining.
     */
    fun withValue(value: Any?): Call = apply { values.add(value) }

    /**
     * Adds multiple values and returns this Call for chaining.
     */
    fun withValues(vararg values: Any?): Call = apply {
        for (v in values) this.values.add(v)
    }

    /**
     * Sets an attribute and returns this Call for chaining.
     */
    fun withAttribute(name: String, namespace: String = "", value: Any?): Call = apply {
        setAttribute(name, namespace, value)
    }

    /**
     * Sets an attribute and returns this Call for chaining.
     */
    fun withAttribute(nsid: NSID, value: Any?): Call = apply {
        setAttribute(nsid, value)
    }

    /**
     * Returns true if this tag entity (including its values and attributes) is
     * equivalent to the given tag entity.
     *
     * @return true if the tag entities are equivalent
     */
    override fun equals(other: Any?): Boolean = other is Call && other.toString() == toString()

    /**
     * @return The hash (based on the output from toString())
     */
    override fun hashCode(): Int = toString().hashCode()

    companion object : Parseable<Call> {
        /**
         * Parses a Ki Call literal string into a Call instance.
         *
         * **Note:** Call parsing requires the full KD value parser which is implemented
         * in the Ki.KD library. This method in KiCore will always throw a ParseException.
         * Use the Ki.KD library for full Call parsing support.
         *
         * @param text The Ki Call literal string to parse
         * @return The parsed Call (never returns in KiCore)
         * @throws ParseException always, indicating that Call parsing requires Ki.KD
         */
        override fun parseLiteral(text: String): Call {
            throw ParseException(
                "Call parsing requires the full KD value parser. " +
                        "Use the Ki.KD library for Call parsing support."
            )
        }

        /**
         * Attempts to parse a Call literal, returning null on failure.
         *
         * **Note:** In KiCore, this always returns null since Call parsing requires Ki.KD.
         *
         * @param text The Ki Call literal string to parse
         * @return null (Call parsing requires Ki.KD)
         */
        @JvmStatic
        fun parseOrNull(text: String): Call? = try {
            parseLiteral(text)
        } catch (e: Exception) {
            null
        }
    }
}