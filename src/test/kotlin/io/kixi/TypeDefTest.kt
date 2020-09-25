package io.kixi

import io.kixi.uom.Length
import io.kixi.uom.Quantity
import io.kixi.uom.Unit
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class TypeDefTest {
    @Test fun bools() {
        assertTrue(TypeDef.Bool.matches(true))
        assertTrue(TypeDef.Bool.matches(false))
        assertFalse(TypeDef.Bool.matches(5))
        assertTrue(TypeDef.Bool_N.matches(null))
        assertFalse(TypeDef.Bool.matches(null))
    }

    @Test fun numbers() {
        assertTrue(TypeDef.Int.matches(5))
        assertFalse(TypeDef.Int.matches(6L))
        assertFalse(TypeDef.Int.matches(null))
        assertTrue(TypeDef.Int_N.matches(null))

        assertTrue(TypeDef.Number.matches(5))
        assertTrue(TypeDef.Number.matches(6L))
        assertFalse(TypeDef.Number.matches(true))
        assertFalse(TypeDef.Number.matches(null))
        assertTrue(TypeDef.Number_N.matches(null))
    }

    @Test fun quantities() {
        assertTrue(QuantityDef(true, Length::class, Type.Dec).matches(
            Quantity("5.0", Unit.cm)
        ))
        assertFalse(QuantityDef(true, Length::class, Type.Dec).matches(
            Quantity(5, Unit.cm)
        ))
        assertFalse(QuantityDef(true, Length::class, Type.Dec).matches(
            Quantity("5.0", Unit.g)
        ))
    }

    @Test fun ranges() {
        assertTrue(RangeDef(true, TypeDef.Int).matches(Range(1,5)))
        assertFalse(RangeDef(true, TypeDef.Int).matches(Range(1.0,5.2)))
    }

    @Test fun lists() {
        assertTrue(ListDef(false, TypeDef.Int).matches(emptyList<Int>()))
        assertTrue(ListDef(false, TypeDef.Int).matches(listOf(1, 2, 3)))
        assertTrue(ListDef(false, TypeDef.Number).matches(listOf(1, 2, 3.0)))
        assertFalse(ListDef(false, TypeDef.Int).matches(listOf(1, null, 3)))
        assertTrue(ListDef(false, TypeDef.Int_N).matches(listOf(1, null, 3)))

        // Check List of Lists & Maps

        val listOfLists = ListDef(false, ListDef(false, TypeDef.Int))
        assertTrue(listOfLists.matches(listOf(listOf(1,2), listOf(3,4), listOf(5,6))))
        assertFalse(listOfLists.matches(listOf(listOf(1,2), listOf(3,4), listOf(5L,6L))))

        val listOfMaps = ListDef(false, MapDef(false, TypeDef.Char, TypeDef.Int))
        assertTrue(listOfMaps.matches(listOf(mapOf('1' to 2), mapOf('3' to 4), mapOf('5' to 6))))
        assertFalse(listOfMaps.matches(listOf(mapOf('1' to 2), mapOf('3' to 4), mapOf('5' to 6L))))
    }

    @Test fun maps() {
        assertTrue(MapDef(false, TypeDef.String, TypeDef.Int)
            .matches(emptyMap<String, Int>()))
        assertTrue(MapDef(false, TypeDef.String, TypeDef.Int)
            .matches(mapOf("one" to 1, "one" to 2, "three" to 3)))
        assertFalse(MapDef(false, TypeDef.String, TypeDef.Int)
            .matches(mapOf("one" to 1, "one" to 2, "three" to 3.0)))
        assertFalse(MapDef(false, TypeDef.String, TypeDef.Int)
            .matches(mapOf("one" to 1, "one" to 2, "three" to null)))
        assertTrue(MapDef(false, TypeDef.String, TypeDef.Int_N)
            .matches(mapOf("one" to 1, "one" to 2, "three" to null)))

        // Check Map of Lists & Maps

        val mapOfLists = MapDef(false, TypeDef.String, ListDef(false, TypeDef.Int))
        assertTrue(mapOfLists.matches(mapOf("even" to listOf(2,4,6))))
        assertFalse(mapOfLists.matches(mapOf("even" to listOf(2.0,4.0,6.0))))

        val mapOfMaps = MapDef(false, TypeDef.String, MapDef(false, TypeDef.Char, TypeDef.Int))
        assertTrue(mapOfMaps.matches(mapOf("nums" to mapOf('1' to 1, '2' to 2))))
        assertFalse(mapOfMaps.matches(mapOf("nums" to mapOf('1' to "1", '2' to "2"))))
    }
}