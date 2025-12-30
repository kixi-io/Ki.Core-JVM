package io.kixi.text

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow

class TextExtensionsTest : FunSpec({

    context("countDigits") {
        test("counts leading digits") {
            "123abc".countDigits() shouldBe 3
        }

        test("returns 0 for no leading digits") {
            "abc123".countDigits() shouldBe 0
        }

        test("all digits") {
            "12345".countDigits() shouldBe 5
        }

        test("empty string") {
            "".countDigits() shouldBe 0
        }
    }

    context("countAlpha") {
        test("counts leading letters") {
            "abc123".countAlpha() shouldBe 3
        }

        test("returns 0 for no leading letters") {
            "123abc".countAlpha() shouldBe 0
        }

        test("all letters") {
            "abcde".countAlpha() shouldBe 5
        }
    }

    context("countAlphaNum") {
        test("counts leading alphanumeric") {
            "abc123!@#".countAlphaNum() shouldBe 6
        }

        test("stops at first non-alphanumeric") {
            "ab-cd".countAlphaNum() shouldBe 2
        }
    }

    context("unicodeEscape") {
        test("escape ASCII char") {
            'A'.unicodeEscape() shouldBe "\\u0041"
        }

        test("escape unicode char") {
            '日'.unicodeEscape() shouldBe "\\u65e5"
        }

        test("escape newline") {
            '\n'.unicodeEscape() shouldBe "\\u000a"
        }
    }

    context("escape") {
        test("escape newline") {
            "line1\nline2".escape() shouldBe "line1\\nline2"
        }

        test("escape tab") {
            "col1\tcol2".escape() shouldBe "col1\\tcol2"
        }

        test("escape carriage return") {
            "text\rhere".escape() shouldBe "text\\rhere"
        }

        test("escape backslash") {
            "path\\file".escape() shouldBe "path\\\\file"
        }

        test("escape double quote (default)") {
            "say \"hello\"".escape() shouldBe "say \\\"hello\\\""
        }

        test("escape single quote when specified") {
            "it's".escape('\'') shouldBe "it\\'s"
        }

        test("no escaping needed") {
            "normal text".escape() shouldBe "normal text"
        }

        test("multiple escapes") {
            "line1\nline2\ttab".escape() shouldBe "line1\\nline2\\ttab"
        }
    }

    context("resolveEscapes") {
        test("resolve newline") {
            "line1\\nline2".resolveEscapes() shouldBe "line1\nline2"
        }

        test("resolve tab") {
            "col1\\tcol2".resolveEscapes() shouldBe "col1\tcol2"
        }

        test("resolve carriage return") {
            "text\\rhere".resolveEscapes() shouldBe "text\rhere"
        }

        test("resolve backslash") {
            "path\\\\file".resolveEscapes() shouldBe "path\\file"
        }

        test("resolve double quote") {
            "say \\\"hello\\\"".resolveEscapes() shouldBe "say \"hello\""
        }

        test("resolve unicode escape") {
            "\\u0041".resolveEscapes() shouldBe "A"
        }

        test("resolve unicode Japanese char") {
            "\\u65e5".resolveEscapes() shouldBe "日"
        }

        test("throws on invalid escape") {
            shouldThrow<ParseException> {
                "\\x".resolveEscapes()
            }
        }

        test("throws on incomplete unicode") {
            shouldThrow<ParseException> {
                "\\u00".resolveEscapes()
            }
        }

        test("throws on invalid unicode hex") {
            shouldThrow<ParseException> {
                "\\uZZZZ".resolveEscapes()
            }
        }
    }

    context("isKiIDStart") {
        test("letters are valid starts") {
            'a'.isKiIDStart() shouldBe true
            'Z'.isKiIDStart() shouldBe true
        }

        test("underscore is valid start") {
            '_'.isKiIDStart() shouldBe true
        }

        test("dollar sign is NOT valid start (reserved for currency prefix)") {
            '$'.isKiIDStart() shouldBe false
        }

        test("unicode letters are valid") {
            '日'.isKiIDStart() shouldBe true
        }

        test("digits are not valid starts") {
            '5'.isKiIDStart() shouldBe false
        }
    }

    context("isKiIDChar") {
        test("letters are valid") {
            'a'.isKiIDChar() shouldBe true
        }

        test("digits are valid (not at start)") {
            '5'.isKiIDChar() shouldBe true
        }

        test("underscore is valid") {
            '_'.isKiIDChar() shouldBe true
        }

        test("dollar is valid") {
            '$'.isKiIDChar() shouldBe true
        }
    }

    context("isKiIdentifier") {
        test("simple identifier") {
            "myVar".isKiIdentifier() shouldBe true
        }

        test("with underscore") {
            "my_var".isKiIdentifier() shouldBe true
        }

        test("with digits") {
            "var123".isKiIdentifier() shouldBe true
        }

        test("starts with underscore") {
            "_private".isKiIdentifier() shouldBe true
        }

        test("starts with dollar is NOT valid (reserved for currency prefix)") {
            "\$special".isKiIdentifier() shouldBe false
        }

        test("dollar in middle is valid") {
            "my\$var".isKiIdentifier() shouldBe true
        }

        test("unicode identifier") {
            "日本語".isKiIdentifier() shouldBe true
        }

        test("empty string is not valid") {
            "".isKiIdentifier() shouldBe false
        }

        test("single underscore is not valid") {
            "_".isKiIdentifier() shouldBe false
        }

        test("starts with digit is not valid") {
            "123abc".isKiIdentifier() shouldBe false
        }

        test("contains space is not valid") {
            "my var".isKiIdentifier() shouldBe false
        }
    }

    context("upTo") {
        test("basic upTo with CharSequence") {
            "hello world".upTo("world") shouldBe "hello "
        }

        test("upTo with include=true") {
            "hello world".upTo("world", include = true) shouldBe "hello world"
        }

        test("upTo not found returns original") {
            "hello".upTo("xyz") shouldBe "hello"
        }

        test("upTo with Char") {
            "path/to/file".upTo('/') shouldBe "path"
        }

        test("upTo Char with include") {
            "path/to/file".upTo('/', include = true) shouldBe "path/"
        }
    }

    context("after") {
        test("basic after with CharSequence") {
            "hello world".after("hello ") shouldBe "world"
        }

        test("after with include=true") {
            "hello world".after("hello", include = true) shouldBe "hello world"
        }

        test("after not found returns empty") {
            "hello".after("xyz") shouldBe ""
        }

        test("after with Char") {
            "path/to/file".after('/') shouldBe "to/file"
        }

        test("after Char with include") {
            "path/to/file".after('/', include = true) shouldBe "/to/file"
        }
    }

    context("toList") {
        test("split by default delimiters (space and tab)") {
            "one two three".toList() shouldBe listOf("one", "two", "three")
        }

        test("split by custom delimiter") {
            "a,b,c".toList(",") shouldBe listOf("a", "b", "c")
        }

        test("with trim") {
            "  one   two  ".toList() shouldBe listOf("one", "two")
        }

        test("empty string") {
            "".toList() shouldBe emptyList()
        }
    }
})

class ParseExceptionTest : FunSpec({

    context("creation") {
        test("with message only") {
            val e = ParseException("Test error")
            e.message shouldBe "ParseException \"Test error\""
        }

        test("with line and index") {
            val e = ParseException("Error", line = 5, index = 10)
            e.message shouldBe "ParseException \"Error\" line: 5 index: 10"
        }

        test("with cause") {
            val cause = RuntimeException("Root cause")
            val e = ParseException("Error", cause = cause)
            e.message shouldBe "ParseException \"Error\" cause: Root cause"
        }

        test("line factory method") {
            val e = ParseException.line("Error", index = 15)
            e.message shouldBe "ParseException \"Error\" index: 15"
        }
    }

    context("toString") {
        test("toString returns message") {
            val e = ParseException("Test", line = 1, index = 0)
            e.toString() shouldBe e.message
        }
    }
})