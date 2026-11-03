package com.metroup

import kotlin.math.abs

data class TripProgress(val position: String, val stopsLeft: Int, val remainingMillis: Long, val progress: Int, val total: Int) {
    companion object {
        fun from(j: Journey, now: Long): TripProgress {
            val gpsFresh = j.source.startsWith("GPS") && now - j.anchorElapsed in 0..30_000L
            val station = if (gpsFresh) j.anchor else j.estimatedStation(now)
            return TripProgress(
                "${if (gpsFresh) "GPS yakını" else "Süre tahmini"}: ${M1A.stations[station]}",
                abs(j.destination - station), (j.alarmElapsed - now).coerceAtLeast(0),
                abs(station - j.start), abs(j.destination - j.start))
        }
    }
}
