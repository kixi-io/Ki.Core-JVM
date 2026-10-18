package io.kixi

import io.kixi.text.ParseException
import java.security.SecureRandom

/**
 * A 128-bit, time-ordered unique identifier (a ULID).
 *
 * The high 48 bits are a millisecond timestamp and the low 80 bits are random.
 * Ids generated in the same process are strictly increasing, including within a
 * single millisecond, so sequential inserts sort next to each other. The text
 * form is 26 characters of Crockford base32 and sorts the same way as the
 * binary form.
 *
 * ## Ki Literal Format
 * ```
 * .id(01J9A0X5K2M7N3P8Q1R6T4V9AB)
 * ```
 *
 * ## Usage
 * ```kotlin
 * val id = Id.generate()
 * val same = Id.parse("01J9A0X5K2M7N3P8Q1R6T4V9AB")
 * val literal = Id.parseLiteral(".id(01J9A0X5K2M7N3P8Q1R6T4V9AB)")
 *
 * id.timestamp          // milliseconds since the epoch
 * id.toBytes()          // 16 bytes, big-endian, same order as compareTo
 * println(id)           // .id(01J9A0X5K2M7N3P8Q1R6T4V9AB)
 * ```
 *
 * `Id` is identity for anything: a database node, a transaction, a blob. Use
 * [Ref] to point at a node.
 *
 * @see Ref
 * @see <a href="https://github.com/ulid/spec">ULID specification</a>
 */
