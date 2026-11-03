# M1A ve M2 durak koordinatları

Kaynak: [İBB Metro İstanbul GetStations](https://api.ibb.gov.tr/MetroIstanbul/api/MetroMobile/V2/GetStations). Kaynak, İBB Açık Veri Portalı CKAN package_search servisindeki Metro İstanbul İstasyon Bilgi Listesi kaydından doğrulandı. Erişim: 3 Kasım 2026.

JSON cevabında `Success=true`, `LineName=M1A` kayıtları alındı; `Order` alanına göre 1–18 sıralandı. `DetailInfo.Latitude`/`Longitude` ondalık derece alanları kullanıldı. İBB'nin kaydını uygulama isimlerine bağlamak için sıra kullanılır; Otogar kaydı da dahil 18 benzersiz sıra doğrulandı. İndirilen M1A alanları `m1a-stations.json` içinde saklanır; uygulama kopyası `StationCatalog.kt` içindedir. Güncellemede iki dosya birlikte değiştirilmelidir. Katalogda Otogar istasyon kimliği 203, Terazidere 114, Yenikapı 121'dir. Katalog canlı tren konumu veya gerçek zamanlı varış verisi içermez.

Katalog uygulamaya gömülüdür. Yolculuk sırasında bu API'ye istek atılmaz. Yakınlık Haversine ile, duraklar arasındaki yaklaşık konum yerel düzleme izdüşümle bulunur. Ardışık durakları birleştiren çizgiler gerçek ray geometrisi değildir. Eşikler, hızlı ölçüm onayı ve belirsizlik sınırları README'de açıklanır. Önceden durağı öğretmek gerekmez; tüm hesaplar cihazda yapılır. Yeni Fused Location kaynağı cihazın Wi-Fi/hücresel/uydu konum hizmetlerini kullanabilir; fiziksel cihazda gecikme ölçümü henüz yapılmadı.


## M2 ve Seyrantepe kolu

Aynı resmi API'nin `LineName=M2` kayıtları 3 Kasım 2026 tarihinde yeniden alındı. Kaynak kopyası `m2-stations.json`, uygulama modeli `Routes.kt` içindedir. İBB 16 kayıt verir; Seyrantepe'nin Order=12 değeri ana hat üzerinde doğrusal sıra olarak kullanılmaz. Ana hat Seyrantepe (Id=31) çıkarılarak 15 durakla tanımlanır; Sanayi Mahallesi (Id=30) → İTÜ-Ayazağa (Id=32) bitişiktir. Ayrı kol Sanayi Mahallesi → Seyrantepe olarak tanımlanır. Kaynak: [Metro İstanbul peron değişimi açıklaması](https://www.metro.istanbul/Home/SikcaSorulanSorular?q=12).

Her eşleşme ve yolculuk bir güzergâh kimliği taşır: M1A, M2 veya M2_SHUTTLE. Hat değişiminden önceki GPS doğrulaması yeni hatta kullanılamaz. Yenikapı'nın M1A/M2 noktaları yakın olduğundan iki hat da konumsal olarak mümkün olabilir; GPS peron veya tren tespiti değildir. Kullanıcı seçimi korunur. Yeni kataloglar da internet olmadan kullanılabilir.
