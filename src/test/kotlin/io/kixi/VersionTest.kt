package io.kixi

import io.kixi.text.ParseException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VersionTest {

    @Test fun testVersionParser() {
        assertEquals(Version.parse("5.2.3").toString(), "5.2.3")
        assertEquals(Version.parse("05.02.03").toString(), "5.2.3")
        assertEquals(Version.parse("5.2").toString(), "5.2.0")
        assertEquals(Version.parse("5").toString(), "5.0.0")
        assertEquals(
            Version.parse("5.2.3-alpha").toString(),
            "5.2.3-alpha")
        assertEquals(
            Version.parse("5.2.3-alpha-5").toString(),
            "5.2.3-alpha-5")
        assertEquals(
            Version.parse("5.2.3-alpha5").toString(),
            "5.2.3-alpha-5")
    }

    @Test fun testVersionParserInvalid() {
        assertThrows(ParseException::class.java) { Version.parse("-5.0.1") }
        assertThrows(ParseException::class.java) { Version.parse("5.-0.1") }
        assertThrows(ParseException::class.java) { Version.parse("5.0.-1") }
        assertThrows(ParseException::class.java) { Version.parse("5.") }
        assertThrows(ParseException::class.java) { Version.parse("5.0-") }
        assertThrows(ParseException::class.java) { Version.parse("-") }
    }

    /**
     * Check for comparison of numeric components, qualifiers and qualifier numbers,
     * including cases where one qualifier isn't present.
     */
    @Test fun testCompareTo() {
        assertEquals(Version.parse("5"), Version.parse("5.0.0"))
        assertTrue(Version(5) > Version(4))
        assertTrue(Version.parse("5") > Version.parse("5-alpha"))
        assertTrue(Version.parse("5.8-alpha") < Version.parse("5.8"))
        assertTrue(Version.parse("5.2-alpha") < Version.parse("5.2-beta"))
        assertTrue(Version.parse("7.2-beta-2") > Version.parse("7.2-beta-1"))
    }
}
