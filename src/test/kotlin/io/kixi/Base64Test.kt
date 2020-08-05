package io.kixi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.format.DateTimeFormatter

class Base64Test {

    @Test fun testBase64() {
        var base64Literal = ".base64(SGVsbG8=)"

        var byteArray = Ki.parseBase64(base64Literal)
        assertEquals(base64Literal, Ki.formatBase64(byteArray))
    }

}