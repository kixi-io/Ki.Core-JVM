package io.kixi

import io.kixi.uom.*
import kotlin.reflect.KClass
import java.math.BigDecimal as Dec

open class TypeDef(val type: Type, val nullable: Boolean) {

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
        val Any_N = TypeDef(Type.Any, true)

        val Number = TypeDef(Type.Number, false)
        val Number_N = TypeDef(Type.Number, true)

        // Base types
        val String = TypeDef(Type.String, false)
        val String_N = TypeDef(Type.String, true)

        val Char = TypeDef(Type.Char, false)
        val Char_N = TypeDef(Type.Char, true)

        val Int = TypeDef(Type.Int, false)
        val Int_N = TypeDef(Type.Int, true)

        val Long = TypeDef(Type.Long, false)
        val Long_N = TypeDef(Type.Long, true)

        val Float = TypeDef(Type.Float, false)
        val Float_N = TypeDef(Type.Float, true)

        val Double = TypeDef(Type.Double, false)
        val Double_N = TypeDef(Type.Double, true)

        val Dec = TypeDef(Type.Dec, false)
        val Dec_N = TypeDef(Type.Dec, true)

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

        val Email = TypeDef(Type.Email, false)
        val Email_N = TypeDef(Type.Email, true)

        val GeoPoint = TypeDef(Type.GeoPoint, false)
        val GeoPoint_N = TypeDef(Type.GeoPoint, true)

        val Coordinate = TypeDef(Type.Coordinate, false)
        val Coordinate_N = TypeDef(Type.Coordinate, true)

        val Grid = TypeDef(Type.Grid, false)
        val Grid_N = TypeDef(Type.Grid, true)

        // nil
        val nil = TypeDef(Type.nil, true)

        /**
         * @param name String A KTS name such as String, Int or Bool. Nullable types
         *   should be suffixed with _N (ex. String_N)
         * @return TypeDef? A TypeDef for the KTS name or null if no type matches the
         *   given name or null if there is no match
         */
        fun forName(name: kotlin.String): TypeDef? = when (name) {
            "null", "nil" -> nil
            "String" -> String; "String_N" -> String_N
            "Char" -> Char; "Char_N" -> Char_N
            "Int" -> Int; "Int_N" -> Int_N
            "Long" -> Long; "Long_N" -> Long_N
            "Float" -> Float; "Float_N" -> Float_N
            "Double" -> Double; "Double_N" -> Double_N
            "Dec" -> Dec; "Dec_N" -> Dec_N
            "Number" -> Number; "Number_N" -> Number_N
            "Bool" -> Bool; "Bool_N" -> Bool_N
            "URL" -> URL; "URL_N" -> URL_N
            "Date" -> Date; "Date_N" -> Date_N
            "LocalDateTime" -> LocalDateTime; "LocalDateTime_N" -> LocalDateTime_N
            "ZonedDateTime" -> ZonedDateTime; "ZonedDateTime_N" -> ZonedDateTime_N
            "Duration" -> Duration; "Duration_N" -> Duration_N
            "Version" -> Version; "Version_N" -> Version_N
            "Blob" -> Blob; "Blob_N" -> Blob_N
            "Email" -> Email; "Email_N" -> Email_N
            "GeoPoint" -> GeoPoint; "GeoPoint_N" -> GeoPoint_N
            "Coordinate" -> Coordinate; "Coordinate_N" -> Coordinate_N
            "Grid" -> Grid; "Grid_N" -> Grid_N
            "Any" -> Any; "Any_N" -> Any_N

            else -> null
        }

        fun inferCollectionType(options: Collection<kotlin.Any?>): TypeDef {
            var widestType = Type.nil
            var gotNil = false

            for (opt in options) {
                if (opt == null) {
                    gotNil = true
                    continue
                }

                val itemType = Type.typeOf(opt)!!

                if (widestType == Type.nil) {
                    widestType = itemType
                } else if (widestType != itemType && !widestType.isAssignableFrom(itemType)) {
                    widestType = if (widestType.isNumber() && itemType.isNumber()) {
                        Type.Number
                    } else {
                        Type.Any
                    }
                }
            }

            return TypeDef(widestType, gotNil)
        }
    }

    open val generic: Boolean get() = false

    open fun matches(value: kotlin.Any?): Boolean = when {
        value == null -> this == nil || nullable
        // Generic types override matches and do their own comparison
        !generic -> {
            val valueType = Type.typeOf(value)
            if (valueType == null) {
                false
            } else {
                this.type.isAssignableFrom(valueType)
            }
        }
        else -> false
    }
}

