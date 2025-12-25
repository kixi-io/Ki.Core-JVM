package io.kixi

import io.kixi.text.ParseException
import io.kixi.text.resolveEscapes

/**
 * Utility object for parsing Ki string literals.
 *
 * Ki supports four types of string literals:
 *
 * ## 1. Basic String (`"..."`)
 * Standard double-quoted strings with escape sequence processing.
 * - Supports: `\n`, `\t`, `\r`, `\\`, `\"`, `\uXXXX`
 * - Line continuation: backslash at end of line joins lines
 *
 * ```kotlin
 * "Hello, World!"
 * "line1\nline2"
 * "This is a long string broken \
 * into two lines."
 * ```
 *
 * ## 2. Raw String (`` `...` ``)
 * Backtick-quoted strings with NO escape processing.
 * - Only `` \` `` is processed to allow literal backticks
 * - All other backslashes are kept as-is
 *
 * ```kotlin
 * `C:\path\to\file`
 * `regex: \d+\.\d+`
 * `contains a \` backtick`
 * ```
 *
 * ## 3. Multiline String (`"""..."""`)
 * Triple double-quoted strings for multi-line content with escape processing.
 * - Supports all standard escape sequences
 * - Swift-style indentation stripping based on closing delimiter position
 * - Line continuation with trailing backslash
 * - Escape triple quotes with `\"""`
 *
 * ```kotlin
 * """
 * ABC
 *     def
 * 123
 * """
 * ```
 *
 * The indentation is determined by the position of the closing `"""`.
 * Content lines have that many leading spaces/tabs stripped.
 *
 * ## 4. Raw Multiline String (``` ```...``` ```)
 * Triple backtick-quoted strings for multi-line content with NO escape processing.
 * - Only ``` \``` ``` is processed to allow literal triple backticks
 * - Swift-style indentation stripping based on closing delimiter position
 *
 * ```kotlin
 * ```
 * {
 *     "json": "data",
 *     "path": "C:\Windows\System32"
 * }
 * ```
 * ```
 *
 * @see Ki.parse
 */
object Strings {

    /**
     * Parses a Ki string literal and returns the string value.
     *
     * Automatically detects the literal type based on delimiters:
     * - `"""..."""` - Multiline string (with escapes)
     * - ``` ```...``` ``` - Raw multiline string
     * - `"..."` - Basic string (with escapes)
     * - `` `...` `` - Raw string
     *
     * @param text The complete string literal including delimiters
     * @return The parsed string value
     * @throws ParseException if the literal is malformed
     */
    @JvmStatic
    fun parse(text: String): String {
        if (text.isEmpty()) {
            throw ParseException("String literal cannot be empty")
        }

        return when {
            text.startsWith("\"\"\"") -> parseMultilineString(text)
            text.startsWith("```") -> parseRawMultilineString(text)
            text.startsWith("\"") -> parseBasicString(text)
            text.startsWith("`") -> parseRawString(text)
            else -> throw ParseException("Invalid string literal: must start with \", `, \"\"\", or ```")
        }
    }

    /**
     * Parses a basic double-quoted string literal.
     *
     * Supports:
     * - Escape sequences: `\n`, `\t`, `\r`, `\\`, `\"`, `\uXXXX`, `\0`
     * - Line continuation: `\` at end of line joins lines
     *
     * @param text The string literal including quotes
     * @return The parsed string value
     * @throws ParseException if the literal is malformed
     */
    @JvmStatic
    fun parseBasicString(text: String): String {
        val trimmed = text.trim()

        if (trimmed.length < 2) {
            throw ParseException("Basic string literal must be at least 2 characters (empty string \"\")")
        }

        if (!trimmed.startsWith("\"")) {
            throw ParseException("Basic string literal must start with double quote")
        }

        if (!trimmed.endsWith("\"")) {
            throw ParseException("Basic string literal must end with double quote")
        }

        // Check for triple quote (multiline) - delegate to multiline parser
        if (trimmed.startsWith("\"\"\"")) {
            return parseMultilineString(trimmed)
        }

        val content = trimmed.substring(1, trimmed.length - 1)

        // Process line continuation first
        val continuedContent = processLineContinuation(content)

        // Then resolve escapes
        return continuedContent.resolveEscapes('"')
    }

