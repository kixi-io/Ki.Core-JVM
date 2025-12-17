package io.kixi

import io.kixi.text.ParseException
import java.math.BigDecimal as Dec
import java.math.RoundingMode

/**
 * A geographic point (GPS coordinates) representing a location on Earth.
 *
 * ## Ki Literal Format
 * ```
 * .geo(37.7749, -122.4194)           // San Francisco (lat, lon)
 * .geo(35.6762, 139.6503, 40.0)      // Tokyo with altitude in meters
 * .geo(-33.8688, 151.2093)           // Sydney
 * .geo(0.0, 0.0)                     // Null Island
 * ```
 *
 * ## Coordinate System
 * - **Latitude**: -90.0 to +90.0 (south to north)
 * - **Longitude**: -180.0 to +180.0 (west to east)
 * - **Altitude**: Optional, in meters above WGS84 ellipsoid
 *
 * ## Precision
 * Coordinates are stored as Dec for maximum precision. The default
 * formatting precision is 6 decimal places (~0.1 meter accuracy).
 *
 * ## Usage
 * ```kotlin
 * // Create from coordinates
 * val sf = GeoPoint.of(37.7749, -122.4194)
 * val tokyo = GeoPoint.of(35.6762, 139.6503, 40.0)
 *
 * // Parse Ki literal
 * val point = GeoPoint.parse(".geo(37.7749, -122.4194)")
 *
 * // Calculate distance
 * val distanceKm = sf.distanceTo(tokyo)
 *
 * // Format as Ki literal
 * println(sf)  // .geo(37.774900, -122.419400)
 * ```
 *
 * @property latitude The latitude in degrees (-90 to +90)
 * @property longitude The longitude in degrees (-180 to +180)
 * @property altitude Optional altitude in meters above WGS84 ellipsoid
 *
 * @see Ki.parse
 * @see Ki.format
 */
