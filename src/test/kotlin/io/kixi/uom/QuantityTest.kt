package io.kixi.uom

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class QuantityTest {

    @Test fun testParsing() {
        assertEquals(Quantity(5, Unit.cm), Quantity("5cm"))
        assertEquals(Quantity(BigDecimal("8.2"), Unit.kg), Quantity("8.2kg"))
        assertEquals(Quantity(BigDecimal("-43.2"), Unit.kg), Quantity("-43.2kg"))
        assertEquals("8.3kg", Quantity(BigDecimal("8.3"), Unit.kg).toString())
        assertEquals("8.2kg:d", Quantity(8.2, Unit.kg).toString())

        // Test liter with LT and ℓ
        assertEquals(Quantity("5ℓ"), Quantity("5LT"))
    }

    @Test fun testExponents() {
        assertEquals("24m²", Quantity("24m2").toString())
        assertEquals("4.8km³", Quantity("4.8km3").toString())
    }

    @Test fun testConversion() {
        assertEquals(Quantity(5, Unit.cm) convertTo Unit.mm, Quantity("50mm"))
        assertEquals(Quantity(500, Unit.m) convertTo Unit.km, Quantity(".5km"))
    }

    @Test fun numberOperandOperations() {
        assertEquals(Quantity("3cm"), Quantity("5cm") - 2)
        assertEquals(Quantity("25mm"), Quantity("24mm") + 1)

        // Note: Quantities with fractional amounts default to BigDecimal
        assertEquals(Quantity("5.5cm2"), Quantity("5cm2") + BigDecimal(".5"))
        assertEquals(Quantity("5.5cm2:d"), Quantity("5cm2") + .5)
    }

    @Test fun quantityOperandOperations() {
        assertEquals(Quantity("22mm"), Quantity("2cm") + Quantity("2mm"))
        assertEquals(Quantity("34mm"), Quantity("24mm") + Quantity("1cm"))

        assertEquals(Quantity("18mm"), Quantity("2cm") - Quantity("2mm"))
        assertEquals(Quantity("14mm"), Quantity("24mm") - Quantity("1cm"))
    }
}
