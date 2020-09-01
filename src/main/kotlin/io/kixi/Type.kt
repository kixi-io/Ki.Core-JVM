package io.kixi

import java.math.BigDecimal
import kotlin.reflect.KClass
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.Duration

enum class Type(kclass: KClass<*>, supertype: Type?) {
    // Super types
    Any(kotlin.Any::class, null),
    Number(kotlin.Number::class, Any),

    // Base types
    String(kotlin.String::class, Any), Char(kotlin.Char::class, Any),
    Int(kotlin.Int::class, Number), Long(kotlin.Long::class, Number),
    Float(kotlin.Float::class, Number), Double(kotlin.Double::class, Number), Decimal(BigDecimal::class, Number),
    Bool(kotlin.Boolean::class, Any),
    URL(java.net.URL::class, Any),
    Date(java.time.LocalDate::class, Any), LocalDateTime(java.time.LocalDateTime::class, Any),
        ZonedDateTime(java.time.ZonedDateTime::class, Any),
    Duration(java.time.Duration::class, Any),
    Version(io.kixi.Version::class, Any),
    Blob(ByteArray::class, Any),
    Quantity(io.kixi.uom.Quantity::class, Any),
    Range(io.kixi.Range::class, Any),
    List(java.util.List::class, Any), Map(java.util.Map::class, Any),

    // nil
    nil(KClass::class,null);
}