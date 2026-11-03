package com.metroup

object BoardingGuard {
    fun problem(fix: GpsFix?, confirmed: StationMatch?, start: Int, now: Long,
                hasPermission: Boolean, gpsEnabled: Boolean, route: MetroRoute = Routes.m1a, destination: Int? = null): String? {
        if (!hasPermission) return "Alarmı başlatmak için hassas konum iznini aç."
        if (!gpsEnabled) return "Telefonun konum servisini aç. Alarm başlatılmadı."
        if (fix == null || !StationMatcher.usable(fix, now))
            return "Güncel ve yeterli doğrulukta konum bekleniyor. Alarm başlatılmadı."
        val nearest = StationMatcher(route).locate(fix, now)
            ?: return "Şu anda ${route.label} güzergâhına yakın bulunmuyorsunuz. Alarm başlatılmadı."
        if (confirmed == null || confirmed.routeId != route.id || confirmed.elapsed != fix.elapsed || kotlin.math.abs(confirmed.position-nearest.position)>0.2)
            return "Hat konumu doğrulanıyor; daha iyi veya tutarlı bir ölçüm bekleniyor."
        if (destination != null) {
            if (nearest.position !in minOf(start,destination).toDouble()..maxOf(start,destination).toDouble())
                return "Konumun seçtiğin biniş ve varış arasındaki bölümde değil. Biniş durağını güncelle."
        } else if (start != nearest.station)
            return "Biniş durağını ${route.stations[nearest.station]} olarak seç. Alarm başlatılmadı."
        return null
    }
}
