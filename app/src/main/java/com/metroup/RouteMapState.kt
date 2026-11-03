package com.metroup

import kotlin.math.floor

data class RouteMapState(
    val route: MetroRoute, val start: Int, val destination: Int, val alarmStation: Int?,
    val measured: GpsFix?, val live: Boolean, val estimated: GeoPoint?, val estimatedPosition: Double?,
    val status: String
) {
    companion object {
        fun interpolate(route: MetroRoute, position: Double): GeoPoint {
            val p = position.coerceIn(0.0, route.points.lastIndex.toDouble())
            val i = floor(p).toInt()
            val b = route.points[(i + 1).coerceAtMost(route.points.lastIndex)]
            val a = route.points[i]
            val t = p - i
            return GeoPoint(a.latitude + (b.latitude-a.latitude)*t, a.longitude + (b.longitude-a.longitude)*t)
        }
        fun create(route: MetroRoute, journey: Journey?, fix: GpsFix?, match: StationMatch?,
                   start: Int, destination: Int, before: Int, now: Long): RouteMapState {
            val trip = journey?.takeIf { it.routeId == route.id }
            val measured = fix?.takeIf {
                it.point.latitude.isFinite() && it.point.longitude.isFinite() &&
                it.point.latitude in -90.0..90.0 && it.point.longitude in -180.0..180.0 &&
                it.accuracyMeters.isFinite() && it.accuracyMeters in 0.0..60.0 && now-it.elapsed in 0..300_000L
            }
            val live = measured != null && now-measured.elapsed in 0..10_000L
            val position = trip?.position(now) ?: match?.takeIf {
                it.routeId == route.id && now-it.elapsed in 0..30_000L && it.position in 0.0..route.points.lastIndex.toDouble()
            }?.position
            val from = trip?.start ?: start
            val to = trip?.destination ?: destination
            val alarm = trip?.alarmStation ?: if (from != to && before in 1..3 && before <= kotlin.math.abs(to-from))
                to - (if (to > from) 1 else -1)*before else null
            val status = when {
                live -> "Son konum: ${(now-measured!!.elapsed)/1000} sn önce • ±${measured.accuracyMeters.toInt()} m"
                measured != null -> "Konum eski: ${(now-measured.elapsed)/1000} sn önce • turuncu işaret tahminidir"
                trip != null -> "Konum sinyali yok • süreye göre tahmini ilerleme"
                else -> "Konum bekleniyor • henüz konum işareti yok"
            }
            return RouteMapState(route, from, to, alarm, measured, live, position?.let { interpolate(route, it) }, position, status)
        }
    }
}
