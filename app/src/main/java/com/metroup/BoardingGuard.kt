package com.metroup

object BoardingGuard {
    fun problem(fix: GpsFix?, confirmed: StationMatch?, start: Int, now: Long,
                hasPermission: Boolean, gpsEnabled: Boolean): String? {
        if (!hasPermission) return "Alarmı başlatmak için hassas konum iznini aç."
        if (!gpsEnabled) return "Telefonun konum servisini aç. Alarm başlatılmadı."
        if (fix == null || !StationMatcher.usable(fix, now))
            return "Güncel ve yeterli doğrulukta konum bekleniyor. Alarm başlatılmadı."
        val nearest = StationMatcher().nearest(fix, now)
            ?: return "Şu anda M1A hattının bir durağına yakın bulunmuyorsunuz. Alarm başlatılmadı."
        if (confirmed == null || now - confirmed.elapsed !in 0..30_000L || confirmed.station != nearest.station)
            return "Durak yakınlığını doğrulamak için ikinci GPS ölçümü bekleniyor."
        if (start != nearest.station)
            return "Biniş durağını ${M1A.stations[nearest.station]} olarak seç. Alarm başlatılmadı."
        return null
    }
}
