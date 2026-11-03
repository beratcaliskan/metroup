package com.metroup

data class MetroRoute(val id: String, val code: String, val label: String, val stations: List<String>, val points: List<GeoPoint>) {
    init { require(stations.size == points.size && stations.size >= 2) }
}

object Routes {
    val m1a = MetroRoute("M1A", "M1A", "M1A · Yenikapı – Atatürk Havalimanı", M1A.stations, StationCatalog.points)
    val m2 = MetroRoute("M2", "M2", "M2 · Yenikapı – Hacıosman",
        listOf("Yenikapı", "Vezneciler-İstanbul Ü.", "Haliç", "Şişhane", "Taksim", "Osmanbey", "Şişli-Mecidiyeköy", "Gayrettepe", "Levent", "4.Levent", "Sanayi Mahallesi", "İTÜ-Ayazağa", "Atatürk Oto Sanayi", "Darüşşafaka", "Hacıosman"),
        listOf(
            GeoPoint(41.0055971704, 28.9513306172), // Yenikapı
            GeoPoint(41.011475, 28.960705), // Vezneciler-İstanbul Ü.
            GeoPoint(41.0224366195, 28.9664183343), // Haliç
            GeoPoint(41.028298, 28.97285), // Şişhane
            GeoPoint(41.038496, 28.985783), // Taksim
            GeoPoint(41.052955, 28.987375), // Osmanbey
            GeoPoint(41.0645069127, 28.9926697578), // Şişli-Mecidiyeköy
            GeoPoint(41.0690012375, 29.0109200808), // Gayrettepe
            GeoPoint(41.0767734293, 29.0136876237), // Levent
            GeoPoint(41.0860080659, 29.0070897722), // 4.Levent
            GeoPoint(41.0943240181, 29.0053380126), // Sanayi Mahallesi
            GeoPoint(41.108094796, 29.0207947235), // İTÜ-Ayazağa
            GeoPoint(41.1182574386, 29.0241897659), // Atatürk Oto Sanayi
            GeoPoint(41.1295823098, 29.0251244919), // Darüşşafaka
            GeoPoint(41.1397745691, 29.0304324362), // Hacıosman
        ))
    val m2Shuttle = MetroRoute("M2_SHUTTLE", "M2", "M2 · Sanayi Mahallesi – Seyrantepe",
        listOf("Sanayi Mahallesi", "Seyrantepe"),
        listOf(
            GeoPoint(41.0943240181, 29.0053380126), // Sanayi Mahallesi
            GeoPoint(41.100906, 28.997775), // Seyrantepe
        ))
    val all = listOf(m1a, m2, m2Shuttle)
    fun get(id: String): MetroRoute = all.firstOrNull { it.id == id } ?: error("Bilinmeyen hat: $id")
}
