package io.kixi

import io.kixi.text.ParseException
import java.util.Base64

/**
 * A binary large object (Blob) representing arbitrary byte data.
 *
 * ## Ki Literal Format
 * ```
 * .blob(SGVsbG8gV29ybGQh)
 * .blob()  // empty blob
 * .blob(
 *     SGVsbG8gV29ybGQhIFRoaXMgaXMgYSBsb25nZXIgYmxvYiB0
 *     aGF0IHNwYW5zIG11bHRpcGxlIGxpbmVzLg==
 * )
 * ```
 *
 * ## Base64 Encoding
 * Blob supports both standard Base64 (RFC 4648 §4) and URL-safe Base64 (RFC 4648 §5):
 *
 * - **Standard Base64**: Uses `+` and `/` with `=` padding
 * - **URL-safe Base64**: Uses `-` and `_` with `=` padding
 *
 * When parsing, the variant is auto-detected. When encoding (via [toString] or [toBase64]),
 * standard Base64 is used by default. Use [toBase64UrlSafe] for URL-safe output.
 *
 * ## Parsing Methods
 * - [parse]: Decodes raw Base64 content (e.g., "SGVsbG8=")
 * - [parseLiteral]: Parses Ki literal format (e.g., ".blob(SGVsbG8=)")
 *
 * ## Usage
 * ```kotlin
 * // Create from bytes
 * val blob = Blob.of(byteArrayOf(0x48, 0x65, 0x6C, 0x6C, 0x6F))
 *
 * // Create from string (UTF-8)
 * val blob = Blob.of("Hello World!")
 *
 * // Parse raw Base64
 * val blob = Blob.parse("SGVsbG8gV29ybGQh")
 *
 * // Parse Ki literal
 * val blob = Blob.parseLiteral(".blob(SGVsbG8gV29ybGQh)")
 *
 * // Access data
 * val bytes = blob.toByteArray()
 * val size = blob.size
 *
 * // Format as Ki literal
 * println(blob)  // .blob(SGVsbG8gV29ybGQh)
 * ```
 *
 * @see Ki.parse
 * @see Ki.format
 */
