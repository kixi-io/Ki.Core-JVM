package io.kixi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.format.DateTimeFormatter

class BlobTest {

    @Test fun testBlob() {
        var blobLiteral = ".blob(SGVsbG8=)"

        var byteArray = Ki.parseBlob(blobLiteral)
        assertEquals(blobLiteral, Ki.formatBlob(byteArray))
    }

    @Test fun testHello() {
        assertEquals("Hello",
            String(Ki.parseBlob(".blob(SGVsbG8=)")))
    }

}