package com.metroup

import org.junit.Assert.*
import org.junit.Test

class StationMatcherTest {
    private fun fix(station: Int = 8, time: Long = 1000, accuracy: Double = 40.0) =
        GpsFix(StationCatalog.points[station], accuracy, time)
    private val route = (0..8).toList()
    @Test fun catalogContainsAllM1AStationsInOrder() {
        assertEquals(M1A.stations.size, StationCatalog.points.size)
        StationCatalog.points.indices.forEach { i ->
            assertEquals(i, StationMatcher().nearest(fix(i), 1000)!!.station)
        }
    }
    @Test fun moderateFixesNeedTwoIndependentMeasurements() {
        val m = StationMatcher()
        assertNull(m.accept(fix(), 1000, route))
        assertNull(m.accept(fix(), 1001, route))
        assertEquals(8, m.accept(fix(time=3000), 3000, route)!!.station)
        assertEquals(8, m.accept(fix(time=6000), 6000, route)!!.station)
    }
    @Test fun rejectsStaleFutureAndInaccurateFixes() {
        val m = StationMatcher()
        assertNull(m.nearest(fix(), 31001))
        assertNull(m.nearest(fix(), 999))
        assertNull(m.nearest(fix(accuracy=61.0), 1000))
        assertNull(m.nearest(fix(accuracy=Double.NaN), 1000))
        assertNull(m.nearest(GpsFix(GeoPoint(Double.NaN, 28.9), 10.0, 1000), 1000))
    }
    @Test fun rejectsFarAwayAndOutOfRouteStations() {
        val m = StationMatcher()
        assertNull(m.nearest(GpsFix(GeoPoint(41.1, 29.1), 10.0, 1000),1000))
        assertNull(m.nearest(fix(9), 1000, route))
    }
    @Test fun oneStationSpikeCannotReanchor() {
        val m = StationMatcher()
        assertNull(m.accept(fix(8), 1000, route))
        assertNull(m.accept(fix(7,6000), 6000, route))
        assertNull(m.accept(fix(8,11000), 11000, route))
        assertEquals(8,m.accept(fix(8,16000),16000,route)!!.station)
    }
    @Test fun tunnelGapRequiresFreshConfirmation() {
        val m = StationMatcher()
        assertNull(m.accept(fix(), 1000, route))
        assertNull(m.accept(fix(time=40000), 40000, route))
        assertEquals(8,m.accept(fix(time=45000),45000,route)!!.station)
    }
    @Test fun gpsCanCorrectAnEstimateMoreThanTwoStationsAway() {
        val j = Journey.create(8,0,1,120,0)
        assertEquals(2,j.estimatedStation(720000))
        val m = StationMatcher()
        m.accept(fix(7,720000),720000,route)
        val match = m.accept(fix(7,725000),725000,route)!!
        val corrected = j.reanchor(match.station,match.elapsed,"GPS")
        assertEquals(7,corrected.anchor)
        assertEquals(1445000L,corrected.alarmElapsed)
    }
    @Test fun reachingAlarmStationMakesAlarmDueNowInBothDirections() {
        for (j in listOf(Journey.create(8,0,2,120,0),Journey.create(0,8,2,120,0))) {
            val m = StationMatcher()
            m.accept(fix(j.alarmStation,1000),1000,route)
            val match = m.accept(fix(j.alarmStation,6000),6000,route)!!
            assertEquals(6000L,j.reanchor(match.station,match.elapsed,"GPS").alarmElapsed)
        }
    }
}
