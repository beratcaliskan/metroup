package com.metroup

import org.junit.Assert.*
import org.junit.Test

class TripProgressTest {
    @Test fun offlineProgressContinuesInBothDirections() {
        for (j in listOf(Journey.create(8,0,2,120,0),Journey.create(0,8,2,120,0))) {
            val status = TripProgress.from(j,240000)
            assertEquals(6,status.stopsLeft)
            assertEquals(2,status.progress)
            assertEquals(480000L,status.remainingMillis)
        }
    }
    @Test fun recentGpsIsShownAsGpsThenFallsBackToTimeEstimate() {
        val j = Journey.create(8,0,2,120,0).copy(source="GPS")
        assertTrue(TripProgress.from(j,1000).position.startsWith("Konum"))
        assertTrue(TripProgress.from(j,31000).position.startsWith("Süre"))
    }
    @Test fun countdownAndProgressAreBoundedAfterArrival() {
        val status = TripProgress.from(Journey.create(8,0,2,120,0),9999999)
        assertEquals(0L,status.remainingMillis)
        assertEquals(0,status.stopsLeft)
        assertEquals(status.total,status.progress)
    }
}
