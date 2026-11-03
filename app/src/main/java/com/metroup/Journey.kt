package com.metroup

import kotlin.math.abs
import kotlin.math.min

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
    val source: String = "Süre tahmini", val ringing: Boolean = false, val routeId: String = "M1A"
) {
    val route get() = Routes.get(routeId)
    val direction get() = if (destination > start) 1 else -1
    val alarmStation get() = destination - direction * before
    fun estimatedStation(now: Long): Int {
        val steps = ((now - anchorElapsed).coerceAtLeast(0) / (segmentSeconds * 1000L)).toInt()
        return anchor + direction * min(steps, abs(destination - anchor))
    }
    fun reanchor(station: Int, now: Long, source: String): Journey {
        require(station in minOf(start, destination)..maxOf(start, destination))
        val remaining = ((alarmStation - station) * direction).coerceAtLeast(0)
        return copy(anchor = station, anchorElapsed = now,
            alarmElapsed = now + remaining * segmentSeconds * 1000L, source = source)
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
