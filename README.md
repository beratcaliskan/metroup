# MetroUp

İstanbul M1A yolculukları için çevrimdışı durak alarmı. Android 8+; Kotlin, Jetpack Compose Material 3. Hesap, sunucu ve internet olmadan çalışır. İlk çalışan MVP; fiziksel cihazda alarm güvenilirliği henüz doğrulanmadı.

## Bilgisayarında derleme

1. Android Studio kur. SDK Manager üzerinden Android SDK Platform 35 ve Build Tools 35.0.0 yükle.
2. Bu klasörü Android Studio'da aç. Gradle JDK olarak **17** seç. İlk Gradle senkronizasyonu internet gerektirir.
3. Telefonunda geliştirici seçenekleri ve USB hata ayıklamayı aç; USB ile bağla, **Run app** seç.
4. APK: **Build → Generate App Bundles or APKs → Generate APKs**. Çıktı: `app/build/outputs/apk/debug/app-debug.apk`.

PowerShell (JAVA_HOME JDK 17, ANDROID_HOME SDK klasörü olmalı):

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Bu bilgisayara proje içinde JDK/SDK kurulmuşsa `./scripts/build.ps1` yerel JDK'yı seçer ve Windows Java loopback hatası için yalnızca derleme sürecinde TCP geri dönüşünü etkinleştirir. Yerel SDK yolu Git'e alınmayan `local.properties` dosyasındadır.

Kaynak kod ve derleme kendi bilgisayarındadır. GitHub yalnızca kod deposudur; uygulamayı çalıştırmak için GitHub ya da sürekli açık bir PC gerekmez. APK kurulduktan sonra alarm telefonda çalışır. iOS sürümü bu projeye dahil değildir.

## Kullanım

Biniş ve varış durağını, 1–3 durak öncesi uyarıyı ve 60–300 sn arası ortalama durak süresini seç. Trene binerken yolculuğu başlat. Bildirim ve kesin alarm izinleri zorunlu; GPS isteğe bağlıdır. Alarm sesi telefonun **alarm ses düzeyini** kullanır.

Terazidere → Yenikapı 8 aralıktır. 120 sn/aralık ve 2 durak önce alarm: **12 dakika sonra**, Emniyet-Fatih tahmini. Bu süre resmi sefer süresi değildir. Durak sırası kaynağı: [Metro İstanbul M1A](https://www.metro.istanbul/Hatlarimiz/HatDetay?hat=M1A).

Yer altında internet ve GPS ayrı konulardır: GPS internet olmadan çalışabilir ama tünelde uydu sinyali kaybolabilir. Uygulama konum yokken `elapsedRealtime` üzerinden geçen süreyi kullanır; telefon saatini değiştirmek tahmini bozmaz. Alarm Android AlarmManager'a kurulur, arayüz sayacına bağlı değildir.

Durakta **Buradayım** onayı, zaman tahminini yeniden başlatır. Son 15 sn içinde alınmış, doğruluğu ≤40 m GPS noktası varsa durağın konumu yalnızca telefona kaydedilir. Hazır koordinatlar yoktur; ilk yolculukta otomatik GPS düzeltmesi bekleme. Sonraki yolculuklarda konum servisi öğrenilmiş noktalara 100 m yakın, ≤60 m doğrulukta, son 30 sn içindeki ölçümleri ve güzergâh yönünü kullanır; en fazla iki durak tahmin farkına izin verir. Aynı durağı tekrar tekrar sabitlemez. GPS izni reddedilirse süre alarmı devam eder.

Alarmı bildirimden veya uygulamadan kapatabilirsin. Ses/titreşim 5 dk ile sınırlıdır. Ekran kilitliyken bildirim gelir; tam ekran açılma uygulanmamıştır. Gecikme, erken hareket ve hat değişimi otomatik olarak bilinemediğinden varış doğruluğu garanti değildir. Yeniden başlatma, zorla durdurma, kesin alarm izninin geri alınması veya üretici pil kısıtlamaları alarmı iptal edebilir. Yeniden başlatmada eski yolculuk temizlenir. Uygulama verilerini silmek öğrenilen koordinatları siler.

## Cihaz doğrulaması

- 1 dk/aralık, en az 3 aralık yolculukla alarmı kur; ekran kilitli ve uçak modunda ses/titreşim/bildirim kontrol et.
- Bildirim ve kesin alarm izni reddedildiğinde başlatmanın engellendiğini kontrol et.
- Yolculuğu bitirince alarmın iptal olduğunu; uygulamayı son uygulamalardan kaydırınca sistem alarmının kaldığını kontrol et (zorla durdurma farklıdır).
- Elle durak düzeltmesiyle alarm saatinin değiştiğini ve her iki yönün çalıştığını dene.
- Telefonu yeniden başlatınca eski yolculuğun geri gelmediğini kontrol et.
- GPS öğrenme, arka planda konum, Bluetooth kulaklık yönlendirmesi, DND ve üretici pil optimizasyonlarını gerçek telefonda test et.

## Supabase ve GitHub

Supabase şu anda bağlanmadı: URL/anahtar gerekmeden MVP çalışır. Opsiyonel senkronizasyon planı için [docs/architecture.md](docs/architecture.md). Kişisel konumlar sunucuya gönderilmez. `.env`, yerel SDK/JDK, APK ve imza anahtarları Git'e alınmaz.

GitHub deposu: [beratcaliskan/metroup](https://github.com/beratcaliskan/metroup). İlk sürüm dalı: `codex/android-mvp`.
