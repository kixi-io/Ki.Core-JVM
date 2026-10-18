package io.kixi

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeNegative
import io.kotest.matchers.ints.shouldBePositive
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.long
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.choice
import io.kotest.property.checkAll
import java.time.Instant
import java.time.ZoneOffset
import java.util.TreeSet
import kotlin.math.sign

class KiTZDateTimeTest : FunSpec({

    val tokyo = KiTZDateTime(2024, 3, 15, 10, 32, 0, KiTZ.JP_JST)
    val utc = tokyo.withKiTZ(KiTZ.UTC)
    val pacific = tokyo.withKiTZ(KiTZ.US_PST)

    context("comparison") {
        test("different instants order by instant, whatever the zones") {
            val laterInUtc = KiTZDateTime(2024, 3, 15, 1, 33, 0, KiTZ.UTC)
            // Local time 01:33 is earlier than 10:32, but the instant is a minute later.
            tokyo shouldBeLessThan laterInUtc
            laterInUtc shouldBeGreaterThan tokyo
            tokyo.isBefore(laterInUtc) shouldBe true
        }

        test("the same instant in different zones is not equal and does not compare as 0") {
            utc.localDateTime shouldBe java.time.LocalDateTime.of(2024, 3, 15, 1, 32)
            tokyo.isEqual(utc) shouldBe true
            tokyo shouldNotBe utc
            tokyo.compareTo(utc).shouldBePositive()   // same instant, later local time
            utc.compareTo(tokyo).shouldBeNegative()
            pacific.compareTo(utc).shouldBeNegative()
        }

        test("same instant and offset, different KiTZ id: ordered by id") {
            val us = KiTZDateTime(2024, 3, 15, 9, 0, 0, KiTZ.US_PST)
            val ca = KiTZDateTime(2024, 3, 15, 9, 0, 0, KiTZ["CA/PST"]!!)
            us.offset shouldBe ca.offset
            us.isEqual(ca) shouldBe true
            us shouldNotBe ca
            ca.compareTo(us).shouldBeNegative()   // "CA/PST" < "US/PST"
        }

        test("custom KiTZ differing only in country: ordered by country") {
            val a = KiTZDateTime(2024, 3, 15, 9, 0, 0, KiTZ("XX/ABC", ZoneOffset.ofHours(2), "Alpha"))
            val b = KiTZDateTime(2024, 3, 15, 9, 0, 0, KiTZ("XX/ABC", ZoneOffset.ofHours(2), "Beta"))
            a shouldNotBe b
            a.compareTo(b).shouldBeNegative()
            b.compareTo(a).shouldBePositive()
        }

        test("equal values compare as 0") {
            val copy = KiTZDateTime(tokyo.localDateTime, KiTZ["JP/JST"]!!)
            copy shouldBe tokyo
            copy.compareTo(tokyo) shouldBe 0
        }

        test("isBefore, isAfter and isEqual compare instants only") {
            tokyo.isBefore(utc) shouldBe false
            tokyo.isAfter(utc) shouldBe false
            tokyo.isEqual(pacific) shouldBe true
            utc.isEqual(pacific) shouldBe true
        }

        test("sorted collections keep the same instant in different zones") {
            sortedSetOf(tokyo, utc, pacific).size shouldBe 3
            TreeSet(listOf(tokyo, utc, tokyo)).toList() shouldContainExactly listOf(utc, tokyo)
            listOf(tokyo, utc, pacific).sorted() shouldContainExactly listOf(pacific, utc, tokyo)
        }

        test("nanoseconds take part in the instant") {
            val a = tokyo.withNano(1)
            val b = utc.withNano(2)
            a shouldBeLessThan b   // a is 1 ns earlier, though its local time is later
        }
    }

    context("compareTo is consistent with equals and orders by instant first") {
        // Many values share an instant (a small set of instants in many zones), so
        // ties are exercised as often as distinct instants.
        // Half the zones come from a small set of near-collisions: same offset with
        // different ids, and custom zones differing only in country or offset.
        val tricky = listOf(
            KiTZ.UTC, KiTZ.JP_JST, KiTZ.US_PST, KiTZ["CA/PST"]!!,
            KiTZ("XX/ABC", ZoneOffset.ofHours(2), "Alpha"),
            KiTZ("XX/ABC", ZoneOffset.ofHours(2), "Beta"),
            KiTZ("XX/ABC", ZoneOffset.ofHours(3), "Alpha"),
        )
        val zones = Arb.choice(Arb.element(KiTZ.all().toList()), Arb.element(tricky))
        val instants = Arb.bind(Arb.long(1_700_000_000L, 1_700_000_003L), Arb.int(0, 1)) { s, n ->
            Instant.ofEpochSecond(s, n.toLong())
        }
        val values = Arb.bind(instants, zones) { i, z -> KiTZDateTime.ofInstant(i, z) }

        test("compareTo == 0 exactly when equals") {
            checkAll(5_000, values, values) { a, b ->
                (a.compareTo(b) == 0) shouldBe (a == b)
            }
        }

        test("antisymmetric, and different instants order by instant") {
            checkAll(5_000, values, values) { a, b ->
                a.compareTo(b).sign shouldBe -b.compareTo(a).sign
                if (!a.isEqual(b)) a.compareTo(b).sign shouldBe a.toInstant().compareTo(b.toInstant()).sign
            }
        }

        test("transitive") {
            checkAll(3_000, values, values, values) { a, b, c ->
                if (a <= b && b <= c) (a <= c) shouldBe true
            }
        }

        test("hash-based and sorted collections agree") {
            checkAll(500, Arb.bind(values, values, values, values, values) { a, b, c, d, e -> listOf(a, b, c, d, e) }
                .map { it + it.take(2) }) { list ->
                TreeSet(list).size shouldBe list.toSet().size
            }
        }
    }
})
