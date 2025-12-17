package io.kixi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.assertions.throwables.shouldThrow
import io.kixi.text.ParseException
import io.kixi.uom.Quantity
import java.math.BigDecimal as Dec
import java.net.URL
import java.time.*
import kotlin.reflect.typeOf

class KiTest : FunSpec({

    context("format") {
        test("format null as nil") {
            Ki.format(null) shouldBe "nil"
        }

        test("format String with quotes and escaping") {
            Ki.format("hello") shouldBe "\"hello\""
            Ki.format("line1\nline2") shouldBe "\"line1\\nline2\""
            Ki.format("tab\there") shouldBe "\"tab\\there\""
        }

        test("format Char") {
            Ki.format('a') shouldBe "'a'"
        }

        test("format BigDecimal with bd suffix") {
            Ki.format(Dec("3.14")) shouldBe "3.14bd"
            Ki.format(Dec("100.00")) shouldBe "100bd" // strips trailing zeros
        }

        test("format Float with f suffix") {
            Ki.format(3.14f) shouldBe "3.14f"
        }

        test("format Long with L suffix") {
            Ki.format(42L) shouldBe "42L"
        }

        test("format Int without suffix") {
            Ki.format(42) shouldBe "42"
        }

        test("format Double without suffix") {
            Ki.format(3.14) shouldBe "3.14"
        }

        test("format Boolean") {
            Ki.format(true) shouldBe "true"
            Ki.format(false) shouldBe "false"
        }

        test("format URL with angle brackets") {
            Ki.format(URL("https://example.com")) shouldBe "<https://example.com>"
        }

        test("format LocalDate") {
            Ki.format(LocalDate.of(2024, 3, 15)) shouldBe "2024/3/15"
        }

        test("format LocalDateTime") {
            Ki.format(LocalDateTime.of(2024, 3, 15, 10, 30, 0)) shouldBe "2024/3/15@10:30:00"
        }

        test("format Duration") {
            Ki.format(Duration.ofHours(5)) shouldBe "5h"
            Ki.format(Duration.ofMinutes(30)) shouldBe "30min"
            Ki.format(Duration.ofSeconds(45)) shouldBe "45s"
        }

        test("format Version") {
            Ki.format(Version(1, 2, 3)) shouldBe "1.2.3"
        }

        test("format Range") {
            Ki.format(Range(1, 10)) shouldBe "1..10"
        }

        test("format List") {
            Ki.format(listOf(1, 2, 3)) shouldBe "[1, 2, 3]"
            Ki.format(listOf("a", "b")) shouldBe "[\"a\", \"b\"]"
        }

        test("format Map") {
            Ki.format(mapOf("a" to 1)) shouldBe "[\"a\"=1]"
        }

        test("format nested collections") {
            Ki.format(listOf(listOf(1, 2), listOf(3, 4))) shouldBe "[[1, 2], [3, 4]]"
        }
    }

    context("parse literals") {
        test("parse nil and null") {
            Ki.parse("nil") shouldBe null
            Ki.parse("null") shouldBe null
        }

        test("parse Boolean") {
            Ki.parse("true") shouldBe true
            Ki.parse("false") shouldBe false
        }

        test("parse String with double quotes") {
            Ki.parse("\"hello\"") shouldBe "hello"
            Ki.parse("\"line1\\nline2\"") shouldBe "line1\nline2"
        }

        test("parse Char with single quotes") {
            Ki.parse("'a'") shouldBe 'a'
        }

        test("parse URL") {
            Ki.parse("<https://example.com>").shouldBeInstanceOf<URL>()
            (Ki.parse("<https://example.com>") as URL).toString() shouldBe "https://example.com"
        }

        test("parse Integer") {
            Ki.parse("42") shouldBe 42
            Ki.parse("-17") shouldBe -17
        }

        test("parse Long with L suffix") {
            Ki.parse("42L") shouldBe 42L
        }

        test("parse large integer as Long automatically") {
            val big = Ki.parse("9999999999")
            big.shouldBeInstanceOf<Long>()
        }

        test("parse Float with f suffix") {
            Ki.parse("3.14f") shouldBe 3.14f
        }

        test("parse Double with d suffix") {
            Ki.parse("3.14d") shouldBe 3.14
        }

        test("parse Double without suffix (has decimal point)") {
            val foo = Ki.parse("3.14.0")
            foo shouldBe Version(3, 14)
        }

        test("parse BigDecimal with bd suffix") {
            Ki.parse("3.14bd") shouldBe Dec("3.14")
        }

        test("parse with underscore separators") {
            Ki.parse("1_000_000") shouldBe 1000000
        }

        test("parse Blob") {
            val blob = Ki.parse(".blob(SGVsbG8=)")
            blob.shouldBeInstanceOf<Blob>()
            (blob as Blob).decodeToString() shouldBe "Hello"
        }

        test("parse Version") {
            val v = Ki.parse("1.2.3")
            v.shouldBeInstanceOf<Version>()
            (v as Version).major shouldBe 1
        }

        test("parse Quantity") {
            val q = Ki.parse("5cm")
            q.shouldBeInstanceOf<Quantity<*>>()
        }

        test("parseOrNull returns null on failure") {
            Ki.parseOrNull("invalid!!!") shouldBe null
        }

        test("parseOrNull returns value on success") {
            Ki.parseOrNull("42") shouldBe 42
        }

        test("throws on empty string") {
            shouldThrow<ParseException> {
                Ki.parse("")
            }
        }
    }

    context("parse dates") {
        test("parse LocalDate") {
            val date = Ki.parseLocalDate("2024/3/15")
            date shouldBe LocalDate.of(2024, 3, 15)
        }

        test("parse LocalDateTime") {
            val dt = Ki.parseLocalDateTime("2024/3/15@10:30:00")
            dt shouldBe LocalDateTime.of(2024, 3, 15, 10, 30, 0)
        }

        test("parse LocalDateTime with nanoseconds") {
            val dt = Ki.parseLocalDateTime("2024/3/15@10:30:00.123456789")
            dt.nano shouldBe 123456789
        }

        test("parse ZonedDateTime with offset") {
            val zdt = Ki.parseZonedDateTime("2024/3/15@10:30:00+5")
            zdt.offset shouldBe ZoneOffset.ofHours(5)
        }

        test("parse ZonedDateTime with negative offset") {
            val zdt = Ki.parseZonedDateTime("2024/3/15@10:30:00-8")
            zdt.offset shouldBe ZoneOffset.ofHours(-8)
        }

        test("parse ZonedDateTime with UTC") {
            val zdt = Ki.parseZonedDateTime("2024/3/15@10:30:00-UTC")
            zdt.offset shouldBe ZoneOffset.UTC
        }

        test("parse ZonedDateTime with Z") {
            val zdt = Ki.parseZonedDateTime("2024/3/15@10:30:00-Z")
            zdt.offset shouldBe ZoneOffset.UTC
        }

        test("parse ZonedDateTime with KiTZ") {
            val zdt = Ki.parseZonedDateTime("2024/3/15@10:30:00-US/PST")
            zdt.offset shouldBe ZoneOffset.ofHours(-8)
        }
    }

    context("format dates") {
        test("format LocalDate") {
            Ki.formatLocalDate(LocalDate.of(2024, 3, 15)) shouldBe "2024/3/15"
        }

        test("format LocalDate with zero padding") {
            Ki.formatLocalDate(LocalDate.of(2024, 3, 5), zeroPad = true) shouldBe "2024/03/05"
        }

        test("format LocalDateTime") {
            Ki.formatLocalDateTime(LocalDateTime.of(2024, 3, 15, 9, 5, 3)) shouldBe "2024/3/15@9:05:03"
        }

        test("format LocalDateTime with zero padding") {
            val formatted = Ki.formatLocalDateTime(
                LocalDateTime.of(2024, 3, 5, 9, 5, 3),
                zeroPad = true
            )
            formatted shouldBe "2024/03/05@09:05:03"
        }

        test("format ZonedDateTime") {
            val zdt = ZonedDateTime.of(
                LocalDateTime.of(2024, 3, 15, 10, 30, 0),
                ZoneOffset.ofHours(-8)
            )
            Ki.formatZonedDateTime(zdt) shouldBe "2024/3/15@10:30:00-08"
        }

        test("format ZonedDateTime UTC") {
            val zdt = ZonedDateTime.of(
                LocalDateTime.of(2024, 3, 15, 10, 30, 0),
                ZoneOffset.UTC
            )
            Ki.formatZonedDateTime(zdt) shouldBe "2024/3/15@10:30:00-Z"
        }
    }

    context("parse Duration") {
        test("parse single unit durations") {
            Ki.parseDuration("5day") shouldBe Duration.ofDays(5)
            Ki.parseDuration("3days") shouldBe Duration.ofDays(3)
            Ki.parseDuration("2h") shouldBe Duration.ofHours(2)
            Ki.parseDuration("30min") shouldBe Duration.ofMinutes(30)
            Ki.parseDuration("45s") shouldBe Duration.ofSeconds(45)
            Ki.parseDuration("100ms") shouldBe Duration.ofMillis(100)
            Ki.parseDuration("500ns") shouldBe Duration.ofNanos(500)
        }

        test("parse compound duration without days") {
            Ki.parseDuration("1:30:00") shouldBe Duration.ofHours(1).plusMinutes(30)
        }

        test("parse compound duration with days") {
            Ki.parseDuration("2days:3:30:00") shouldBe
                    Duration.ofDays(2).plusHours(3).plusMinutes(30)
        }

        test("parse duration with fractional seconds") {
            val d = Ki.parseDuration("5.5s")
            d shouldBe Duration.ofSeconds(5).plusMillis(500)
        }

        test("parse negative duration") {
            Ki.parseDuration("-5h") shouldBe Duration.ofHours(-5)
        }
    }

    context("format Duration") {
        test("format nanoseconds") {
            Ki.formatDuration(Duration.ofNanos(500)) shouldBe "500ns"
        }

        test("format milliseconds") {
            Ki.formatDuration(Duration.ofMillis(100)) shouldBe "100ms"
        }

        test("format seconds") {
            Ki.formatDuration(Duration.ofSeconds(45)) shouldBe "45s"
        }

        test("format minutes (single unit)") {
            Ki.formatDuration(Duration.ofMinutes(30)) shouldBe "30min"
        }

        test("format hours (single unit)") {
            Ki.formatDuration(Duration.ofHours(5)) shouldBe "5h"
        }

        test("format days (single unit)") {
            Ki.formatDuration(Duration.ofDays(1)) shouldBe "1day"
            Ki.formatDuration(Duration.ofDays(3)) shouldBe "3days"
        }

        test("format compound duration") {
            Ki.formatDuration(Duration.ofHours(1).plusMinutes(30).plusSeconds(45)) shouldBe "1:30:45"
        }

        test("format compound duration with days") {
            val d = Duration.ofDays(2).plusHours(3).plusMinutes(30)
            Ki.formatDuration(d) shouldBe "2days:3:30:0"
        }

        test("format with zero padding") {
            val d = Duration.ofHours(1).plusMinutes(5).plusSeconds(3)
            Ki.formatDuration(d, zeroPad = true) shouldBe "01:05:03"
        }

        test("format fractional seconds") {
            val d = Duration.ofSeconds(5).plusMillis(500)
            Ki.formatDuration(d) shouldBe "5.5s"
        }

        test("format negative duration") {
            Ki.formatDuration(Duration.ofHours(-5)) shouldBe "-5h"
        }
    }

    context("typeOf") {
        test("returns correct types") {
            Ki.typeOf(null) shouldBe Type.nil
            Ki.typeOf("test") shouldBe Type.String
            Ki.typeOf(42) shouldBe Type.Int
        }
    }
})