package com.metroup

import org.junit.Assert.*
import org.junit.Test

class FastPositionTest {
    private val route=Routes.m2
    private val all=route.stations.indices.toList()
    private fun fix(position: Double, time: Long, accuracy: Double=10.0): GpsFix {
        val index=kotlin.math.floor(position).toInt(); val t=position-index
        val a=route.points[index]; val b=route.points[(index+1).coerceAtMost(route.points.lastIndex)]
        return GpsFix(GeoPoint(a.latitude+(b.latitude-a.latitude)*t,a.longitude+(b.longitude-a.longitude)*t),accuracy,time)
    }
    @Test fun freshPreciseFirstFixIsImmediatelyAccepted() {
        assertNotNull(StationMatcher(route).accept(fix(4.0,1000),1000,all))
    }
    @Test fun movingBetweenStopsDoesNotRequireDwellingAtStation() {
        val m=StationMatcher(route)
        assertNull(m.accept(fix(4.4,1000,40.0),1000,all))
        val match=m.accept(fix(4.42,2000,40.0),2000,all)!!
        assertEquals(4.42,match.position,0.01)
    }
    @Test fun moderateFixesAtDifferentPositionsCanConfirm() {
        val m=StationMatcher(route)
        assertNull(m.accept(fix(4.3,1000,40.0),1000,all))
        assertNotNull(m.accept(fix(4.32,2000,40.0),2000,all))
    }
    @Test fun frequentHalfSecondUpdatesDoNotRestartConfirmationWindow() {
        val m=StationMatcher(route)
        assertNull(m.accept(fix(4.3,1000,40.0),1000,all))
        assertNull(m.accept(fix(4.31,1500,40.0),1500,all))
        assertNotNull(m.accept(fix(4.32,2000,40.0),2000,all))
    }
    @Test fun stalePreciseCacheDoesNotGetFastConfirmation() {
        assertNull(StationMatcher(route).accept(fix(4.0,1000),12000,all))
        assertNull(StationMatcher(route).accept(fix(4.0,1000),31001,all))
    }
    @Test fun duplicatedCachedFixCannotCountAsTwoMeasurements() {
        val m=StationMatcher(route); val point=fix(4.3,1000,40.0)
        assertNull(m.accept(point,1000,all)); assertNull(m.accept(point,2000,all))
    }
    @Test fun implausibleJumpIsRejectedEvenWithReportedHighAccuracy() {
        val m=StationMatcher(route)
        assertNotNull(m.accept(fix(4.0,1000),1000,all))
        assertNull(m.accept(fix(8.0,2000),2000,all))
    }
    @Test fun farOffLineStillBlocked() {
        val far=GpsFix(GeoPoint(41.0,29.2),10.0,1000)
        assertNull(StationMatcher(route).locate(far,1000))
    }
    @Test fun canStartOnPlannedSectionAfterPassingOriginalBoardingStation() {
        val point=fix(4.4,1000); val match=StationMatcher(route).accept(point,1000,all)!!
        assertNull(BoardingGuard.problem(point,match,0,1000,true,true,route,14))
        assertNotNull(BoardingGuard.problem(point,match,8,1000,true,true,route,14))
    }
    @Test fun fractionalPositionAdjustsAlarmBothDirections() {
        val forward=Journey.create(0,14,2,120,0,route).reanchorPosition(4.5,1000,"GPS")
        assertEquals(901000L,forward.alarmElapsed)
        assertEquals(4,forward.estimatedStation(1000))
        assertEquals(5,forward.estimatedStation(61000))
        val back=Journey.create(14,0,2,120,0,route).reanchorPosition(4.5,1000,"GPS")
        assertEquals(301000L,back.alarmElapsed)
        assertEquals(5,back.estimatedStation(1000))
        assertEquals(4,back.estimatedStation(61000))
        assertTrue(TripProgress.from(back,1000).position.contains("arası"))
    }
    @Test fun expiredOrOtherLineConfirmationCannotStartJourney() {
        val point=fix(4.4,1000);val match=StationMatcher(route).accept(point,1000,all)!!
        assertNotNull(BoardingGuard.problem(point.copy(elapsed=2000),match,0,2000,true,true,route,14))
        assertNotNull(BoardingGuard.problem(point,match.copy(routeId="M1A"),0,1000,true,true,route,14))
    }
}
