package com.metroup

import kotlin.math.abs

data class TripProgress(val position: String, val stopsLeft: Int, val remainingMillis: Long, val progress: Int, val total: Int) {
    companion object {
        fun from(j: Journey, now: Long): TripProgress {
            val gpsFresh = (j.source.startsWith("GPS") || j.source.startsWith("Konum")) && now - j.anchorElapsed in 0..10_000L
            val station = j.estimatedStation(if (gpsFresh) j.anchorElapsed else now)
            val label = if (gpsFresh) StationMatch(j.anchor,0.0,j.anchorElapsed,j.routeId,j.anchorFraction).label(j.route) else j.route.stations[station]
            return TripProgress(
                "${if (gpsFresh) "Konum" else "Süre tahmini"}: ${label}",
                abs(j.destination - station), (j.alarmElapsed - now).coerceAtLeast(0),
                abs(station - j.start), abs(j.destination - j.start))
        }
    }
}
