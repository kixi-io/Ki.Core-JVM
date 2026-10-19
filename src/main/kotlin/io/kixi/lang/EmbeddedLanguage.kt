package io.kixi.lang

import io.kixi.KiException
import java.util.Locale
import java.util.ServiceConfigurationError
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap

/**
 * A language whose source can be written inside another Ki language, as
 * `lang KQ { ... }` is inside KS.
 *
 * The host language does not read the block itself. It asks the language to
 * [scan] the block, which says where the block ends and where its
 * interpolations are, so that the block's strings, comments and literals are
 * read by the language's own rules. The host then evaluates each
 * interpolation's expression in its own scope and calls [evaluate] with the
 * block's text, unchanged, and the values. The language decides what the
 * values mean. KQ (RevDB's query language), for example, binds each value as a
 * query parameter, so no value is ever spliced into the text.
 *
 * Implementations are found by [EmbeddedLanguages.find], either registered with
 * [EmbeddedLanguages.register] or discovered with [ServiceLoader]: list the
 * implementation class in `META-INF/services/io.kixi.lang.EmbeddedLanguage`. A
 * discovered implementation needs a public no-argument constructor.
 *
 * To report an error at a position in the block, throw an
 * [EmbeddedLanguageException]; the host translates its offset into a line and
 * column in its own source.
 */
interface EmbeddedLanguage {

    /** The name written after `lang`. Matched without regard to case. */
    val name: String

    /**
     * Finds the end of a block and its interpolations. [start] is the position in
     * [source] just after the block's opening `{`. All positions in the result,
     * and the offset of an [EmbeddedLanguageException] thrown here, are positions
     * in [source].
     *
     * The default is [EmbeddedText.scan], a generic scanner. A language whose
     * strings, comments or literals differ from it should scan with its own
     * lexer, so that the host and the language always agree on what is
     * interpolation and what is text.
     */
    fun scan(source: String, start: Int): EmbeddedBlock = EmbeddedText.scan(source, start)

    /**
     * Evaluates a block.
     *
     * @param source the block's text, exactly as written between its braces,
     *   interpolations included, and where it is in the host's source.
     * @param values the values of the block's interpolations, by
     *   [EmbeddedInterpolation.key].
     * @return the block's value in the host language. An
     *   [EmbeddedLanguageException] thrown here has an offset in `source.text`.
     */
    fun evaluate(source: EmbeddedSource, values: Map<String, Any?>): Any?
}

/**
 * The extent of a block found by [EmbeddedLanguage.scan]: [end] is the position of
 * the closing `}`, and [interpolations] are in order of appearance.
 */
data class EmbeddedBlock(val end: Int, val interpolations: List<EmbeddedInterpolation>)

/**
 * An interpolation in a block, from [start] (its `$`) to [end] (exclusive).
 * [expression] is the host-language source to evaluate, starting at
 * [expressionStart]: the name for `$name`, the text between the brackets for
 * `${expression}` and `$(expression)`. [key] identifies the value passed to
 * [EmbeddedLanguage.evaluate]: the name, or the expression trimmed.
 */
data class EmbeddedInterpolation(
    val start: Int,
    val end: Int,
    val key: String,
    val expression: String,
    val expressionStart: Int,
)

/**
 * A block's [text] and where it starts in the host's source: [file], [line] and
 * [column] (1-based) of the text's first character.
 */
data class EmbeddedSource @JvmOverloads constructor(
    val text: String,
    val line: Int = 1,
    val column: Int = 1,
    val file: String = "",
) {
    /** The host's 1-based line and column of [offset], a position in [text]. */
    fun locate(offset: Int): Pair<Int, Int> {
        var l = line
        var c = column
        for (k in 0 until offset.coerceIn(0, text.length)) {
            if (text[k] == '\n') { l++; c = 1 } else c++
        }
        return l to c
    }
}

/**
 * An error in an embedded language's block at [offset], a 0-based position in the
 * text the language was given (`-1` when the position is unknown). [reason] is
 * the message without position information; the host adds its own.
 */
