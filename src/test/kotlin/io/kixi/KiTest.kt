package io.kixi

import io.kixi.uom.Length
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.kixi.uom.Unit

/**
 * Tests for convenience methods in the Ki class
 */
class KiTest {

    @Test fun testSimpleTypes() {
        assertEquals("Decimal", TypeDef.Decimal.toString())
        assertEquals("Decimal?", TypeDef.Decimal_N.toString())
    }

    @Test fun testQuantityTypes() {
        assertEquals("Quantity<Length>", QuantityDef(false, Length::class, Type.Decimal).toString())
        assertEquals("Quantity<Length:L>?", QuantityDef(true, Length::class, Type.Long).toString())
    }

    @Test fun testGenericStructureTypes() {
        assertEquals("Range<Version>", RangeDef(false, TypeDef.Version).toString())
        val listDef = ListDef(false, TypeDef.Int)
        assertEquals("List<Int>", listDef.toString())
        assertEquals("Map<String, Int?>?", MapDef(true, TypeDef.String, TypeDef.Int_N).toString())
        assertEquals("Map<String, List<Int>>", MapDef(false, TypeDef.String, listDef).toString())
    }
}