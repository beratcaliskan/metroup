# Mimari ve sonraki aşama

## Çalışan yerel uygulama

`Journey.kt`: Android'den bağımsız hesaplama; yön, durak sayısı, tekdüze aralık süresi, alarm noktası. Birim testleri iki yönü, gecikme düzeltmesini ve sınırları kapsar.

`Store.kt`: SharedPreferences içinde tek aktif yolculuk ve kullanıcının doğruladığı GPS noktaları. Boot count ile yeniden başlatma tespiti. Bulut hesabı yok.

`Alarms.kt`: monotonic kesin alarm, BroadcastReceiver, mediaPlayback foreground service ile döngülü alarm sesi ve titreşim. AlarmManager uygulama süreci ölse de alarmı tutar; force-stop veya yeniden başlatma hariç.

`TrackingService.kt`: kullanıcı uygulama ön plandayken başlatılan location foreground service. GPS olmadan temel zaman alarmı kalır. Koordinat eşleme tek ölçümlüdür; üretim için birden çok tutarlı ölçüm, hat geometrisi ve ölçülmüş aralık dağılımlarıyla iyileştirilmelidir.

`MainActivity.kt`: Compose Türkçe UI, izinler, durak seçimi, geri sayım ve elle doğrulama.

## Opsiyonel Supabase tasarımı (uygulanmadı)

Alarm yolu her zaman yerel kalmalı. İlk ağ bağlantısında doğrulanmış ve sürümlenmiş hat kataloğu indirilip yerel SQLite/Room deposuna atomik yazılabilir. Her aralığa ayrı süre, örnek sayısı, güncelleme tarihi ve kaynak eklenmeli. Mevcut MVP her aralık için kullanıcının seçtiği aynı süreyi kullanıyor.

Bulutta `lines`, `stations`, `segments`, `catalog_versions` tabloları; yalnızca doğrulanmış katalogya istemciler için SELECT. Public schema'daki tüm tablolarda RLS; anon/authenticated rollerine yalnızca gerekli SELECT grant ve policy. Katalog yazma mobil istemciden yasak. Hesaplı favoriler eklenirse `user_id = (select auth.uid())` temelli SELECT/INSERT/UPDATE/DELETE politikaları, UPDATE için hem USING hem WITH CHECK kullanılmalı. service_role/secret anahtar mobil uygulamaya konmamalı. GPS ve yolculuk geçmişi senkronizasyonu varsayılan kapalı olmalı.

Supabase bilgisayarda [Docker ile barındırılabilir](https://supabase.com/docs/guides/self-hosting/docker). Bu tam backend kurulumu Docker, yedekleme ve güncelleme yönetimi ister; henüz kurulmadı. Telefonun localhost'u PC değildir: yerel geliştirmede LAN IP veya Android emulator için 10.0.2.2 kullanılmalı. PC kapalı veya erişilemezse yerel alarm etkilenmemeli.

## Üretime geçmeden

Fiziksel Android cihazlarda Doze, kilit ekranı, alarm izni iptali, batarya optimizasyonu ve ses yönlendirmesini test et. Başlangıç/izin durumları için instrumentation testleri ekle. Ölçülmüş durak süreleri, resmi/doğrulanmış koordinatlar ve belirsizlik aralıkları hazırla. Kullanıcıya bildirilen GPS kaynağı son doğrulama kaynağıdır; sürekli GPS bağlantısı göstergesi değildir. iOS için ayrı platform alarm ve background-location sınırları değerlendirilmelidir.
