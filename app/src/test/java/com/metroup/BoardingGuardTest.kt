package com.metroup

import org.junit.Assert.*
import org.junit.Test

class BoardingGuardTest {
    private val fix = GpsFix(StationCatalog.points[8],10.0,1000)
    private val confirmed = StationMatch(8,0.0,1000)
    private fun problem(f: GpsFix? = fix, c: StationMatch? = confirmed, start: Int = 8, now: Long = 1000,
                        permission: Boolean = true, enabled: Boolean = true) =
        BoardingGuard.problem(f,c,start,now,permission,enabled)
    @Test fun allowsConfirmedCurrentStation() { assertNull(problem()) }
    @Test fun rejectsFarAwayEvenWithPreviousNearConfirmation() {
        assertNotNull(problem(f = fix.copy(point=GeoPoint(41.1,29.1))))
    }
    @Test fun requiresPermissionAndEnabledGps() {
        assertNotNull(problem(permission=false)); assertNotNull(problem(enabled=false))
    }
    @Test fun rejectsMissingStaleAndInaccurateLocation() {
        assertNotNull(problem(f=null)); assertNotNull(problem(now=31001))
        assertNotNull(problem(f=fix.copy(accuracyMeters=100.0)))
    }
    @Test fun rejectsSingleMeasurementAndStaleConfirmation() {
        assertNotNull(problem(c=null))
        assertNotNull(problem(f=fix.copy(elapsed=40000),now=40000))
    }
    @Test fun rejectsWrongBoardingStation() { assertNotNull(problem(start=0)) }
    @Test fun rejectsChangedStationBeforeSecondConfirmation() {
        assertNotNull(problem(f=fix.copy(point=StationCatalog.points[7])))
    }
}
