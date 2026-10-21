package io.kixi.types

/**
 * A dot literal whose bag is not loaded (`.dna("ATG")` read without Bio), kept
 * exactly as written so that it reads and writes back unchanged. KD produces it for
 * a `.name(…)` no bag claims; it formats as [literal], and two are equal when their
 * literals are.
 */
open class ExtLiteral(val literal: String) {

    /** The literal's name: the letters after the dot (`dna`). */
    val name: String = literal.drop(1).takeWhile { it.isLetter() }

    init {
        require(literal.length > 1 && literal[0] == '.' && name.isNotEmpty()) { "Not a dot literal: $literal" }
    }

    final override fun equals(other: Any?): Boolean = other is ExtLiteral && other.literal == literal

    final override fun hashCode(): Int = literal.hashCode()

    override fun toString(): String = literal
}
