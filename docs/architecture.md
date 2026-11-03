# Mimari ve sonraki aşama

## Çalışan yerel uygulama

`Journey.kt`: Android'den bağımsız hesaplama; yön, durak sayısı, tekdüze aralık süresi, alarm noktası. Birim testleri iki yönü, gecikme düzeltmesini ve sınırları kapsar.

`Store.kt`: SharedPreferences içinde tek aktif yolculuk ve son doğrulanan durak/zaman. Boot count ile yeniden başlatma tespiti. Bulut hesabı yok.

`Alarms.kt`: monotonic kesin alarm, BroadcastReceiver, mediaPlayback foreground service ile döngülü alarm sesi ve titreşim. AlarmManager uygulama süreci ölse de alarmı tutar; force-stop veya yeniden başlatma hariç.

`StationCatalog.kt`: İBB kaynağından sıralanmış 18 M1A koordinatı. Kaynak kopyası `docs/m1a-stations.json` ve ayrıntılar `docs/station-data.md` içinde.

`StationMatcher.kt`: Android'den bağımsız yakınlık, doğruluk, tazelik, belirsiz eşleşme ve iki ölçüm kontrolü. Sonlu olmayan koordinatlar, eski/tekrarlı GPS ölçümleri reddedilir. Seçili güzergâh dışındaki duraklar alarmı değiştirmez.

`BoardingGuard.kt`: güncel ve kaliteli GPS, iki ölçümle durak doğrulaması ve biniş eşleşmesi olmadan başlatmayı reddeder. Konum bulunamaması hattın uzağında olma durumundan ayrı mesajla açıklanır.

`TripProgress.kt`: bildirimde yön, kalan duraklar ve GPS/süre tahmini durumu.

`TrackingService.kt`: aktif yolculukta location foreground service. Kabul edilen GPS durağıyla alarmı yeniden kurar; aynı durakta bekleme sırasında süre tahminini sabitler. GPS yoksa mevcut sistem alarmı ve süre tahmini sürer. Bildirim 15 sn aralıkla yenilenir, Android chronometer geri sayımı gösterir; broadcast action yolculuğu bitirir. Foreground service arayüz kapansa da devam eder; sistem servis sürecini öldürürse START_STICKY ile geri yüklemeyi talep eder (force-stop/yeniden başlatma hariç). Alarm zamanı geçtiyse GPS alarmı ertelemez.

`MainActivity.kt`: yalnızca ekran görünürken GPS ile yakın durak önerisi; kullanıcı binişi onaylar. Aktif yolculukta GPS izni sonradan verilirse takip servisi başlatılır. Ham koordinatlar kalıcı kaydedilmez.

## Opsiyonel Supabase tasarımı (uygulanmadı)

Alarm yolu her zaman yerel kalmalı. İlk ağ bağlantısında doğrulanmış ve sürümlenmiş hat kataloğu indirilip yerel SQLite/Room deposuna atomik yazılabilir. Her aralığa ayrı süre, örnek sayısı, güncelleme tarihi ve kaynak eklenmeli. Mevcut MVP her aralık için kullanıcının seçtiği aynı süreyi kullanıyor.

Bulutta `lines`, `stations`, `segments`, `catalog_versions` tabloları; yalnızca doğrulanmış katalogya istemciler için SELECT. Public schema'daki tüm tablolarda RLS; anon/authenticated rollerine yalnızca gerekli SELECT grant ve policy. Katalog yazma mobil istemciden yasak. Hesaplı favoriler eklenirse `user_id = (select auth.uid())` temelli SELECT/INSERT/UPDATE/DELETE politikaları, UPDATE için hem USING hem WITH CHECK kullanılmalı. service_role/secret anahtar mobil uygulamaya konmamalı. GPS ve yolculuk geçmişi senkronizasyonu varsayılan kapalı olmalı.

Supabase bilgisayarda [Docker ile barındırılabilir](https://supabase.com/docs/guides/self-hosting/docker). Bu tam backend kurulumu Docker, yedekleme ve güncelleme yönetimi ister; henüz kurulmadı. Telefonun localhost'u PC değildir: yerel geliştirmede LAN IP veya Android emulator için 10.0.2.2 kullanılmalı. PC kapalı veya erişilemezse yerel alarm etkilenmemeli.

## Üretime geçmeden

Fiziksel Android cihazlarda Doze, kilit ekranı, alarm izni iptali, batarya optimizasyonu ve ses yönlendirmesini test et. Başlangıç/izin durumları için instrumentation testleri ekle. Ölçülmüş durak süreleri, katalog koordinatlarını sahada kontrol et ve belirsizlik aralıkları hazırla. Kullanıcıya bildirilen GPS kaynağı son doğrulama kaynağıdır; sürekli GPS bağlantısı göstergesi değildir. iOS için ayrı platform alarm ve background-location sınırları değerlendirilmelidir.
