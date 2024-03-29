package io.kixi.core

/**
 * A Ki Range can be inclusive or exclusive on both ends, may be reversed (e.g. 5..1),
 * and can be open on the left or right. Note: Ranges that are open should set the same
 * value for left and right. This is necessary because Comparable types don't require
 * a max and min value.
 *
 * Reversed ranges represent downward progressions. Open ended ranges indicate that one
 * end is not bounded.
 *
 * **Examples**
 * ```
 *     0..5     # >= 0 and <= 5
 *     5..0     # <= 5 and >= 0
 *     0<..<5   # > 0 and < 5
 *     0..<5    # >= 0 and < 5
 *     0<..5    # > 0 and <= 5
 *     0.._     # >= 0
 *     _..5     # <= 5
 *     0<.._    # > 0
 *     _..<5    # < 5
 * ```
 */
data class Range<T : Comparable<T>>(val left:T, val right:T,
                                    val type: Type = Type.Inclusive,
                                    val openLeft: Boolean = false, val openRight: Boolean = false) {

    enum class Type(val operator: String) {
        Inclusive(".."),
        Exclusive("<..<"),
        ExclusiveLeft ("<.."),
        ExclusiveRight("..<")
    }

    override fun toString(): String {
        val leftString = if (openLeft) "_" else Ki.format(left)
        val rightString = if (openRight) "_" else Ki.format(right)

        return leftString + type.operator + rightString
    }

    var min : T = if(left.compareTo(right)<0) left else right
    var max : T = if(left.compareTo(right)>0) left else right

    var reversed : Boolean = left.compareTo(right) > 0

    operator fun contains(element:T) : Boolean {
        if(openLeft) {
            return when (type) {
                Type.Inclusive -> element <= right
                Type.ExclusiveRight -> element < right
                else -> throw IllegalArgumentException("Left open ranges can only use .. and ..< operators.")
            }
        } else if(openRight) {
            return when (type) {
                Type.Inclusive -> element >= left
                Type.ExclusiveLeft -> element > left
                else -> throw IllegalArgumentException("Right open ranges can only use .. and <.. operators.")
            }
        } else {
            return when (type) {
                Type.Inclusive -> element in left..right
                Type.Exclusive -> element > min && element < max
                Type.ExclusiveLeft -> if (reversed) element < left && element >= right
                else element > left && element <= right
                Type.ExclusiveRight -> if (reversed) element <= left && element > right
                else element >= left && element < right
            }
        }
    }
}
