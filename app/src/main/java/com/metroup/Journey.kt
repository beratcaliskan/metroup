package com.metroup

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.ceil

// Order checked against Metro Istanbul M1A. Segment durations are USER estimates.
object M1A {
    val stations = listOf("Yenikapı", "Aksaray", "Emniyet-Fatih", "Topkapı-Ulubatlı",
        "Bayrampaşa-Maltepe", "Sağmalcılar", "Kocatepe", "Otogar", "Terazidere",
        "Davutpaşa-YTÜ", "Merter", "Zeytinburnu", "Bakırköy-İncirli", "Bahçelievler",
        "Ataköy-Şirinevler", "Yenibosna", "DTM-İstanbul Fuar Merkezi", "Atatürk Havalimanı")
}

data class Journey(
    val start: Int, val destination: Int, val before: Int, val segmentSeconds: Int,
    val anchor: Int, val anchorElapsed: Long, val alarmElapsed: Long,
    val source: String = "Süre tahmini", val ringing: Boolean = false, val routeId: String = "M1A", val anchorFraction: Double = 0.0
) {
    init {
        require(anchorFraction.isFinite() && anchorFraction in 0.0..1.0)
        require(segmentSeconds in 60..300)
        require(start in Routes.get(routeId).stations.indices && destination in Routes.get(routeId).stations.indices)
        require(anchor+anchorFraction in minOf(start,destination).toDouble()..maxOf(start,destination).toDouble())
    }
    val route get() = Routes.get(routeId)
    val direction get() = if (destination > start) 1 else -1
    val alarmStation get() = destination - direction * before
    fun position(now: Long): Double = (anchor + anchorFraction + direction *
        (now-anchorElapsed).coerceAtLeast(0)/(segmentSeconds*1000.0))
        .coerceIn(minOf(start,destination).toDouble(),maxOf(start,destination).toDouble())
    fun estimatedStation(now: Long): Int = (if (direction > 0) floor(position(now)) else ceil(position(now))).toInt()
    fun reanchor(station: Int, now: Long, source: String): Journey = reanchorPosition(station.toDouble(),now,source)
    fun reanchorPosition(position: Double, now: Long, source: String): Journey {
        require(position.isFinite() && position in minOf(start,destination).toDouble()..maxOf(start,destination).toDouble())
        val remaining = ((alarmStation-position)*direction).coerceAtLeast(0.0)
        val base=floor(position).toInt()
        return copy(anchor=base,anchorFraction=position-base,anchorElapsed=now,
            alarmElapsed=now+(remaining*segmentSeconds*1000).toLong(),source=source)
    }
    companion object {
        fun create(start: Int, destination: Int, before: Int, seconds: Int, now: Long, route: MetroRoute = Routes.m1a): Journey {
            require(start in route.stations.indices && destination in route.stations.indices)
            require(start != destination && before in 1..3 && before <= abs(destination - start))
            require(seconds in 60..300)
            return Journey(start, destination, before, seconds, start, now, now, routeId = route.id)
                .reanchor(start, now, "Başlangıç onayı")
        }
    }
}
