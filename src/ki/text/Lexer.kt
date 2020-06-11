package ki.text

import ki.log
import java.io.BufferedReader
import java.io.Reader

// Todo single quote strings to handle double quotes
open class Lexer {

    companion object {
        val DIGITS = "0123456789"
        val DIGITS_NONZERO = "123456789"

        val WS = " \t\r"
        val WS_WITH_NL = " \t\r\n"

        val WS_MATCHER = CharMatcher(WS)
        val WS_WITH_NL_MATCHER = CharMatcher(WS_WITH_NL)
    }

    var line = 0
    var index = 0 // position in the current line
    var position = 0 // position in the code

    var currentChar = '\u0000'
    var lastChar = '\u0000'

    var text = StringBuilder()

    constructor(text: CharSequence) {
        this.text.append(text)
    }

    constructor(reader: Reader) {
        val breader = if (reader is BufferedReader) reader else BufferedReader(reader)
        try {
            text.append(breader.readText());
        } finally {
            breader.close();
        }
    }

    fun nextChar(): Char {
        lastChar = currentChar.apply {}

        var c = '\u0000'

        if (hasNextChar()) {
            c = text[position]
            position++
            index++

            if (c == '\n') {
                index = 0
                line++
            }
        }

        currentChar = c
        return c
    }

    fun hasNextChar(): Boolean {
        return position<text.size;
    }

    fun nextChars(count: Int = 1): String {
        var tok = StringBuilder()
        for (i in 1..count) {
            tok.append(nextChar())
        }
        return tok.toString()
    }

    fun peekChar(): Char {
        return if (hasNextChar()) text[position] else '\u0000'
    }

    fun peekChars(count:Int = 1): String {
        var start = position
        var end = position+count

        if(end>text.size) end=text.size

        return text.substring(start, end)
    }

    fun startsWith(string:CharSequence): Boolean {
        return text.startsWith(string, position)
    }

    ////
    /**
     * Peeks forward by the length of the match, if any. Returns the number of chars matched, or -1 if none were
     * matched.
     */
    fun peekMatch(matcher:Matcher): Int {
        var endIndex = matcher.match(text, position)
        if(endIndex==-1) {
            return -1;
        } else {
            return endIndex - position;
        }
    }

    fun skipChar() {

        var c = '\u0000'

        if(hasNextChar()) {
            c = text[position]
            position++
            index++

            if(c=='\n') {
                index = 0
                line++
            }
        }
    }

    fun skipChars(count:Int = 1) {
        var iteration = 0;
        while(iteration < count && hasNextChar()) {
            skipChar()
            iteration++
        }
    }

    ////

    /**
     * Finds the first occurrence of text in the remainder of the stream and returns the index or -1 if text is not
     * found. This method does not affect position.
     */
    fun find(string:String): Int {
        return text.indexOf(string, position);
    }

    /**
     * Finds and returns the space between the current position and first occurrence of text in the remainder of the
     * stream or null if no match is found. This method moves the position forward to the beginning of the matching
     * String.
     *
     * @return The space between the current position and first occurrence of text in the remainder of the stream or
     *         the null if no occurence is found.
     */
    fun until(string:String): String? {
        var start = position;
        var stop = find(string)

        if(stop==-1)
            return null

        var i = start.apply{}

        var token = StringBuilder()

        while(i<stop) {
            token.append(nextChar())
            i++
        }

        return token.toString()
    }

    /**
     * Read until the end of the Stream. This method updates the position.
     */
    fun untilEnd(): String {
        val sb = StringBuilder()
        while(hasNextChar()) {
            sb.append(nextChar())
        }
        return sb.toString()
    }

    /**
     * Finds the first occurrence of c in the remainder of the stream and returns the index.
     * This method does not affect position.
     */
    fun find(char:Char): Int {
        return text.indexOf(char, position);
    }

    /**
     * Finds and returns the space between the current position and first occurrence of char in the remainder of the
     * stream or null if no match is found. This method moves the position forward to the beginning of the matching
     * char.
     *
     * @return The space between the current position and first occurrence of char in the remainder of the stream or
     *         null if no occurance is found.
     */
    fun until(char:Char): String? {
        var start = position;
        var stop = find(char)

        if(stop==-1)
            return null

        var i = start.apply{}

        var token = StringBuilder()

        while(i<stop) {
            token.append(nextChar())
            i++
        }

        return token.toString()
    }

    /**
     * Finds the first occurrence of and char in chars within the remainder of the stream and returns the index.
     * This method does not affect position.
     */
    fun findAny(chars:String): Int {
        return text.indexOfAny(chars.toCharArray(), position);
    }

    /**
     * Finds and returns the chars between the current position and first occurrence of a char in chars within the
     * remainder of the stream or null if no match is found. This method moves the position forward to the beginning of
     * the matching char.
     *
     * @return The space between the current position and first occurrence of char in the remainder of the stream or
     *         null if no occurance is found.
     */
    fun untilAny(chars:String): String? {
        var start = position;
        var stop = findAny(chars)

        if(stop==-1)
            return null

        var i = start.apply{}

        var token = StringBuilder()

        while(i<stop) {
            token.append(nextChar())
            i++
        }

        return token.toString()
    }

