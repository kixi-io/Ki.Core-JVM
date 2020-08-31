package io.kixi.uom

import org.junit.jupiter.api.Assertions.assertEquals
import io.kixi.uom.Quantity.Companion.length
import io.kixi.uom.Quantity.Companion.area
import io.kixi.uom.Quantity.Companion.volume
import io.kixi.uom.Quantity.Companion.mass
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class QuantityTest {

    @Test fun testParsing() {
        assertEquals(Q(5, Unit.cm), length("5cm"))
        assertEquals(Q(BigDecimal("8.2"), Unit.kg), mass("8.2kg"))
        assertEquals(Q(BigDecimal("-43.2"), Unit.kg), mass("-43.2kg"))
        assertEquals("8.3kg", Q(BigDecimal("8.3"), Unit.kg).toString())
        assertEquals("8.2kg:d", Q(8.2, Unit.kg).toString())

        // Test liter with LT and ℓ
        assertEquals(volume("5ℓ"), volume("5LT"))
    }

    @Test fun testExponents() {
        assertEquals("24m²", area("24m2").toString())
        assertEquals("4.8km³", area("4.8km3").toString())
    }

    @Test fun testConversion() {
        assertEquals(Q(5, Unit.cm) convertTo Unit.mm, length("50mm"))
        assertEquals(Q(500, Unit.m) convertTo Unit.km, length(".5km"))
    }

    @Test fun numberOperandOperations() {
        assertEquals(length("3cm"), length("5cm") - 2)
        assertEquals(length("25mm"), length("24mm") + 1)

        // Note: Quantities with fractional amounts default to BigDecimal
        assertEquals(area("5.5cm2"), area("5cm2") + BigDecimal(".5"))
        assertEquals(area("5.5cm2:d"), area("5cm2") + .5)
    }

    @Test fun quantityOperandOperations() {
        assertEquals(length("22mm"), length("2cm") + length("2mm"))
        assertEquals(length("34mm"), length("24mm") + length("1cm"))

        assertEquals(length("18mm"), length("2cm") - length("2mm"))
        assertEquals(length("14mm"), length("24mm") - length("1cm"))
    }

    companion object {
        fun <T:Unit>Q(value:Number, unit:T) = Quantity(value, unit)
    }
}
