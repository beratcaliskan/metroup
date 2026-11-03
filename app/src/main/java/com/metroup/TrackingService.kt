package com.metroup

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.location.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class TrackingService : Service(), LocationListener {
    private lateinit var manager: LocationManager
    private val matcher = StationMatcher()
    override fun onBind(intent: Intent?) = null
    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(LocationManager::class.java)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val j = Store(this).load() ?: run { stopSelf(); return START_NOT_STICKY }
        Alarms.channel(this, "trip", "Aktif yolculuk", NotificationManager.IMPORTANCE_LOW)
        startForeground(41, NotificationCompat.Builder(this, "trip")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("MetroUp • ${M1A.stations[j.destination]}")
            .setContentText("Çevrimdışı alarm kurulu. Konum varsa tahmin düzeltilir.")
            .setContentIntent(Alarms.open(this)).setOngoing(true).build())
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            runCatching { manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5_000L, 0f, this) }
        }
        return START_NOT_STICKY
    }
    override fun onLocationChanged(location: Location) {
        val store = Store(this)
        val j = store.load() ?: run { stopSelf(); return }
        if (j.ringing || !location.hasAccuracy()) return
        val now = SystemClock.elapsedRealtime()
        val fix = GpsFix(GeoPoint(location.latitude, location.longitude), location.accuracy.toDouble(), location.elapsedRealtimeNanos / 1_000_000)
        val match = matcher.accept(fix, now, (minOf(j.start, j.destination)..maxOf(j.start, j.destination)).toList()) ?: return
        // Renew at the same station while waiting there, rather than drifting
        // forward during a delay. Never postpone an already due alarm.
        if (j.alarmElapsed <= now) return
        if (match.station == j.anchor && now - j.anchorElapsed < 15_000) return
        val updated = j.reanchor(match.station, match.elapsed, "GPS yakınlık doğrulaması")
        runCatching { Alarms.schedule(this, updated); store.save(updated) }
    }

    @Deprecated("Legacy location callback")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) = Unit
    override fun onDestroy() { manager.removeUpdates(this); super.onDestroy() }
}
