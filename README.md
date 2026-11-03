# MetroUp

İstanbul M1A ve M2 yolculukları için çevrimdışı durak alarmı. Android 8+; Kotlin, Jetpack Compose Material 3. Alarm, hat katmanı ve süre tahmini hesap veya sunucu gerektirmeden çevrimdışı çalışır; sokak haritası internet kullanır. İlk çalışan MVP; fiziksel cihazda alarm güvenilirliği henüz doğrulanmadı.

## Bilgisayarında derleme

1. Android Studio kur. SDK Manager üzerinden Android SDK Platform 35 ve Build Tools 35.0.0 yükle.
2. Bu klasörü Android Studio'da aç. Gradle JDK olarak **17** seç. İlk Gradle senkronizasyonu internet gerektirir.
3. Telefonunda geliştirici seçenekleri ve USB hata ayıklamayı aç; USB ile bağla, **Run app** seç.
4. APK: **Build → Generate App Bundles or APKs → Generate APKs**. Çıktı: `app/build/outputs/apk/debug/app-debug.apk`.

PowerShell (JAVA_HOME JDK 17, ANDROID_HOME SDK klasörü olmalı):

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Bu bilgisayara proje içinde JDK/SDK kurulmuşsa `./scripts/build.ps1` yerel JDK'yı seçer ve Windows Java loopback hatası için yalnızca derleme sürecinde TCP geri dönüşünü etkinleştirir. Yerel SDK yolu Git'e alınmayan `local.properties` dosyasındadır. Derleme betiği OneDrive kilitlerini önlemek için geçici derleme çıktılarını `%LOCALAPPDATA%/MetroUp/build/<proje-kimliği>` altında tutar, başarılı APK'yı yukarıdaki proje yoluna kopyalar. XML/HTML raporlarının yerini terminalde gösterir. İstersen `METROUP_BUILD_ROOT` ile çıktı kökünü belirleyebilirsin.

Kaynak kod ve derleme kendi bilgisayarındadır. GitHub yalnızca kod deposudur; uygulamayı çalıştırmak için GitHub ya da sürekli açık bir PC gerekmez. APK kurulduktan sonra alarm telefonda çalışır. iOS sürümü bu projeye dahil değildir.

## Kullanım

