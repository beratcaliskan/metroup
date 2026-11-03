package com.metroup

import org.junit.Assert.*
import org.junit.Test

class RouteMapStateTest {
    private val route = Routes.m2
    private fun state(fix: GpsFix?=null, match: StationMatch?=null, journey: Journey?=null, now: Long=1000) =
        RouteMapState.create(route,journey,fix,match,0,14,2,now)
    @Test fun noLocationDoesNotInventAMeasuredPosition() {
        val s=state()
        assertNull(s.measured); assertNull(s.estimated); assertFalse(s.live)
        assertEquals(12,s.alarmStation)
    }
    @Test fun currentMeasurementIsNotSnappedToTheLine() {
        val point=GeoPoint(41.04,29.1)
        val s=state(GpsFix(point,15.0,1000))
        assertEquals(point,s.measured!!.point); assertTrue(s.live); assertNull(s.estimated)
    }
    @Test fun staleMeasurementIsMarkedOldThenRemoved() {
        val fix=GpsFix(route.points[4],15.0,1000)
        assertFalse(state(fix,now=12000).live)
        assertNotNull(state(fix,now=12000).measured)
        assertNull(state(fix,now=301001).measured)
    }
    @Test fun offlineEstimateContinuesInBothDirections() {
        for ((from,to) in listOf(0 to 14,14 to 0)) {
            val j=Journey.create(from,to,2,120,1000,route)
            val s=state(journey=j,now=61000)
            assertNull(s.measured)
            assertEquals(if(from==0)0.5 else 13.5,s.estimatedPosition!!,0.00001)
            assertEquals(RouteMapState.interpolate(route,s.estimatedPosition!!),s.estimated)
        }
    }
    @Test fun measurementAndEstimateAreSeparateMarkers() {
        val s=state(GpsFix(GeoPoint(41.04,29.1),15.0,1000),journey=Journey.create(0,14,2,120,1000,route))
        assertNotEquals(s.measured!!.point,s.estimated)
    }
    @Test fun switchingRouteDropsPreviousRouteEstimate() {
        val j=Journey.create(0,17,2,120,1000,Routes.m1a)
        assertNull(state(match=StationMatch(4,0.0,1000,"M1A"),journey=j).estimated)
    }
    @Test fun fractionalPositionAndEndpointsInterpolateCorrectly() {
        assertEquals(route.points.first(),RouteMapState.interpolate(route,-1.0))
        assertEquals(route.points.last(),RouteMapState.interpolate(route,99.0))
        val p=RouteMapState.interpolate(route,4.5)
        assertEquals((route.points[4].latitude+route.points[5].latitude)/2,p.latitude,0.00000001)
    }
    @Test fun invalidAndFutureFixesAreNotDisplayed() {
        assertNull(state(GpsFix(GeoPoint(Double.NaN,29.0),10.0,1000)).measured)
        assertNull(state(GpsFix(route.points[4],10.0,2000)).measured)
    }
}