open class EmbeddedLanguageException @JvmOverloads constructor(
    open val reason: String,
    open val offset: Int,
    message: String = reason,
    cause: Throwable? = null,
) : KiException(message, null, cause)

/**
 * The generic block scanner, the default for [EmbeddedLanguage.scan]. Braces
 * nest. These are skipped, so a brace or `$` inside them is text:
 * strings (`"…"` and `` `…` ``, which may span lines, `"""…"""`, `@"…"`; `\`
 * escapes only in `"…"`), a `'…'` quote on one line (an apostrophe with no
 * partner on its line is plain text), URL literals (`<scheme://…>`) and comments
 * (`//`, `/* */`).
 *
 * Interpolations: `$name` (a letter or `_`, then letters, digits and `_`),
 * `${expression}` and `$(expression)`, whose brackets are matched skipping
 * strings. `$` followed by anything else, a digit for example (`$5`, a currency
 * literal), is text.
 */
object EmbeddedText {

    @JvmStatic
    fun scan(source: String, start: Int): EmbeddedBlock {
        val out = ArrayList<EmbeddedInterpolation>()
        var depth = 0
        var i = start
        while (true) {
            if (i >= source.length) throw EmbeddedLanguageException("Unterminated block: expected '}'", start - 1)
            val c = source[i]
            when {
                c == '}' -> { if (depth == 0) return EmbeddedBlock(i, out); depth--; i++ }
                c == '{' -> { depth++; i++ }
                c == '/' && at(source, i + 1) == '/' -> { while (i < source.length && source[i] != '\n') i++ }
                c == '/' && at(source, i + 1) == '*' -> {
                    val e = source.indexOf("*/", i + 2)
                    if (e < 0) throw EmbeddedLanguageException("Unterminated comment", i)
                    i = e + 2
                }
                c == '<' && isUrlStart(source, i) -> {
                    val e = source.indexOf('>', i)
                    i = if (e < 0) source.length else e + 1
                }
                c == '$' -> i = interpolation(source, i, out)
                else -> i = skipQuoted(source, i) ?: (i + 1)
            }
        }
    }

    /** Reads the interpolation at [start] (a `$`) into [out]; returns the position after it. */
    private fun interpolation(source: String, start: Int, out: MutableList<EmbeddedInterpolation>): Int {
        val next = at(source, start + 1)
        return when {
            next == '{' || next == '(' -> {
                val close = if (next == '{') '}' else ')'
                val end = closing(source, start + 1, next, close)
                    ?: throw EmbeddedLanguageException("Unterminated interpolation: expected '$close'", start)
                val expression = source.substring(start + 2, end)
                if (expression.isBlank()) throw EmbeddedLanguageException("Empty interpolation", start)
                out.add(EmbeddedInterpolation(start, end + 1, expression.trim(), expression, start + 2))
                end + 1
            }
            next != null && (next.isLetter() || next == '_') -> {
                var end = start + 2
                while (end < source.length && (source[end].isLetterOrDigit() || source[end] == '_')) end++
                val name = source.substring(start + 1, end)
                out.add(EmbeddedInterpolation(start, end, name, name, start + 1))
                end
            }
            else -> start + 1
        }
    }

    /**
     * The position of the bracket that closes the one at [open], skipping nested
     * brackets and strings, or null. Used for an interpolation's brackets: the
     * text inside is the host language's expression.
     */
    @JvmStatic
    fun closing(source: String, open: Int, openChar: Char, closeChar: Char): Int? {
        var depth = 0
        var i = open
        while (i < source.length) {
            val c = source[i]
            when (c) {
                openChar -> depth++
                closeChar -> { depth--; if (depth == 0) return i }
            }
            i = skipQuoted(source, i) ?: (i + 1)
        }
        return null
    }

    /** If a string or quote starts at [i], the position after it; otherwise null. */
    private fun skipQuoted(source: String, i: Int): Int? {
        val c = source[i]
        return when {
            c == '"' && source.startsWith("\"\"\"", i) -> {
                val e = source.indexOf("\"\"\"", i + 3)
                if (e < 0) throw EmbeddedLanguageException("Unterminated string", i)
                e + 3
            }
            c == '@' && at(source, i + 1) == '"' -> closeQuote(source, i + 2, '"', escapes = false) ?: throw EmbeddedLanguageException("Unterminated string", i)
            c == '"' -> closeQuote(source, i + 1, '"', escapes = true) ?: throw EmbeddedLanguageException("Unterminated string", i)
            c == '`' -> closeQuote(source, i + 1, '`', escapes = false) ?: throw EmbeddedLanguageException("Unterminated string", i)
            c == '\'' -> {
                var k = i + 1
                while (k < source.length && source[k] != '\'' && source[k] != '\n') { if (source[k] == '\\') k++; k++ }
                if (k < source.length && source[k] == '\'') k + 1 else null   // unpaired: plain text
            }
            else -> null
        }
    }

    private fun closeQuote(source: String, from: Int, quote: Char, escapes: Boolean): Int? {
        var k = from
        while (k < source.length) {
            val c = source[k]
            if (escapes && c == '\\') { k += 2; continue }
            if (c == quote) return k + 1
            k++
        }
        return null
    }

    private fun isUrlStart(source: String, i: Int): Boolean {
        var k = i + 1
        while (k < source.length && source[k].isLetter()) k++
        return k > i + 1 && source.startsWith("://", k)
    }

    private fun at(source: String, i: Int): Char? = if (i < source.length) source[i] else null
}

/**
 * The embedded languages available to host languages: those registered with
 * [register], then those discovered through [ServiceLoader] from the calling
 * thread's context class loader and from Ki.Core's own class loader.
 */
object EmbeddedLanguages {

    private val registered = ConcurrentHashMap<String, EmbeddedLanguage>()

    /**
     * Discovered languages, per class loader. Like other ServiceLoader caches, this
     * keeps each loader's providers, and so the loader, for the life of the process.
     */
    private val discovered = HashMap<ClassLoader, Map<String, EmbeddedLanguage>>()

    /** Makes [language] available by its name, replacing any language of the same name. */
    @JvmStatic
    fun register(language: EmbeddedLanguage) {
        registered[key(language.name)] = language
    }

    /** Removes a language added with [register]. Discovered languages are not affected. */
    @JvmStatic
    fun unregister(name: String) {
        registered.remove(key(name))
    }

    /** The language called [name] (case is ignored), or `null`. */
    @JvmStatic
    fun find(name: String): EmbeddedLanguage? {
        val k = key(name)
        registered[k]?.let { return it }
        for (loader in loaders()) discover(loader)[k]?.let { return it }
        return null
    }

    /** The names of every available language, in upper case. */
    @JvmStatic
    fun names(): Set<String> = (registered.keys + loaders().flatMap { discover(it).keys }).toSortedSet()

    private fun key(name: String) = name.uppercase(Locale.ROOT)

    private fun loaders(): List<ClassLoader> =
        listOfNotNull(Thread.currentThread().contextClassLoader, EmbeddedLanguage::class.java.classLoader).distinct()

    /**
     * The languages [loader] provides. A provider that cannot be loaded (a
     * missing class, a bad services entry) is skipped, so one broken library does
     * not hide the others.
     */
    private fun discover(loader: ClassLoader): Map<String, EmbeddedLanguage> {
        synchronized(discovered) { discovered[loader]?.let { return it } }
        val found = LinkedHashMap<String, EmbeddedLanguage>()
        val it = ServiceLoader.load(EmbeddedLanguage::class.java, loader).iterator()
        var failures = 0
        while (failures < 100) {
            try {
                if (!it.hasNext()) break
                val language = it.next()
                found.putIfAbsent(key(language.name), language)
            } catch (_: ServiceConfigurationError) {
                failures++
            }
        }
        synchronized(discovered) { discovered.putIfAbsent(loader, found) }
        return found
    }
}
