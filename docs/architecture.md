# Mimari ve sonraki aşama

## Çalışan yerel uygulama

`Journey.kt`: Android'den bağımsız hesaplama; yön, durak sayısı, tekdüze aralık süresi, kesirli ilerleme ve alarm noktası. Birim testleri iki yönü, gecikme düzeltmesini, hattın korunmasını ve sınırları kapsar. Journey hat kimliğini taşır; Store hat kimliği olmayan eski kayıtlarda M1A kullanır.

`Store.kt`: SharedPreferences içinde tek aktif yolculuk ve son doğrulanan durak/aralık kesri/zaman. Boot count ile yeniden başlatma tespiti. Bulut hesabı yok.

`Alarms.kt`: monotonic kesin alarm, BroadcastReceiver, mediaPlayback foreground service ile döngülü alarm sesi ve titreşim. AlarmManager uygulama süreci ölse de alarmı tutar; force-stop veya yeniden başlatma hariç.

`StationCatalog.kt`: İBB kaynağından sıralanmış 18 M1A koordinatı. `Routes.kt` M1A, M2 ana hat ve Seyrantepe kolunu istasyon isimleri ve koordinatlarıyla ayrı güzergâhlar olarak sunar. Kaynak kopyaları `docs/m1a-stations.json`, `docs/m2-stations.json` ve ayrıntılar `docs/station-data.md` içinde.

`QuickLocationSource.kt`: ekran/servis yaşam döngüsüne bağlı Fused Location + native GPS/network/passive sağlayıcıları. Cache yaş/doğruluk filtresinden geçer; getCurrentLocation max-age/duration 10 sn, akış hedef aralığı 1 sn, minimum 500 ms, batching ve waitForAccurateLocation kapalı. Nesiller arası eski callback'ler yok sayılır; stop token ve dinleyicileri kaldırır. Google Play services yoksa yerel sağlayıcılar devam eder. Fused dependency'nin eski transitive Fragment sürümü ActivityResult izin akışıyla uyumsuz olduğu için Fragment 1.8.6 açıkça sabitlenir.

`StationMatcher.kt`: durak + yaklaşık doğrusal koridor eşlemesi, kesirli konum. ≤25 m ve ≤10 sn ölçüm ilk seferde; diğer geçerli ölçümler 1–15 sn arayla tutarlılık ve hız kontrolüyle kabul edilir. 500 ms güncellemeler doğrulama penceresini sürekli yeniden başlatmaz. Gerçek ray geometrisi yoktur; çizgi geometrisinin sınırlamaları README'de açıklanır.

`BoardingGuard.kt`: hat eşleşmesi ve konum kalitesini korur; başlatma mevcut konum planlanan biniş–varış aralığındaysa duraklar arasında da yapılır. Hat değişince eski doğrulama kullanılamaz. Konum yokluğu uzak hat durumundan ayrı mesajla açıklanır.

`TripProgress.kt`: bildirimde yön, kalan duraklar ve GPS/süre tahmini durumu.

`TrackingService.kt`: aktif yolculukta location foreground service. Kabul edilen GPS durağıyla alarmı yeniden kurar; aynı durakta bekleme sırasında süre tahminini sabitler. GPS yoksa mevcut sistem alarmı ve süre tahmini sürer. Bildirim 15 sn aralıkla yenilenir, Android chronometer geri sayımı gösterir; broadcast action yolculuğu bitirir. Foreground service arayüz kapansa da devam eder; sistem servis sürecini öldürürse START_STICKY ile geri yüklemeyi talep eder (force-stop/yeniden başlatma hariç). Alarm zamanı geçtiyse GPS alarmı ertelemez.

`MainActivity.kt`: yalnızca ekran görünürken GPS ile yakın durak önerisi; kullanıcı binişi onaylar. Aktif yolculukta GPS izni sonradan verilirse takip servisi başlatılır. Ham koordinatlar kalıcı kaydedilmez.

## Harita katmanı

`RouteMapState.kt` ölçülen koordinat ile hat üzerindeki tahmini ilerlemeyi ayırır; rota değişiminde önceki hattın eşleşmesini kullanmaz. Tahmin hat segmentleri arasında enterpolasyonla çizilir; gerçek ray şekli değildir. Ham ölçüm çizgiye yapıştırılmaz. 10 sn sonrasında ölçüm eski işaretlenir; 5 dk sonrasında gizlenir. No-fix halinde gerçek konum üretilmez.

