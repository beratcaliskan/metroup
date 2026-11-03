package com.metroup

import android.app.*
import android.content.*
import android.media.*
import android.os.*
import androidx.core.app.NotificationCompat

object Alarms {
    private fun pending(c: Context) = PendingIntent.getBroadcast(c, 1, Intent(c, AlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun allowed(c: Context): Boolean = Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun schedule(c: Context, j: Journey) {
        check(allowed(c)) { "Kesin alarm izni gerekli" }
        c.getSystemService(AlarmManager::class.java).setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
            maxOf(j.alarmElapsed, SystemClock.elapsedRealtime() + 1000), pending(c))
    }
    fun cancel(c: Context) { c.getSystemService(AlarmManager::class.java).cancel(pending(c)) }
    fun stop(c: Context) {
        cancel(c)
        Store(c).clear()
        c.stopService(Intent(c, TrackingService::class.java))
        c.stopService(Intent(c, RingingService::class.java))
    }
    fun open(c: Context): PendingIntent = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun channel(c: Context, id: String, name: String, importance: Int) {
        c.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(id, name, importance).apply {
            if (id == "ring") { setSound(null, null); enableVibration(false) }
        })
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = Store(context)
        val journey = store.load() ?: return
        if (journey.ringing) return
        // An obsolete broadcast after rescheduling must not wake the passenger early.
        if (SystemClock.elapsedRealtime() + 1000 < journey.alarmElapsed) return
        store.save(journey.copy(ringing = true))
        context.startForegroundService(Intent(context, RingingService::class.java))
    }
}

class RingingService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { Alarms.stop(this); return START_NOT_STICKY }
        val j = Store(this).load() ?: run { stopSelf(); return START_NOT_STICKY }
        Alarms.channel(this, "ring", "Varış alarmı", NotificationManager.IMPORTANCE_HIGH)
        val stop = PendingIntent.getService(this, 2, Intent(this, RingingService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE)
        startForeground(42, NotificationCompat.Builder(this, "ring")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Uyanma zamanı!")
            .setContentText("${j.route.stations[j.destination]} için ${j.before} durak önce • tahmini")
            .setContentIntent(Alarms.open(this)).setOngoing(true).setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(0, "Alarmı durdur", stop).build())
        if (player == null) {
            runCatching {
                player = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    setDataSource(this@RingingService, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    isLooping = true
                    setWakeMode(this@RingingService, PowerManager.PARTIAL_WAKE_LOCK)
                    prepare(); start()
                }
            }
            vibrator = getSystemService(Vibrator::class.java).also { it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 400, 700, 400), 0)) }
            // Bound playback to five minutes if the user cannot interact.
            handler.postDelayed({ Alarms.stop(this) }, 300_000)
        }
        return START_NOT_STICKY
    }
    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        player?.release(); vibrator?.cancel()
        super.onDestroy()
    }
}
