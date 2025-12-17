package io.kixi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.assertions.throwables.shouldThrow
import io.kixi.text.ParseException

class VersionTest : FunSpec({

    context("creation") {
        test("create with major only") {
            val v = Version(5)
            v.major shouldBe 5
            v.minor shouldBe 0
            v.micro shouldBe 0
            v.qualifier shouldBe ""
            v.qualifierNumber shouldBe 0
        }

        test("create with major and minor") {
            val v = Version(5, 2)
            v.major shouldBe 5
            v.minor shouldBe 2
            v.micro shouldBe 0
        }

        test("create full version") {
            val v = Version(5, 2, 7)
            v.major shouldBe 5
            v.minor shouldBe 2
            v.micro shouldBe 7
        }

        test("create with qualifier") {
            val v = Version(1, 0, 0, "beta")
            v.qualifier shouldBe "beta"
            v.qualifierNumber shouldBe 0
        }

        test("create with qualifier and number") {
            val v = Version(1, 0, 0, "rc", 2)
            v.qualifier shouldBe "rc"
            v.qualifierNumber shouldBe 2
        }

        test("throws on negative major") {
            shouldThrow<IllegalArgumentException> {
                Version(-1, 0, 0)
            }
        }

        test("throws on negative minor") {
            shouldThrow<IllegalArgumentException> {
                Version(1, -1, 0)
            }
        }

        test("throws on negative micro") {
            shouldThrow<IllegalArgumentException> {
                Version(1, 0, -1)
            }
        }

        test("throws on qualifierNumber without qualifier") {
            shouldThrow<IllegalArgumentException> {
                Version(1, 0, 0, "", 5)
            }
        }
    }

    context("parsing") {
        test("parse major only") {
            val v = Version.parse("5")
            v.major shouldBe 5
            v.minor shouldBe 0
            v.micro shouldBe 0
        }

        test("parse major.minor") {
            val v = Version.parse("5.2")
            v.major shouldBe 5
            v.minor shouldBe 2
        }

        test("parse full version") {
            val v = Version.parse("5.2.7")
            v.major shouldBe 5
            v.minor shouldBe 2
            v.micro shouldBe 7
        }

        test("parse with qualifier") {
            val v = Version.parse("1.0.0-beta")
            v.qualifier shouldBe "beta"
        }

        test("parse with qualifier and dash separator") {
            val v = Version.parse("1.0.0-beta-2")
            v.qualifier shouldBe "beta"
            v.qualifierNumber shouldBe 2
        }

        test("parse with qualifier number (no dash)") {
            val v = Version.parse("1.0.0-beta2")
            v.qualifier shouldBe "beta"
            v.qualifierNumber shouldBe 2
        }

        test("parse qualifier attached to minor") {
            val v = Version.parse("5.2-alpha")
            v.minor shouldBe 2
            v.qualifier shouldBe "alpha"
        }

        test("parse qualifier attached to major") {
            val v = Version.parse("5-beta")
            v.major shouldBe 5
            v.qualifier shouldBe "beta"
        }

        test("throws on empty") {
            shouldThrow<ParseException> {
                Version.parse("")
            }
        }

        test("throws on too many components") {
            shouldThrow<ParseException> {
                Version.parse("1.2.3.4")
            }
        }

        test("throws on negative component") {
            shouldThrow<ParseException> {
                Version.parse("-1.0.0")
            }
        }

        test("throws on trailing dash") {
            shouldThrow<ParseException> {
                Version.parse("1.0.0-")
            }
        }

        test("throws on non-digit in major") {
            shouldThrow<ParseException> {
                Version.parse("1a.0.0")
            }
        }

        test("parseOrNull returns null on failure") {
            Version.parseOrNull("invalid") shouldBe null
        }

        test("parseOrNull returns version on success") {
            Version.parseOrNull("1.2.3")?.major shouldBe 1
        }
    }

    context("properties") {
        test("hasQualifier") {
            Version(1, 0, 0, "beta").hasQualifier shouldBe true
            Version(1, 0, 0).hasQualifier shouldBe false
        }

        test("isStable") {
            Version(1, 0, 0).isStable shouldBe true
            Version(1, 0, 0, "beta").isStable shouldBe false
        }

        test("isPreRelease") {
            Version(1, 0, 0, "alpha").isPreRelease shouldBe true
            Version(1, 0, 0).isPreRelease shouldBe false
        }
    }

    context("toString") {
        test("full format") {
            Version(1, 2, 3).toString() shouldBe "1.2.3"
        }

        test("with qualifier") {
            Version(1, 0, 0, "beta").toString() shouldBe "1.0.0-beta"
        }

        test("with qualifier number") {
            Version(1, 0, 0, "rc", 2).toString() shouldBe "1.0.0-rc-2"
        }
    }

    context("toShortString") {
        test("omits trailing zeros") {
            Version(5, 0, 0).toShortString() shouldBe "5"
            Version(5, 2, 0).toShortString() shouldBe "5.2"
            Version(5, 2, 7).toShortString() shouldBe "5.2.7"
        }

        test("with qualifier") {
            Version(5, 0, 0, "beta").toShortString() shouldBe "5-beta"
            Version(5, 0, 0, "rc", 2).toShortString() shouldBe "5-rc-2"
        }
    }

    context("comparison") {
        test("compare by major") {
            Version(2, 0, 0) shouldBeGreaterThan Version(1, 0, 0)
        }

        test("compare by minor") {
            Version(1, 2, 0) shouldBeGreaterThan Version(1, 1, 0)
        }

        test("compare by micro") {
            Version(1, 0, 2) shouldBeGreaterThan Version(1, 0, 1)
        }

        test("release > pre-release") {
            Version(1, 0, 0) shouldBeGreaterThan Version(1, 0, 0, "beta")
        }

        test("compare qualifiers alphabetically") {
            Version(1, 0, 0, "beta") shouldBeGreaterThan Version(1, 0, 0, "alpha")
            Version(1, 0, 0, "rc") shouldBeGreaterThan Version(1, 0, 0, "beta")
        }

        test("compare qualifier numbers") {
            Version(1, 0, 0, "beta", 2) shouldBeGreaterThan Version(1, 0, 0, "beta", 1)
        }

        test("qualifier comparison is case-insensitive") {
            val v1 = Version(1, 0, 0, "BETA")
            val v2 = Version(1, 0, 0, "beta")
            v1.compareTo(v2) shouldBe 0
        }
    }

    context("equality") {
        test("equal versions") {
            Version(1, 2, 3) shouldBe Version(1, 2, 3)
            Version(1, 0, 0, "beta") shouldBe Version(1, 0, 0, "beta")
        }

        test("different versions") {
            Version(1, 2, 3) shouldNotBe Version(1, 2, 4)
        }

        test("hashCode consistent") {
            Version(1, 2, 3).hashCode() shouldBe Version(1, 2, 3).hashCode()
        }
    }

    context("increment methods") {
        test("incrementMajor") {
            Version(1, 2, 3).incrementMajor() shouldBe Version(2, 0, 0)
        }

        test("incrementMinor") {
            Version(1, 2, 3).incrementMinor() shouldBe Version(1, 3, 0)
        }

        test("incrementMicro") {
            Version(1, 2, 3).incrementMicro() shouldBe Version(1, 2, 4)
        }

        test("incrementQualifierNumber") {
            Version(1, 0, 0, "beta", 1).incrementQualifierNumber() shouldBe
                    Version(1, 0, 0, "beta", 2)
        }

        test("incrementQualifierNumber throws without qualifier") {
            shouldThrow<IllegalArgumentException> {
                Version(1, 0, 0).incrementQualifierNumber()
            }
        }
    }

    context("transformation methods") {
        test("toStable removes qualifier") {
            Version(1, 0, 0, "beta", 2).toStable() shouldBe Version(1, 0, 0)
        }

        test("withQualifier adds qualifier") {
            Version(1, 0, 0).withQualifier("rc", 1) shouldBe Version(1, 0, 0, "rc", 1)
        }
    }

    context("compatibility") {
        test("isCompatibleWith same major") {
            Version(1, 2, 0).isCompatibleWith(Version(1, 5, 0)) shouldBe true
        }

        test("isCompatibleWith different major") {
            Version(1, 0, 0).isCompatibleWith(Version(2, 0, 0)) shouldBe false
        }
    }

    context("range satisfaction") {
        test("satisfies inclusive range") {
            val range = Range(Version(1, 0, 0), Version(2, 0, 0))
            Version(1, 5, 0).satisfies(range) shouldBe true
            Version(3, 0, 0).satisfies(range) shouldBe false
        }
    }

    context("constants") {
        test("EMPTY") {
            Version.EMPTY shouldBe Version(0)
        }

        test("MIN") {
            Version.MIN.major shouldBe 0
            Version.MIN.qualifier shouldBe "AAA"
        }

        test("MAX") {
            Version.MAX.major shouldBe Int.MAX_VALUE
        }
    }
})