class Id private constructor(
    private val hi: Long,
    private val lo: Long
) : Comparable<Id> {

    /**
     * The millisecond timestamp (milliseconds since the Unix epoch) encoded in
     * the high 48 bits.
     */
    val timestamp: Long get() = hi ushr 16

    /**
     * The 16-byte big-endian binary form. Unsigned lexicographic order of these
     * bytes equals [compareTo] order.
     */
    fun toBytes(): ByteArray {
        val bytes = ByteArray(16)
        for (i in 0 until 8) {
            bytes[i] = (hi ushr (56 - 8 * i)).toByte()
            bytes[8 + i] = (lo ushr (56 - 8 * i)).toByte()
        }
        return bytes
    }

    /**
     * The bare 26-character Crockford base32 form, without the `.id(...)` wrapper.
     */
    fun toBase32(): String {
        val chars = CharArray(26)
        // 128 bits into 26 five-bit groups (130 bits; the top two bits are zero).
        var bitPos = 0
        for (i in 25 downTo 0) {
            chars[i] = ALPHABET[bits(bitPos, 5)]
            bitPos += 5
        }
        return String(chars)
    }

    /** Returns the [count] bits starting [offset] bits above the least significant bit. */
    private fun bits(offset: Int, count: Int): Int {
        val mask = (1L shl count) - 1
        return when {
            offset >= 64 -> ((hi ushr (offset - 64)) and mask).toInt()
            offset + count <= 64 -> ((lo ushr offset) and mask).toInt()
            else -> {
                val lowPart = lo ushr offset
                val highPart = hi shl (64 - offset)
                ((lowPart or highPart) and mask).toInt()
            }
        }
    }

    /**
     * Returns the Ki literal representation: `.id(01J9A0X5K2M7N3P8Q1R6T4V9AB)`.
     */
    override fun toString(): String = ".id(${toBase32()})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Id) return false
        return hi == other.hi && lo == other.lo
    }

    override fun hashCode(): Int = (hi xor (hi ushr 32) xor lo xor (lo ushr 32)).toInt()

    /**
     * Unsigned comparison of the 128-bit value. Because the timestamp occupies
     * the high bits, ids order by creation time first.
     */
    override fun compareTo(other: Id): Int {
        val h = java.lang.Long.compareUnsigned(hi, other.hi)
        return if (h != 0) h else java.lang.Long.compareUnsigned(lo, other.lo)
    }

    companion object : Parseable<Id> {
        private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
        private const val ID_PREFIX = ".id("
        private const val LENGTH = 26

        private val DECODE = IntArray(128) { -1 }.also { table ->
            ALPHABET.forEachIndexed { i, c ->
                table[c.code] = i
                table[c.lowercaseChar().code] = i
            }
            // Crockford's decoding aliases
            for (c in "oO") table[c.code] = 0
            for (c in "iIlL") table[c.code] = 1
        }

        private val random = SecureRandom()
        private val lock = Any()
        private var lastTimestamp = -1L
        private var lastRandomHi = 0L   // 16 bits used
        private var lastRandomLo = 0L   // 64 bits

        /**
         * Generates a new Id for the current time. Ids generated this way in one
         * process are strictly increasing, including within the same millisecond
         * and across a backwards step of the system clock.
         */
        @JvmStatic
        fun generate(): Id = synchronized(lock) {
            generate(maxOf(System.currentTimeMillis(), lastTimestamp))
        }

        /**
         * Generates a new Id for the given millisecond timestamp, which is honored
         * exactly. Ids generated for the same timestamp in one process are strictly
         * increasing: the random component is incremented rather than redrawn.
         *
         * @throws IllegalArgumentException if the timestamp does not fit in 48 bits
         */
        @JvmStatic
        fun generate(timestamp: Long): Id {
            require(timestamp >= 0 && timestamp < (1L shl 48)) {
                "Timestamp must fit in 48 bits, got: $timestamp"
            }
            synchronized(lock) {
                if (timestamp != lastTimestamp) {
                    lastTimestamp = timestamp
                    lastRandomHi = random.nextInt(1 shl 16).toLong()
                    lastRandomLo = random.nextLong()
                } else {
                    // Same millisecond: increment the 80-bit random part.
                    lastRandomLo++
                    if (lastRandomLo == 0L) {
                        lastRandomHi = (lastRandomHi + 1) and 0xFFFF
                        if (lastRandomHi == 0L) {
                            // 80-bit overflow within one millisecond; move the clock forward.
                            lastTimestamp++
                            lastRandomHi = random.nextInt(1 shl 16).toLong()
                            lastRandomLo = random.nextLong()
                        }
                    }
                }
                return Id((lastTimestamp shl 16) or lastRandomHi, lastRandomLo)
            }
        }

        /**
         * Creates an Id from its 16-byte big-endian form, as produced by [toBytes].
         */
        @JvmStatic
        fun fromBytes(bytes: ByteArray): Id {
            require(bytes.size == 16) { "Id requires exactly 16 bytes, got: ${bytes.size}" }
            var hi = 0L
            var lo = 0L
            for (i in 0 until 8) {
                hi = (hi shl 8) or (bytes[i].toLong() and 0xFF)
                lo = (lo shl 8) or (bytes[8 + i].toLong() and 0xFF)
            }
            return Id(hi, lo)
        }

        /**
         * Creates an Id from its two 64-bit halves.
         */
        @JvmStatic
        fun of(hi: Long, lo: Long): Id = Id(hi, lo)

        /**
         * Parses the bare 26-character base32 form (no `.id(...)` wrapper).
         * Case-insensitive; accepts Crockford's O/0 and I/L/1 aliases.
         *
         * @throws ParseException if the text is not a valid Id
         */
        @JvmStatic
        fun parse(text: String): Id {
            val trimmed = text.trim()
            if (trimmed.length != LENGTH) {
                throw ParseException(
                    "Id must be $LENGTH characters, got ${trimmed.length}: '$trimmed'", index = 0
                )
            }
            var hi = 0L
            var lo = 0L
            for ((i, c) in trimmed.withIndex()) {
                val v = if (c.code < 128) DECODE[c.code] else -1
                if (v < 0) throw ParseException("Invalid character '$c' in Id: '$trimmed'", index = i)
                // Shift the 128-bit accumulator left by 5 and or in v.
                hi = (hi shl 5) or (lo ushr 59)
                lo = (lo shl 5) or v.toLong()
            }
            // 26 x 5 = 130 bits; the first character may only contribute 3 bits.
            if (DECODE[trimmed[0].code] > 7) {
                throw ParseException("Id overflows 128 bits (first character must be 0-7): '$trimmed'", index = 0)
            }
            return Id(hi, lo)
        }

        /**
         * Parses a Ki Id literal: `.id(01J9A0X5K2M7N3P8Q1R6T4V9AB)`.
         *
         * @throws ParseException if the text cannot be parsed as a valid Id literal
         */
        override fun parseLiteral(text: String): Id {
            val trimmed = text.trim()
            if (!trimmed.startsWith(ID_PREFIX)) {
                throw ParseException("Id literal must start with '.id('", index = 0)
            }
            if (!trimmed.endsWith(")")) {
                throw ParseException("Id literal must end with ')'", index = trimmed.length - 1)
            }
            val content = trimmed.substring(ID_PREFIX.length, trimmed.length - 1)
            return parse(content)
        }

        /**
         * Parses a bare or literal Id, returning null on failure instead of throwing.
         */
        @JvmStatic
        fun parseOrNull(text: String): Id? = try {
            val trimmed = text.trim()
            if (trimmed.startsWith(ID_PREFIX)) parseLiteral(trimmed) else parse(trimmed)
        } catch (e: Exception) {
            null
        }

        /**
         * Checks if a string appears to be a Ki Id literal.
         * This is a quick structural check, not a full validation.
         */
        @JvmStatic
        fun isLiteral(text: String): Boolean {
            val trimmed = text.trim()
            return trimmed.startsWith(ID_PREFIX) && trimmed.endsWith(")")
        }
    }
}
