package com.metroup

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class TrackingService : Service() {
    private val locations by lazy { QuickLocationSource(this, ::onLocationChanged) }
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

    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val j = Store(this).load() ?: run { stopSelf(); return START_NOT_STICKY }
        Alarms.channel(this, "trip", "Aktif yolculuk", NotificationManager.IMPORTANCE_LOW)
        startForeground(41, notification(j))
        handler.removeCallbacks(refresh)
        handler.postDelayed(refresh, 15_000)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locations.start()
        }
        return START_STICKY
    }
    private fun onLocationChanged(location: Location) {
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
        if (kotlin.math.abs(match.position-(j.anchor+j.anchorFraction)) < 0.05 && now - j.anchorElapsed < 10_000) return
        val updated = j.reanchorPosition(match.position, match.elapsed, "Konum hat tahmini")
        runCatching { Alarms.schedule(this, updated); store.save(updated); startForeground(41, notification(updated)) }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        locations.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}

class TripActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { Alarms.stop(context) }
}
