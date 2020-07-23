package ki.text

import ki.text.isKiIdentifier

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TextTest {

    @Test fun testKiIdentifiers() {
        assertTrue("Foo".isKiIdentifier())
        assertTrue("foo5".isKiIdentifier())
        assertTrue("😀".isKiIdentifier(), "Test single emoji")
        assertTrue("👽alien".isKiIdentifier(), "Test emoji start")
        // assertTrue("👨‍👩‍👦‍👦 $IS_ID".isKiIdentifier()) // TODO: Broken. Allow non-BMP emoji.
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
