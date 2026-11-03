package com.metroup

import kotlin.math.*

data class GpsFix(val point: GeoPoint, val accuracyMeters: Double, val elapsed: Long)
data class StationMatch(val station: Int, val distanceMeters: Double, val elapsed: Long, val routeId: String = "M1A", val fraction: Double = 0.0) {
    val position get() = station + fraction
    fun label(route: MetroRoute): String = if (fraction > 0.01 && station < route.stations.lastIndex)
        "${route.stations[station]} – ${route.stations[station + 1]} arası (tahmini)" else route.stations[station]
}

// Pure matching logic: no Android dependency, timestamps use elapsedRealtime.
class StationMatcher(private val route: MetroRoute = Routes.m1a) {
    private var pending: Pair<StationMatch, GpsFix>? = null
    private var trusted: GpsFix? = null
    private var lastObserved = -1L
    fun nearest(fix: GpsFix, now: Long, indices: List<Int> = route.stations.indices.toList()): StationMatch? {
        if (!usable(fix, now)) return null
        // Rank all selected-route stations first, so an out-of-route station cannot be
        // mistaken for a less close in-route neighbour.
        val ranked = route.points.mapIndexed { i, point -> StationMatch(i, distance(fix.point, point), fix.elapsed, route.id) }.sortedBy { it.distanceMeters }
        val best = ranked.first()
        if (best.station !in indices || best.distanceMeters + fix.accuracyMeters > 180 ||
            ranked[1].distanceMeters - best.distanceMeters < 100) return null
        return best
    }
    // Straight station-to-station corridors are an approximation, not track geometry.
    fun locate(fix: GpsFix, now: Long, indices: List<Int> = route.stations.indices.toList()): StationMatch? {
        nearest(fix, now, indices)?.let { return it }
        if (!usable(fix, now)) return null
        val candidates = route.points.zipWithNext().mapIndexedNotNull { index, (a, b) ->
            if (index !in indices || index + 1 !in indices) return@mapIndexedNotNull null
            val scale = cos(Math.toRadians(fix.point.latitude))
            fun x(p: GeoPoint) = Math.toRadians(p.longitude - fix.point.longitude) * 6_371_000 * scale
            fun y(p: GeoPoint) = Math.toRadians(p.latitude - fix.point.latitude) * 6_371_000
            val ax=x(a); val ay=y(a); val dx=x(b)-ax; val dy=y(b)-ay
            val length=dx*dx+dy*dy
            if (length < 1) return@mapIndexedNotNull null
            val t=(-(ax*dx+ay*dy)/length).coerceIn(0.0,1.0)
            val distance=hypot(ax+t*dx,ay+t*dy)
            StationMatch(index,distance,fix.elapsed,route.id,t)
        }.sortedBy { it.distanceMeters }
        val best=candidates.firstOrNull() ?: return null
        if (best.distanceMeters + fix.accuracyMeters > 200) return null
        if (candidates.any { abs(it.station-best.station)>1 && it.distanceMeters-best.distanceMeters<100 }) return null
        return best
    }
    fun accept(fix: GpsFix, now: Long, indices: List<Int>): StationMatch? {
        if (fix.elapsed <= lastObserved) return null
        lastObserved = fix.elapsed
        val match = locate(fix, now, indices)
        if (match == null) { pending = null; return null }
        fun plausible(previous: GpsFix): Boolean {
            val seconds = (fix.elapsed-previous.elapsed)/1000.0
            return seconds > 0 && distance(fix.point,previous.point) <= 55*seconds + fix.accuracyMeters + previous.accuracyMeters
        }
        // Fresh precise fixes need no artificial dwell at the same station.
        val previousTrusted=trusted
        if (previousTrusted != null && fix.elapsed-previousTrusted.elapsed <= 30_000 && !plausible(previousTrusted)) {
            pending = null; return null
        }
        if (fix.accuracyMeters <= 25 && now-fix.elapsed <= 10_000) {
            trusted=fix; pending=match to fix; return match
        }
        val previous = pending
        if (previous == null || fix.elapsed-previous.second.elapsed > 15_000 ||
            abs(match.position-previous.first.position)>1.5 || !plausible(previous.second)) {
            pending=match to fix; return null
        }
        if (fix.elapsed-previous.second.elapsed < 1_000) return null
        pending=match to fix
        trusted=fix
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
