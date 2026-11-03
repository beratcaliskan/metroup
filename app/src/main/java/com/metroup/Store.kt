package com.metroup

import android.content.Context
import org.json.JSONObject

class Store(context: Context) {
    private val prefs = context.getSharedPreferences("metroup", Context.MODE_PRIVATE)
    fun load(): Journey? = runCatching {
        val o = JSONObject(prefs.getString("journey", null) ?: return null)
        // elapsedRealtime resets on reboot. Boot count prevents stale rides being restored.
        if (o.getInt("boot") != boot()) { clear(); return null }
        val route = Routes.get(o.optString("route", "M1A"))
        require(o.getInt("start") in route.stations.indices && o.getInt("dest") in route.stations.indices && o.getInt("anchor") in route.stations.indices)
        Journey(o.getInt("start"), o.getInt("dest"), o.getInt("before"), o.getInt("seconds"),
            o.getInt("anchor"), o.getLong("at"), o.getLong("alarm"), o.getString("source"), o.optBoolean("ringing"), route.id, o.optDouble("fraction", 0.0))
    }.getOrNull()
    private val app = context.applicationContext
    private fun boot() = android.provider.Settings.Global.getInt(app.contentResolver, "boot_count", -1)
    fun save(j: Journey) {
        val o = JSONObject().put("start", j.start).put("dest", j.destination).put("before", j.before)
            .put("seconds", j.segmentSeconds).put("anchor", j.anchor).put("at", j.anchorElapsed)
            .put("alarm", j.alarmElapsed).put("source", j.source).put("ringing", j.ringing).put("boot", boot()).put("route", j.routeId).put("fraction", j.anchorFraction)
        check(prefs.edit().putString("journey", o.toString()).commit())
    }
    fun clear() { prefs.edit().remove("journey").commit() }
}
