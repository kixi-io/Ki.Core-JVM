package io.kixi

import io.kixi.uom.Length
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tests for convenience methods in the Ki class
 */
class KiTest {

    @Test fun simpleTypes() {
        assertEquals("Dec", TypeDef.Dec.toString())
        assertEquals("Dec?", TypeDef.Dec_N.toString())
    }

    @Test fun quantityTypes() {
        assertEquals("Quantity<Length>", QuantityDef(false, Length::class, Type.Dec).toString())
        assertEquals("Quantity<Length:L>?", QuantityDef(true, Length::class, Type.Long).toString())
    }

    @Test fun genericStructureTypes() {
        assertEquals("Range<Version>", RangeDef(false, TypeDef.Version).toString())
        val listDef = ListDef(false, TypeDef.Int)
        assertEquals("List<Int>", listDef.toString())
        assertEquals("Map<String, Int?>?", MapDef(true, TypeDef.String, TypeDef.Int_N).toString())
        assertEquals("Map<String, List<Int>>", MapDef(false, TypeDef.String, listDef).toString())
    }
}