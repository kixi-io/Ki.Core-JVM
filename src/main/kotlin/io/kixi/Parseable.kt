package io.kixi

import io.kixi.text.ParseException

/**
 * An interface for types that can be parsed from a Ki literal string representation.
 *
 * This interface is implemented by the companion objects of Ki types, providing a
 * consistent parsing contract across the Ki Type System (KTS). Each implementing
 * type defines its own literal format.
 *
 * ## Usage
 * ```kotlin
 * // All Ki types that implement Parseable follow this pattern:
 * val version = Version.parseLiteral("2.1.0-beta")
 * val geo = GeoPoint.parseLiteral(".geo(37.7749, -122.4194)")
 * val blob = Blob.parseLiteral(".blob(SGVsbG8=)")
 * val quantity = Quantity.parseLiteral("5.5kg")
 * val range = Range.parseLiteral("1..10")
 * val kitz = KiTZ.parseLiteral("US/PST")
 * ```
 *
 * ## Implementation Pattern
 * Companion objects implement this interface to provide static-like parsing:
 * ```kotlin
 * class MyType private constructor(...) {
 *     companion object : Parseable<MyType> {
 *         override fun parseLiteral(text: String): MyType {
 *             // Parse and return, or throw ParseException
 *         }
 *     }
 * }
 * ```
 *
 * ## Error Handling
 * All implementations must throw [ParseException] when the input cannot be parsed.
 * This provides consistent error handling across the Ki ecosystem and allows
 * callers to catch parsing errors uniformly.
 *
 * ## Conventions
 * Implementing types typically also provide:
 * - `parseOrNull(text: String): T?` - Returns null instead of throwing
 *
 * ## KiCore Implementations
 *
 * The following KiCore types implement Parseable:
 *
 * | Type | Example Literal | Description |
 * |------|-----------------|-------------|
 * | [Blob] | `.blob(SGVsbG8=)` | Binary data in Base64 |
 * | [GeoPoint] | `.geo(37.7749, -122.4194)` | Geographic coordinates |
 * | [KiTZ] | `US/PST` | Ki timezone identifier |
 * | [KiTZDateTime] | `2024/3/15@14:30:00-US/PST` | DateTime with KiTZ |
 * | [NSID] | `namespace:name` | Namespaced identifier |
 * | [Range] | `0..10`, `0<..<10` | Inclusive/exclusive ranges |
 * | [Version] | `1.2.3-beta` | Semantic version |
 * | [io.kixi.uom.Unit] | `km`, `kg`, `°C` | Unit of measure symbol |
 * | [io.kixi.uom.Quantity] | `5cm`, `23.5kg` | Value with unit |
 *
 * **Note:** [Call] parsing requires the full KD value parser and is implemented
 * in the Ki.KD library rather than KiCore.
 *
 * @param T The type produced by parsing
 * @see ParseException
 * @see Ki.parse
 */
interface Parseable<T> {
    /**
     * Parses a Ki literal string into an instance of type [T].
     *
     * @param text The Ki literal string to parse
     * @return The parsed instance
     * @throws ParseException if the text cannot be parsed as a valid [T]
     */
    fun parseLiteral(text: String): T
}