package com.manekelsa.utils

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * LocationUtils provides pure, stateless utility functions for GPS-based
 * distance calculations used in worker sorting and display.
 *
 * All functions are in an object (singleton) so they can be called without
 * instantiation: LocationUtils.distanceKm(lat1, lon1, lat2, lon2)
 */
object LocationUtils {

    // Earth's mean radius in kilometres (WGS-84 approximation)
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calculates the great-circle distance between two GPS coordinates using
     * the Haversine formula. Accurate for short to medium distances.
     *
     * Formula steps:
     *   1. Convert lat/lon differences to radians
     *   2. Apply haversine function: hav(θ) = sin²(θ/2)
     *   3. Compute the angular distance using arcsin
     *   4. Multiply by Earth's radius to get km
     *
     * @param lat1 Latitude of point 1 (user's location)
     * @param lon1 Longitude of point 1 (user's location)
     * @param lat2 Latitude of point 2 (worker's location)
     * @param lon2 Longitude of point 2 (worker's location)
     * @return Distance in kilometres (always non-negative)
     */
    fun distanceKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Formats a distance value into a human-readable string for display.
     *
     * Rules:
     *  - distance < 0        → "Location unavailable"
     *  - distance < 1 km     → "XXX m away"  (show metres)
     *  - distance >= 1 km    → "X.X km away" (one decimal)
     *  - distance >= 10 km   → "XX km away"  (whole number)
     *
     * @param distanceKm Distance in kilometres; pass -1.0 if location is unknown
     * @return Formatted display string
     */
    fun formatDistance(distanceKm: Double): String {
        return when {
            distanceKm < 0      -> "Location unavailable"
            distanceKm < 0.001  -> "Nearby"
            distanceKm < 1.0    -> "${(distanceKm * 1000).toInt()} m away"
            distanceKm < 10.0   -> "${"%.1f".format(distanceKm)} km away"
            else                -> "${distanceKm.toInt()} km away"
        }
    }

    /**
     * Returns true if the given coordinates represent a valid, non-zero GPS fix.
     * A (0.0, 0.0) coordinate usually means "no GPS data yet".
     *
     * Note: The point (0°N, 0°E) is in the Gulf of Guinea — effectively never
     * a valid domestic worker location for this app.
     */
    fun isValidLocation(latitude: Double, longitude: Double): Boolean {
        return !(latitude == 0.0 && longitude == 0.0) &&
                latitude in -90.0..90.0 &&
                longitude in -180.0..180.0
    }
}
