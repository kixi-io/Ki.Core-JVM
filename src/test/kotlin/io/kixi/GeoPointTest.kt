package io.kixi

import io.kixi.text.ParseException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.string.shouldEndWith
import io.kotest.property.Arb
import io.kotest.property.arbitrary.double
import io.kotest.property.arbitrary.numericDouble
import io.kotest.property.checkAll
import java.math.BigDecimal as Dec

class GeoPointTest : StringSpec({

    // ========== Creation Tests ==========

    "should create GeoPoint from Double coordinates" {
        val point = GeoPoint.of(37.7749, -122.4194)
        point.lat shouldBe (37.7749 plusOrMinus 0.0001)
        point.lon shouldBe (-122.4194 plusOrMinus 0.0001)
        point.alt shouldBe null
    }

    "should create GeoPoint with altitude" {
        val point = GeoPoint.of(35.6762, 139.6503, 40.0)
        point.lat shouldBe (35.6762 plusOrMinus 0.0001)
        point.lon shouldBe (139.6503 plusOrMinus 0.0001)
        point.alt shouldBe (40.0 plusOrMinus 0.0001)
        point.hasAltitude shouldBe true
    }

    "should create GeoPoint from BigDecimal coordinates" {
        val point = GeoPoint.of(
            Dec("37.774900"),
            Dec("-122.419400")
        )
        point.latitude shouldBe Dec("37.774900")
        point.longitude shouldBe Dec("-122.419400")
    }

    "should reject latitude out of range" {
        shouldThrow<IllegalArgumentException> {
            GeoPoint.of(91.0, 0.0)
        }.message shouldContain "Latitude must be between -90 and +90"

        shouldThrow<IllegalArgumentException> {
            GeoPoint.of(-90.1, 0.0)
        }.message shouldContain "Latitude must be between -90 and +90"
    }

    "should reject longitude out of range" {
        shouldThrow<IllegalArgumentException> {
            GeoPoint.of(0.0, 180.1)
        }.message shouldContain "Longitude must be between -180 and +180"

        shouldThrow<IllegalArgumentException> {
            GeoPoint.of(0.0, -181.0)
        }.message shouldContain "Longitude must be between -180 and +180"
    }

    "should accept boundary values" {
        GeoPoint.of(90.0, 0.0).lat shouldBe 90.0
        GeoPoint.of(-90.0, 0.0).lat shouldBe -90.0
        GeoPoint.of(0.0, 180.0).lon shouldBe 180.0
        GeoPoint.of(0.0, -180.0).lon shouldBe -180.0
    }

    // ========== Parsing Tests ==========

    "should parse basic geo literal" {
        val point = GeoPoint.parse(".geo(37.7749, -122.4194)")
        point.lat shouldBe (37.7749 plusOrMinus 0.0001)
        point.lon shouldBe (-122.4194 plusOrMinus 0.0001)
        point.hasAltitude shouldBe false
    }

    "should parse geo literal with altitude" {
        val point = GeoPoint.parse(".geo(35.6762, 139.6503, 40.0)")
        point.lat shouldBe (35.6762 plusOrMinus 0.0001)
        point.lon shouldBe (139.6503 plusOrMinus 0.0001)
        point.alt shouldBe (40.0 plusOrMinus 0.0001)
    }

    "should parse geo literal with whitespace" {
        val point = GeoPoint.parse("  .geo( 37.7749 , -122.4194 )  ")
        point.lat shouldBe (37.7749 plusOrMinus 0.0001)
        point.lon shouldBe (-122.4194 plusOrMinus 0.0001)
    }

    "should parse geo literal with high precision" {
        val point = GeoPoint.parse(".geo(37.77490000001, -122.41940000002)")
        point.latitude.toPlainString() shouldContain "37.77490000001"
        point.longitude.toPlainString() shouldContain "-122.41940000002"
    }

    "should parse negative coordinates" {
        val point = GeoPoint.parse(".geo(-33.8688, -151.2093)")
        point.lat shouldBe (-33.8688 plusOrMinus 0.0001)
        point.lon shouldBe (-151.2093 plusOrMinus 0.0001)
    }

    "should parse origin coordinates" {
        val point = GeoPoint.parse(".geo(0, 0)")
        point.lat shouldBe 0.0
        point.lon shouldBe 0.0
        point.isOrigin shouldBe true
    }

    "should parse integer coordinates" {
        val point = GeoPoint.parse(".geo(37, -122)")
        point.lat shouldBe 37.0
        point.lon shouldBe -122.0
    }

    "should fail parsing empty literal" {
        shouldThrow<ParseException> {
            GeoPoint.parse("")
        }
    }

    "should fail parsing literal without prefix" {
        shouldThrow<ParseException> {
            GeoPoint.parse("37.7749, -122.4194")
        }.message shouldContain "must start with '.geo('"
    }

    "should fail parsing literal without closing paren" {
        shouldThrow<ParseException> {
            GeoPoint.parse(".geo(37.7749, -122.4194")
        }.message shouldContain "must end with ')'"
    }

    "should fail parsing with only one coordinate" {
        shouldThrow<ParseException> {
            GeoPoint.parse(".geo(37.7749)")
        }.message shouldContain "requires at least latitude and longitude"
    }

    "should fail parsing with too many coordinates" {
        shouldThrow<ParseException> {
            GeoPoint.parse(".geo(37.7749, -122.4194, 40.0, 100.0)")
        }.message shouldContain "at most 3 components"
    }

    "should fail parsing with invalid number" {
        shouldThrow<ParseException> {
            GeoPoint.parse(".geo(abc, -122.4194)")
        }.message shouldContain "Invalid number format"
    }

    "should fail parsing with out-of-range latitude" {
        shouldThrow<ParseException> {
            GeoPoint.parse(".geo(91.0, -122.4194)")
        }.message shouldContain "Latitude must be between -90 and +90"
    }

    "should fail parsing with out-of-range longitude" {
        shouldThrow<ParseException> {
            GeoPoint.parse(".geo(37.7749, 181.0)")
        }.message shouldContain "Longitude must be between -180 and +180"
    }

    "parseOrNull should return null for invalid input" {
        GeoPoint.parseOrNull("invalid") shouldBe null
        GeoPoint.parseOrNull(".geo(invalid)") shouldBe null
        GeoPoint.parseOrNull(".geo(91, 0)") shouldBe null
    }

    "parseOrNull should return GeoPoint for valid input" {
        val point = GeoPoint.parseOrNull(".geo(37.7749, -122.4194)")
        point shouldNotBe null
        point!!.lat shouldBe (37.7749 plusOrMinus 0.0001)
    }

    // ========== Formatting Tests ==========

    "toString should produce valid Ki literal" {
        val point = GeoPoint.of(37.7749, -122.4194)
        val str = point.toString()
        str shouldStartWith ".geo("
        str shouldEndWith ")"
        str shouldContain "37.774900"
        str shouldContain "-122.419400"
    }

    "toString should include altitude when present" {
        val point = GeoPoint.of(35.6762, 139.6503, 40.0)
        val str = point.toString()
        str shouldContain "40.000000"
    }

    "toString with custom precision" {
        val point = GeoPoint.of(37.7749123456, -122.4194987654)
        point.toString(2) shouldContain "37.77"
        point.toString(2) shouldContain "-122.42"
    }

    "toCompactString should strip trailing zeros" {
        val point = GeoPoint.of(37.0, -122.0)
        val str = point.toCompactString()
        str shouldBe ".geo(37, -122)"
    }

    "round-trip parsing and formatting" {
        val original = GeoPoint.of(37.7749, -122.4194, 40.5)
        val formatted = original.toString()
        val parsed = GeoPoint.parse(formatted)
        parsed.lat shouldBe (original.lat plusOrMinus 0.000001)
        parsed.lon shouldBe (original.lon plusOrMinus 0.000001)
        parsed.alt shouldBe (original.alt!! plusOrMinus 0.000001)
    }

    "toDecimalDegrees format" {
        val point = GeoPoint.of(37.7749, -122.4194)
        val dd = point.toDecimalDegrees()
        dd shouldContain "N"
        dd shouldContain "W"
        dd shouldContain "37"
        dd shouldContain "122"
    }

    "toDecimalDegrees with southern/eastern coordinates" {
        val point = GeoPoint.of(-33.8688, 151.2093)
        val dd = point.toDecimalDegrees()
        dd shouldContain "S"
        dd shouldContain "E"
    }

    "toDMS format" {
        val point = GeoPoint.of(37.7749, -122.4194)
        val dms = point.toDMS()
        dms shouldContain "37°"
        dms shouldContain "122°"
        dms shouldContain "\""
    }

    // ========== isLiteral Tests ==========

    "isLiteral should return true for valid formats" {
        GeoPoint.isLiteral(".geo(37.7749, -122.4194)") shouldBe true
        GeoPoint.isLiteral("  .geo(0, 0)  ") shouldBe true
        GeoPoint.isLiteral(".geo(1,2,3)") shouldBe true
    }

    "isLiteral should return false for invalid formats" {
        GeoPoint.isLiteral("geo(37, -122)") shouldBe false
        GeoPoint.isLiteral(".blob(abc)") shouldBe false
        GeoPoint.isLiteral("37.7749, -122.4194") shouldBe false
    }

    // ========== Hemisphere Tests ==========

    "should identify Northern hemisphere" {
        GeoPoint.of(45.0, 0.0).isNorthern shouldBe true
        GeoPoint.of(45.0, 0.0).isSouthern shouldBe false
    }

    "should identify Southern hemisphere" {
        GeoPoint.of(-45.0, 0.0).isSouthern shouldBe true
        GeoPoint.of(-45.0, 0.0).isNorthern shouldBe false
    }

    "should identify Eastern hemisphere" {
        GeoPoint.of(0.0, 90.0).isEastern shouldBe true
        GeoPoint.of(0.0, 90.0).isWestern shouldBe false
    }

    "should identify Western hemisphere" {
        GeoPoint.of(0.0, -90.0).isWestern shouldBe true
        GeoPoint.of(0.0, -90.0).isEastern shouldBe false
    }

    "equator point is neither northern nor southern" {
        val point = GeoPoint.of(0.0, 0.0)
        point.isNorthern shouldBe false
        point.isSouthern shouldBe false
    }

    "prime meridian point is neither eastern nor western" {
        val point = GeoPoint.of(0.0, 0.0)
        point.isEastern shouldBe false
        point.isWestern shouldBe false
    }

    // ========== Distance Calculation Tests ==========

    "distance between same point should be zero" {
        val point = GeoPoint.of(37.7749, -122.4194)
        point.distanceTo(point) shouldBe (0.0 plusOrMinus 0.001)
    }

    "distance from SF to LA should be approximately 560km" {
        val sf = GeoPoint.of(37.7749, -122.4194)
        val la = GeoPoint.of(34.0522, -118.2437)
        sf.distanceTo(la) shouldBe (559.0 plusOrMinus 10.0)  // ~559 km
    }

    "distance from London to Paris should be approximately 344km" {
        val london = GeoPoint.of(51.5074, -0.1278)
        val paris = GeoPoint.of(48.8566, 2.3522)
        london.distanceTo(paris) shouldBe (344.0 plusOrMinus 5.0)
    }

    "distance from Tokyo to Sydney should be approximately 7820km" {
        val tokyo = GeoPoint.of(35.6762, 139.6503)
        val sydney = GeoPoint.of(-33.8688, 151.2093)
        tokyo.distanceTo(sydney) shouldBe (7820.0 plusOrMinus 50.0)
    }

    "distance should be symmetric" {
        val sf = GeoPoint.of(37.7749, -122.4194)
        val tokyo = GeoPoint.of(35.6762, 139.6503)
        sf.distanceTo(tokyo) shouldBe (tokyo.distanceTo(sf) plusOrMinus 0.001)
    }

    // ========== Bearing Calculation Tests ==========

    "bearing due north should be 0 degrees" {
        val start = GeoPoint.of(0.0, 0.0)
        val end = GeoPoint.of(10.0, 0.0)
        start.bearingTo(end) shouldBe (0.0 plusOrMinus 1.0)
    }

    "bearing due east should be 90 degrees" {
        val start = GeoPoint.of(0.0, 0.0)
        val end = GeoPoint.of(0.0, 10.0)
        start.bearingTo(end) shouldBe (90.0 plusOrMinus 1.0)
    }

    "bearing due south should be 180 degrees" {
        val start = GeoPoint.of(10.0, 0.0)
        val end = GeoPoint.of(0.0, 0.0)
        start.bearingTo(end) shouldBe (180.0 plusOrMinus 1.0)
    }

    "bearing due west should be 270 degrees" {
        val start = GeoPoint.of(0.0, 10.0)
        val end = GeoPoint.of(0.0, 0.0)
        start.bearingTo(end) shouldBe (270.0 plusOrMinus 1.0)
    }

    // ========== Destination Calculation Tests ==========

    "destination 1000km north from equator" {
        val start = GeoPoint.of(0.0, 0.0)
        val dest = start.destination(1000.0, 0.0)
        dest.lat shouldBeGreaterThan 0.0
        dest.lon shouldBe (0.0 plusOrMinus 0.001)
    }

    "destination 100km east from equator" {
        val start = GeoPoint.of(0.0, 0.0)
        val dest = start.destination(100.0, 90.0)
        dest.lat shouldBe (0.0 plusOrMinus 0.1)
        dest.lon shouldBeGreaterThan 0.0
    }

    "destination round trip should return near original" {
        val start = GeoPoint.of(37.7749, -122.4194)
        val dest = start.destination(500.0, 45.0)
        val returned = dest.destination(500.0, 225.0)  // Reverse bearing
        returned.lat shouldBe (start.lat plusOrMinus 0.5)  // Some error due to spherical geometry
        returned.lon shouldBe (start.lon plusOrMinus 0.5)
    }

    // ========== Altitude Operations ==========

    "withAltitude should add altitude" {
        val point = GeoPoint.of(37.7749, -122.4194)
        val withAlt = point.withAltitude(100.0)
        withAlt.alt shouldBe 100.0
        withAlt.hasAltitude shouldBe true
    }

    "withAltitude should replace existing altitude" {
        val point = GeoPoint.of(37.7749, -122.4194, 50.0)
        val withAlt = point.withAltitude(100.0)
        withAlt.alt shouldBe 100.0
    }

    "withoutAltitude should remove altitude" {
        val point = GeoPoint.of(37.7749, -122.4194, 100.0)
        val withoutAlt = point.withoutAltitude()
        withoutAlt.hasAltitude shouldBe false
        withoutAlt.alt shouldBe null
    }

    "withoutAltitude on point without altitude should return same" {
        val point = GeoPoint.of(37.7749, -122.4194)
        point.withoutAltitude() shouldBe point
    }

    // ========== Equality and Comparison Tests ==========

    "equal points should be equal" {
        val p1 = GeoPoint.of(37.7749, -122.4194)
        val p2 = GeoPoint.of(37.7749, -122.4194)
        p1 shouldBe p2
        p1.hashCode() shouldBe p2.hashCode()
    }

    "equal points with altitude should be equal" {
        val p1 = GeoPoint.of(37.7749, -122.4194, 100.0)
        val p2 = GeoPoint.of(37.7749, -122.4194, 100.0)
        p1 shouldBe p2
    }

    "different points should not be equal" {
        val p1 = GeoPoint.of(37.7749, -122.4194)
        val p2 = GeoPoint.of(37.7750, -122.4194)
        p1 shouldNotBe p2
    }

    "points with different altitude should not be equal" {
        val p1 = GeoPoint.of(37.7749, -122.4194, 100.0)
        val p2 = GeoPoint.of(37.7749, -122.4194, 200.0)
        p1 shouldNotBe p2
    }

    "point with altitude should not equal point without" {
        val p1 = GeoPoint.of(37.7749, -122.4194, 100.0)
        val p2 = GeoPoint.of(37.7749, -122.4194)
        p1 shouldNotBe p2
    }

    "compareTo should order by latitude first" {
        val north = GeoPoint.of(50.0, 0.0)
        val south = GeoPoint.of(40.0, 0.0)
        north shouldBeGreaterThan south
    }

    "compareTo should order by longitude second" {
        val east = GeoPoint.of(0.0, 50.0)
        val west = GeoPoint.of(0.0, 40.0)
        east shouldBeGreaterThan west
    }

    "compareTo should order by altitude third" {
        val high = GeoPoint.of(0.0, 0.0, 100.0)
        val low = GeoPoint.of(0.0, 0.0, 50.0)
        high shouldBeGreaterThan low
    }

    "compareTo with null altitude should be less than with altitude" {
        val withAlt = GeoPoint.of(0.0, 0.0, 100.0)
        val withoutAlt = GeoPoint.of(0.0, 0.0)
        withoutAlt shouldBeLessThan withAlt
    }

    // ========== Well-known Points ==========

    "ORIGIN should be at (0, 0)" {
        GeoPoint.ORIGIN.lat shouldBe 0.0
        GeoPoint.ORIGIN.lon shouldBe 0.0
        GeoPoint.ORIGIN.isOrigin shouldBe true
    }

    "NORTH_POLE should be at (90, 0)" {
        GeoPoint.NORTH_POLE.lat shouldBe 90.0
        GeoPoint.NORTH_POLE.lon shouldBe 0.0
    }

    "SOUTH_POLE should be at (-90, 0)" {
        GeoPoint.SOUTH_POLE.lat shouldBe -90.0
        GeoPoint.SOUTH_POLE.lon shouldBe 0.0
    }

    // ========== DMS Creation Tests ==========

    "fromDMS for San Francisco" {
        val point = GeoPoint.fromDMS(37, 46, 30.0, 'N', 122, 25, 10.0, 'W')
        point.lat shouldBe (37.775 plusOrMinus 0.01)
        point.lon shouldBe (-122.42 plusOrMinus 0.01)
    }

    "fromDMS for Sydney" {
        val point = GeoPoint.fromDMS(33, 52, 10.0, 'S', 151, 12, 30.0, 'E')
        point.lat shouldBe (-33.87 plusOrMinus 0.01)
        point.lon shouldBe (151.21 plusOrMinus 0.01)
    }

    "fromDMS should reject invalid direction" {
        shouldThrow<IllegalArgumentException> {
            GeoPoint.fromDMS(37, 46, 30.0, 'X', 122, 25, 10.0, 'W')
        }
        shouldThrow<IllegalArgumentException> {
            GeoPoint.fromDMS(37, 46, 30.0, 'N', 122, 25, 10.0, 'Z')
        }
    }

    // ========== Center Calculation Tests ==========

    "center of single point should be that point" {
        val point = GeoPoint.of(37.7749, -122.4194)
        val center = GeoPoint.center(listOf(point))
        center.lat shouldBe (point.lat plusOrMinus 0.0001)
        center.lon shouldBe (point.lon plusOrMinus 0.0001)
    }

    "center of opposite points on equator should be at equator" {
        val west = GeoPoint.of(0.0, -90.0)
        val east = GeoPoint.of(0.0, 90.0)
        val center = GeoPoint.center(listOf(west, east))
        center.lat shouldBe (0.0 plusOrMinus 0.1)
    }

    "center of empty list should throw" {
        shouldThrow<IllegalArgumentException> {
            GeoPoint.center(emptyList())
        }
    }

    // ========== Bounding Box Tests ==========

    "bounding box of single point" {
        val point = GeoPoint.of(37.7749, -122.4194)
        val (sw, ne) = GeoPoint.boundingBox(listOf(point))
        sw shouldBe point
        ne shouldBe point
    }

    "bounding box of multiple points" {
        val points = listOf(
            GeoPoint.of(37.0, -122.0),
            GeoPoint.of(38.0, -121.0),
            GeoPoint.of(37.5, -122.5)
        )
        val (sw, ne) = GeoPoint.boundingBox(points)
        sw.lat shouldBe (37.0 plusOrMinus 0.001)
        sw.lon shouldBe (-122.5 plusOrMinus 0.001)
        ne.lat shouldBe (38.0 plusOrMinus 0.001)
        ne.lon shouldBe (-121.0 plusOrMinus 0.001)
    }

    "bounding box of empty list should throw" {
        shouldThrow<IllegalArgumentException> {
            GeoPoint.boundingBox(emptyList())
        }
    }

    // ========== Property-Based Tests ==========

    "any valid coordinates should create valid GeoPoint" {
        checkAll(
            Arb.numericDouble(-90.0, 90.0),
            Arb.numericDouble(-180.0, 180.0)
        ) { lat, lon ->
            val point = GeoPoint.of(lat, lon)
            point.lat shouldBe (lat plusOrMinus 0.0001)
            point.lon shouldBe (lon plusOrMinus 0.0001)
        }
    }

    "toString and parse should be inverses" {
        checkAll(
            Arb.numericDouble(-90.0, 90.0),
            Arb.numericDouble(-180.0, 180.0)
        ) { lat, lon ->
            val original = GeoPoint.of(lat, lon)
            val formatted = original.toString()
            val parsed = GeoPoint.parse(formatted)
            parsed.lat shouldBe (original.lat plusOrMinus 0.0001)
            parsed.lon shouldBe (original.lon plusOrMinus 0.0001)
        }
    }

    "distance is always non-negative" {
        checkAll(
            Arb.numericDouble(-90.0, 90.0),
            Arb.numericDouble(-180.0, 180.0),
            Arb.numericDouble(-90.0, 90.0),
            Arb.numericDouble(-180.0, 180.0)
        ) { lat1, lon1, lat2, lon2 ->
            val p1 = GeoPoint.of(lat1, lon1)
            val p2 = GeoPoint.of(lat2, lon2)
            p1.distanceTo(p2) shouldBeGreaterThan -0.001
        }
    }
})