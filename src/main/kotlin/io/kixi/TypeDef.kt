package io.kixi

import io.kixi.uom.Quantity
import io.kixi.uom.Unit
import java.math.BigDecimal
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.Duration
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

open class TypeDef(val type:Type, val nullable:Boolean) {

    override fun toString(): String {
        val nullChar = if (nullable) "?" else ""
        return "$type$nullChar"
    }

    /**
     * Singletons for simple types. The suffix _N indicates nullable.
     */
    companion object {
        // Super types
        val Any = TypeDef(Type.Any, false)
        val Any_N= TypeDef(Type.Any, true)

        val Number= TypeDef(Type.Number, false)
        val Number_N = TypeDef(Type.Number, true)

        // Base types
        val String = TypeDef(Type.String, false)
        val String_N = TypeDef(Type.String, true)

        val Char = TypeDef(Type.Char, false)
        val Char_N = TypeDef(Type.Char, true)

        val Int = TypeDef(Type.Int, false)
        val Int_N= TypeDef(Type.Int, true)

        val Long = TypeDef(Type.Long, false)
        val Long_N = TypeDef(Type.Long, true)

        val Float = TypeDef(Type.Float, false)
        val Float_N = TypeDef(Type.Float, true)

        val Double = TypeDef(Type.Double, false)
        val Double_N = TypeDef(Type.Double, true)

        val Decimal = TypeDef(Type.Decimal, false)
        val Decimal_N = TypeDef(Type.Decimal, true)

        val Bool = TypeDef(Type.Bool, false)
        val Bool_N = TypeDef(Type.Bool, true)

        val URL = TypeDef(Type.URL, false)
        val URL_N = TypeDef(Type.URL, true)

        val Date = TypeDef(Type.Date, false)
        val Date_N = TypeDef(Type.Date, true)

        val LocalDateTime = TypeDef(Type.LocalDateTime, false)
        val LocalDateTime_N = TypeDef(Type.LocalDateTime, true)

        val ZonedDateTime = TypeDef(Type.ZonedDateTime, false)
        val ZonedDateTime_N = TypeDef(Type.ZonedDateTime, true)

        val Duration = TypeDef(Type.Duration, false)
        val Duration_N = TypeDef(Type.Duration, true)

        val Version = TypeDef(Type.Version, false)
        val Version_N = TypeDef(Type.Version, true)

        val Blob = TypeDef(Type.Blob, false)
        val Blob_N = TypeDef(Type.Blob, true)

        // nil
        val nil = TypeDef(Type.nil, true)
    }

    open val generic: Boolean get() = false;

    open fun matches(value: Any?) = when {
        value == null -> this == TypeDef.nil || nullable
        !generic -> type.kclass == value!!::class ||
                value!!::class.isSubclassOf(type.kclass)
        else -> false
    }
}

class QuantityDef(nullable:Boolean, val unitType:KClass<*>, val numType:Type) :
    TypeDef(Type.Quantity, nullable) {

    val nullChar = if (nullable) "?" else ""
    val numTypeSuffix = when(numType) {
        Type.Decimal, Type.Int -> ""
        Type.Double -> ":d"
        Type.Long -> ":L"
        Type.Float -> ":f"
        else -> throw Error("Unkown type $numType")
    }

    override fun toString() = "$type<${this.unitType.simpleName}$numTypeSuffix>$nullChar"
    override fun hashCode() = type.hashCode() or nullable.hashCode()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TypeDef
        return other.type == type && other.nullable == nullable
    }

    override val generic: Boolean get() = true;

    override fun matches(value: Any?) : Boolean =
        value is Quantity<*> &&
        unitType == value.unit::class &&
                (value.value::class == numType.kclass ||
                        value.value::class.isSubclassOf(numType.kclass))
}

class RangeDef(nullable:Boolean, val valueDef: TypeDef) : TypeDef(Type.Range, nullable) {
    val nullChar = if (nullable) "?" else ""
    override fun toString() = "$type<$valueDef>$nullChar"
    override val generic: Boolean get() = true;

    override fun matches(value: Any?) : Boolean =
        value is Range<*> && (value.left::class == valueDef.type.kclass ||
                value.left::class.isSubclassOf(valueDef.type.kclass))
}

class ListDef(nullable:Boolean, val valueDef: TypeDef) : TypeDef(Type.List, nullable) {
    val nullChar = if (nullable) "?" else ""
    override fun toString() = "$type<$valueDef>$nullChar"
    override val generic: Boolean get() = true;


    override fun matches(list: Any?) : Boolean {
        if(list == null && !nullable) return false

        list as List<*>

        // This seems odd but is necessary because we don't have runtime generics
        if(list.isEmpty()) return true

        for(e in list) {
            if(!valueDef.matches(e))
                return false
        }

        return true
    }
}

class MapDef(nullable:Boolean, val keyDef: TypeDef, val valueDef: TypeDef) :
    TypeDef(Type.Map, nullable) {
    val nullChar = if (nullable) "?" else ""
    override fun toString() = "$type<$keyDef, $valueDef>$nullChar"
    override val generic: Boolean get() = true;

    override fun matches(map: Any?) : Boolean {
        if(map == null && !nullable) return false

        map as Map<*,*>

        // This seems odd but is necessary because we don't have runtime generics
        if(map.isEmpty()) return true

        for(e in map) {
            if(!keyDef.matches(e.key))
                return false
            if(!valueDef.matches(e.value))
                return false
        }

        return true
    }
}