package io.kixi.text

import io.kixi.log
import java.util.*

/**
 * A set of convenience methods for CharSequences
 */

fun CharSequence.toList(
    delimiters: String = " \t",
    trim: Boolean = true,
    list: MutableList<String> = ArrayList<String>()
):
        List<String> {

    val st = StringTokenizer(this.toString(), delimiters)
    while (st.hasMoreTokens())
        list+=(if (trim) st.nextToken().trim() else st.nextToken())

    return list
}

fun CharSequence.countDigits(): Int {
    var i = 0
    for(c in this) {
        if(!c.isDigit()) return i
        i++
    }
    return i
}

fun CharSequence.countAlpha(): Int {
    var i = 0
    for(c in this) {
        if(!c.isLetter()) return i
        i++
    }
    return i
}

fun CharSequence.countAlphaNum(): Int {
    var i = 0
    for(c in this) {
        if(!c.isLetterOrDigit()) return i
        i++
    }
    return i
}

fun Char.unicodeEscape(): String {
    val cval = this.toInt()
    val buf = StringBuffer()
    buf.append("\\u")
    val ns = Integer.toHexString(cval)
    buf.append(ns.padStart(4, '0'))

    return buf.toString()
}

const val ESCAPE_CHARS = "\t\n\r\\"

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

        var c = this[index]

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
                    var hexDigits = this.substring(index, index+4)

                    try {
                        val intValue = hexDigits.toInt(16)
                        sb.append(intValue.toChar())
                    } catch(nfe:NumberFormatException) {
                        throw ParseException("Invalid char in unicode escape.", index, nfe)
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
 * Returns true for any unicode letter, '_', '$' or emoji.
 *
 * TODO: Fix - Currently only handles BMP emoji.
 */
fun Char.isKiIDStart(): Boolean = this.isLetter() || this=='_' || this=='$' ||
        this.isSurrogate()

/**
 * Returns true for any unicode letter, digit, '_', '$' or emoji.
 *
 * TODO: Fix - Currently only handles BMP emoji.
 */
fun Char.isKiIDChar(): Boolean = this.isKiIDStart() || this.isDigit() ||
        this.isSurrogate()

fun CharSequence.isKiIdentifier(): Boolean {
    if (this.isEmpty() || !this[0].isKiIDStart() || this.equals("_"))
        return false

    for(c in this) if(!c.isKiIDChar()) return false

    return true
}

val LINE_SEP: String = System.lineSeparator()!!