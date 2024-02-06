package io.kixi.core.text

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests for ki.text.Text+
 *
 * TODO: Exercise all functions in Text+
 */
class TextTest {

    @Test fun testKiIdentifiers() {
        assertTrue("Foo".isKiIdentifier())
        assertTrue("foo5".isKiIdentifier())
        assertTrue("😀".isKiIdentifier(), "Test single emoji")
        assertTrue("👽alien".isKiIdentifier(), "Test emoji start")
        // assertTrue("👨‍👩‍👦‍👦".isKiIdentifier()) // TODO: Broken. Allow non-BMP emoji.
    }

    @Test fun testEscapeCharResolution() {
        assertEquals("São Paulo", "S\\u00e3o Paulo".resolveEscapes())
        assertEquals("\t\r\n\\", "\\t\\r\\n\\\\".resolveEscapes())
        assertEquals("👌", "\\ud83d\\udc4c".resolveEscapes())
    }

    @Test fun testNotKiIdentifiers() {
        assertFalse("".isKiIdentifier(), "Test false for the empty string.")
        assertFalse("\"_\"".isKiIdentifier(), "\"_\" is reserved for open " +
                "ended ranges.")
        assertFalse("5Foo".isKiIdentifier(), "Identifiers can't start " +
                "with numbers")
        assertFalse("f-oo".isKiIdentifier(), "Identifiers can't contain dashes")
    }
}
