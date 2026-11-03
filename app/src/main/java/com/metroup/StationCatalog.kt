package com.metroup

data class GeoPoint(val latitude: Double, val longitude: Double)

// IBB MetroMobile GetStations snapshot, 2026-11-03. See docs/station-data.md.
object StationCatalog {
    val points = listOf(
        GeoPoint(41.004755, 28.952549), // Yenikapı
        GeoPoint(41.0120281897, 28.9480625565), // Aksaray
        GeoPoint(41.0176115439, 28.9395963977), // Emniyet-Fatih
        GeoPoint(41.0240250582, 28.9305034257), // Topkapı-Ulubatlı
        GeoPoint(41.0340978572, 28.920238689), // Bayrampaşa-Maltepe
        GeoPoint(41.0408544497, 28.9072352877), // Sağmalcılar
        GeoPoint(41.0484928183, 28.8953862076), // Kocatepe
        GeoPoint(41.0401441651, 28.8945600984), // Otogar
        GeoPoint(41.0303317032, 28.8979522419), // Terazidere
        GeoPoint(41.0206387059, 28.9001417888), // Davutpaşa-YTÜ
        GeoPoint(41.0076458753, 28.8961771147), // Merter
        GeoPoint(41.0017008338, 28.8896520683), // Zeytinburnu
        GeoPoint(40.9966121333, 28.8753997556), // Bakırköy-İncirli
        GeoPoint(40.9953532384, 28.8630661969), // Bahçelievler
        GeoPoint(40.9913500997, 28.846082309), // Ataköy-Şirinevler
        GeoPoint(40.9893149835, 28.8367043004), // Yenibosna
        GeoPoint(40.9866455145, 28.8285503243), // DTM-İstanbul Fuar Merkezi
        GeoPoint(40.9795429431, 28.8211244027), // Atatürk Havalimanı
    )
}
