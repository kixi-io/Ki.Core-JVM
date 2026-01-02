package io.kixi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kixi.text.ParseException
import io.kixi.uom.IncompatibleUnitsException
import io.kixi.uom.NoSuchUnitException
import io.kixi.uom.Unit

class KiExceptionTest : FunSpec({

    context("KiException") {
        test("message without suggestion") {
            val ex = KiException("Something went wrong")
            ex.message shouldBe "Something went wrong"
            ex.suggestion shouldBe null
        }

        test("message with suggestion") {
            val ex = KiException("Something went wrong", "Try doing X instead")
            ex.message shouldBe "Something went wrong Suggestion: Try doing X instead"
            ex.suggestion shouldBe "Try doing X instead"
        }

        test("inherits from RuntimeException") {
            val ex = KiException("Error")
            ex.shouldBeInstanceOf<RuntimeException>()
        }

        test("preserves cause") {
            val cause = IllegalArgumentException("Original error")
            val ex = KiException("Wrapped error", null, cause)
            ex.cause shouldBe cause
        }

        test("toString returns message") {
            val ex = KiException("Error message", "Helpful suggestion")
            ex.toString() shouldBe "Error message Suggestion: Helpful suggestion"
        }
    }

    context("ParseException") {
        test("inherits from KiException") {
            val ex = ParseException("Parse error")
            ex.shouldBeInstanceOf<KiException>()
        }

        test("message without location info") {
            val ex = ParseException("Unexpected token")
            ex.message shouldContain "Unexpected token"
        }

        test("message with line number") {
            val ex = ParseException("Unexpected token", line = 5)
            ex.message shouldContain "line: 5"
        }

        test("message with index") {
            val ex = ParseException("Unexpected token", index = 10)
            ex.message shouldContain "index: 10"
        }

        test("message with line and index") {
            val ex = ParseException("Unexpected token", line = 5, index = 10)
            ex.message shouldContain "line: 5"
            ex.message shouldContain "index: 10"
        }

        test("message with suggestion") {
            val ex = ParseException("Unexpected token", suggestion = "Check syntax")
            ex.message shouldContain "Suggestion: Check syntax"
        }

        test("message with cause") {
            val cause = NumberFormatException("Not a number")
            val ex = ParseException("Parse error", cause = cause)
            ex.message shouldContain "cause:"
        }

        test("backward compatibility - cause as 4th positional parameter") {
            val cause = IllegalArgumentException("Original")
            val ex = ParseException("Error", -1, 5, cause)
            ex.cause shouldBe cause
            ex.message shouldContain "index: 5"
        }

        test("line factory method") {
            val ex = ParseException.line("Error at position", index = 15)
            ex.message shouldContain "index: 15"
            ex.message shouldNotContain "line:"
        }

        test("line factory method with suggestion") {
            val ex = ParseException.line("Error", index = 5, suggestion = "Fix this")
            ex.message shouldContain "Suggestion: Fix this"
        }

        test("line factory method with cause") {
            val cause = NumberFormatException("Bad number")
            val ex = ParseException.line("Error", index = 5, cause = cause)
            ex.cause shouldBe cause
        }
    }

    context("NoSuchUnitException") {
        test("inherits from KiException") {
            val ex = NoSuchUnitException("xyz")
            ex.shouldBeInstanceOf<KiException>()
        }

        test("includes symbol in message") {
            val ex = NoSuchUnitException("xyz")
            ex.message shouldContain "xyz"
            ex.message shouldContain "not recognized"
        }

        test("includes default suggestion") {
            val ex = NoSuchUnitException("xyz")
            ex.suggestion.shouldNotBeNull()
            ex.message shouldContain "Suggestion:"
            ex.message shouldContain "Check the unit symbol spelling"
        }

        test("accepts custom suggestion") {
            val ex = NoSuchUnitException("xyz", "Did you mean 'xz'?")
            ex.message shouldContain "Did you mean 'xz'?"
        }
    }

    context("IncompatibleUnitsException") {
        test("inherits from KiException") {
            val ex = IncompatibleUnitsException(Unit.m, Unit.kg)
            ex.shouldBeInstanceOf<KiException>()
        }

        test("includes unit types in message") {
            val ex = IncompatibleUnitsException(Unit.m, Unit.kg)
            ex.message shouldContain "Length"
            ex.message shouldContain "Mass"
        }

        test("includes default suggestion") {
            val ex = IncompatibleUnitsException(Unit.m, Unit.kg)
            ex.suggestion.shouldNotBeNull()
            ex.message shouldContain "Suggestion:"
            ex.message shouldContain "same dimension"
        }

        test("accepts custom suggestion") {
            val ex = IncompatibleUnitsException(Unit.m, Unit.kg, "Convert mass to force first")
            ex.message shouldContain "Convert mass to force first"
        }
    }

    context("WrongRowLengthException") {
        test("inherits from ParseException") {
            val ex = WrongRowLengthException(3, 4, 2)
            ex.shouldBeInstanceOf<ParseException>()
        }

        test("inherits from KiException") {
            val ex = WrongRowLengthException(3, 4, 2)
            ex.shouldBeInstanceOf<KiException>()
        }

        test("includes row info in message") {
            val ex = WrongRowLengthException(3, 4, 2)
            ex.message shouldContain "Row 2"
            ex.message shouldContain "4 columns"
            ex.message shouldContain "expected 3"
        }

        test("includes default suggestion") {
            val ex = WrongRowLengthException(3, 4, 2)
            ex.message shouldContain "Suggestion:"
            ex.message shouldContain "3 columns"
        }

        test("properties are accessible") {
            val ex = WrongRowLengthException(5, 3, 1)
            ex.expectedLength shouldBe 5
            ex.actualLength shouldBe 3
            ex.rowIndex shouldBe 1
        }

        test("create factory method") {
            val ex = WrongRowLengthException.create(4, 2, 3, line = 10, index = 5)
            ex.expectedLength shouldBe 4
            ex.actualLength shouldBe 2
            ex.rowIndex shouldBe 3
            ex.message shouldContain "line: 10"
            ex.message shouldContain "index: 5"
        }

        test("accepts custom suggestion") {
            val ex = WrongRowLengthException(3, 4, 2, suggestion = "Remove the extra element")
            ex.message shouldContain "Remove the extra element"
        }
    }

    context("exception hierarchy") {
        test("all exceptions are KiExceptions") {
            listOf(
                KiException("base"),
                ParseException("parse"),
                NoSuchUnitException("unit"),
                IncompatibleUnitsException(Unit.m, Unit.kg),
                WrongRowLengthException(3, 4, 2)
            ).forEach { ex ->
                ex.shouldBeInstanceOf<KiException>()
            }
        }

        test("ParseException subclasses maintain hierarchy") {
            val ex = WrongRowLengthException(3, 4, 2)
            ex.shouldBeInstanceOf<ParseException>()
            ex.shouldBeInstanceOf<KiException>()
            ex.shouldBeInstanceOf<RuntimeException>()
        }
    }
})