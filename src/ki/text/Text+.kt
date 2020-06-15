package ki.text

import java.util.*

import ki.log

fun CharSequence.toList(delimiters:String = " \t", trim:Boolean = true, list:MutableList<String> = ArrayList<String>()):
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

fun CharSequence.isKiIdentifier(): Boolean {
    if (this.isEmpty() || !this[0].isKiIDStart() || this.equals("_"))
        return false

    return true
}

val CharSequence.size: Int get() = this.length;

fun Char.unicodeEscape(): String {
    val cval = this.toInt()
    val buf = StringBuffer()
    buf.append("\\u")
    val ns = Integer.toHexString(cval)
    buf.append(ns.padStart(4, '0'))

    return buf.toString()
}

const val ESCAPE_CHARS = "\t\n\r\\"

fun String.escape(quoteChar:Char = '"'): String {

    var sb = StringBuilder()
    var escapeChars = ESCAPE_CHARS + quoteChar

    for(c in this) {
       if(escapeChars.contains(c)) {
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

// TODO: Needs to handle unicode escapes
fun String.resolveEscapes(quoteChar:Char = '"'): String {
    var escape = false
    var sb = StringBuilder()

    for(c in this) {
        if(escape) {
            sb.append(
                when(c) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    '\\' -> sb.append('\\')
                    quoteChar -> sb.append(quoteChar)
                    else -> throw ParseException("Invalid escape character '$c'")
                })
            escape = false
        } else if(c=='\\') {
            escape = true
            continue
        } else {
            sb.append(c)
        }
    }
    return sb.toString()
}

// What we really want is  this.isLetter() || this=='_' || this.isEmojiSurrogate, but
// I haven't put together the ranges for the latter, so this works for now.

/**
 * Returns true for any unicode letter, '_', '$' or emoji surrogate.
 */
fun Char.isKiIDStart(): Boolean = this.isLetter() || this=='_' || this=='$' ||
        this.isSurrogate()

/**
 * Returns true for any unicode letter, digit, '_', '$' or emoji surrogate.
 */
fun Char.isKiIDChar(): Boolean = this.isKiIDStart() || this.isDigit()

