package io.kixi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.math.BigDecimal as Dec
import java.time.Duration
import java.time.ZoneOffset

class CollectionExtensionsTest : FunSpec({

    context("Collection.toString") {
        test("default separator") {
            listOf(1, 2, 3).toString() shouldBe "[1, 2, 3]"
        }

        test("custom separator") {
            listOf("a", "b", "c").toString(separator = " | ") shouldBe "a | b | c"
        }

        test("empty collection") {
            emptyList<Int>().toString() shouldBe "[]"
        }

        test("single element") {
            listOf("only").toString() shouldBe "[only]"
        }

        test("with custom formatter") {
            listOf(1, 2, 3).toString(formatter = { "[$it]" }) shouldBe "[1], [2], [3]"
        }
    }

    context("Array.contentToString") {
        test("default separator") {
            arrayOf(1, 2, 3).contentToString() shouldBe "[1, 2, 3]"
        }

        test("custom separator") {
            arrayOf("x", "y").toString(separator = "-") shouldBe "x-y"
        }

        test("empty array") {
            emptyArray<String>().contentToString() shouldBe "[]"
        }
    }
})

class MapExtensionsTest : FunSpec({

    context("Map.toString") {
        test("default format") {
            mapOf("a" to 1, "b" to 2).toString() shouldBe "{a=1, b=2}"
        }

        test("custom separator") {
            mapOf("x" to 1, "y" to 2).toString(separator = "; ") shouldBe "x=1; y=2"
        }

        test("custom assignment") {
            mapOf("key" to "value").toString(assignment = ": ") shouldBe "key: value"
        }

        test("empty map") {
            emptyMap<String, Int>().toString() shouldBe "{}"
        }

        test("with custom formatter") {
            mapOf("a" to 1).toString(formatter = { "\"$it\"" }) shouldBe "\"a\"=\"1\""
        }
    }
})

class DecExtensionsTest : FunSpec({

    context("whole property") {
        test("integer value is whole") {
            Dec("5").whole shouldBe true
            Dec("100").whole shouldBe true
        }

        test("zero is whole") {
            Dec("0").whole shouldBe true
        }

        test("fractional value is not whole") {
            Dec("3.14").whole shouldBe false
            Dec("0.5").whole shouldBe false
        }

        test("trailing zeros still whole") {
            Dec("5.00").whole shouldBe true
            Dec("100.000").whole shouldBe true
        }

        test("negative whole number") {
            Dec("-7").whole shouldBe true
        }

        test("negative fractional") {
            Dec("-3.5").whole shouldBe false
        }
    }
})

class DurationExtensionsTest : FunSpec({

    context("kiFormat") {
        test("formats hours") {
            Duration.ofHours(5).kiFormat() shouldBe "5h"
        }

        test("formats with zero padding") {
            val d = Duration.ofHours(1).plusMinutes(5).plusSeconds(3)
            d.kiFormat(zeroPad = true) shouldBe "01:05:03"
        }

        test("formats compound duration") {
            Duration.ofHours(2).plusMinutes(30).kiFormat() shouldBe "2:30:0"
        }
    }
})