Hat menüsünde **M1A · Yenikapı–Atatürk Havalimanı**, **M2 · Yenikapı–Hacıosman** ve **M2 · Sanayi Mahallesi–Seyrantepe** vardır. M2 ana güzergâh 15 duraktır; Seyrantepe ayrı bir kol olduğundan Sanayi Mahallesi–İTÜ-Ayazağa arasında fazladan durak sayılmaz. Aktarmalı Seyrantepe yolculuklarında ana hat ve kol ayrı ayrı başlatılır; aktarma bekleme süresi otomatik hesaplanmaz. [Metro İstanbul aktarma açıklaması](https://www.metro.istanbul/Home/SikcaSorulanSorular?q=12).

Sanayi Mahallesi–Seyrantepe yalnızca bir durak aralığıdır. **1 durak önce** seçimi binişte hemen alarm verir; aynı kural diğer hatlarda alarm noktası binişe denk geldiğinde de geçerlidir. Ekranda bu durum belirtilir.

Hat değişince biniş/varış ve GPS doğrulaması sıfırlanır. Yenikapı gibi ortak istasyonlarda GPS hangi trende olduğunu ayıramaz; hat seçimi kullanıcıya aittir. Kaydedilmiş yolculuklarda hat kimliği de tutulur; eski sürümdeki hat alanı olmayan yolculuklar M1A olarak okunur.


Önce hattı/güzergâhı, ardından biniş ve varış durağını, 1–3 durak öncesi uyarıyı ve 60–300 sn arası ortalama durak süresini seç. Trene binerken yolculuğu başlat. Bildirim ve kesin alarm izinleri zorunlu; Başlatırken hassas GPS izni, açık konum servisi ve seçilen hat yakınlığının doğrulanması zorunludur. Alarm sesi telefonun **alarm ses düzeyini** kullanır.

Terazidere → Yenikapı 8 aralıktır. 120 sn/aralık ve 2 durak önce alarm: **12 dakika sonra**, Emniyet-Fatih tahmini. Bu süre resmi sefer süresi değildir. Durak sırası kaynağı: [Metro İstanbul M1A](https://www.metro.istanbul/Hatlarimiz/HatDetay?hat=M1A).

Yer altında internet ve GPS ayrı konulardır: GPS internet olmadan çalışabilir ama tünelde uydu sinyali kaybolabilir. Uygulama konum yokken `elapsedRealtime` üzerinden geçen süreyi kullanır; telefon saatini değiştirmek tahmini bozmaz. Alarm Android AlarmManager'a kurulur, arayüz sayacına bağlı değildir.

M1A'nın 18 ve M2'nin 16 durağının enlem/boylamı İBB MetroMobile servisinden alınarak uygulamaya gömülüdür (3 Kasım 2026). Veri kaynağı ve eşleme kuralları: [docs/station-data.md](docs/station-data.md). Önceden konum öğretmek gerekmez; katalog internet olmadan kullanılabilir.

Konum izni ve telefonun konum servisi açıkken biniş ekranında yakın durak veya **iki durak arası tahmini konum** gösterilir. Fused Location (uydu/Wi-Fi/hücresel kaynakların cihaz tarafından birleştirilmesi), cihazdaki son konum, anlık konum isteği ve yerel GPS/ağ sağlayıcıları birlikte kullanılır. Google Play services bulunmazsa yerel sağlayıcılar çalışır. Güncellemeler 1 sn hedef aralıkla, gereksiz yüksek doğruluk beklemesi kapalı istenir; Android ve ortamın daha hızlı ölçüm üreteceği garanti değildir. [Fused Location dokümanı](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient).

Son 10 sn içinde alınmış, doğruluk tahmini ≤25 m olan güvenilir hat eşleşmesi ilk ölçümde kabul edilir. 25–60 m arası doğruluk veya daha eski (en fazla 30 sn) ölçümde, 1–15 sn arayla iki tutarlı ve fiziksel olarak mümkün ölçüm aranır; aynı durakta beklemek gerekmez. Aynı önbellek ölçümü iki kez sayılmaz. Konum sıçramaları geçmiş ölçüme göre hız/tutarlılık kontrolünden geçer. Ölçüm yaşı ve yaklaşık doğruluğu ekranda gösterilir. Gerekirse **Konumu yeniden iste** ve **Telefonun konum ayarları** kullanılabilir.

Durak yakınlığında önceki 180 m (mesafe + doğruluk) eşiği korunur. Duraklar arasında, ardışık durak merkezlerini birleştiren çizgilere uzaklık + doğruluk ≤200 m ise yaklaşık hat koridoru kullanılır. Bu **gerçek ray geometrisi değildir**; virajlı kesimlerde hat kaçırılabilir, yakın paralel yollar ayırt edilemeyebilir. Başka uzak kesimlerle belirsiz eşleşmeler reddedilir. Uygulama hangi trende olduğunu kanıtlamaz; seçili hat ve yön kullanıcıya aittir.

Başlatma anında konum tekrar kontrol edilir. Seçilen güzergâhtan uzaktayken veya yeterli konum yoksa alarm başlamaz. Biniş durağını geçmiş olsan da konum planlanan biniş–varış aralığındaysa hareket halinde başlatılabilir. İki durak arasındaki kesirli ilerleme alarm süresinden düşülür. Tünelde hiçbir sağlayıcı ölçüm üretemiyor ve kullanılabilir yakın tarihli ölçüm de yoksa ilk başlatma yine doğrulanamaz. Veri yokken konum uydurulmaz. Telefonun konum doğruluğu/Wi-Fi taraması ayarları ağ konumunu etkileyebilir; uygulama bunları sessizce açmaz.

Başlatılmış yolculukta konum gelirse alarm güncellenir; konum 10 sn'den eski olduğunda ekranda süre tahmini gösterilir. Sinyal kesilirse son doğrulanan durak/aralık ve zamandan devam edilir. **Buradayım** elle düzeltmesi de kalır. Ham kullanıcı koordinatları uygulama tarafından sunucuya gönderilmez ve diske yazılmaz; yalnızca hat üzerindeki kesirli ilerleme ve zaman tutulur. Google/Android konum hizmetlerinin kendi ayarları ve veri işleme kuralları ayrıdır.

Aktif yolculuk foreground service bildirimiyle, uygulama ekranını kapattığında da takip edilir. Bildirimde hedef, konum/süre tahmini, varışa kalan duraklar, alarm durağı, ilerleme çubuğu ve Android'in geri sayımı bulunur. Durak bilgisi yaklaşık 15 sn aralıkla ve GPS düzeltmesinde yenilenir; geri sayımı Android gösterir. **Yolculuğu bitir** işlemi uygulamayı açmadan alarmı iptal eder, servisi ve bildirimi kapatır. Kilit ekranında gösterim telefonun bildirim/gizlilik ayarlarına bağlıdır. Zorla durdurma veya telefonu yeniden başlatma sonrası takip sürmez; kullanıcı yolculuğu önce uygulamadan başlatmalıdır.

Alarmı bildirimden veya uygulamadan kapatabilirsin. Ses/titreşim 5 dk ile sınırlıdır. Ekran kilitliyken bildirim gelir; tam ekran açılma uygulanmamıştır. Gecikme, erken hareket ve hat değişimi otomatik olarak bilinemediğinden varış doğruluğu garanti değildir. Yeniden başlatma, zorla durdurma, kesin alarm izninin geri alınması veya üretici pil kısıtlamaları alarmı iptal edebilir. Yeniden başlatmada eski yolculuk temizlenir.

## Harita

Ana ekrandaki haritada seçili hat, duraklar, biniş, **V** ile varış ve **!** ile alarm durağı görünür. Harita parmakla sürüklenebilir/yakınlaştırılabilir; **Büyüt** tam ekran açar. Duraklara dokunmak isimlerini gösterir. **Hattı göster** bütün hattı çerçeveler. **Konumuma git** son güncel ölçümü veya tahmini konumu merkezler ve takibi açar; haritayı sürüklemek takibi durdurur.

- Mavi nokta: son 10 sn içindeki gerçek konum ölçümü; çevresindeki halka bildirilen doğruluk tahminidir.
- Gri nokta: eski konum ölçümü; 5 dk sonra kaldırılır. Güncel konum gibi gösterilmez.
- Turuncu elmas: hat üzerindeki eşleşme/süre tahmini. Uydu ölçümüyle aynı şey değildir. Yolculukta internet/konum kesilse de ilerler.

Sokak zemini [OpenStreetMap](https://www.openstreetmap.org/copyright) üzerinden alınır; yerel Leaflet 1.9.4 ve hat katmanları APK içindedir. Zemin yüklenemediğinde uyarı gösterilir, hat/duraklar ve işaretler boş coğrafi zeminde çalışır. Sokak haritalarını çevrimdışı toplu indirme yoktur. Hat çizgisi durak merkezlerini birleştirir; gerçek ray geometrisi değildir.

Harita görünürken OpenStreetMap sunucusuna görüntülenen bölgenin tile istekleri ve standart bağlantı bilgileri gönderilir; haritayı konuma merkezlemek yaklaşık bölgeni açığa çıkarabilir. Kullanıcı koordinatları ayrı bir analitik veya backend servisine gönderilmez. [Tile kullanım politikası](https://operations.osmfoundation.org/policies/tiles/). WebView yalnızca paketlenmiş harita dosyalarını ve HTTPS OSM tile görsellerini yükler; yerel dosya/konum erişimi ve native JavaScript köprüsü yoktur.

## Cihaz doğrulaması

- Haritada yakınlaştırma, tam ekran, konumu takip, konum kesilince gri nokta/turuncu tahmin ayrımı ve uçak modunda hat katmanını kontrol et.
- Seçili hat duraklarından uzaktayken, konum kapalıyken ve eski/belirsiz ölçümle başlatmanın engellendiğini kontrol et.
- Doğrulanmış yakın hat durağında 1 dk/aralık, en az 3 aralık yolculukla alarmı kur; ekran kilitli ve uçak modunda ses/titreşim/bildirim kontrol et.
- Bildirim ve kesin alarm izni reddedildiğinde başlatmanın engellendiğini kontrol et.
- Uygulamayı arka plana al, bildirimde geri sayımı ve tahmini durağı kontrol et. Bildirimden **Yolculuğu bitir** seçince alarmın ve bildirimin kaldırıldığını kontrol et.
- Yolculuğu bitirince alarmın iptal olduğunu; uygulamayı son uygulamalardan kaydırınca sistem alarmının kaldığını kontrol et (zorla durdurma farklıdır).
- Elle durak düzeltmesiyle alarm saatinin değiştiğini ve her iki yönün çalıştığını dene.
- Telefonu yeniden başlatınca eski yolculuğun geri gelmediğini kontrol et.
- İlk açılış, Google Play services olmayan cihaz, iyi önbellek, düşük doğruluk, 500 ms güncellemeler, hareket halindeyken duraklar arası başlatma ve GPS/network kesilmesini gerçek cihazda kontrol et.
- GPS ile yakın durak tespiti, arka planda konum, Bluetooth kulaklık yönlendirmesi, DND ve üretici pil optimizasyonlarını gerçek telefonda test et.

Harita tarayıcı smoke testi: `scripts/map-smoke.cjs`. Node.js ve Playwright Core 1.56.1 gerektirir. `PLAYWRIGHT_CORE_MODULE` ortam değişkeniyle kurulu modül yolunu, gerekirse `CHROME_PATH` ile Chrome yolunu verip `node scripts/map-smoke.cjs` çalıştırılabilir. Yerel sunucu ve izole headless Chrome kullanır; örnek M2/M1A/Seyrantepe verileriyle konuma gitme/takip, kamera kararlılığı, eski ölçüm ve tile kesintisi kontrol edilir. Ekran görüntüleri Git'e alınmayan `.tools/map-*.png` dosyalarına yazılır. Android WebView ve fiziksel cihaz testi yerine geçmez.

## Supabase ve GitHub

Supabase şu anda bağlanmadı: URL/anahtar gerekmeden MVP çalışır. Opsiyonel senkronizasyon planı için [docs/architecture.md](docs/architecture.md). Kişisel konumlar sunucuya gönderilmez. `.env`, yerel SDK/JDK, APK ve imza anahtarları Git'e alınmaz.

GitHub deposu: [beratcaliskan/metroup](https://github.com/beratcaliskan/metroup). İlk sürüm dalı: `codex/android-mvp`.
