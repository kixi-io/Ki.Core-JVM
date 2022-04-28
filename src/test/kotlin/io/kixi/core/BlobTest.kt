package io.kixi.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BlobTest {

    @Test fun basic() {
        val blobLiteral = ".blob(SGVsbG8=)"

        val byteArray = Ki.parseBlob(blobLiteral)
        assertEquals(blobLiteral, Ki.formatBlob(byteArray))
    }

    @Test fun hello() {
        assertEquals("Hello",
            String(Ki.parseBlob(".blob(SGVsbG8=)")))
    }
}