    ////

    /**
     * Consumes whitespace and returns the whitespace sans '\r'. If newlineIsWS==false the '\n' is terminal
     * and is not included in the String. This method appropriately updates line, index and position.
     *
     * @param newLineIsWS Defaults to false.
     */
    fun nextWhiteSpace(newLineIsWS:Boolean = false): String {
        val wsChars = if(newLineIsWS) WS_WITH_NL else WS

        var sb = StringBuilder()
        while(hasNextChar()) {
            var c = peekChar()

            if (!wsChars.contains(c)) {
                return sb.toString();
            } else if(c=='\r') {
                skipChar()
            } else {
                sb.append(nextChar())
            }
        }

        return sb.toString()
    }

    // Strings - Double and Triple quotes, regular and raw (@"", @""") ///////////////////////////////////

    /**
     * Parses quoted strings including raw strings starting with '@' and triple quoted string blocks.
     *
     * @returns the String content with the enclosing quotes and prefixed '@' for literal strings removed.
     *          Indents for triple quoted strings strip all whiteSpace before the closing triple quote.
     */
    fun nextString() : String {
        var c = peekChar();

        var raw = false

        if(c=='@') {
            skipChar()
            raw = true
            c = peekChar()
        }

        if(startsWith("\"\"\"")) {
            return if(raw) nextRawStringBlock() else nextStringBlock()
        } else if(peekChar()=='"') {
            return if(raw) nextRawQuoteString() else nextQuoteString()
        } else {
            log("peeking:'${peekChar()}' line:${line} index:${index+1}")
            throw ParseException("String must be prefixed with \", \"\"\", @\" or @\"\"\"", line=line, index=index)
        }
    }

    private fun nextQuoteString() : String {
        skipChar()

        var sb = StringBuilder()

        var escape = false
        while(hasNextChar()) {
            var c = nextChar()

            if(c=='\\') {
                if (escape) {
                    sb.append(c)
                    escape = false
                } else {
                    escape = true
                }
            } else if(c=='"') {
                if(escape) {
                    sb.append(c)
                    escape = false
                } else {
                    return sb.toString()
                }
            } else if(escape){
                sb.append(when(c) {
                    't' -> '\t'
                    'n' -> '\n'
                    'r' -> '\r'
                    else -> throw ParseException("String format exception: '$c' is not an allowed escape character.",
                        line=line, index=index+1)
                })
                escape = false
            } else {
                sb.append(c)
            }
        }
        throw ParseException("Unterminated double quote string.", line=line, index=index);
    }

    private fun nextRawQuoteString() : String {
        skipChar()

        var sb = StringBuilder()
        // var escape = false

        while(hasNextChar()) {
            var c = nextChar()
            if(c=='"') {
                return sb.toString()
            } else {
                sb.append(c)
            }
        }
        throw ParseException("Unterminated double quote raw string.", line=line, index=index);
    }

    /**
     * Returns the content of a triple quoted string with escaped characters resolved.
     */
    private fun nextStringBlock() : String {
        skipChars(3)

        var string = until("\"\"\"")
        if(string == null)
            throw ParseException("Unterminated triple quote String block.", line=line, index=index)

        // Skip ending triple quotes
        skipChars(3)

        // TODO - Handle escape chars

        var lines = string.lines()

        // handleEscapesInStringBlockLine(lines)

        if(lines.size == 1)
            return resolveEscapes(string)

        if (lines[0].isEmpty()) {
            lines = lines.subList(1, lines.size)

            if(lines.size == 1)
                return resolveEscapes(string)
        }

        var lastLine = lines.last()
        lines = lines.dropLast(1)

        if(lastLine.isBlank()) {
            var i = 0
            val newList = ArrayList<String>()
            for (line in lines) {
                if (i < line.size - 1) {
                    if (line.startsWith(lastLine)) {
                        newList.add(line.substring(lastLine.size))
                    } else {
                        newList.add(line)
                    }
                }
            }

            return resolveEscapes(newList.joinToString("\n"))
            // return newList.joinToString("\n")
        }  else {
            return resolveEscapes(lines.joinToString("\n"))
            // return lines.joinToString("\n")
        }
    }

    private fun resolveEscapes(string:String, quoteChar:Char = '"'): String {
        var escape = false
        var sb = StringBuilder()

        for(c in string) {
            if(escape) {
                sb.append(
                    when(c) {
                        'n' -> '\n'
                        't' -> '\t'
                        'r' -> '\r'
                        '\\' -> '\\'
                        quoteChar -> quoteChar
                        else -> throw ParseException("Invalid escape character '$c'", line=line,
                            index=index+1)
                    })
                escape = false
            } else if(c=='\\') {
                escape = true
            } else {
                sb.append(c)
            }
        }

        return sb.toString();
    }