class KiTZTest : FunSpec({

    context("US time zones") {
        test("US Pacific zones") {
            KiTZ["US/PST"]?.offset shouldBe ZoneOffset.ofHours(-8)
            KiTZ["US/PDT"]?.offset shouldBe ZoneOffset.ofHours(-7)
        }

        test("US Mountain zones") {
            KiTZ["US/MST"]?.offset shouldBe ZoneOffset.ofHours(-7)
            KiTZ["US/MDT"]?.offset shouldBe ZoneOffset.ofHours(-6)
        }

        test("US Central zones") {
            KiTZ["US/CST"]?.offset shouldBe ZoneOffset.ofHours(-6)
            KiTZ["US/CDT"]?.offset shouldBe ZoneOffset.ofHours(-5)
        }

        test("US Eastern zones") {
            KiTZ["US/EST"]?.offset shouldBe ZoneOffset.ofHours(-5)
            KiTZ["US/EDT"]?.offset shouldBe ZoneOffset.ofHours(-4)
        }

        test("US Alaska zones") {
            KiTZ["US/AKST"]?.offset shouldBe ZoneOffset.ofHours(-9)
            KiTZ["US/AKDT"]?.offset shouldBe ZoneOffset.ofHours(-8)
        }

        test("US Hawaii") {
            KiTZ["US/HST"]?.offset shouldBe ZoneOffset.ofHours(-10)
        }
    }

    context("European time zones") {
        test("Central European zones") {
            KiTZ["DE/CET"]?.offset shouldBe ZoneOffset.ofHours(1)
            KiTZ["DE/CEST"]?.offset shouldBe ZoneOffset.ofHours(2)
            KiTZ["FR/CET"]?.offset shouldBe ZoneOffset.ofHours(1)
        }

        test("UK zones") {
            KiTZ["GB/GMT"]?.offset shouldBe ZoneOffset.UTC
            KiTZ["GB/BST"]?.offset shouldBe ZoneOffset.ofHours(1)
        }

        test("Eastern European zones") {
            KiTZ["GR/EET"]?.offset shouldBe ZoneOffset.ofHours(2)
            KiTZ["GR/EEST"]?.offset shouldBe ZoneOffset.ofHours(3)
        }
    }

    context("Asian time zones") {
        test("Japan") {
            KiTZ["JP/JST"]?.offset shouldBe ZoneOffset.ofHours(9)
        }

        test("China") {
            KiTZ["CN/CST"]?.offset shouldBe ZoneOffset.ofHours(8)
        }

        test("India") {
            KiTZ["IN/IST"]?.offset shouldBe ZoneOffset.ofHoursMinutes(5, 30)
        }

        test("Korea") {
            KiTZ["KR/KST"]?.offset shouldBe ZoneOffset.ofHours(9)
        }

        test("Singapore") {
            KiTZ["SG/SGT"]?.offset shouldBe ZoneOffset.ofHours(8)
        }
    }

    context("Oceania time zones") {
        test("Australia Eastern") {
            KiTZ["AU/AEST"]?.offset shouldBe ZoneOffset.ofHours(10)
            KiTZ["AU/AEDT"]?.offset shouldBe ZoneOffset.ofHours(11)
        }

        test("Australia Central") {
            KiTZ["AU/ACST"]?.offset shouldBe ZoneOffset.ofHoursMinutes(9, 30)
            KiTZ["AU/ACDT"]?.offset shouldBe ZoneOffset.ofHoursMinutes(10, 30)
        }

        test("New Zealand") {
            KiTZ["NZ/NZST"]?.offset shouldBe ZoneOffset.ofHours(12)
            KiTZ["NZ/NZDT"]?.offset shouldBe ZoneOffset.ofHours(13)
        }
    }

    context("South American time zones") {
        test("Argentina") {
            KiTZ["AR/ART"]?.offset shouldBe ZoneOffset.ofHours(-3)
        }

        test("Brazil zones") {
            KiTZ["BR/BRT"]?.offset shouldBe ZoneOffset.ofHours(-3)
            KiTZ["BR/AMT"]?.offset shouldBe ZoneOffset.ofHours(-4)
        }

        test("Colombia") {
            KiTZ["CO/COT"]?.offset shouldBe ZoneOffset.ofHours(-5)
        }
    }

    context("Russian time zones") {
        test("Moscow") {
            KiTZ["RU/MSK"]?.offset shouldBe ZoneOffset.ofHours(3)
        }

        test("Vladivostok") {
            KiTZ["RU/VLAT"]?.offset shouldBe ZoneOffset.ofHours(10)
        }

        test("Kamchatka") {
            KiTZ["RU/PETT"]?.offset shouldBe ZoneOffset.ofHours(12)
        }
    }

    context("KiTZ data class properties") {
        test("country property") {
            KiTZ.US_PST.country shouldBe "United States"
            KiTZ.JP_JST.country shouldBe "Japan"
            KiTZ.DE_CET.country shouldBe "Germany"
        }

        test("countryCode property") {
            KiTZ.US_PST.countryCode shouldBe "US"
            KiTZ.JP_JST.countryCode shouldBe "JP"
        }

        test("abbreviation property") {
            KiTZ.US_PST.abbreviation shouldBe "PST"
            KiTZ.JP_JST.abbreviation shouldBe "JST"
        }

        test("id property") {
            KiTZ.US_PST.id shouldBe "US/PST"
            KiTZ.JP_JST.id shouldBe "JP/JST"
        }

        test("toString returns id") {
            KiTZ.US_PST.toString() shouldBe "US/PST"
        }
    }

    context("KiTZ lookup methods") {
        test("get by id") {
            KiTZ["US/PST"] shouldBe KiTZ.US_PST
            KiTZ["JP/JST"] shouldBe KiTZ.JP_JST
            KiTZ["INVALID"] shouldBe null
        }

        test("require by id") {
            KiTZ.require("US/PST") shouldBe KiTZ.US_PST
        }

        test("fromOffset returns a valid KiTZ for the offset") {
            // fromOffset is mainly for converting legacy ZonedDateTime with only offset info
            // When possible, use KiTZ constants directly (KiTZ.US_PST, KiTZ.JP_JST, etc.)
            val tz = KiTZ.fromOffset(ZoneOffset.ofHours(-8))
            tz?.offset shouldBe ZoneOffset.ofHours(-8)

            KiTZ.fromOffset(ZoneOffset.UTC) shouldBe KiTZ.UTC
        }

        test("isValid") {
            KiTZ.isValid("US/PST") shouldBe true
            KiTZ.isValid("INVALID") shouldBe false
        }
    }
})