    /**
     * Parses a raw backtick-quoted string literal.
     *
     * No escape processing except for `` \` `` to include literal backticks.
     *
     * @param text The string literal including backticks
     * @return The parsed string value
     * @throws ParseException if the literal is malformed
     */
    @JvmStatic
    fun parseRawString(text: String): String {
        val trimmed = text.trim()

        if (trimmed.length < 2) {
            throw ParseException("Raw string literal must be at least 2 characters (empty string ``)")
        }

        if (!trimmed.startsWith("`")) {
            throw ParseException("Raw string literal must start with backtick")
        }

        if (!trimmed.endsWith("`")) {
            throw ParseException("Raw string literal must end with backtick")
        }

        // Check for triple backtick (raw multiline) - delegate to raw multiline parser
        if (trimmed.startsWith("```")) {
            return parseRawMultilineString(trimmed)
        }

        val content = trimmed.substring(1, trimmed.length - 1)

        // Only process escaped backticks, nothing else
        return content.replace("\\`", "`")
    }

    /**
     * Parses a multiline triple double-quoted string literal.
     *
     * Supports:
     * - All standard escape sequences
     * - Swift-style indentation stripping
     * - Line continuation with trailing backslash
     * - Escaped triple quotes: `\"""`
     *
     * Indentation stripping: The common leading whitespace (based on the closing `"""`)
     * is removed from all content lines.
     *
     * @param text The string literal including triple quotes
     * @return The parsed string value
     * @throws ParseException if the literal is malformed
     */
    @JvmStatic
    fun parseMultilineString(text: String): String {
        val trimmed = text.trim()

        if (!trimmed.startsWith("\"\"\"")) {
            throw ParseException("Multiline string must start with triple double-quote")
        }

        // Find the closing triple quote, accounting for escaped triple quotes
        val closingIndex = findClosingTripleQuote(trimmed, 3, '"')
        if (closingIndex == -1) {
            throw ParseException("Multiline string must end with triple double-quote")
        }

        // Extract content between the triple quotes
        var content = trimmed.substring(3, closingIndex)

        // Handle the case where opening """ is followed by newline
        if (content.startsWith("\n")) {
            content = content.substring(1)
        } else if (content.startsWith("\r\n")) {
            content = content.substring(2)
        }

        // Process indentation stripping (Swift-style)
        content = stripIndentation(content, trimmed, closingIndex)

        // Process line continuation
        content = processLineContinuation(content)

        // Process escaped triple quotes: \""" -> """
        content = content.replace("\\\"\"\"", "\"\"\"")

        // Resolve other escape sequences
        return resolveMultilineEscapes(content)
    }

    /**
     * Parses a raw multiline triple backtick-quoted string literal.
     *
     * Supports:
     * - NO escape processing (except ``` \``` ``` for literal triple backticks)
     * - Swift-style indentation stripping
     *
     * @param text The string literal including triple backticks
     * @return The parsed string value
     * @throws ParseException if the literal is malformed
     */
    @JvmStatic
    fun parseRawMultilineString(text: String): String {
        val trimmed = text.trim()

        if (!trimmed.startsWith("```")) {
            throw ParseException("Raw multiline string must start with triple backtick")
        }

        // Find the closing triple backtick
        val closingIndex = findClosingTripleQuote(trimmed, 3, '`')
        if (closingIndex == -1) {
            throw ParseException("Raw multiline string must end with triple backtick")
        }

        // Extract content between the triple backticks
        var content = trimmed.substring(3, closingIndex)

        // Handle the case where opening ``` is followed by newline
        if (content.startsWith("\n")) {
            content = content.substring(1)
        } else if (content.startsWith("\r\n")) {
            content = content.substring(2)
        }

        // Process indentation stripping (Swift-style)
        content = stripIndentation(content, trimmed, closingIndex)

        // Only process escaped triple backticks: \``` -> ```
        content = content.replace("\\```", "```")

        return content
    }