class QuantityDef(
    nullable: Boolean,
    val unitType: KClass<*>,
    val numType: Type
) : TypeDef(Type.Quantity, nullable) {

    private val nullChar = if (nullable) "?" else ""
    private val numTypeSuffix = when (numType) {
        Type.Dec, Type.Int -> ""
        Type.Double -> ":d"
        Type.Long -> ":L"
        Type.Float -> ":f"
        else -> throw Error("Unknown type $numType")
    }

    override fun toString() = "$type<${this.unitType.simpleName}$numTypeSuffix>$nullChar"

    override fun hashCode(): Int = type.hashCode() xor nullable.hashCode()

    override fun equals(other: kotlin.Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QuantityDef
        return other.type == type && other.nullable == nullable &&
                other.unitType == unitType && other.numType == numType
    }

    override val generic: Boolean get() = true

    override fun matches(value: kotlin.Any?): Boolean {
        if (value == null) return nullable

        return value is Quantity<*> &&
                unitType == value.unit::class &&
                (value.value::class == numType.kclass ||
                        numType.isAssignableFrom(Type.typeOf(value.value)!!))
    }
}

class RangeDef(nullable: Boolean, val valueDef: TypeDef) : TypeDef(Type.Range, nullable) {
    private val nullChar = if (nullable) "?" else ""

    override fun toString() = "$type<$valueDef>$nullChar"

    override val generic: Boolean get() = true

    override fun matches(value: kotlin.Any?): Boolean {
        if (value == null) return nullable

        return value is Range<*> && (value.left::class == valueDef.type.kclass ||
                type.isAssignableFrom(Type.typeOf(value.left)!!))
    }
}

class ListDef(nullable: Boolean, val valueDef: TypeDef) : TypeDef(Type.List, nullable) {
    private val nullChar = if (nullable) "?" else ""

    override fun toString() = "$type<$valueDef>$nullChar"

    override val generic: Boolean get() = true

    override fun matches(value: kotlin.Any?): Boolean {
        if (value == null) return nullable
        if (value !is List<*>) return false

        // This seems wrong but is necessary because we don't have runtime generics
        if (value.isEmpty()) return true

        for (e in value) {
            if (!valueDef.matches(e)) return false
        }

        return true
    }
}

class MapDef(
    nullable: Boolean,
    val keyDef: TypeDef,
    val valueDef: TypeDef
) : TypeDef(Type.Map, nullable) {
    private val nullChar = if (nullable) "?" else ""

    override fun toString() = "$type<$keyDef, $valueDef>$nullChar"

    override val generic: Boolean get() = true

    override fun matches(value: kotlin.Any?): Boolean {
        if (value == null) return nullable
        if (value !is Map<*, *>) return false

        // This seems odd but is necessary because we don't have runtime generics
        if (value.isEmpty()) return true

        for (e in value) {
            if (!keyDef.matches(e.key)) return false
            if (!valueDef.matches(e.value)) return false
        }

        return true
    }
}

/**
 * Type definition for Grid with element type.
 *
 * Represents a typed grid definition such as `Grid<Int>` or `Grid<String>?`.
 *
 * @property elementDef The type definition for grid elements
 */
class GridDef(
    nullable: Boolean,
    val elementDef: TypeDef
) : TypeDef(Type.Grid, nullable) {
    private val nullChar = if (nullable) "?" else ""

    override fun toString() = "$type<$elementDef>$nullChar"

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + nullable.hashCode()
        result = 31 * result + elementDef.hashCode()
        return result
    }

    override fun equals(other: kotlin.Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GridDef
        return other.type == type && other.nullable == nullable &&
                other.elementDef == elementDef
    }

    override val generic: Boolean get() = true

    override fun matches(value: kotlin.Any?): Boolean {
        if (value == null) return nullable
        if (value !is io.kixi.Grid<*>) return false

        // Check that all grid values match the element type
        return value.all { elementDef.matches(it) }
    }
}