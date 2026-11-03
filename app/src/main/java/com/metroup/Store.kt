package com.metroup

import android.content.Context
import org.json.JSONObject

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("metroup", Context.MODE_PRIVATE)
    fun load(): Journey? = runCatching {
        val o = JSONObject(prefs.getString("journey", null) ?: return null)
        // elapsedRealtime resets on reboot. Boot count prevents stale rides being restored.
        if (o.getInt("boot") != boot()) { clear(); return null }
        Journey(o.getInt("start"), o.getInt("dest"), o.getInt("before"), o.getInt("seconds"),
            o.getInt("anchor"), o.getLong("at"), o.getLong("alarm"), o.getString("source"), o.optBoolean("ringing"))
    }.getOrNull()
    private val app = context.applicationContext
    private fun boot() = android.provider.Settings.Global.getInt(app.contentResolver, "boot_count", -1)
    fun save(j: Journey) {
        val o = JSONObject().put("start", j.start).put("dest", j.destination).put("before", j.before)
            .put("seconds", j.segmentSeconds).put("anchor", j.anchor).put("at", j.anchorElapsed)
            .put("alarm", j.alarmElapsed).put("source", j.source).put("ringing", j.ringing).put("boot", boot())
        check(prefs.edit().putString("journey", o.toString()).commit())
    }
    fun clear() { prefs.edit().remove("journey").commit() }
    fun coordinate(station: Int): Pair<Double, Double>? = runCatching {
        val parts = prefs.getString("station_$station", null)?.split(",") ?: return null
        parts[0].toDouble() to parts[1].toDouble()
    }.getOrNull()
    fun learn(station: Int, lat: Double, lon: Double) {
        prefs.edit().putString("station_$station", "$lat,$lon").apply()
    }
}
