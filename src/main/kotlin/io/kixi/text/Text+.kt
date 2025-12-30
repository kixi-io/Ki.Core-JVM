@file:Suppress("unused")

package io.kixi.text

import java.util.*

/**
 * Tokenizes this CharSequence into a list of strings using the specified delimiters.
 *
 * @param delimiters Characters to use as token separators (default: space and tab)
 * @param trim Whether to trim whitespace from each token (default: true)
 * @param list The mutable list to populate with tokens (default: new ArrayList)
 * @return List of tokens extracted from the CharSequence
 */
fun CharSequence.toList(
    delimiters: String = " \t",
    trim: Boolean = true,
    list: MutableList<String> = ArrayList<String>()
): List<String> {
    val st = StringTokenizer(this.toString(), delimiters)
    while (st.hasMoreTokens())
        list+=(if (trim) st.nextToken().trim() else st.nextToken())

    return list
}

/**
 * Counts the number of consecutive digits from the start of this CharSequence.
 *
 * @return The count of leading digit characters
 */
fun CharSequence.countDigits(): Int {
    var i = 0
    for(c in this) {
        if(!c.isDigit()) return i
        i++
    }
    return i
}

/**
 * Counts the number of consecutive letters from the start of this CharSequence.
 *
 * @return The count of leading letter characters
 */
@Suppress("unused")
fun CharSequence.countAlpha(): Int {
    var i = 0
    for(c in this) {
        if(!c.isLetter()) return i
        i++
    }
    return i
}

/**
 * Counts the number of consecutive alphanumeric characters from the start of this CharSequence.
 *
 * @return The count of leading letter or digit characters
 */
fun CharSequence.countAlphaNum(): Int {
    var i = 0
    for(c in this) {
        if(!c.isLetterOrDigit()) return i
        i++
    }
    return i
}

/**
 * Converts this character to a Unicode escape sequence (e.g., `\u0041` for 'A').
 *
 * @return The Unicode escape string representation
 */
fun Char.unicodeEscape(): String {
    val buf = StringBuffer()
    buf.append("\\u")
    val ns = Integer.toHexString(this.code)
    buf.append(ns.padStart(4, '0'))

    return buf.toString()
}

/** Characters that require escaping in Ki strings: tab, newline, carriage return, and backslash. */
const val ESCAPE_CHARS = "\t\n\r\\"

/**
 * Escapes special characters in this string for use in Ki literals.
 *
 * Converts control characters and the quote character to their escape sequences:
 * - `\t` for tab
 * - `\n` for newline
 * - `\r` for carriage return
 * - `\\` for backslash
 * - The quote character is also escaped
 *
 * @param quoteChar The quote character to escape (default: double quote)
 * @return The escaped string
 */
fun String.escape(quoteChar: Char = '"'): String {

    val sb = StringBuilder()
    val escapeChars = ESCAPE_CHARS + quoteChar

    for(c in this) {
        if(escapeChars.contains(c) || c==quoteChar) {
            sb.append("\\")

            when(c) {
                '\n' -> sb.append('n')
                '\t' -> sb.append('t')
                '\r' -> sb.append('r')
                '\\' -> sb.append('\\')
                quoteChar -> sb.append(quoteChar)
            }
        } else sb.append(c)
    }

    return sb.toString()
}

/**
 * Resolve escapes within a string. For example, the text `\t` will be converted into a
 * tab. This also handles unicode escapes in the form `\uxxxx`, where `x` is a
 * hexidecimal digit.
 *
 * @param quoteChar The quote char being used to define this String, so we know to escape
 * it.
 */