    /**
     * Finds the closing triple quote, accounting for escaped triple quotes.
     *
     * Rules:
     * - `\"""` (exactly 3 quotes after backslash) in middle of string is escaped triple, skip 4
     * - `\` followed by 6+ quotes: `\"""` + `"""` (escaped triple + closing), skip 4
     * - `\` followed by 4-5 quotes: `\"` + `"""` (escaped single + closing), skip 2
     * - `\` followed by 1-2 quotes: `\"` or `\""` (escaped quotes), skip 2
     */
    private fun findClosingTripleQuote(text: String, startIndex: Int, quoteChar: Char): Int {
        var index = startIndex
        while (index <= text.length - 3) {
            val c = text[index]

            if (c == '\\') {
                // Check what follows the backslash
                if (index + 1 < text.length && text[index + 1] == quoteChar) {
                    // Count how many consecutive quotes follow the backslash
                    var quoteCount = 0
                    var pos = index + 1
                    while (pos < text.length && text[pos] == quoteChar) {
                        quoteCount++
                        pos++
                    }

                    // Decide whether to interpret as \""" or \"
                    // If quoteCount == 3: it's \""" (escaped triple in middle of string)
                    // If quoteCount >= 6: \""" + remaining (enough for closing)
                    // If quoteCount == 4 or 5: \" + remaining (not enough for \""" + closing)
                    if (quoteCount == 3 || (quoteCount > 3 && quoteCount - 3 >= 3)) {
                        // Use \""" interpretation, skip 4
                        index += 4
                    } else {
                        // Use \" interpretation, skip 2
                        index += 2
                    }
                } else if (index + 1 < text.length && text[index + 1] == '\\') {
                    // \\ - escaped backslash, skip 2
                    index += 2
                } else {
                    // Other escape like \n, \t, etc - skip 2
                    index += 2
                }
                continue
            }

            // Check for unescaped triple quote
            if (c == quoteChar &&
                text[index + 1] == quoteChar &&
                text[index + 2] == quoteChar) {
                return index
            }

            index++
        }

        return -1
    }

    /**
     * Strips common leading indentation from multiline string content.
     *
     * The indentation is determined by the position of the closing delimiter.
     * This follows Swift's multiline string literal behavior.
     */
    private fun stripIndentation(content: String, fullText: String, closingIndex: Int): String {
        if (content.isEmpty()) return content

        // Find the indentation of the closing delimiter
        // Look backwards from closingIndex to find the start of the line
        var lineStart = closingIndex - 1
        while (lineStart >= 0 && fullText[lineStart] != '\n') {
            lineStart--
        }
        lineStart++ // Move past the newline (or to 0 if no newline found)

        // Calculate the indentation (spaces/tabs before the closing delimiter)
        val indentation = fullText.substring(lineStart, closingIndex)

        // If the closing delimiter is on its own line with only whitespace before it
        if (indentation.isNotEmpty() && indentation.all { it == ' ' || it == '\t' }) {
            val indentLength = indentation.length

            // Split content into lines and remove the common indentation
            val lines = content.split("\n")
            val strippedLines = lines.mapIndexed { index, line ->
                when {
                    // Don't strip from empty lines
                    line.isEmpty() -> ""
                    // Last line is just whitespace before closing delimiter - remove it
                    index == lines.size - 1 && line.all { it == ' ' || it == '\t' } -> ""
                    // Strip the common indentation if present
                    line.length >= indentLength && line.substring(0, indentLength) == indentation -> {
                        line.substring(indentLength)
                    }
                    else -> {
                        // Try to strip as much as matches
                        var commonLen = 0
                        for (i in 0 until minOf(indentLength, line.length)) {
                            if (line[i] == indentation[i]) commonLen++ else break
                        }
                        if (commonLen > 0) line.substring(commonLen) else line
                    }
                }
            }

            // Rejoin, but remove the trailing empty line (from the closing delimiter line)
            val result = strippedLines.dropLastWhile { it.isEmpty() && strippedLines.isNotEmpty() }
            return if (result.isEmpty()) "" else result.joinToString("\n")
        }

        // If closing delimiter is not on its own indented line, just return content as-is
        // But remove trailing newline if present (the line with closing delimiter)
        return content.trimEnd('\n', '\r')
    }

