package com.metroup

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.location.*
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs

class TrackingService : Service(), LocationListener {
    private lateinit var manager: LocationManager
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
            runCatching { manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10_000L, 25f, this) }
        }
        return START_NOT_STICKY
    }
    override fun onLocationChanged(location: Location) {
        val age = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        if (!location.hasAccuracy() || location.accuracy > 60 || age !in 0..30_000_000_000L) return
        val store = Store(this)
        val j = store.load() ?: run { stopSelf(); return }
        if (j.ringing) return
        val now = SystemClock.elapsedRealtime()
        val estimated = j.estimatedStation(now)
        val candidates = (minOf(j.start, j.destination)..maxOf(j.start, j.destination)).mapNotNull { i ->
            val point = store.coordinate(i) ?: return@mapNotNull null
            val distance = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, point.first, point.second, distance)
            i to distance[0]
        }.filter { (i, distance) ->
            distance <= 100 && (i - j.anchor) * j.direction > 0 && abs(i - estimated) <= 2
        }
        val station = candidates.minByOrNull { it.second }?.first ?: return
        // Once a station is accepted it cannot continuously reset its own timer.
        val updated = j.reanchor(station, now, "GPS ile doğrulandı")
        runCatching { Alarms.schedule(this, updated); store.save(updated) }
    }
    @Deprecated("Legacy location callback")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) = Unit
    override fun onDestroy() { manager.removeUpdates(this); super.onDestroy() }
}