fun String.resolveEscapes(quoteChar: Char? = '"'): String {
    var escape = false
    val sb = StringBuilder()

    var index = 0

    outer@ while(index<length) {

        val c = this[index]

        if(escape) {
            when(c) {
                't' -> sb.append('\t')
                'r' -> sb.append('\r')
                'n' -> sb.append('\n')
                'u' -> {
                    if (this.length < index + 5)
                        throw ParseException(
                            """Unicode escape requires four hexidecimal
                            digits. Got ${this.substring(index)}"""
                        )
                    index++
                    val hexDigits = this.substring(index, index+4)

                    try {
                        val intValue = hexDigits.toInt(16)
                        sb.append(intValue.toChar())
                    } catch(nfe:NumberFormatException) {
                        throw ParseException.line("Invalid char in unicode escape.", index, nfe)
                    }
                    index+=4
                    escape = false
                    continue@outer
                }
                '\\' -> sb.append('\\')
                quoteChar -> sb.append(quoteChar)
                else -> throw ParseException("Invalid escape character '$c'", index = index)
            }
            escape = false
        } else if(c=='\\') {
            escape = true
        } else {
            sb.append(c)
        }
        index++
    }

    return if(quoteChar==null) sb.toString() else
        sb.toString().replace("\\$quoteChar", "$quoteChar")
}

// What we really want is this.isLetter() || this=='_' || this=='$' ||
// this.isEmojiSurrogate(), but I haven't put together the ranges for the latter, so this
// works for now. It handles Unicode BMP emoji.

/**
 * Returns true for any unicode letter, '_', or emoji.
 * Note: '$' is NOT allowed at the start of identifiers to avoid
 * ambiguity with currency prefix notation ($100, €50, etc.)
 *
 * TODO: Fix - Currently only handles BMP emoji.
 */
fun Char.isKiIDStart(): Boolean = this.isLetter() || this=='_' ||
        this.isSurrogate()

/**
 * Returns true for any unicode letter, digit, '_', '$' or emoji.
 * Note: '$' is allowed in identifiers, just not at the start.
 *
 * TODO: Fix - Currently only handles BMP emoji.
 */
fun Char.isKiIDChar(): Boolean = this.isKiIDStart() || this.isDigit() ||
        this == '$' || this.isSurrogate()

/**
 * Checks whether this CharSequence is a valid Ki identifier.
 *
 * A valid Ki identifier must:
 * - Be non-empty
 * - Start with a letter, underscore, or emoji
 * - Contain only letters, digits, underscores, dollar signs, or emoji
 * - Not be a single underscore (reserved)
 *
 * Note: '$' can appear anywhere in an identifier except the first position,
 * to avoid ambiguity with currency prefix notation.
 *
 * @return true if this is a valid Ki identifier
 */
fun CharSequence.isKiIdentifier(): Boolean {
    if (this.isEmpty() || !this[0].isKiIDStart() || this == "_")
        return false

    for(c in this) if(!c.isKiIDChar()) return false

    return true
}

// upTo and after are very useful String functions.
// TODO: support regex

/**
 * Returns the substring from the start up to the first occurrence of [stop].
 * @param stop The CharSequence to search for
 * @param include If true, includes [stop] at the end of the result
 * @return The substring, or the original string if [stop] is not found
 */
fun String.upTo(stop: CharSequence, include: Boolean = false): String {
    val index = indexOf(stop.toString())
    return when {
        index == -1 -> this
        include -> substring(0, index + stop.length)
        else -> substring(0, index)
    }
}

/**
 * Returns the substring after the first occurrence of [start].
 * @param start The CharSequence to search for
 * @param include If true, includes [start] at the beginning of the result
 * @return The substring, or an empty string if [start] is not found
 */
fun String.after(start: CharSequence, include: Boolean = false): String {
    val index = indexOf(start.toString())
    return when {
        index == -1 -> ""
        include -> substring(index)
        else -> substring(index + start.length)
    }
}

/**
 * Returns the substring from the start up to the first occurrence of [stop].
 * @param stop The Char to search for
 * @param include If true, includes [stop] at the end of the result
 * @return The substring, or the original string if [stop] is not found
 */
fun String.upTo(stop: Char, include: Boolean = false): String {
    val index = indexOf(stop)
    return when {
        index == -1 -> this
        include -> substring(0, index + 1)
        else -> substring(0, index)
    }
}

/**
 * Returns the substring after the first occurrence of [start].
 * @param start The Char to search for
 * @param include If true, includes [start] at the beginning of the result
 * @return The substring, or an empty string if [start] is not found
 */
fun String.after(start: Char, include: Boolean = false): String {
    val index = indexOf(start)
    return when {
        index == -1 -> ""
        include -> substring(index)
        else -> substring(index + 1)
    }
}

/** The platform-specific line separator. */
val LINE_SEP: String = System.lineSeparator()!!