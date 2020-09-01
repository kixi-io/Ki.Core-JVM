package io.kixi

import io.kixi.uom.Quantity
import java.math.BigDecimal
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.Duration
import kotlin.reflect.KClass

open class TypeDef(val type:Type, val nullable:Boolean, val kclass:KClass<*>) {

    override fun toString(): String {
        val nullChar = if (nullable) "?" else ""
        return "$type$nullChar"
    }

    /**
     * Singletons for simple types. The suffix _N indicates nullable.
     */
    companion object {
        // Super types
        val Any = TypeDef(Type.Any, false, Any::class)
        val Any_N= TypeDef(Type.Any, true, Any::class)

        val Number= TypeDef(Type.Number, false, Number::class)
        val Number_N = TypeDef(Type.Number, true, Number::class)

        // Base types
        val String = TypeDef(Type.String, false, String::class)
        val String_N = TypeDef(Type.String, true, String::class)

        val Char = TypeDef(Type.Char, false, Char::class)
        val Char_N = TypeDef(Type.Char, true, Char::class)

        val Int = TypeDef(Type.Int, false, Int::class)
        val Int_N= TypeDef(Type.Int, true, Int::class)

        val Long = TypeDef(Type.Long, false, Long::class)
        val Long_N = TypeDef(Type.Long, true, Long::class)

        val Float = TypeDef(Type.Float, false, Float::class)
        val Float_N = TypeDef(Type.Float, true, Float::class)

        val Double = TypeDef(Type.Double, false, Double::class)
        val Double_N = TypeDef(Type.Double, true, Double::class)

        val Decimal = TypeDef(Type.Decimal, false, BigDecimal::class)
        val Decimal_N = TypeDef(Type.Decimal, true, BigDecimal::class)

        val Bool = TypeDef(Type.Bool, false, Boolean::class)
        val Bool_N = TypeDef(Type.Bool, true, Boolean::class)

        val URL = TypeDef(Type.URL, false, URL::class)
        val URL_N = TypeDef(Type.URL, true, URL::class)

        val Date = TypeDef(Type.Date, false, LocalDate::class)
        val Date_N = TypeDef(Type.Date, true, LocalDate::class)

        val LocalDateTime = TypeDef(Type.LocalDateTime, false, LocalDateTime::class)
        val LocalDateTime_N = TypeDef(Type.LocalDateTime, true, LocalDateTime::class)

        val ZonedDateTime = TypeDef(Type.ZonedDateTime, false, ZonedDateTime::class)
        val ZonedDateTime_N = TypeDef(Type.ZonedDateTime, true, ZonedDateTime::class)

        val Duration = TypeDef(Type.Duration, false, Duration::class)
        val Duration_N = TypeDef(Type.Duration, true, Duration::class)

        val Version = TypeDef(Type.Version, false, Version::class)
        val Version_N = TypeDef(Type.Version, true, Version::class)

        val Blob = TypeDef(Type.Blob, false, ByteArray::class)
        val Blob_N = TypeDef(Type.Blob, true, ByteArray::class)

        // nil
        val nil = TypeDef(Type.nil, true, KClass::class)
    }

    open val generic: Boolean get() = false;
}

// TODO
class QuantityDef(nullable:Boolean, val unit:Unit, val numType:Type) :
    TypeDef(Type.Quantity, nullable, Quantity::class) {

    val nullChar = if (nullable) "?" else ""
    val numTypeSuffix = when(numType) {
        Type.Decimal, Type.Int -> ""
        Type.Double -> ":d"
        Type.Long -> ":L"
        Type.Float -> ":f"
        else -> throw Error("Unkown type $numType")
    }

    override fun toString() = "$type<${this.unit}$numTypeSuffix>$nullChar"
    override fun hashCode() = type.hashCode() or nullable.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TypeDef
        return other.type == type && other.nullable == nullable
    }

    override val generic: Boolean get() = true;
}

class RangeDef(nullable:Boolean, val valueDef: TypeDef) : TypeDef(Type.Range, nullable, Range::class) {
    val nullChar = if (nullable) "?" else ""
    override fun toString() = "$type<$valueDef>$nullChar"
    override val generic: Boolean get() = true;
}

class ListDef(nullable:Boolean, val valueDef: TypeDef) : TypeDef(Type.List, nullable, List::class) {
    val nullChar = if (nullable) "?" else ""
    override fun toString() = "$type<$valueDef>$nullChar"
    override val generic: Boolean get() = true;
}

class MapDef(nullable:Boolean, val keyTypeDef: TypeDef, val valueTypeDef: TypeDef) :
    TypeDef(Type.Map, nullable, Map::class) {
    val nullChar = if (nullable) "?" else ""
    override fun toString() = "$type<$keyTypeDef, $valueTypeDef>$nullChar"
    override val generic: Boolean get() = true;
}