    /**
     * Returns the content of a triple quoted string with escapes included as written.
     */
    private fun nextRawStringBlock() : String {
        skipChars(3)

        var string = until("\"\"\"")
        if(string == null)
            throw ParseException("Unterminated String block.", line=line, index=index)

        // Skip ending triple quotes
        skipChars(3)

        var lines = string.lines()

        if(lines.size == 1)
            return string

        if (lines[0].isEmpty()) {
            lines = lines.subList(1, lines.size)

            if(lines.size == 1)
                return string
        }

        var lastLine = lines.last()
        lines = lines.dropLast(1)

        if(lastLine.isBlank()) {
            var i = 0
            val newList = ArrayList<String>()
            for (line in lines) {
                if (i < line.size - 1) {
                    if (line.startsWith(lastLine)) {
                        newList.add(line.substring(lastLine.size))
                    } else {
                        newList.add(line)
                    }
                }
            }

            return newList.joinToString("\n")
        }  else {
            return lines.joinToString("\n")
        }
    }


}

fun main() {
    var lex = Lexer("""
            Hello there
            Bill!
        """.trimIndent())

    while(lex.hasNextChar()) {
        var nextChar = "" + lex.nextChar()
        if(nextChar=="\n") nextChar = "\\n"

        log("$nextChar line:${lex.line}, index:${lex.index-1}, pos:${lex.position-1} next:${lex.peekChar()}")
    }
    log()

    log("--- Checking line, index (line pos) and position ---")
    lex = Lexer("abc\n123\n\n1\n")
    while(lex.hasNextChar()) {
        var text = "" + lex.nextChar();
        if(text.equals("\n")) text = "\\n"

        var nextChar = "" + lex.peekChar();
        if(nextChar.equals("\n")) nextChar = "\\n"

        var lastChar = "" + lex.lastChar;
        if(lastChar.equals("\n")) lastChar = "\\n"

        log( "$text line:${lex.line} index:${lex.index} - pos:${lex.position} - next:$nextChar - " +
                "lastChar:$lastChar")
    }

    log()
    log("--- Consume white space ---")
    lex = Lexer("   abc")
    lex.nextWhiteSpace()
    log(lex.peekChar())
    log(lex.peekChars(3))
    log(lex.peekChars(5))

    log()
    log("--- Skipping chars ---")
    lex = Lexer("   abc")
    log("before skipChar: pos=${lex.position}")
    lex.skipChar()
    log("after skipChar: pos=${lex.position}")
    log()
    log("before skipChars(2): pos=${lex.position}")
    lex.skipChars(2)
    log("after skipChars(2): pos=${lex.position}")
    log(lex.peekChars(2)) // should give us "ab"
    log()
    lex = Lexer(" \t\na")
    var NLisWS = lex.nextWhiteSpace(true).escape();
    log("skip(NLisWS=true) \" \\t\\na\": \"${NLisWS}\"")

    lex = Lexer(" \t\na")
    var NLisNotWS = lex.nextWhiteSpace(false).escape();
    log("skip(NLisWS=false) \" \\t\\na\": \"${NLisNotWS}\"")

    log()
    log("--- Using Matchers ---")
    lex = Lexer("123abc")
    var matchSize = lex.peekMatch(RegexMatcher("[\\d+]"))
    log("[\\d+] match size: $matchSize")
    log(lex.peekChars(matchSize))
    log()
    log("--- Double Quotes Strings ---")
    lex = Lexer("\"quoted string\"");
    log("\"${lex.nextString()}\"")
    lex = Lexer("\" quoted string w/ space prefix\"");
    log("\"${lex.nextString()}\"")
    lex = Lexer("\"quoted string w/ space suffix \"");
    log("\"${lex.nextString()}\"")
    lex = Lexer("\"You can escape \\\"hi\\\" ...\"");
    log("\"${lex.nextString()}\"")

    lex = Lexer(java.io.FileReader("/Users/danielleuck/etc/line.txt"))
    lex.nextWhiteSpace()
    log("\"${lex.nextString()}\"")
    lex.nextWhiteSpace(true)
    log("\"${lex.nextString()}\"")

    lex = Lexer(java.io.FileReader("/Users/danielleuck/etc/line2.txt"))
    log("\"${lex.nextString()}\"")
    lex.nextWhiteSpace(true)
    log("\"${lex.nextString()}\"")
    lex.nextWhiteSpace(true)
    log("\"${lex.nextString()}\"")
    lex.nextWhiteSpace(true)
    log("\"${lex.nextString()}\"")
    log()

    // untilString

    log("--- Find & Until ---")
    lex = Lexer(" abc 123 _:@")
    log("index of '_:' ${lex.find("_:")}")
    log(lex.until("_:") ?: "not found")
    log(lex.nextChar())
    log()

    log("--- String Blocks")

    log("- String block")
    lex = Lexer(java.io.FileReader("/Users/danielleuck/etc/line4.txt"))
    log(lex.nextString())
    log(lex.nextChar())


    log("- Raw string block")
    lex = Lexer(java.io.FileReader("/Users/danielleuck/etc/line3.txt"))
    log(lex.nextString())
    log(lex.nextChar())
}