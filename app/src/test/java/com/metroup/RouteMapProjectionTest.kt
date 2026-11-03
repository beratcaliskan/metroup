package com.metroup

import org.junit.Assert.*
import org.junit.Test

class RouteMapProjectionTest {
    @Test fun allRoutePointsFitNativeViewport() {
        Routes.all.forEach { route ->
            val projection=RouteMapProjection(route.points,320f,240f,32f)
            route.points.forEach { point ->
                val xy=projection.project(point)
                assertTrue(xy.x in 31.9f..288.1f)
                assertTrue(xy.y in 31.9f..208.1f)
            }
        }
    }
    @Test fun northIsUpAndEastIsRight() {
        val p=RouteMapProjection(listOf(GeoPoint(41.0,28.9),GeoPoint(41.1,29.0)),320f,240f,32f)
        val a=p.project(GeoPoint(41.0,28.9));val b=p.project(GeoPoint(41.1,29.0))
        assertTrue(b.y<a.y);assertTrue(b.x>a.x)
    }
    @Test fun nativeMarkerAndEstimateAreProjectedIndependently() {
        val p=RouteMapProjection(Routes.m2.points,320f,240f,32f)
        val estimate=RouteMapState.interpolate(Routes.m2,4.5)
        assertNotEquals(p.project(estimate),p.project(Routes.m2.points[5]))
        assertTrue(p.radius(25.0)>0)
    }
}