class GeoPoint private constructor(
    val latitude: Dec,
    val longitude: Dec,
    val altitude: Dec? = null
) : Comparable<GeoPoint> {

    /**
     * Returns the latitude as a Double.
     */
    val lat: Double get() = latitude.toDouble()

    /**
     * Returns the longitude as a Double.
     */
    val lon: Double get() = longitude.toDouble()

    /**
     * Returns the altitude as a Double, or null if not specified.
     */
    val alt: Double? get() = altitude?.toDouble()

    /**
     * Returns true if this point has an altitude component.
     */
    val hasAltitude: Boolean get() = altitude != null

    /**
     * Returns true if this point is at the origin (0, 0) - "Null Island"
     */
    val isOrigin: Boolean
        get() = latitude.compareTo(Dec.ZERO) == 0 &&
                longitude.compareTo(Dec.ZERO) == 0

    /**
     * Returns true if this point is in the Northern Hemisphere.
     */
    val isNorthern: Boolean get() = latitude > Dec.ZERO

    /**
     * Returns true if this point is in the Southern Hemisphere.
     */
    val isSouthern: Boolean get() = latitude < Dec.ZERO

    /**
     * Returns true if this point is in the Eastern Hemisphere.
     */
    val isEastern: Boolean get() = longitude > Dec.ZERO

    /**
     * Returns true if this point is in the Western Hemisphere.
     */
    val isWestern: Boolean get() = longitude < Dec.ZERO

    /**
     * Calculates the great-circle distance to another point using the Haversine formula.
     *
     * @param other The destination point
     * @return Distance in kilometers
     */
    fun distanceTo(other: GeoPoint): Double {
        val earthRadiusKm = 6371.0

        val lat1Rad = Math.toRadians(lat)
        val lat2Rad = Math.toRadians(other.lat)
        val deltaLatRad = Math.toRadians(other.lat - lat)
        val deltaLonRad = Math.toRadians(other.lon - lon)

        val a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadiusKm * c
    }

    /**
     * Calculates the initial bearing (forward azimuth) to another point.
     *
     * @param other The destination point
     * @return Bearing in degrees (0-360, where 0 is north)
     */
    fun bearingTo(other: GeoPoint): Double {
        val lat1Rad = Math.toRadians(lat)
        val lat2Rad = Math.toRadians(other.lat)
        val deltaLonRad = Math.toRadians(other.lon - lon)

        val y = Math.sin(deltaLonRad) * Math.cos(lat2Rad)
        val x = Math.cos(lat1Rad) * Math.sin(lat2Rad) -
                Math.sin(lat1Rad) * Math.cos(lat2Rad) * Math.cos(deltaLonRad)

        val bearingRad = Math.atan2(y, x)
        return (Math.toDegrees(bearingRad) + 360) % 360
    }

    /**
     * Returns a new GeoPoint at the given distance and bearing from this point.
     *
     * @param distanceKm Distance in kilometers
     * @param bearing Bearing in degrees (0-360, where 0 is north)
     * @return A new GeoPoint at the destination
     */
    fun destination(distanceKm: Double, bearing: Double): GeoPoint {
        val earthRadiusKm = 6371.0

        val lat1Rad = Math.toRadians(lat)
        val lon1Rad = Math.toRadians(lon)
        val bearingRad = Math.toRadians(bearing)
        val angularDistance = distanceKm / earthRadiusKm

        val lat2Rad = Math.asin(
            Math.sin(lat1Rad) * Math.cos(angularDistance) +
                    Math.cos(lat1Rad) * Math.sin(angularDistance) * Math.cos(bearingRad)
        )

        val lon2Rad = lon1Rad + Math.atan2(
            Math.sin(bearingRad) * Math.sin(angularDistance) * Math.cos(lat1Rad),
            Math.cos(angularDistance) - Math.sin(lat1Rad) * Math.sin(lat2Rad)
        )

        return of(Math.toDegrees(lat2Rad), Math.toDegrees(lon2Rad), alt)
    }

    /**
     * Returns a new GeoPoint with the specified altitude.
     */
    fun withAltitude(altitudeMeters: Double): GeoPoint =
        GeoPoint(latitude, longitude, Dec.valueOf(altitudeMeters))

    /**
     * Returns a new GeoPoint with the specified altitude.
     */
    fun withAltitude(altitudeMeters: Dec): GeoPoint =
        GeoPoint(latitude, longitude, altitudeMeters)

    /**
     * Returns a new GeoPoint without altitude information.
     */
    fun withoutAltitude(): GeoPoint =
        if (altitude == null) this else GeoPoint(latitude, longitude, null)

    /**
     * Returns the Ki literal representation.
     *
     * Uses 6 decimal places for coordinates (approximately 0.1 meter precision).
     *
     * ```
     * .geo(37.774900, -122.419400)
     * .geo(35.676200, 139.650300, 40.000000)
     * ```
     */
    override fun toString(): String = toString(DEFAULT_PRECISION)

    /**
     * Returns the Ki literal representation with the specified decimal precision.
     *
     * @param precision Number of decimal places for coordinates
     */
    fun toString(precision: Int): String {
        val latStr = latitude.setScale(precision, RoundingMode.HALF_UP).toPlainString()
        val lonStr = longitude.setScale(precision, RoundingMode.HALF_UP).toPlainString()

        return if (altitude != null) {
            val altStr = altitude.setScale(precision, RoundingMode.HALF_UP).toPlainString()
            ".geo($latStr, $lonStr, $altStr)"
        } else {
            ".geo($latStr, $lonStr)"
        }
    }

    /**
     * Returns a compact string representation with minimal decimal places.
     */
    fun toCompactString(): String {
        val latStr = latitude.stripTrailingZeros().toPlainString()
        val lonStr = longitude.stripTrailingZeros().toPlainString()

        return if (altitude != null) {
            val altStr = altitude.stripTrailingZeros().toPlainString()
            ".geo($latStr, $lonStr, $altStr)"
        } else {
            ".geo($latStr, $lonStr)"
        }
    }

    /**
     * Returns coordinates in decimal degrees format (DD).
     * Example: "37.7749°N, 122.4194°W"
     */
    fun toDecimalDegrees(): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"
        val latAbs = Math.abs(lat)
        val lonAbs = Math.abs(lon)

        return if (altitude != null) {
            "%.6f°$latDir, %.6f°$lonDir, %.1fm".format(latAbs, lonAbs, alt)
        } else {
            "%.6f°$latDir, %.6f°$lonDir".format(latAbs, lonAbs)
        }
    }

    /**
     * Returns coordinates in degrees, minutes, seconds format (DMS).
     * Example: "37°46'29.6"N, 122°25'9.8"W"
     */
    fun toDMS(): String {
        fun toDMSPart(decimal: Double): Triple<Int, Int, Double> {
            val absDecimal = Math.abs(decimal)
            val degrees = absDecimal.toInt()
            val minutesDecimal = (absDecimal - degrees) * 60
            val minutes = minutesDecimal.toInt()
            val seconds = (minutesDecimal - minutes) * 60
            return Triple(degrees, minutes, seconds)
        }

        val (latDeg, latMin, latSec) = toDMSPart(lat)
        val (lonDeg, lonMin, lonSec) = toDMSPart(lon)
        val latDir = if (lat >= 0) "N" else "S"
        val lonDir = if (lon >= 0) "E" else "W"

        return "%d°%d'%.1f\"$latDir, %d°%d'%.1f\"$lonDir".format(
            latDeg, latMin, latSec, lonDeg, lonMin, lonSec
        )
    }

    /**
     * Two GeoPoints are equal if they have the same coordinates and altitude.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GeoPoint) return false

        return latitude.compareTo(other.latitude) == 0 &&
                longitude.compareTo(other.longitude) == 0 &&
                (altitude == null && other.altitude == null ||
                        altitude != null && other.altitude != null &&
                        altitude.compareTo(other.altitude) == 0)
    }

    /**
     * Hash code based on coordinates.
     */
    override fun hashCode(): Int {
        var result = latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + (altitude?.hashCode() ?: 0)
        return result
    }

    /**
     * Compares by latitude first, then longitude, then altitude.
     */
    override fun compareTo(other: GeoPoint): Int {
        var result = latitude.compareTo(other.latitude)
        if (result != 0) return result

        result = longitude.compareTo(other.longitude)
        if (result != 0) return result

        return when {
            altitude == null && other.altitude == null -> 0
            altitude == null -> -1
            other.altitude == null -> 1
            else -> altitude.compareTo(other.altitude)
        }
    }

    companion object : Parseable<GeoPoint> {
        /** Default precision for coordinate formatting (6 decimal places ≈ 0.1m) */
        const val DEFAULT_PRECISION = 6

        /** Minimum latitude value */
        val MIN_LATITUDE: Dec = Dec.valueOf(-90)

        /** Maximum latitude value */
        val MAX_LATITUDE: Dec = Dec.valueOf(90)

        /** Minimum longitude value */
        val MIN_LONGITUDE: Dec = Dec.valueOf(-180)

        /** Maximum longitude value */
        val MAX_LONGITUDE: Dec = Dec.valueOf(180)

        private const val GEO_PREFIX = ".geo("

        // Well-known locations
        /** The origin point (0, 0) - "Null Island" */
        @JvmField
        val ORIGIN = GeoPoint(Dec.ZERO, Dec.ZERO)

        /** North Pole */
        @JvmField
        val NORTH_POLE = GeoPoint(MAX_LATITUDE, Dec.ZERO)

        /** South Pole */
        @JvmField
        val SOUTH_POLE = GeoPoint(MIN_LATITUDE, Dec.ZERO)

        /**
         * Create a GeoPoint from Double coordinates.
         *
         * @param latitude Latitude in degrees (-90 to +90)
         * @param longitude Longitude in degrees (-180 to +180)
         * @param altitude Optional altitude in meters
         * @throws IllegalArgumentException if coordinates are out of range
         */
        @JvmStatic
        @JvmOverloads
        fun of(latitude: Double, longitude: Double, altitude: Double? = null): GeoPoint {
            return of(
                Dec.valueOf(latitude),
                Dec.valueOf(longitude),
                altitude?.let { Dec.valueOf(it) }
            )
        }

        /**
         * Create a GeoPoint from Dec coordinates.
         *
         * @param latitude Latitude in degrees (-90 to +90)
         * @param longitude Longitude in degrees (-180 to +180)
         * @param altitude Optional altitude in meters
         * @throws IllegalArgumentException if coordinates are out of range
         */
        @JvmStatic
        @JvmOverloads
        fun of(
            latitude: Dec,
            longitude: Dec,
            altitude: Dec? = null
        ): GeoPoint {
            require(latitude >= MIN_LATITUDE && latitude <= MAX_LATITUDE) {
                "Latitude must be between -90 and +90 degrees, got: $latitude"
            }
            require(longitude >= MIN_LONGITUDE && longitude <= MAX_LONGITUDE) {
                "Longitude must be between -180 and +180 degrees, got: $longitude"
            }
            return GeoPoint(latitude, longitude, altitude)
        }

        /**
         * Parse a Ki geo literal.
         *
         * ```kotlin
         * GeoPoint.parse(".geo(37.7749, -122.4194)")
         * GeoPoint.parse(".geo(35.6762, 139.6503, 40.0)")
         * ```
         *
         * @param geoLiteral The Ki geo literal string
         * @return The parsed GeoPoint
         * @throws ParseException if the literal is malformed or coordinates are invalid
         */
        @JvmStatic
        fun parse(geoLiteral: String): GeoPoint {
            val text = geoLiteral.trim()

            if (text.isBlank())
                throw ParseException("Geo literal cannot be empty.", index = 0)

            if (!text.startsWith(GEO_PREFIX))
                throw ParseException(
                    "Geo literal must start with '.geo(' but was: ${
                        text.take(10).let { if (text.length > 10) "$it..." else it }
                    }",
                    index = 0
                )

            if (!text.endsWith(")"))
                throw ParseException(
                    "Geo literal must end with ')' but ended with: '${text.lastOrNull() ?: ""}'",
                    index = text.length - 1
                )

            // Extract the content between .geo( and )
            val content = text.substring(GEO_PREFIX.length, text.length - 1).trim()

            if (content.isEmpty())
                throw ParseException("Geo literal requires at least latitude and longitude.", index = GEO_PREFIX.length)

            // Split by comma and parse components
            val parts = content.split(",").map { it.trim() }

            if (parts.size < 2)
                throw ParseException(
                    "Geo literal requires at least latitude and longitude, got: $content",
                    index = GEO_PREFIX.length
                )

            if (parts.size > 3)
                throw ParseException(
                    "Geo literal accepts at most 3 components (lat, lon, alt), got ${parts.size}",
                    index = GEO_PREFIX.length
                )

            try {
                val latitude = Dec(parts[0])
                val longitude = Dec(parts[1])
                val altitude = if (parts.size == 3) Dec(parts[2]) else null

                // Validate ranges
                if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE)
                    throw ParseException(
                        "Latitude must be between -90 and +90 degrees, got: $latitude",
                        index = GEO_PREFIX.length
                    )

                if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE)
                    throw ParseException(
                        "Longitude must be between -180 and +180 degrees, got: $longitude",
                        index = GEO_PREFIX.length + parts[0].length + 2
                    )

                return GeoPoint(latitude, longitude, altitude)

            } catch (e: NumberFormatException) {
                throw ParseException(
                    "Invalid number format in geo literal: ${e.message}",
                    index = GEO_PREFIX.length,
                    cause = e
                )
            }
        }

        /**
         * Parses a Ki geo literal string into a GeoPoint instance.
         *
         * @param text The Ki geo literal string to parse
         * @return The parsed GeoPoint
         * @throws ParseException if the text cannot be parsed as a valid GeoPoint
         */
        override fun parseLiteral(text: String): GeoPoint = parse(text)

        /**
         * Parse a geo literal, returning null on failure instead of throwing.
         *
         * @param geoLiteral The Ki geo literal string
         * @return The parsed GeoPoint, or null if parsing fails
         */
        @JvmStatic
        fun parseOrNull(geoLiteral: String): GeoPoint? = try {
            parse(geoLiteral)
        } catch (e: Exception) {
            null
        }

        /**
         * Check if a string appears to be a Ki geo literal.
         * This is a quick structural check, not a full validation.
         */
        @JvmStatic
        fun isLiteral(text: String): Boolean {
            val trimmed = text.trim()
            return trimmed.startsWith(".geo(") && trimmed.endsWith(")")
        }

        /**
         * Create a GeoPoint from degrees, minutes, seconds format.
         *
         * @param latDegrees Latitude degrees
         * @param latMinutes Latitude minutes
         * @param latSeconds Latitude seconds
         * @param latDirection 'N' or 'S'
         * @param lonDegrees Longitude degrees
         * @param lonMinutes Longitude minutes
         * @param lonSeconds Longitude seconds
         * @param lonDirection 'E' or 'W'
         */
        @JvmStatic
        fun fromDMS(
            latDegrees: Int, latMinutes: Int, latSeconds: Double, latDirection: Char,
            lonDegrees: Int, lonMinutes: Int, lonSeconds: Double, lonDirection: Char,
            altitude: Double? = null
        ): GeoPoint {
            require(latDirection == 'N' || latDirection == 'S') {
                "Latitude direction must be 'N' or 'S'"
            }
            require(lonDirection == 'E' || lonDirection == 'W') {
                "Longitude direction must be 'E' or 'W'"
            }

            val lat = (latDegrees + latMinutes / 60.0 + latSeconds / 3600.0) *
                    (if (latDirection == 'S') -1 else 1)
            val lon = (lonDegrees + lonMinutes / 60.0 + lonSeconds / 3600.0) *
                    (if (lonDirection == 'W') -1 else 1)

            return of(lat, lon, altitude)
        }

        /**
         * Calculate the center point of multiple GeoPoints.
         * Uses the geographic midpoint (centroid) formula.
         */
        @JvmStatic
        fun center(points: List<GeoPoint>): GeoPoint {
            require(points.isNotEmpty()) { "Cannot calculate center of empty list" }

            if (points.size == 1) return points[0]

            var x = 0.0
            var y = 0.0
            var z = 0.0

            for (point in points) {
                val latRad = Math.toRadians(point.lat)
                val lonRad = Math.toRadians(point.lon)

                x += Math.cos(latRad) * Math.cos(lonRad)
                y += Math.cos(latRad) * Math.sin(lonRad)
                z += Math.sin(latRad)
            }

            val n = points.size.toDouble()
            x /= n
            y /= n
            z /= n

            val lonRad = Math.atan2(y, x)
            val hyp = Math.sqrt(x * x + y * y)
            val latRad = Math.atan2(z, hyp)

            return of(Math.toDegrees(latRad), Math.toDegrees(lonRad))
        }

        /**
         * Calculate the bounding box that contains all points.
         *
         * @return Pair of (southwest, northeast) corners
         */
        @JvmStatic
        fun boundingBox(points: List<GeoPoint>): Pair<GeoPoint, GeoPoint> {
            require(points.isNotEmpty()) { "Cannot calculate bounding box of empty list" }

            var minLat = points[0].latitude
            var maxLat = points[0].latitude
            var minLon = points[0].longitude
            var maxLon = points[0].longitude

            for (point in points) {
                if (point.latitude < minLat) minLat = point.latitude
                if (point.latitude > maxLat) maxLat = point.latitude
                if (point.longitude < minLon) minLon = point.longitude
                if (point.longitude > maxLon) maxLon = point.longitude
            }

            return Pair(
                GeoPoint(minLat, minLon),  // Southwest
                GeoPoint(maxLat, maxLon)   // Northeast
            )
        }
    }
}