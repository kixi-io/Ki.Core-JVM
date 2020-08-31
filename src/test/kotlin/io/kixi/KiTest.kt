package io.kixi

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import io.kixi.uom.Unit

/**
 * Tests for convenience methods in the Ki class
 */
class KiTest {

    @Test fun testSimpleTypes() {
        assertEquals("Decimal", Ki.TypeDef.Decimal.toString())
        assertEquals("Decimal?", Ki.TypeDef.Decimal_N.toString())
    }

    @Test fun testQuentityTypes() {
        assertEquals("Quantity<cm>", Ki.QuantityDef(false, Unit.cm, Ki.Type.Decimal).toString())
        assertEquals("Quantity<cm:L>?", Ki.QuantityDef(true, Unit.cm, Ki.Type.Long).toString())
    }

    @Test fun testGenericStructureTypes() {
        assertEquals("Range<Version>", Ki.RangeDef(false, Ki.TypeDef.Version).toString())
        val listDef = Ki.ListDef(false, Ki.TypeDef.Int)
        assertEquals("List<Int>", listDef.toString())
        assertEquals("Map<String, Int?>?", Ki.MapDef(true, Ki.TypeDef.String, Ki.TypeDef.Int_N).toString())
        assertEquals("Map<String, List<Int>>", Ki.MapDef(false, Ki.TypeDef.String, listDef).toString())
    }
}