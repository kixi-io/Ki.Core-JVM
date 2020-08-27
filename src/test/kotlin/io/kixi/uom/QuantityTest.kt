package io.kixi.uom

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class QuantityTest {

    @Test fun testParsing() {
        assertEquals(Quantity(5, Unit.cm), Quantity.parse("5cm"))
        assertEquals(Quantity(BigDecimal("8.2"), Unit.kg), Quantity.parse("8.2kg"))
    }

    @Test fun testConversion() {
        assertEquals(Quantity(5, Unit.cm) convertTo Unit.mm, Quantity.parse("50mm"))
        assertEquals(Quantity(500, Unit.m) convertTo Unit.km, Quantity.parse(".5km"))
    }
}