`RouteMap.kt` yerel harita varlıklarını WebViewAssetLoader HTTPS origin'inden sunar ve JSON verisini evaluateJavascript ile iletir. Uzak script, keyfi URL navigasyonu, dosya/içerik erişimi, WebView geolocation ve native JS bridge kapalıdır. İzin verilen tek uzak görsel kaynağı tile.openstreetmap.org'dur. OSM atıf bağlantısı sistem tarayıcısında açılır. Açık uygulama User-Agent'i ve varsayılan HTTP cache kullanılır; bulk/prefetch indirme yoktur. Harita taşınırken kamera veri güncellemelerinde sıfırlanmaz; yalnızca açık takip modunda konuma pan yapılır.

`assets/map`: Leaflet 1.9.4 dağıtımı (BSD-2-Clause lisansı yanında), yerel HTML/CSS/JS, çevrimdışı hat katmanı ve etkileşimler. Görünür OSM atfı ve harita yüklenemedi uyarısı vardır. İnternet izni yalnızca sokak haritası için eklendi; alarm/hat katmanı çevrimdışı kalır. `RouteMapStateTest` gerçek/eski/tahmini konum ayrımını ve iki yönde ilerlemeyi kapsar. Harita HTML'i ayrıca masaüstü Chrome'da mobil viewport ile online/offline, rota geçişi, konuma gitme, kamera kararlılığı ve JS hata kontrolünden geçirildi. Android WebView/kilit ekranı/fiziksel cihaz doğrulaması henüz yapılmadı.

### WebView başlatma ve kurtarma (0.4.1)

Kullanıcı cihazında düğmeler görünürken harita boş kaldı. Fiziksel cihaz/logcat erişimi olmadığından kök neden kesinleştirilmedi; sıfır yükseklikli WebView içeriğiyle benzer başlatma sorunu testte oluşturuldu. Native `onSizeChanged` ve sayfa yüklemesinden sonra `metroFrame` boyut aktarımı yapar. JavaScript viewport'u piksel olarak kurar, Leaflet'i yeniden ölçer ve başarılı ölçüm/durak çizimini native'e döndürür. `onPageFinished` artık tek başına başarılı çizim kabul edilmez.

10 sn çizim onayı alınmazsa veya ana sayfa yüklemesi hata verirse native Compose Canvas `OfflineRouteMap` açılır. Manuel hat görünümü seçimi GPU/WebView kaynaklı ve JS'nin fark edemediği çizim sorunları için de kullanılabilir. WebView kaldırılırken callback/watchdog temizlenir. `RouteMapProjection` kuzey yukarı, enlem düzeltmeli bir coğrafi izdüşümle hat ve konumu çizer; ayrı birim testleri bütün güzergâhların çerçeveye sığmasını kontrol eder. Gerçek telefon doğrulaması bekliyor.

## Opsiyonel Supabase tasarımı (uygulanmadı)

Alarm yolu her zaman yerel kalmalı. İlk ağ bağlantısında doğrulanmış ve sürümlenmiş hat kataloğu indirilip yerel SQLite/Room deposuna atomik yazılabilir. Her aralığa ayrı süre, örnek sayısı, güncelleme tarihi ve kaynak eklenmeli. Mevcut MVP her aralık için kullanıcının seçtiği aynı süreyi kullanıyor.

Bulutta `lines`, `stations`, `segments`, `catalog_versions` tabloları; yalnızca doğrulanmış katalogya istemciler için SELECT. Public schema'daki tüm tablolarda RLS; anon/authenticated rollerine yalnızca gerekli SELECT grant ve policy. Katalog yazma mobil istemciden yasak. Hesaplı favoriler eklenirse `user_id = (select auth.uid())` temelli SELECT/INSERT/UPDATE/DELETE politikaları, UPDATE için hem USING hem WITH CHECK kullanılmalı. service_role/secret anahtar mobil uygulamaya konmamalı. GPS ve yolculuk geçmişi senkronizasyonu varsayılan kapalı olmalı.

Supabase bilgisayarda [Docker ile barındırılabilir](https://supabase.com/docs/guides/self-hosting/docker). Bu tam backend kurulumu Docker, yedekleme ve güncelleme yönetimi ister; henüz kurulmadı. Telefonun localhost'u PC değildir: yerel geliştirmede LAN IP veya Android emulator için 10.0.2.2 kullanılmalı. PC kapalı veya erişilemezse yerel alarm etkilenmemeli.

## Üretime geçmeden

Fiziksel Android cihazlarda Doze, kilit ekranı, alarm izni iptali, batarya optimizasyonu ve ses yönlendirmesini test et. Başlangıç/izin durumları için instrumentation testleri ekle. Ölçülmüş durak süreleri, katalog koordinatlarını sahada kontrol et ve belirsizlik aralıkları hazırla. Kullanıcıya bildirilen GPS kaynağı son doğrulama kaynağıdır; sürekli GPS bağlantısı göstergesi değildir. iOS için ayrı platform alarm ve background-location sınırları değerlendirilmelidir.