    /**
     * Processes line continuation: removes backslash followed by newline and leading whitespace.
     *
     * Example: `"Hello \\\n   World"` becomes `"Hello World"`
     */
    private fun processLineContinuation(content: String): String {
        val sb = StringBuilder()
        var i = 0

        while (i < content.length) {
            if (content[i] == '\\' && i + 1 < content.length) {
                val nextChar = content[i + 1]

                // Check for line continuation: \ followed by newline
                if (nextChar == '\n') {
                    i += 2
                    // Skip leading whitespace on the continued line
                    while (i < content.length && (content[i] == ' ' || content[i] == '\t')) {
                        i++
                    }
                    continue
                } else if (nextChar == '\r' && i + 2 < content.length && content[i + 2] == '\n') {
                    i += 3
                    // Skip leading whitespace on the continued line
                    while (i < content.length && (content[i] == ' ' || content[i] == '\t')) {
                        i++
                    }
                    continue
                }
            }

            sb.append(content[i])
            i++
        }

        return sb.toString()
    }

    /**
     * Resolves escape sequences in multiline string content.
     *
     * This is similar to resolveEscapes but handles multiline content specially,
     * preserving actual newlines in the content.
     */
    private fun resolveMultilineEscapes(content: String): String {
        val sb = StringBuilder()
        var i = 0

        while (i < content.length) {
            if (content[i] == '\\' && i + 1 < content.length) {
                val nextChar = content[i + 1]
                var handled = true

                when (nextChar) {
                    'n' -> { sb.append('\n'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    'r' -> { sb.append('\r'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    '"' -> { sb.append('"'); i += 2 }
                    '0' -> { sb.append('\u0000'); i += 2 }
                    'u' -> {
                        if (i + 5 < content.length) {
                            val hexDigits = content.substring(i + 2, i + 6)
                            try {
                                val intValue = hexDigits.toInt(16)
                                sb.append(intValue.toChar())
                                i += 6
                            } catch (e: NumberFormatException) {
                                throw ParseException("Invalid unicode escape: \\u$hexDigits", index = i)
                            }
                        } else {
                            throw ParseException("Incomplete unicode escape at index $i", index = i)
                        }
                    }
                    else -> { handled = false }
                }

                if (handled) continue
            }

            sb.append(content[i])
            i++
        }

        return sb.toString()
    }

    /**
     * Checks if a string is a valid string literal (quick structural check).
     */
    @JvmStatic
    fun isStrings(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false

        return when {
            trimmed.startsWith("\"\"\"") -> trimmed.endsWith("\"\"\"") && trimmed.length >= 6
            trimmed.startsWith("```") -> trimmed.endsWith("```") && trimmed.length >= 6
            trimmed.startsWith("\"") -> trimmed.endsWith("\"") && trimmed.length >= 2
            trimmed.startsWith("`") -> trimmed.endsWith("`") && trimmed.length >= 2
            else -> false
        }
    }

    /**
     * Determines the type of string literal.
     */
    @JvmStatic
    fun literalType(text: String): StringsType {
        val trimmed = text.trim()
        return when {
            trimmed.startsWith("\"\"\"") -> StringsType.MULTILINE
            trimmed.startsWith("```") -> StringsType.RAW_MULTILINE
            trimmed.startsWith("\"") -> StringsType.BASIC
            trimmed.startsWith("`") -> StringsType.RAW
            else -> throw ParseException("Not a valid string literal")
        }
    }
}

/**
 * Enum representing the four types of Ki string literals.
 */
enum class StringsType {
    /** Basic double-quoted string with escape processing */
    BASIC,
    /** Raw backtick-quoted string without escape processing */
    RAW,
    /** Multiline triple double-quoted string with escape processing */
    MULTILINE,
    /** Raw multiline triple backtick-quoted string without escape processing */
    RAW_MULTILINE
}