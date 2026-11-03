package com.metroup

import kotlin.math.*

data class GpsFix(val point: GeoPoint, val accuracyMeters: Double, val elapsed: Long)
data class StationMatch(val station: Int, val distanceMeters: Double, val elapsed: Long)

// Pure matching logic: no Android dependency, timestamps use elapsedRealtime.
class StationMatcher {
    private var pending: StationMatch? = null
    private var lastObserved = -1L
    fun nearest(fix: GpsFix, now: Long, indices: List<Int> = M1A.stations.indices.toList()): StationMatch? {
        if (!usable(fix, now)) return null
        // Rank all M1A stations first, so an out-of-route station cannot be
        // mistaken for a less close in-route neighbour.
        val ranked = StationCatalog.points.mapIndexed { i, point -> StationMatch(i, distance(fix.point, point), fix.elapsed) }.sortedBy { it.distanceMeters }
        val best = ranked.first()
        if (best.station !in indices || best.distanceMeters + fix.accuracyMeters > 180 ||
            ranked[1].distanceMeters - best.distanceMeters < 100) return null
        return best
    }
    fun accept(fix: GpsFix, now: Long, indices: List<Int>): StationMatch? {
        // Replayed/cached measurements are not independent confirmations.
        if (fix.elapsed <= lastObserved) return null
        lastObserved = fix.elapsed
        val match = nearest(fix, now, indices)
        if (match == null) { pending = null; return null }
        val previous = pending
        if (previous == null || previous.station != match.station || match.elapsed - previous.elapsed > 30_000) {
            pending = match
            return null
        }
        if (match.elapsed - previous.elapsed < 5_000) return null
        pending = match
        return match
    }
    companion object {
        fun usable(fix: GpsFix, now: Long): Boolean =
            fix.accuracyMeters.isFinite() && fix.accuracyMeters in 0.0..60.0 &&
            now - fix.elapsed in 0..30_000L && fix.point.latitude.isFinite() && fix.point.longitude.isFinite() &&
            fix.point.latitude in -90.0..90.0 && fix.point.longitude in -180.0..180.0

        fun distance(a: GeoPoint, b: GeoPoint): Double {
            val lat = Math.toRadians(b.latitude - a.latitude)
            val lon = Math.toRadians(b.longitude - a.longitude)
            val h = sin(lat/2).pow(2) + cos(Math.toRadians(a.latitude)) * cos(Math.toRadians(b.latitude)) * sin(lon/2).pow(2)
            return 6_371_000 * 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
        }
    }
}
