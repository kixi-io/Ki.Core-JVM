package io.kixi

import java.math.BigDecimal
import java.math.BigDecimal as Dec
import kotlin.reflect.KClass

enum class Type(val kclass: KClass<*>, val supertype: Type?) {
    // Super types
    Any(kotlin.Any::class, null),
    Number(kotlin.Number::class, Any),

    // Base types
    String(kotlin.String::class, Any), Char(kotlin.Char::class, Any),
    Int(kotlin.Int::class, Number), Long(kotlin.Long::class, Number),
    Float(kotlin.Float::class, Number), Double(kotlin.Double::class, Number),
    Dec(BigDecimal::class, Number),
    Bool(kotlin.Boolean::class, Any),
    URL(java.net.URL::class, Any),
    Date(java.time.LocalDate::class, Any), LocalDateTime(java.time.LocalDateTime::class, Any),
    ZonedDateTime(java.time.ZonedDateTime::class, Any),
    Duration(java.time.Duration::class, Any),
    Version(io.kixi.Version::class, Any),
    Blob(io.kixi.Blob::class, Any),
    GeoPoint(io.kixi.GeoPoint::class, Any),
    Email(io.kixi.Email::class, Any),
    Coordinate(io.kixi.Coordinate::class, Any),
    Grid(io.kixi.Grid::class, Any),
    Quantity(io.kixi.uom.Quantity::class, Any),
    Range(io.kixi.Range::class, Any),
    List(java.util.List::class, Any), Map(java.util.Map::class, Any),

    // nil
    nil(KClass::class,null);

    fun isAssignableFrom(other:Type): Boolean {
        return this == other || this == Any ||
                other.supertype == this
    }

    fun isNumber(): Boolean = this == Number || (Number == supertype)

    companion object {
        fun typeOf(obj:kotlin.Any?): Type? = when(obj) {
            null -> nil
            is kotlin.String -> String
            is kotlin.Char -> Char
            is kotlin.Int -> Int
            is kotlin.Long -> Long
            is kotlin.Float -> Float
            is kotlin.Double -> Double
            is java.math.BigDecimal -> Type.Dec
            is kotlin.Boolean -> Bool
            is java.net.URL -> URL
            is java.time.LocalDate -> Date
            is java.time.LocalDateTime -> LocalDateTime
            is java.time.ZonedDateTime -> ZonedDateTime
            is java.time.Duration -> Duration
            is io.kixi.Version -> Version
            is io.kixi.Blob -> Blob
            is io.kixi.GeoPoint -> GeoPoint
            is io.kixi.Email -> Email
            is io.kixi.Coordinate -> Coordinate
            is io.kixi.Grid<*> -> Grid
            is io.kixi.uom.Quantity<*> -> Quantity
            is io.kixi.Range<*> -> Range
            is kotlin.collections.List<*> -> List
            is kotlin.collections.Map<*,*> -> Map
            else -> null
        }
    }
}