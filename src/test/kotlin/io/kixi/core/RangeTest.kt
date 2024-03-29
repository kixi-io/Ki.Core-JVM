package io.kixi.core

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RangeTest {

    @Test fun containsInclusive() {
        // Closed
        var r = Range(1, 10, Range.Type.Inclusive, false, false)
        assertFalse(-1 in r)
        assertFalse(0 in r)
        assertFalse(11 in r)

        assertTrue(r.contains(1))
        assertTrue(r.contains(5))
        assertTrue(r.contains(10))

        // Open Left
        r = Range(
            10,
            10,
            Range.Type.Inclusive,
            true,
            false
        )
        assertFalse(r.contains(11))
        assertFalse(r.contains(100))

        assertTrue(r.contains(-10))
        assertTrue(r.contains(0))
        assertTrue(r.contains(10))

        // Open Right
        r = Range(
            10,
            10,
            Range.Type.Inclusive,
            false,
            true
        )
        assertFalse(r.contains(9))
        assertFalse(r.contains(-100))

        assertTrue(r.contains(10))
        assertTrue(r.contains(100))
        assertTrue(r.contains(Int.MAX_VALUE))
    }

    @Test fun containsExclusive() {
        // Exclusive (left & right)
        var r = Range(1, 10, Range.Type.Exclusive, false, false)
        assertFalse(r.contains(1))
        assertFalse(r.contains(10))
        assertFalse(r.contains(-1))

        assertTrue(r.contains(2))
        assertTrue(r.contains(5))
        assertTrue(r.contains(9))

        // Exclusive Left
        var dr= Range(
            0.0,
            10.0,
            Range.Type.ExclusiveLeft,
            false,
            false
        )
        assertFalse(dr.contains(0.0))
        assertFalse(dr.contains(10.1))

        assertTrue(dr.contains(10.0))
        assertTrue(dr.contains(5.0))
        assertTrue(dr.contains(0.1))

        // Exclusive Right
        dr = Range(
            0.0,
            10.0,
            Range.Type.ExclusiveRight,
            false,
            false
        )
        assertFalse(dr.contains(10.0))
        assertFalse(dr.contains(10.1))

        assertTrue(dr.contains(9.999))
        assertTrue(dr.contains(5.0))
        assertTrue(dr.contains(0.0))
    }
}
