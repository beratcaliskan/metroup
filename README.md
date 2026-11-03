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

Bu bilgisayara proje içinde JDK/SDK kurulmuşsa `./scripts/build.ps1` yerel JDK'yı seçer ve Windows Java loopback hatası için yalnızca derleme sürecinde TCP geri dönüşünü etkinleştirir. Yerel SDK yolu Git'e alınmayan `local.properties` dosyasındadır. OneDrive klasör kilitleri nedeniyle ikili test sonuçları her çalıştırmada `app/build/test-binary` altında ayrı klasöre yazılır; XML/HTML raporlarının yolu sabittir.

Kaynak kod ve derleme kendi bilgisayarındadır. GitHub yalnızca kod deposudur; uygulamayı çalıştırmak için GitHub ya da sürekli açık bir PC gerekmez. APK kurulduktan sonra alarm telefonda çalışır. iOS sürümü bu projeye dahil değildir.

## Kullanım

Biniş ve varış durağını, 1–3 durak öncesi uyarıyı ve 60–300 sn arası ortalama durak süresini seç. Trene binerken yolculuğu başlat. Bildirim ve kesin alarm izinleri zorunlu; Başlatırken hassas GPS izni, açık konum servisi ve M1A durağı yakınlığının iki ölçümle doğrulanması zorunludur. Alarm sesi telefonun **alarm ses düzeyini** kullanır.

Terazidere → Yenikapı 8 aralıktır. 120 sn/aralık ve 2 durak önce alarm: **12 dakika sonra**, Emniyet-Fatih tahmini. Bu süre resmi sefer süresi değildir. Durak sırası kaynağı: [Metro İstanbul M1A](https://www.metro.istanbul/Hatlarimiz/HatDetay?hat=M1A).

Yer altında internet ve GPS ayrı konulardır: GPS internet olmadan çalışabilir ama tünelde uydu sinyali kaybolabilir. Uygulama konum yokken `elapsedRealtime` üzerinden geçen süreyi kullanır; telefon saatini değiştirmek tahmini bozmaz. Alarm Android AlarmManager'a kurulur, arayüz sayacına bağlı değildir.

M1A'nın 18 durağının enlem/boylamı İBB MetroMobile servisinden alınarak uygulamaya gömülüdür (3 Kasım 2026). Veri kaynağı ve eşleme kuralları: [docs/station-data.md](docs/station-data.md). Önceden konum öğretmek gerekmez; katalog internet olmadan kullanılabilir.

GPS izni ve telefonun konum servisi açıkken biniş ekranında yakın durak önerilir; **Biniş durağı olarak seç** ile kullanılır. Başlatma anında konum tekrar kontrol edilir: güncel ve doğrulanmış yakınlık yoksa alarm kurulmaz; biniş durağı da tespit edilen durakla eşleşmelidir. Hattın uzağındaysan açık uyarı gösterilir. GPS yokken/tünelde ilk başlatma engellenir; başlatılmış yolculuk GPS kesilse de devam eder. Bu kontrol hat geometrisi yerine M1A durak merkezlerine yakınlığı kullanır; istasyonlar arasında ilk başlatma desteklenmez. Yolculukta foreground service GPS'i 5 sn hedef aralıkla dinler (gerçek sıklığı Android belirler). ≤60 m doğruluk, son 30 sn içinde ölçüm, mesafe + doğruluk ≤180 m ve diğer durağa göre en az 100 m ayrım aranır. Aynı durakta 5–30 sn arayla iki ayrı ölçüm doğrulanınca seçili güzergâhta durak ve alarm yeniden hesaplanır. Süre tahmini gerçek konumdan çok sapmış olsa da GPS düzeltmesi kabul edilir. Aynı durakta beklenirse en erken 15 sn aralıkla süre tekrar sabitlenir; alarm zaten zamanı geldiyse ertelenmez. Alarm durağına veya ilerisine ulaşıldığında alarm hemen tetiklenmek üzere kurulur.

GPS bayatlarsa ekranda süre tahmini gösterilir. GPS yokken son doğrulanan durak ve zamandan ilerlenir. **Buradayım** elle düzeltme seçeneği de kalır. GPS durağa yakınlığı belirler; peronda veya trende olduğunu kanıtlamaz. Hat ve varış kullanıcı tarafından seçilir; başka hatlar henüz desteklenmez. Kullanıcı GPS koordinatları diske veya sunucuya yazılmaz; yalnızca doğrulanan durak ve zaman yerel kaydedilir.

Aktif yolculuk foreground service bildirimiyle, uygulama ekranını kapattığında da takip edilir. Bildirimde hedef, GPS yakını/süre tahmini, varışa kalan duraklar, alarm durağı, ilerleme çubuğu ve Android'in geri sayımı bulunur. Durak bilgisi yaklaşık 15 sn aralıkla ve GPS düzeltmesinde yenilenir; geri sayımı Android gösterir. **Yolculuğu bitir** işlemi uygulamayı açmadan alarmı iptal eder, servisi ve bildirimi kapatır. Kilit ekranında gösterim telefonun bildirim/gizlilik ayarlarına bağlıdır. Zorla durdurma veya telefonu yeniden başlatma sonrası takip sürmez; kullanıcı yolculuğu önce uygulamadan başlatmalıdır.

Alarmı bildirimden veya uygulamadan kapatabilirsin. Ses/titreşim 5 dk ile sınırlıdır. Ekran kilitliyken bildirim gelir; tam ekran açılma uygulanmamıştır. Gecikme, erken hareket ve hat değişimi otomatik olarak bilinemediğinden varış doğruluğu garanti değildir. Yeniden başlatma, zorla durdurma, kesin alarm izninin geri alınması veya üretici pil kısıtlamaları alarmı iptal edebilir. Yeniden başlatmada eski yolculuk temizlenir.

## Cihaz doğrulaması

- M1A durağından uzaktayken, GPS kapalıyken ve eski/tek ölçümle başlatmanın engellendiğini kontrol et.
- Doğrulanmış yakın M1A durağında 1 dk/aralık, en az 3 aralık yolculukla alarmı kur; ekran kilitli ve uçak modunda ses/titreşim/bildirim kontrol et.
- Bildirim ve kesin alarm izni reddedildiğinde başlatmanın engellendiğini kontrol et.
- Uygulamayı arka plana al, bildirimde geri sayımı ve tahmini durağı kontrol et. Bildirimden **Yolculuğu bitir** seçince alarmın ve bildirimin kaldırıldığını kontrol et.
- Yolculuğu bitirince alarmın iptal olduğunu; uygulamayı son uygulamalardan kaydırınca sistem alarmının kaldığını kontrol et (zorla durdurma farklıdır).
- Elle durak düzeltmesiyle alarm saatinin değiştiğini ve her iki yönün çalıştığını dene.
- Telefonu yeniden başlatınca eski yolculuğun geri gelmediğini kontrol et.
- GPS ile yakın durak tespiti, arka planda konum, Bluetooth kulaklık yönlendirmesi, DND ve üretici pil optimizasyonlarını gerçek telefonda test et.

## Supabase ve GitHub

Supabase şu anda bağlanmadı: URL/anahtar gerekmeden MVP çalışır. Opsiyonel senkronizasyon planı için [docs/architecture.md](docs/architecture.md). Kişisel konumlar sunucuya gönderilmez. `.env`, yerel SDK/JDK, APK ve imza anahtarları Git'e alınmaz.

GitHub deposu: [beratcaliskan/metroup](https://github.com/beratcaliskan/metroup). İlk sürüm dalı: `codex/android-mvp`.
