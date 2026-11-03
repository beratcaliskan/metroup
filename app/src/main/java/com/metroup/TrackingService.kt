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
    private var matcher = StationMatcher()
    private var matcherRouteId = "M1A"
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            val j = Store(this@TrackingService).load()
            if (j == null || j.ringing) { stopSelf(); return }
            startForeground(41, notification(j))
            handler.postDelayed(this, 15_000)
        }
    }
    private fun notification(j: Journey): Notification {
        val now = SystemClock.elapsedRealtime()
        val status = TripProgress.from(j, now)
        val stop = PendingIntent.getBroadcast(this, 3, Intent(this, TripActionReceiver::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "trip")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("${j.route.code} → ${j.route.stations[j.destination]}")
            .setContentText(status.position)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "${status.position}\nVarışa ${status.stopsLeft} durak • ${j.before} durak önce uyarı\nAlarm durağı: ${j.route.stations[j.alarmStation]}\nGeri sayım tahminidir; GPS yoksa süreyle ilerler."))
            .setSubText("Alarma kalan tahmini süre")
            .setWhen(System.currentTimeMillis() + status.remainingMillis)
            .setUsesChronometer(status.remainingMillis > 0).setChronometerCountDown(true)
            .setProgress(status.total, status.progress, false)
            .setContentIntent(Alarms.open(this)).setOngoing(true).setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .addAction(0, "Yolculuğu bitir", stop).build()
    }
    override fun onBind(intent: Intent?) = null
    override fun onCreate() {
        super.onCreate()
        manager = getSystemService(LocationManager::class.java)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val j = Store(this).load() ?: run { stopSelf(); return START_NOT_STICKY }
        Alarms.channel(this, "trip", "Aktif yolculuk", NotificationManager.IMPORTANCE_LOW)
        startForeground(41, notification(j))
        handler.removeCallbacks(refresh)
        handler.postDelayed(refresh, 15_000)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            runCatching { manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5_000L, 0f, this) }
        }
        return START_STICKY
    }
    override fun onLocationChanged(location: Location) {
        val store = Store(this)
        val j = store.load() ?: run { stopSelf(); return }
        if (j.ringing || !location.hasAccuracy()) return
        if (matcherRouteId != j.routeId) {
            matcher = StationMatcher(j.route); matcherRouteId = j.routeId
        }
        val now = SystemClock.elapsedRealtime()
        val fix = GpsFix(GeoPoint(location.latitude, location.longitude), location.accuracy.toDouble(), location.elapsedRealtimeNanos / 1_000_000)
        val match = matcher.accept(fix, now, (minOf(j.start, j.destination)..maxOf(j.start, j.destination)).toList()) ?: return
        // Renew at the same station while waiting there, rather than drifting
        // forward during a delay. Never postpone an already due alarm.
        if (j.alarmElapsed <= now) return
        if (match.station == j.anchor && now - j.anchorElapsed < 15_000) return
        val updated = j.reanchor(match.station, match.elapsed, "GPS yakınlık doğrulaması")
        runCatching { Alarms.schedule(this, updated); store.save(updated); startForeground(41, notification(updated)) }
    }

    @Deprecated("Legacy location callback")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) = Unit
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        manager.removeUpdates(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}

class TripActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { Alarms.stop(context) }
}
