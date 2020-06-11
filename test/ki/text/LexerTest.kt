package ki.text

import ki.test.Test

class LexerTest : Test() {
    override fun run() {
        banner("white space")
        var lex = Lexer("   Hello")
        lex.nextWhiteSpace()
        eq("untilEnd()", lex.untilEnd(),"Hello")
        isFalse("hasNextChar() at end", lex.hasNextChar())

        banner("peek ahead")
        lex = Lexer("Hello")
        eq("peekChar()", 'H', lex.peekChar())

        // System.err.println("testing fail behavior")
        // notEq("peekChar()", 'H', lex.peekChar())
        eq("peekChars(3)", "Hel", lex.peekChars(3))
        eq("peekChars(6) - safe peek past the end", "Hello", lex.peekChars(6))
    }
}