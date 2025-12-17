package io.kixi

import io.kixi.text.ParseException
import io.kixi.text.isKiIdentifier

/**
 * NSIDs are an ID (key identifier) with an optional namespace. They are used for
 * tag names and attributes. Anonymous tags all use the ANONYMOUS key.
 */
data class NSID(val name: String, val namespace: String = "") : Comparable<NSID> {

    companion object : Parseable<NSID> {
        /**
         * Used for anonymous tags
         */
        @JvmStatic
        val ANONYMOUS = NSID("", "")

        /**
         * Parse a "namespace:name" or "name" string into an NSID.
         *
         * @param text The string to parse
         * @return NSID The parsed NSID
         * @throws ParseException if the text is not a valid NSID
         */
        @JvmStatic
        fun parse(text: String): NSID {
            if (text.isEmpty()) return ANONYMOUS

            val colonIndex = text.indexOf(':')
            return if (colonIndex == -1) {
                NSID(text)
            } else {
                NSID(
                    name = text.substring(colonIndex + 1),
                    namespace = text.substring(0, colonIndex)
                )
            }
        }

        /**
         * Parses a Ki NSID literal string into an NSID instance.
         *
         * @param text The Ki NSID literal string to parse
         * @return The parsed NSID
         * @throws ParseException if the text cannot be parsed as a valid NSID
         */
        override fun parseLiteral(text: String): NSID = parse(text)

        /**
         * Parse an NSID literal, returning null on failure instead of throwing.
         *
         * @param text The NSID literal string
         * @return The parsed NSID, or null if parsing fails
         */
        @JvmStatic
        fun parseOrNull(text: String): NSID? = try {
            parse(text)
        } catch (e: Exception) {
            null
        }
    }

    init {
        if (namespace.isNotEmpty() && name.isEmpty())
            throw ParseException("Anonymous tags cannot have a namespace ($namespace).")

        // Removing the '.' is necessary to allow KD-style dot path style strings, which
        // are not valid KiIdentifiers
        if (name.isNotEmpty() && !name.replace(".", "").isKiIdentifier())
            throw ParseException("NSID name component '$name' is not a valid Ki Identifier.")
        if (namespace.isNotEmpty() && !namespace.isKiIdentifier())
            throw ParseException("NSID namespace component '$namespace' is not a valid Ki Identifier.")
    }

    /**
     * Returns true if this NSID is the anonymous NSID (empty name and namespace)
     */
    val isAnonymous: Boolean = name.isEmpty() && namespace.isEmpty()

    /**
     * Returns true if this NSID has a namespace
     */
    val hasNamespace: Boolean get() = namespace.isNotEmpty()

    override fun toString(): String = if (namespace.isNotEmpty()) "$namespace:$name" else name

    override fun compareTo(other: NSID): Int = toString().compareTo(other.toString())
}