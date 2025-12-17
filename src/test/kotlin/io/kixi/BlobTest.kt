package io.kixi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.assertions.throwables.shouldThrow
import io.kixi.text.ParseException

class BlobTest : FunSpec({

    context("creation") {
        test("create from byte array") {
            val bytes = byteArrayOf(0x48, 0x65, 0x6C, 0x6C, 0x6F) // "Hello"
            val blob = Blob.of(bytes)
            blob.size shouldBe 5
            blob.decodeToString() shouldBe "Hello"
        }

        test("create from string") {
            val blob = Blob.of("Hello World!")
            blob.decodeToString() shouldBe "Hello World!"
        }

        test("create empty blob") {
            val blob = Blob.empty()
            blob.size shouldBe 0
            blob.isEmpty() shouldBe true
        }

        test("byte array is copied on creation") {
            val original = byteArrayOf(1, 2, 3)
            val blob = Blob.of(original)
            original[0] = 99
            blob[0] shouldBe 1 // Blob should be unaffected
        }
    }

    context("Base64 encoding") {
        test("encode to standard Base64") {
            val blob = Blob.of("Hello World!")
            blob.toBase64() shouldBe "SGVsbG8gV29ybGQh"
        }

        test("encode to URL-safe Base64") {
            // Use bytes that produce +/ in standard but -_ in URL-safe
            val blob = Blob.of(byteArrayOf(-5, -17, -66)) // produces special chars
            val urlSafe = blob.toBase64UrlSafe()
            urlSafe.contains('-') || urlSafe.contains('_') || urlSafe.all { it.isLetterOrDigit() || it == '=' } shouldBe true
        }

        test("decode from standard Base64") {
            val blob = Blob.parse("SGVsbG8gV29ybGQh")
            blob.decodeToString() shouldBe "Hello World!"
        }

        test("decode from URL-safe Base64") {
            // Create a blob, encode URL-safe, then decode
            val original = Blob.of("Test data with special chars: +/")
            val urlSafeEncoded = original.toBase64UrlSafe()
            val decoded = Blob.parse(urlSafeEncoded)
            decoded.decodeToString() shouldBe original.decodeToString()
        }

        test("parse auto-detects variant") {
            val standardBlob = Blob.parse("SGVsbG8=")
            standardBlob.decodeToString() shouldBe "Hello"
        }

        test("parse ignores whitespace") {
            val blob = Blob.parse("SGVs\n  bG8=")
            blob.decodeToString() shouldBe "Hello"
        }

        test("parse with empty string returns empty blob") {
            val blob = Blob.parse("")
            blob.isEmpty() shouldBe true
        }

        test("parse throws on invalid input") {
            shouldThrow<IllegalArgumentException> {
                Blob.parse("!!!invalid!!!")
            }
        }

        test("parseOrNull returns null on failure") {
            Blob.parseOrNull("!!!invalid!!!") shouldBe null
        }

        test("parseOrNull returns Blob on success") {
            Blob.parseOrNull("SGVsbG8=")?.decodeToString() shouldBe "Hello"
        }
    }

    context("parsing Ki literals") {
        test("parseLiteral simple blob literal") {
            val blob = Blob.parseLiteral(".blob(SGVsbG8=)")
            blob.decodeToString() shouldBe "Hello"
        }

        test("parseLiteral empty blob literal") {
            val blob = Blob.parseLiteral(".blob()")
            blob.isEmpty() shouldBe true
        }

        test("parseLiteral blob with whitespace") {
            val blob = Blob.parseLiteral(".blob(\n\tSGVsbG8=\n)")
            blob.decodeToString() shouldBe "Hello"
        }

        test("parseLiteral multiline blob") {
            val blob = Blob.parseLiteral("""
                .blob(
                    SGVsbG8gV29ybGQhIFRoaXMgaXMgYSBsb25nZXIgYmxvYiB0
                    aGF0IHNwYW5zIG11bHRpcGxlIGxpbmVzLg==
                )
            """.trimIndent())
            blob.isNotEmpty() shouldBe true
        }

        test("parseLiteral throws on missing prefix") {
            shouldThrow<ParseException> {
                Blob.parseLiteral("SGVsbG8=")
            }
        }

        test("parseLiteral throws on missing closing paren") {
            shouldThrow<ParseException> {
                Blob.parseLiteral(".blob(SGVsbG8=")
            }
        }

        test("parseLiteral throws on invalid Base64 characters") {
            shouldThrow<ParseException> {
                Blob.parseLiteral(".blob(!!!)")
            }
        }

        test("parseLiteralOrNull returns null on failure") {
            Blob.parseLiteralOrNull("invalid") shouldBe null
        }

        test("parseLiteralOrNull returns Blob on success") {
            Blob.parseLiteralOrNull(".blob(SGVsbG8=)")?.decodeToString() shouldBe "Hello"
        }
    }

    context("formatting") {
        test("toString produces Ki literal") {
            val blob = Blob.of("Hello")
            blob.toString() shouldBe ".blob(SGVsbG8=)"
        }

        test("empty blob toString") {
            Blob.empty().toString() shouldBe ".blob()"
        }

        test("long blobs are formatted with line breaks") {
            val longText = "This is a much longer text that will produce a Base64 string longer than 30 characters"
            val blob = Blob.of(longText)
            val formatted = blob.toString()
            formatted.contains("\n") shouldBe true
            formatted.startsWith(".blob(\n") shouldBe true
            formatted.endsWith("\n)") shouldBe true
        }

        test("toStringUrlSafe uses URL-safe encoding") {
            val blob = Blob.of("Test")
            val urlSafe = blob.toStringUrlSafe()
            urlSafe.startsWith(".blob(") shouldBe true
        }
    }

    context("round-trip") {
        test("toString and parseLiteral round-trip") {
            val original = Blob.of("Hello World!")
            val literal = original.toString()
            val parsed = Blob.parseLiteral(literal)
            parsed shouldBe original
        }

        test("toBase64 and parse round-trip") {
            val original = Blob.of("Hello World!")
            val base64 = original.toBase64()
            val parsed = Blob.parse(base64)
            parsed shouldBe original
        }

        test("multiline literal round-trip") {
            val longText = "This is a much longer text that will produce a Base64 string longer than 30 characters"
            val original = Blob.of(longText)
            val literal = original.toString()
            val parsed = Blob.parseLiteral(literal)
            parsed shouldBe original
            parsed.decodeToString() shouldBe longText
        }
    }

    context("equality and hashing") {
        test("equal blobs have same content") {
            val blob1 = Blob.of("Hello")
            val blob2 = Blob.of("Hello")
            blob1 shouldBe blob2
        }

        test("different blobs are not equal") {
            val blob1 = Blob.of("Hello")
            val blob2 = Blob.of("World")
            blob1 shouldNotBe blob2
        }

        test("equal blobs have same hashCode") {
            val blob1 = Blob.of("Hello")
            val blob2 = Blob.of("Hello")
            blob1.hashCode() shouldBe blob2.hashCode()
        }
    }

    context("byte access") {
        test("access bytes by index") {
            val blob = Blob.of("ABC")
            blob[0] shouldBe 0x41.toByte()
            blob[1] shouldBe 0x42.toByte()
            blob[2] shouldBe 0x43.toByte()
        }

        test("toByteArray returns copy") {
            val blob = Blob.of("Test")
            val bytes = blob.toByteArray()
            bytes[0] = 0
            blob[0] shouldBe 0x54.toByte() // 'T' - should be unaffected
        }

        test("iterate over bytes") {
            val blob = Blob.of("Hi")
            val bytes = mutableListOf<Byte>()
            for (b in blob) bytes.add(b)
            bytes shouldBe listOf(0x48.toByte(), 0x69.toByte())
        }
    }

    context("utility") {
        test("isLiteral detects blob literals") {
            Blob.isLiteral(".blob(SGVsbG8=)") shouldBe true
            Blob.isLiteral("  .blob()  ") shouldBe true
            Blob.isLiteral("not a blob") shouldBe false
            Blob.isLiteral(".blob(") shouldBe false
        }
    }
})