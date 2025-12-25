package io.kixi

import io.kixi.text.ParseException
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow

/**
 * Comprehensive tests for Ki String Literal parsing.
 *
 * Ki supports four types of string literals:
 * 1. Basic String ("...") - with escape processing
 * 2. Raw String (`...`) - without escape processing
 * 3. Multiline String ("""...""") - with escape processing and indentation stripping
 * 4. Raw Multiline String (```...```) - without escapes, with indentation stripping
 */
class StringTest : FunSpec({

    // =========================================================================
    // Basic String Tests ("...")
    // =========================================================================

    context("Basic String - simple cases") {
        test("empty string") {
            io.kixi.Strings.parseBasicString("\"\"") shouldBe ""
        }

        test("simple string") {
            Strings.parseBasicString("\"hello\"") shouldBe "hello"
        }

        test("string with spaces") {
            Strings.parseBasicString("\"hello world\"") shouldBe "hello world"
        }

        test("string with numbers") {
            Strings.parseBasicString("\"abc123\"") shouldBe "abc123"
        }

        test("string with special characters") {
            Strings.parseBasicString("\"!@#\$%^&*()\"") shouldBe "!@#\$%^&*()"
        }

        test("string with unicode characters") {
            Strings.parseBasicString("\"日本語\"") shouldBe "日本語"
        }
    }

    context("Basic String - escape sequences") {
        test("escaped newline") {
            Strings.parseBasicString("\"line1\\nline2\"") shouldBe "line1\nline2"
        }

        test("escaped tab") {
            Strings.parseBasicString("\"col1\\tcol2\"") shouldBe "col1\tcol2"
        }

        test("escaped carriage return") {
            Strings.parseBasicString("\"text\\rhere\"") shouldBe "text\rhere"
        }

        test("escaped backslash") {
            Strings.parseBasicString("\"path\\\\file\"") shouldBe "path\\file"
        }

        test("escaped double quote") {
            Strings.parseBasicString("\"say \\\"hello\\\"\"") shouldBe "say \"hello\""
        }

        test("unicode escape - ASCII") {
            Strings.parseBasicString("\"\\u0041\"") shouldBe "A"
        }

        test("unicode escape - Japanese") {
            Strings.parseBasicString("\"\\u65e5\"") shouldBe "日"
        }

        test("unicode escape - emoji") {
            Strings.parseBasicString("\"\\u263A\"") shouldBe "☺"
        }

        test("multiple escape sequences") {
            Strings.parseBasicString("\"\\t\\n\\r\\\\\"") shouldBe "\t\n\r\\"
        }

        test("mixed content with escapes") {
            Strings.parseBasicString("\"Hello\\nWorld\\t!\"") shouldBe "Hello\nWorld\t!"
        }
    }

    context("Basic String - line continuation") {
        test("simple line continuation") {
            Strings.parseBasicString("\"hello \\\nworld\"") shouldBe "hello world"
        }

        test("line continuation with Windows line ending") {
            Strings.parseBasicString("\"hello \\\r\nworld\"") shouldBe "hello world"
        }

        test("line continuation strips leading whitespace") {
            Strings.parseBasicString("\"hello \\\n    world\"") shouldBe "hello world"
        }

        test("line continuation with tabs") {
            Strings.parseBasicString("\"hello \\\n\t\tworld\"") shouldBe "hello world"
        }

        test("multiple line continuations") {
            Strings.parseBasicString("\"one \\\ntwo \\\nthree\"") shouldBe "one two three"
        }

        test("line continuation at beginning") {
            Strings.parseBasicString("\"\\\nhello\"") shouldBe "hello"
        }
    }

    context("Basic String - error cases") {
        test("throws on missing closing quote") {
            shouldThrow<ParseException> {
                Strings.parseBasicString("\"hello")
            }
        }

        test("throws on missing opening quote") {
            shouldThrow<ParseException> {
                Strings.parseBasicString("hello\"")
            }
        }

        test("throws on invalid escape") {
            shouldThrow<ParseException> {
                Strings.parseBasicString("\"\\x\"")
            }
        }

        test("throws on incomplete unicode escape") {
            shouldThrow<ParseException> {
                Strings.parseBasicString("\"\\u00\"")
            }
        }

        test("throws on invalid unicode hex") {
            shouldThrow<ParseException> {
                Strings.parseBasicString("\"\\uZZZZ\"")
            }
        }

        test("throws on empty input") {
            shouldThrow<ParseException> {
                Strings.parseBasicString("")
            }
        }
    }

    // =========================================================================
    // Raw String Tests (`...`)
    // =========================================================================

    context("Raw String - simple cases") {
        test("empty raw string") {
            Strings.parseRawString("``") shouldBe ""
        }

        test("simple raw string") {
            Strings.parseRawString("`hello`") shouldBe "hello"
        }

        test("raw string with spaces") {
            Strings.parseRawString("`hello world`") shouldBe "hello world"
        }
    }

    context("Raw String - no escape processing") {
        test("backslash n is literal") {
            Strings.parseRawString("`\\n`") shouldBe "\\n"
        }

        test("backslash t is literal") {
            Strings.parseRawString("`\\t`") shouldBe "\\t"
        }

        test("backslash backslash is literal") {
            Strings.parseRawString("`\\\\`") shouldBe "\\\\"
        }

        test("Windows path") {
            Strings.parseRawString("`C:\\Users\\name\\file.txt`") shouldBe "C:\\Users\\name\\file.txt"
        }

        test("regex pattern") {
            Strings.parseRawString("`\\d+\\.\\d+`") shouldBe "\\d+\\.\\d+"
        }

        test("unicode escape is literal") {
            Strings.parseRawString("`\\u0041`") shouldBe "\\u0041"
        }
    }

    context("Raw String - escaped backtick") {
        test("escaped backtick") {
            Strings.parseRawString("`contains \\` backtick`") shouldBe "contains ` backtick"
        }

        test("multiple escaped backticks") {
            Strings.parseRawString("`\\` and \\` backticks`") shouldBe "` and ` backticks"
        }

        test("escaped backtick at start") {
            Strings.parseRawString("`\\`start`") shouldBe "`start"
        }

        test("escaped backtick at end") {
            Strings.parseRawString("`end\\``") shouldBe "end`"
        }
    }

    context("Raw String - error cases") {
        test("throws on missing closing backtick") {
            shouldThrow<ParseException> {
                Strings.parseRawString("`hello")
            }
        }

        test("throws on missing opening backtick") {
            shouldThrow<ParseException> {
                Strings.parseRawString("hello`")
            }
        }
    }

    // =========================================================================
    // Multiline String Tests ("""...""")
    // =========================================================================

    context("Multiline String - simple cases") {
        test("empty multiline string") {
            Strings.parseMultilineString("\"\"\"\"\"\"") shouldBe ""
        }

        test("single line content") {
            Strings.parseMultilineString("\"\"\"hello\"\"\"") shouldBe "hello"
        }

        test("multiline content") {
            val input = "\"\"\"\nline1\nline2\n\"\"\""
            val expected = "line1\nline2"
            Strings.parseMultilineString(input) shouldBe expected
        }

        test("content with leading newline") {
            val input = "\"\"\"\nhello\n\"\"\""
            Strings.parseMultilineString(input) shouldBe "hello"
        }
    }

    context("Multiline String - indentation stripping") {
        test("strips common indentation (2 spaces)") {
            val input = """
                |""${'"'}
                |  ABC
                |      def
                |  123
                |  ""${'"'}
            """.trimMargin()
            val expected = "ABC\n    def\n123"
            Strings.parseMultilineString(input) shouldBe expected
        }

        test("strips common indentation (4 spaces)") {
            val input = """
                |""${'"'}
                |    line1
                |    line2
                |    ""${'"'}
            """.trimMargin()
            val expected = "line1\nline2"
            Strings.parseMultilineString(input) shouldBe expected
        }

        test("strips tab indentation") {
            val input = "\"\"\"\n\tline1\n\tline2\n\t\"\"\""
            val expected = "line1\nline2"
            Strings.parseMultilineString(input) shouldBe expected
        }

        test("preserves extra indentation") {
            val input = """
                |""${'"'}
                |  base
                |    indented
                |  base
                |  ""${'"'}
            """.trimMargin()
            val expected = "base\n  indented\nbase"
            Strings.parseMultilineString(input) shouldBe expected
        }

        test("handles mixed indentation levels") {
            val input = """
                |""${'"'}
                |    ABC
                |        def
                |            ghi
                |    123
                |    ""${'"'}
            """.trimMargin()
            val expected = "ABC\n    def\n        ghi\n123"
            Strings.parseMultilineString(input) shouldBe expected
        }

        test("handles empty lines in content") {
            val input = "\"\"\"\n  line1\n\n  line2\n  \"\"\""
            val expected = "line1\n\nline2"
            Strings.parseMultilineString(input) shouldBe expected
        }
    }

    context("Multiline String - escape sequences") {
        test("escaped newline") {
            val input = "\"\"\"hello\\nworld\"\"\""
            Strings.parseMultilineString(input) shouldBe "hello\nworld"
        }

        test("escaped tab") {
            val input = "\"\"\"col1\\tcol2\"\"\""
            Strings.parseMultilineString(input) shouldBe "col1\tcol2"
        }

        test("escaped backslash") {
            val input = "\"\"\"path\\\\file\"\"\""
            Strings.parseMultilineString(input) shouldBe "path\\file"
        }

        test("escaped double quote") {
            val input = "\"\"\"say \\\"hi\\\"\"\"\""
            Strings.parseMultilineString(input) shouldBe "say \"hi\""
        }

        test("unicode escape") {
            val input = "\"\"\"\\u0041\\u0042\\u0043\"\"\""
            Strings.parseMultilineString(input) shouldBe "ABC"
        }
    }

    context("Multiline String - escaped triple quotes") {
        test("escaped triple quotes in content") {
            val input = "\"\"\"\nHere are three quotes: \\\"\"\"\n\"\"\""
            val expected = "Here are three quotes: \"\"\""
            Strings.parseMultilineString(input) shouldBe expected
        }

        test("multiple escaped triple quotes") {
            val input = "\"\"\"\\\"\"\" and \\\"\"\"\"\"\""
            val expected = "\"\"\" and \"\"\""
            Strings.parseMultilineString(input) shouldBe expected
        }
    }

    context("Multiline String - line continuation") {
        test("line continuation in multiline string") {
            val input = "\"\"\"\nhello \\\nworld\n\"\"\""
            Strings.parseMultilineString(input) shouldBe "hello world"
        }

        test("line continuation strips leading whitespace") {
            val input = "\"\"\"\nhello \\\n    world\n\"\"\""
            Strings.parseMultilineString(input) shouldBe "hello world"
        }

        test("multiple line continuations") {
            val input = "\"\"\"\none \\\ntwo \\\nthree\n\"\"\""
            Strings.parseMultilineString(input) shouldBe "one two three"
        }
    }

    context("Multiline String - opening delimiter position") {
        test("opening on same line as content") {
            val input = "\"\"\"hello\nworld\"\"\""
            val expected = "hello\nworld"
            Strings.parseMultilineString(input) shouldBe expected
        }

        test("opening with newline before content") {
            val input = "\"\"\"\nhello\nworld\n\"\"\""
            val expected = "hello\nworld"
            Strings.parseMultilineString(input) shouldBe expected
        }
    }

    context("Multiline String - error cases") {
        test("throws on missing closing triple quote") {
            shouldThrow<ParseException> {
                Strings.parseMultilineString("\"\"\"hello")
            }
        }

        test("throws on incomplete unicode escape") {
            shouldThrow<ParseException> {
                Strings.parseMultilineString("\"\"\"\\u00\"\"\"")
            }
        }
    }

    // =========================================================================
    // Raw Multiline String Tests (```...```)
    // =========================================================================

    context("Raw Multiline String - simple cases") {
        test("empty raw multiline string") {
            Strings.parseRawMultilineString("``````") shouldBe ""
        }

        test("single line content") {
            Strings.parseRawMultilineString("```hello```") shouldBe "hello"
        }

        test("multiline content") {
            val input = "```\nline1\nline2\n```"
            val expected = "line1\nline2"
            Strings.parseRawMultilineString(input) shouldBe expected
        }
    }

    context("Raw Multiline String - no escape processing") {
        test("backslash n is literal") {
            val input = "```\\n```"
            Strings.parseRawMultilineString(input) shouldBe "\\n"
        }

        test("backslash t is literal") {
            val input = "```\\t```"
            Strings.parseRawMultilineString(input) shouldBe "\\t"
        }

        test("Windows path") {
            val input = "```C:\\Users\\name\\file.txt```"
            Strings.parseRawMultilineString(input) shouldBe "C:\\Users\\name\\file.txt"
        }

        test("regex pattern") {
            val input = "```\\d+\\.\\d+```"
            Strings.parseRawMultilineString(input) shouldBe "\\d+\\.\\d+"
        }

        test("double backslash is literal") {
            val input = "```\\\\```"
            Strings.parseRawMultilineString(input) shouldBe "\\\\"
        }

        test("unicode escape is literal") {
            val input = "```\\u0041```"
            Strings.parseRawMultilineString(input) shouldBe "\\u0041"
        }
    }

    context("Raw Multiline String - indentation stripping") {
        test("strips common indentation (2 spaces)") {
            val input = "```\n  ABC\n      def\n  123\n  ```"
            val expected = "ABC\n    def\n123"
            Strings.parseRawMultilineString(input) shouldBe expected
        }

        test("strips common indentation (4 spaces)") {
            val input = "```\n    line1\n    line2\n    ```"
            val expected = "line1\nline2"
            Strings.parseRawMultilineString(input) shouldBe expected
        }

        test("preserves extra indentation") {
            val input = "```\n  base\n    indented\n  base\n  ```"
            val expected = "base\n  indented\nbase"
            Strings.parseRawMultilineString(input) shouldBe expected
        }
    }

    context("Raw Multiline String - escaped triple backticks") {
        test("escaped triple backticks in content") {
            val input = "```\nHere are three backticks: \\```\n```"
            val expected = "Here are three backticks: ```"
            Strings.parseRawMultilineString(input) shouldBe expected
        }

        test("multiple escaped triple backticks") {
            val input = "```\\``` and \\``````"
            val expected = "``` and ```"
            Strings.parseRawMultilineString(input) shouldBe expected
        }
    }

    context("Raw Multiline String - JSON example") {
        test("JSON content") {
            val input = "```\n{\n    \"name\": \"test\",\n    \"path\": \"C:\\\\Windows\"\n}\n```"
            val expected = "{\n    \"name\": \"test\",\n    \"path\": \"C:\\\\Windows\"\n}"
            Strings.parseRawMultilineString(input) shouldBe expected
        }
    }

    context("Raw Multiline String - error cases") {
        test("throws on missing closing triple backtick") {
            shouldThrow<ParseException> {
                Strings.parseRawMultilineString("```hello")
            }
        }
    }

    // =========================================================================
    // Generic parse() method tests
    // =========================================================================

    context("Generic parse - auto-detection") {
        test("detects basic string") {
            Strings.parse("\"hello\"") shouldBe "hello"
        }

        test("detects raw string") {
            Strings.parse("`hello`") shouldBe "hello"
        }

        test("detects multiline string") {
            Strings.parse("\"\"\"hello\"\"\"") shouldBe "hello"
        }

        test("detects raw multiline string") {
            Strings.parse("```hello```") shouldBe "hello"
        }

        test("throws on invalid literal") {
            shouldThrow<ParseException> {
                Strings.parse("hello")
            }
        }
    }

    // =========================================================================
    // isStrings tests
    // =========================================================================

    context("isStrings") {
        test("basic string") {
            Strings.isStrings("\"hello\"") shouldBe true
        }

        test("empty basic string") {
            Strings.isStrings("\"\"") shouldBe true
        }

        test("raw string") {
            Strings.isStrings("`hello`") shouldBe true
        }

        test("multiline string") {
            Strings.isStrings("\"\"\"hello\"\"\"") shouldBe true
        }

        test("raw multiline string") {
            Strings.isStrings("```hello```") shouldBe true
        }

        test("not a string literal") {
            Strings.isStrings("hello") shouldBe false
        }

        test("incomplete basic string") {
            Strings.isStrings("\"hello") shouldBe false
        }

        test("empty input") {
            Strings.isStrings("") shouldBe false
        }
    }

    // =========================================================================
    // literalType tests
    // =========================================================================

    context("literalType") {
        test("basic string type") {
            Strings.literalType("\"hello\"") shouldBe StringsType.BASIC
        }

        test("raw string type") {
            Strings.literalType("`hello`") shouldBe StringsType.RAW
        }

        test("multiline string type") {
            Strings.literalType("\"\"\"hello\"\"\"") shouldBe StringsType.MULTILINE
        }

        test("raw multiline string type") {
            Strings.literalType("```hello```") shouldBe StringsType.RAW_MULTILINE
        }

        test("throws on invalid") {
            shouldThrow<ParseException> {
                Strings.literalType("hello")
            }
        }
    }

    // =========================================================================
    // Edge cases and special scenarios
    // =========================================================================

    context("Edge cases") {
        test("string with only whitespace") {
            Strings.parseBasicString("\"   \"") shouldBe "   "
        }

        test("string with only newlines") {
            Strings.parseBasicString("\"\\n\\n\\n\"") shouldBe "\n\n\n"
        }

        test("raw string with double backslash before backtick") {
            Strings.parseRawString("`\\\\`") shouldBe "\\\\"
        }

        test("multiline string with Windows line endings") {
            val input = "\"\"\"\r\nline1\r\nline2\r\n\"\"\""
            // Content should have \r\n preserved as actual line endings
            val result = Strings.parseMultilineString(input)
            result.contains("line1") shouldBe true
            result.contains("line2") shouldBe true
        }

        test("multiline string single line no newlines") {
            Strings.parseMultilineString("\"\"\"hello world\"\"\"") shouldBe "hello world"
        }

        test("raw multiline preserves all backslashes") {
            val input = "```\\a\\b\\c\\n\\t\\r\\\\```"
            Strings.parseRawMultilineString(input) shouldBe "\\a\\b\\c\\n\\t\\r\\\\"
        }
    }

    context("Complex real-world examples") {
        test("SQL query in multiline string") {
            val input = """
                |""${'"'}
                |SELECT *
                |FROM users
                |WHERE name = 'John'
                |""${'"'}
            """.trimMargin()
            val expected = "SELECT *\nFROM users\nWHERE name = 'John'"
            Strings.parseMultilineString(input) shouldBe expected
        }

        test("JSON in raw multiline string") {
            val input = """
                |```
                |{
                |    "name": "test",
                |    "regex": "\\d+"
                |}
                |```
            """.trimMargin()
            // Raw multiline preserves all backslashes - \\d+ stays as \\d+
            val expected = "{\n    \"name\": \"test\",\n    \"regex\": \"\\\\d+\"\n}"
            Strings.parseRawMultilineString(input) shouldBe expected
        }

        test("path in raw string") {
            Strings.parseRawString("`C:\\Program Files\\App\\file.exe`") shouldBe
                    "C:\\Program Files\\App\\file.exe"
        }

        test("regex in raw string") {
            Strings.parseRawString("`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$`") shouldBe
                    "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$"
        }
    }

    context("Spec example from documentation") {
        test("myTag text example - indentation stripping") {
            // From the spec:
            // myTag text="""
            //     ABC
            //         def
            //     123
            //     """
            // Should produce:
            // ABC
            //     def
            // 123
            val input = """
                |""${'"'}
                |    ABC
                |        def
                |    123
                |    ""${'"'}
            """.trimMargin()
            val expected = "ABC\n    def\n123"
            Strings.parseMultilineString(input) shouldBe expected
        }

        test("line continuation example") {
            // From the spec:
            // "This is a long string broken \
            // into two lines."
            // Should produce: "This is a long string broken into two lines."
            val input = "\"This is a long string broken \\\ninto two lines.\""
            Strings.parseBasicString(input) shouldBe "This is a long string broken into two lines."
        }

        test("escaped triple quotes example") {
            // From the spec:
            // """
            // Here are three more double quotes: \"""
            // """
            val input = "\"\"\"\nHere are three more double quotes: \\\"\"\"\n\"\"\""
            Strings.parseMultilineString(input) shouldBe "Here are three more double quotes: \"\"\""
        }
    }
})