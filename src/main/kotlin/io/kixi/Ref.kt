package io.kixi

import io.kixi.text.ParseException
import io.kixi.text.isKiIdentifier

/**
 * A reference to a node identified by an [Id], optionally qualified with the
 * name of the collection the node lives in.
 *
 * Where [Id] is identity for anything, a `Ref` is specifically a pointer: a
 * `Ref` value inside a document is a link to another node. Two refs are equal
 * when their ids are equal; the collection name is a hint for readers and for
 * stores that can use it to skip a lookup, and does not take part in equality
 * or ordering.
 *
 * ## Ki Literal Format
 * ```
 * .ref(compounds/01J9A0X5K2M7N3P8Q1R6T4V9AB)   // with collection
 * .ref(01J9A0X5K2M7N3P8Q1R6T4V9AB)             // id only
 * ```
 *
 * ## Usage
 * ```kotlin
 * val ref = Ref("compounds", id)
 * val parsed = Ref.parseLiteral(".ref(compounds/01J9A0X5K2M7N3P8Q1R6T4V9AB)")
 *
 * ref.collection        // "compounds"
 * ref.id                // the Id
 * println(ref)          // .ref(compounds/01J9A0X5K2M7N3P8Q1R6T4V9AB)
 * ```
 *
 * @property collection The collection name, a Ki identifier, or null
 * @property id The id of the referenced node
 * @see Id
 */
class Ref(
    val collection: String?,
    val id: Id
) : Comparable<Ref> {

    init {
        require(collection == null || collection.isKiIdentifier()) {
            "Ref collection must be a Ki identifier, got: '$collection'"
        }
    }

    /** Creates a ref with no collection. */
    constructor(id: Id) : this(null, id)

    /** True if this ref names its collection. */
    val hasCollection: Boolean get() = collection != null

    /** Returns a copy pointing at the same node with the given collection name. */
    fun withCollection(collection: String?): Ref = Ref(collection, id)

    /**
     * Returns the Ki literal representation:
     * `.ref(compounds/01J9A0X5K2M7N3P8Q1R6T4V9AB)` or `.ref(01J9A0X5K2M7N3P8Q1R6T4V9AB)`.
     */
    override fun toString(): String =
        if (collection != null) ".ref($collection/${id.toBase32()})" else ".ref(${id.toBase32()})"

    /** Equal when the ids are equal; the collection is not considered. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Ref) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    /** Orders by id; the collection is not considered. */
    override fun compareTo(other: Ref): Int = id.compareTo(other.id)

    companion object : Parseable<Ref> {
        private const val REF_PREFIX = ".ref("

        /**
         * Parses the bare form: `collection/ID` or `ID` (no `.ref(...)` wrapper).
         *
         * @throws ParseException if the text is not a valid Ref
         */
        @JvmStatic
        fun parse(text: String): Ref {
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                throw ParseException("Ref cannot be empty", index = 0)
            }
            val slash = trimmed.indexOf('/')
            if (slash == -1) {
                return Ref(null, Id.parse(trimmed))
            }
            val collection = trimmed.substring(0, slash).trim()
            val idText = trimmed.substring(slash + 1).trim()
            if (!collection.isKiIdentifier()) {
                throw ParseException("Ref collection must be a Ki identifier, got: '$collection'", index = 0)
            }
            if (idText.contains('/')) {
                throw ParseException("Ref may contain only one '/': '$trimmed'", index = slash + 1)
            }
            return Ref(collection, Id.parse(idText))
        }

        /**
         * Parses a Ki Ref literal: `.ref(compounds/01J9A0X5K2M7N3P8Q1R6T4V9AB)`.
         *
         * @throws ParseException if the text cannot be parsed as a valid Ref literal
         */
        override fun parseLiteral(text: String): Ref {
            val trimmed = text.trim()
            if (!trimmed.startsWith(REF_PREFIX)) {
                throw ParseException("Ref literal must start with '.ref('", index = 0)
            }
            if (!trimmed.endsWith(")")) {
                throw ParseException("Ref literal must end with ')'", index = trimmed.length - 1)
            }
            return parse(trimmed.substring(REF_PREFIX.length, trimmed.length - 1))
        }

        /**
         * Parses a bare or literal Ref, returning null on failure instead of throwing.
         */
        @JvmStatic
        fun parseOrNull(text: String): Ref? = try {
            val trimmed = text.trim()
            if (trimmed.startsWith(REF_PREFIX)) parseLiteral(trimmed) else parse(trimmed)
        } catch (e: Exception) {
            null
        }

        /**
         * Checks if a string appears to be a Ki Ref literal.
         * This is a quick structural check, not a full validation.
         */
        @JvmStatic
        fun isLiteral(text: String): Boolean {
            val trimmed = text.trim()
            return trimmed.startsWith(REF_PREFIX) && trimmed.endsWith(")")
        }
    }
}
