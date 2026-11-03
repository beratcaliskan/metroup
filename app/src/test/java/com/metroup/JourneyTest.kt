package com.metroup

import org.junit.Assert.*
import org.junit.Test

class JourneyTest {
    @Test fun terazidereToYenikapi() {
        val j = Journey.create(8, 0, 2, 120, 1000)
        assertEquals(2, j.alarmStation)
        assertEquals(721000L, j.alarmElapsed)
        assertEquals(7, j.estimatedStation(121000))
        assertEquals(0, j.estimatedStation(9999999))
    }
    @Test fun reverseDirection() {
        val j = Journey.create(0, 8, 3, 90, 0)
        assertEquals(5, j.alarmStation)
        assertEquals(450000L, j.alarmElapsed)
        assertEquals(2, j.estimatedStation(180000))
    }
    @Test fun correctionAccountsForDelays() {
        val j = Journey.create(8, 0, 1, 120, 0).reanchor(7, 500000, "manual")
        assertEquals(1220000L, j.alarmElapsed)
        assertEquals(7, j.estimatedStation(500000))
    }
    @Test fun correctionAtOrBeyondTriggerAlarmsImmediately() {
        assertEquals(500000L, Journey.create(8, 0, 2, 120, 0).reanchor(1, 500000, "GPS").alarmElapsed)
    }
    @Test fun wallClockChangesDoNotAffectProgress() {
        val j = Journey.create(8, 0, 1, 120, 10000)
        assertEquals(8, j.estimatedStation(0))
    }
    @Test fun rejectInvalidRoutes() {
        listOf({ Journey.create(8,8,1,120,0) }, { Journey.create(8,7,2,120,0) },
            { Journey.create(8,0,4,120,0) }, { Journey.create(-1,0,1,120,0) },
            { Journey.create(8,0,1,0,0) }).forEach { factory ->
            assertThrows(IllegalArgumentException::class.java) { factory() }
        }
    }
}
