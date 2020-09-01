package io.kixi

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

    @Test fun testQuentityTypes() {
        assertEquals("Quantity<cm>", QuantityDef(false, Unit.cm, Type.Decimal).toString())
        assertEquals("Quantity<cm:L>?", QuantityDef(true, Unit.cm, Type.Long).toString())
    }

    @Test fun testGenericStructureTypes() {
        assertEquals("Range<Version>", RangeDef(false, TypeDef.Version).toString())
        val listDef = ListDef(false, TypeDef.Int)
        assertEquals("List<Int>", listDef.toString())
        assertEquals("Map<String, Int?>?", MapDef(true, TypeDef.String, TypeDef.Int_N).toString())
        assertEquals("Map<String, List<Int>>", MapDef(false, TypeDef.String, listDef).toString())
    }
}