class Blob constructor(
    private val data: ByteArray
) {
    /**
     * Returns a copy of the underlying byte data.
     * Modifications to the returned array do not affect this Blob.
     */
    fun toByteArray(): ByteArray = data.copyOf()

    /**
     * Returns the size in bytes.
     */
    val size: Int get() = data.size

    /**
     * Returns true if the blob contains no data.
     */
    fun isEmpty(): Boolean = data.isEmpty()

    /**
     * Returns true if the blob contains data.
     */
    fun isNotEmpty(): Boolean = data.isNotEmpty()

    /**
     * Access individual bytes by index.
     * @throws IndexOutOfBoundsException if index is out of range
     */
    operator fun get(index: Int): Byte = data[index]

    /**
     * Returns the data encoded as standard Base64 (using `+/`).
     */
    fun toBase64(): String = Base64.getEncoder().encodeToString(data)

    /**
     * Returns the data encoded as URL-safe Base64 (using `-_`).
     */
    fun toBase64UrlSafe(): String = Base64.getUrlEncoder().encodeToString(data)

    /**
     * Decodes the blob data as a UTF-8 string.
     * @throws CharacterCodingException if the data is not valid UTF-8
     */
    fun decodeToString(): String = data.decodeToString()

    /**
     * Decodes the blob data as a string with the specified charset.
     */
    fun decodeToString(charset: java.nio.charset.Charset): String = String(data, charset)

    /**
     * Returns the Ki literal representation using standard Base64.
     *
     * Short blobs (≤30 chars encoded) are single-line:
     * ```
     * .blob(SGVsbG8=)
     * ```
     *
     * Longer blobs are formatted with line breaks:
     * ```
     * .blob(
     *     SGVsbG8gV29ybGQhIFRoaXMgaXMgYSBsb25nZXIgYmxvYiB0
     *     aGF0IHNwYW5zIG11bHRpcGxlIGxpbmVzLg==
     * )
     * ```
     */
    override fun toString(): String {
        if (data.isEmpty()) return ".blob()"

        val encoded = toBase64()

        return if (encoded.length <= 30) {
            ".blob($encoded)"
        } else {
            val lines = encoded.chunked(50)
            buildString {
                append(".blob(\n")
                lines.forEach { line ->
                    append("\t")
                    append(line)
                    append("\n")
                }
                append(")")
            }
        }
    }

    /**
     * Returns the Ki literal representation using URL-safe Base64.
     */
    fun toStringUrlSafe(): String {
        if (data.isEmpty()) return ".blob()"

        val encoded = toBase64UrlSafe()

        return if (encoded.length <= 30) {
            ".blob($encoded)"
        } else {
            val lines = encoded.chunked(50)
            buildString {
                append(".blob(\n")
                lines.forEach { line ->
                    append("\t")
                    append(line)
                    append("\n")
                }
                append(")")
            }
        }
    }

    /**
     * Two Blobs are equal if they contain the same byte sequence.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Blob) return false
        return data.contentEquals(other.data)
    }

    /**
     * Hash code based on byte content.
     */
    override fun hashCode(): Int = data.contentHashCode()

    /**
     * Returns an iterator over the bytes.
     */
    operator fun iterator(): ByteIterator = data.iterator()

    companion object : Parseable<Blob> {
        // Regex for standard Base64: A-Z, a-z, 0-9, +, /, and = padding
        private val STANDARD_BASE64_REGEX = Regex("^[A-Za-z0-9+/]*={0,2}$")

        // Regex for URL-safe Base64: A-Z, a-z, 0-9, -, _, and = padding
        private val URL_SAFE_BASE64_REGEX = Regex("^[A-Za-z0-9\\-_]*={0,2}$")

        // Regex that accepts either variant
        private val ANY_BASE64_REGEX = Regex("^[A-Za-z0-9+/\\-_]*={0,2}$")

        private const val BLOB_PREFIX = ".blob("

        /**
         * Create a Blob from raw bytes.
         * The byte array is copied; modifications to the original do not affect the Blob.
         */
        @JvmStatic
        fun of(data: ByteArray): Blob = Blob(data.copyOf())

        /**
         * Create a Blob from a UTF-8 encoded string.
         */
        @JvmStatic
        fun of(text: String): Blob = Blob(text.toByteArray(Charsets.UTF_8))

        /**
         * Create a Blob from a string with the specified charset.
         */
        @JvmStatic
        fun of(text: String, charset: java.nio.charset.Charset): Blob =
            Blob(text.toByteArray(charset))

        /**
         * Create an empty Blob.
         */
        @JvmStatic
        fun empty(): Blob = Blob(byteArrayOf())

        /**
         * Create a Blob from a Base64-encoded string (not a Ki literal).
         * Auto-detects standard vs URL-safe encoding.
         *
         * @param base64 The Base64-encoded string (without `.blob()` wrapper)
         * @return The decoded Blob
         * @throws IllegalArgumentException if the string is not valid Base64
         */
        @JvmStatic
        fun parse(base64: String): Blob {
            val cleaned = base64.replace(Regex("\\s+"), "")

            if (cleaned.isEmpty()) return empty()

            return try {
                // Detect which variant to use based on characters present
                val decoder = if (cleaned.contains('-') || cleaned.contains('_')) {
                    Base64.getUrlDecoder()
                } else {
                    Base64.getDecoder()
                }
                Blob(decoder.decode(cleaned))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid Base64 string: ${e.message}", e)
            }
        }

        /**
         * Parse a Base64 string, returning null on failure instead of throwing.
         *
         * @param base64 The Base64-encoded string
         * @return The parsed Blob, or null if parsing fails
         */
        @JvmStatic
        fun parseOrNull(base64: String): Blob? = try {
            parse(base64)
        } catch (e: Exception) {
            null
        }

        /**
         * Parses a Ki blob literal string into a Blob instance.
         *
         * Expected format: `.blob(Base64Content)` or `.blob()` for empty
         *
         * @param text The Ki blob literal string to parse
         * @return The parsed Blob
         * @throws ParseException if the text cannot be parsed as a valid Blob literal
         */
        override fun parseLiteral(text: String): Blob {
            val trimmed = text.trim()

            if (!trimmed.startsWith(BLOB_PREFIX)) {
                throw ParseException("Blob literal must start with '.blob('", -1, 0)
            }

            if (!trimmed.endsWith(")")) {
                throw ParseException("Blob literal must end with ')'", -1, trimmed.length)
            }

            // Extract the Base64 content between .blob( and )
            val content = trimmed.substring(BLOB_PREFIX.length, trimmed.length - 1)
            val cleanedContent = content.replace(Regex("\\s+"), "")

            if (cleanedContent.isEmpty()) return empty()

            return try {
                // Detect which variant to use based on characters present
                val decoder = if (cleanedContent.contains('-') || cleanedContent.contains('_')) {
                    Base64.getUrlDecoder()
                } else {
                    Base64.getDecoder()
                }
                Blob(decoder.decode(cleanedContent))
            } catch (e: IllegalArgumentException) {
                throw ParseException("Invalid Base64 content in blob literal: ${e.message}", -1, BLOB_PREFIX.length, e)
            }
        }

        /**
         * Parse a blob literal, returning null on failure instead of throwing.
         *
         * @param text The Ki blob literal string
         * @return The parsed Blob, or null if parsing fails
         */
        @JvmStatic
        fun parseLiteralOrNull(text: String): Blob? = try {
            parseLiteral(text)
        } catch (e: Exception) {
            null
        }

        /**
         * Check if a string appears to be a Ki blob literal.
         * This is a quick structural check, not a full validation.
         */
        @JvmStatic
        fun isLiteral(text: String): Boolean {
            val trimmed = text.trim()
            return trimmed.startsWith(".blob(") && trimmed.endsWith(")")
        }
    }
}