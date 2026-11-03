package com.metroup

import org.junit.Assert.*
import org.junit.Test

class M2Test {
    private val m2 = Routes.m2
    @Test fun mainLineSkipsSeyrantepeBranch() {
        assertEquals(15,m2.stations.size)
        val sanayi = m2.stations.indexOf("Sanayi Mahallesi")
        assertEquals("İTÜ-Ayazağa",m2.stations[sanayi+1])
        assertFalse(m2.stations.contains("Seyrantepe"))
        assertEquals(listOf("Sanayi Mahallesi","Seyrantepe"),Routes.m2Shuttle.stations)
    }
    @Test fun allM2StationsMatchTheirOwnRoute() {
        for (route in listOf(m2,Routes.m2Shuttle)) for (i in route.stations.indices) {
            val match=StationMatcher(route).nearest(GpsFix(route.points[i],10.0,1000),1000)!!
            assertEquals(i,match.station); assertEquals(route.id,match.routeId)
        }
    }
    @Test fun taksimIsRejectedOnM1aAndAcceptedOnM2() {
        val fix=GpsFix(m2.points[4],10.0,1000)
        val confirmed=StationMatch(4,0.0,1000,"M2")
        assertNull(BoardingGuard.problem(fix,confirmed,4,1000,true,true,m2))
        assertNotNull(BoardingGuard.problem(fix,confirmed,4,1000,true,true,Routes.m1a))
    }
    @Test fun changingLinesRequiresNewConfirmationEvenAtSharedYenikapi() {
        val fix=GpsFix(m2.points[0],10.0,1000)
        assertNotNull(BoardingGuard.problem(fix,StationMatch(0,0.0,1000,"M1A"),0,1000,true,true,m2))
        assertNull(BoardingGuard.problem(fix,StationMatch(0,0.0,1000,"M2"),0,1000,true,true,m2))
    }
    @Test fun seyRantepeDoesNotMatchMainLine() {
        assertNull(StationMatcher(m2).nearest(GpsFix(Routes.m2Shuttle.points[1],10.0,1000),1000))
    }
    @Test fun journeyTimingAndProgressWorkBothDirections() {
        for ((start,dest) in listOf(0 to 14,14 to 0)) {
            val j=Journey.create(start,dest,2,120,0,m2)
            assertEquals("M2",j.routeId)
            assertEquals(1440000L,j.alarmElapsed)
            assertEquals(if(start==0)12 else 2,j.alarmStation)
            val status=TripProgress.from(j,240000)
            assertEquals(12,status.stopsLeft)
            assertTrue(status.position.contains(if(start==0)"Haliç" else "Atatürk Oto Sanayi"))
        }
    }
    @Test fun shuttleHasImmediateOneStopBeforeAlarmBothWays() {
        for ((start,dest) in listOf(0 to 1,1 to 0)) {
            val j=Journey.create(start,dest,1,120,1000,Routes.m2Shuttle)
            assertEquals(start,j.alarmStation); assertEquals(1000L,j.alarmElapsed)
            assertEquals("M2_SHUTTLE",j.routeId)
        }
    }
    @Test fun m2ReanchorRetainsLineAndRecalculatesAlarm() {
        val j=Journey.create(0,14,2,120,0,m2).reanchor(6,900000,"GPS")
        assertEquals("M2",j.copy(ringing=true).routeId)
        assertEquals(1620000L,j.alarmElapsed)
        assertTrue(TripProgress.from(j,900000).position.contains("Şişli-Mecidiyeköy"))
    }
    @Test fun rejectsIndicesOutsideSelectedRoute() {
        assertThrows(IllegalArgumentException::class.java) { Journey.create(0,17,2,120,0,m2) }
        assertThrows(IllegalStateException::class.java) { Routes.get("UNKNOWN") }
    }
}
