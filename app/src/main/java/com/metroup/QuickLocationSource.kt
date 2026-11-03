package com.metroup

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource

// One lifecycle-owned source for the screen and foreground service alike.
class QuickLocationSource(private val context: Context, private val receive: (Location) -> Unit) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private var running = false
    private var generation = 0
    private var newest = -1L
    private var token: CancellationTokenSource? = null
    private var fused: FusedLocationProviderClient? = null
    private var fusedCallback: LocationCallback? = null
    private var nativeListener: LocationListener? = null
    @SuppressLint("MissingPermission")
    fun start() {
        if (running || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        running = true
        val session = ++generation
        fun emit(location: Location?) {
            if (!running || session != generation || location == null || !location.hasAccuracy()) return
            val fix = GpsFix(GeoPoint(location.latitude,location.longitude),location.accuracy.toDouble(),location.elapsedRealtimeNanos/1_000_000)
            if (!StationMatcher.usable(fix,SystemClock.elapsedRealtime())) return
            if (location.elapsedRealtimeNanos < newest) return
            newest = location.elapsedRealtimeNanos
            receive(location)
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) { emit(location) }
            @Deprecated("Legacy callback")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        nativeListener = listener
        // Native providers remain usable without Google Play services or internet.
        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)) {
            runCatching {
                if (manager.isProviderEnabled(provider)) {
                    emit(manager.getLastKnownLocation(provider))
                    manager.requestLocationUpdates(provider, 1_000L, 0f, listener, Looper.getMainLooper())
                }
            }
        }
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) != ConnectionResult.SUCCESS) return
        val client = LocationServices.getFusedLocationProviderClient(context)
        fused = client
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) { result.locations.sortedBy { it.elapsedRealtimeNanos }.forEach { emit(it) } }
        }
        fusedCallback = callback
        token = CancellationTokenSource()
        runCatching {
            client.lastLocation.addOnSuccessListener { emit(it) }
            client.getCurrentLocation(CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY).setMaxUpdateAgeMillis(10_000)
                .setDurationMillis(10_000).build(), token!!.token).addOnSuccessListener { emit(it) }
            client.requestLocationUpdates(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000)
                .setMinUpdateIntervalMillis(500).setMaxUpdateDelayMillis(0)
                .setMaxUpdateAgeMillis(10_000).setWaitForAccurateLocation(false).build(), callback, Looper.getMainLooper())
                .addOnSuccessListener { if (!running || session != generation) client.removeLocationUpdates(callback) }
        }
    }
    fun stop() {
        running = false; generation++; newest = -1
        token?.cancel(); token = null
        nativeListener?.let { runCatching { manager.removeUpdates(it) } }; nativeListener = null
        fusedCallback?.let { callback -> runCatching { fused?.removeLocationUpdates(callback) } }
        fusedCallback = null; fused = null
    }
    companion object {
        fun enabled(context: Context) = LocationManagerCompat.isLocationEnabled(context.getSystemService(LocationManager::class.java))
    }
}
