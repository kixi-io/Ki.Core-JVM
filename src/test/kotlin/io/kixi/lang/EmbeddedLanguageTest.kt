package io.kixi.lang

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/** Discovered through META-INF/services in the test resources. */
class EchoLanguage : EmbeddedLanguage {
    override val name: String = "Echo"
    override fun evaluate(source: EmbeddedSource, values: Map<String, Any?>): Any? = source.text to values
}

class EmbeddedLanguageTest : FunSpec({

    context("finding languages") {
        test("a language listed in META-INF/services is discovered by name without regard to case, past a broken entry") {
            EmbeddedLanguages.find("echo").shouldBeInstanceOf<EchoLanguage>()
            EmbeddedLanguages.find("ECHO")!!.evaluate(EmbeddedSource("a \$x"), mapOf("x" to 1)) shouldBe ("a \$x" to mapOf("x" to 1))
            EmbeddedLanguages.names() shouldContain "ECHO"
            EmbeddedLanguages.find("nope") shouldBe null
        }

        test("a registered language is found, takes precedence over a discovered one, and can be removed") {
            val upper = object : EmbeddedLanguage {
                override val name = "echo"
                override fun evaluate(source: EmbeddedSource, values: Map<String, Any?>) = source.text.uppercase()
            }
            EmbeddedLanguages.register(upper)
            EmbeddedLanguages.find("Echo")!!.evaluate(EmbeddedSource("abc"), emptyMap()) shouldBe "ABC"
            EmbeddedLanguages.unregister("ECHO")
            EmbeddedLanguages.find("echo").shouldBeInstanceOf<EchoLanguage>()
        }

        test("an EmbeddedLanguageException carries the reason and the offset; EmbeddedSource locates offsets in the host") {
            val e = EmbeddedLanguageException("Unknown name 'q'", 7, "Unknown name 'q' (line 1, column 8)")
            e.reason shouldBe "Unknown name 'q'"
            e.offset shouldBe 7
            e.message shouldBe "Unknown name 'q' (line 1, column 8)"
            EmbeddedLanguageException("x", -1).message shouldBe "x"
            EmbeddedSource("ab\ncd", line = 4, column = 10).locate(4) shouldBe (5 to 2)
            EmbeddedSource("ab\ncd", line = 4, column = 10).locate(1) shouldBe (4 to 11)
        }
    }

    context("the generic scanner") {
        fun scan(body: String): Pair<String, List<String>> {
            val src = "lang X {$body} tail"
            val b = EmbeddedText.scan(src, 8)
            return src.substring(8, b.end) to b.interpolations.map { it.key }
        }

        test("braces nest; the block ends at the unmatched closing brace") {
            scan(" a { b } c ").first shouldBe " a { b } c "
        }

        test("\$name, \${expr} and \$(expr) with their keys, expressions and positions; \$ before a digit is text") {
            val src = "lang X { \$a + \${ b + 1 } \$(c) \$5 }"
            val b = EmbeddedText.scan(src, 8)
            b.interpolations.map { it.key } shouldBe listOf("a", "b + 1", "c")
            b.interpolations[1].expression shouldBe " b + 1 "
            src.substring(b.interpolations[1].start, b.interpolations[1].end) shouldBe "\${ b + 1 }"
            src.substring(b.interpolations[1].expressionStart).startsWith(" b + 1") shouldBe true
            src.substring(b.interpolations[0].expressionStart, b.interpolations[0].end) shouldBe "a"
        }

        test("strings, quotes, comments and URLs are text: braces and \$ inside them do not count") {
            scan(" \"}\$a\" `}\$b` '}' @\"}\" \"\"\"\n}\$c\n\"\"\" // } \$d\n /* } \$e */ \$f ").second shouldBe listOf("f")
            scan(" c.url == <https://x.com/a> and c.mw < \$n ").second shouldBe listOf("n")
        }

        test("a \"…\" string may span lines; a backtick string has no escapes") {
            scan(" \"a\n}b\" \$x ").second shouldBe listOf("x")
            scan(" `\\` \$x ").second shouldBe listOf("x")
            scan(" don't \$x ").second shouldBe listOf("x")
        }

        test("an interpolation's brackets are matched skipping strings, not comments") {
            scan(" \${ n // x } ").second shouldBe listOf("n // x")
            scan(" \${ m[\"}\"] } ").second shouldBe listOf("m[\"}\"]")
        }

        test("unterminated blocks, strings and interpolations are errors at their position") {
            shouldThrow<EmbeddedLanguageException> { EmbeddedText.scan("lang X { a", 8) }.offset shouldBe 7
            shouldThrow<EmbeddedLanguageException> { EmbeddedText.scan("lang X { \"a }", 8) }.reason shouldContain "Unterminated string"
            shouldThrow<EmbeddedLanguageException> { EmbeddedText.scan("lang X { \${ a }", 8) }.reason shouldContain "Unterminated"
        }
    }
})
