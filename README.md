# NAMAZ VAKTİ: EZAN VE KIBLE — NUR VAKTİ GLOBAL
*Her vakitte, her yerde, huzurla.*

| | |
|---|---|
| 📱 **Android APK** | [Son sürümü indir (GitHub Releases)](https://github.com/hknkaradogan-cpu/Ezanvakti/releases/latest) |
| 🍏💻 **iPhone / Bilgisayar** | https://hknkaradogan-cpu.github.io/Ezanvakti/ |
| 📄 Kurulum sayfası | https://hknkaradogan-cpu.github.io/Ezanvakti/indir.html |

## Proje yapısı
```
android/            Native Android uygulaması (Kotlin, Jetpack Compose, Material 3)
  app/src/main/java/com/nurvakti/global/
    prayer/         Namaz vakti motoru (16 yöntem, Hanefi/Şafii, yüksek enlem), Hicri takvim, AlAdhan
    alarm/          AlarmManager, BroadcastReceiver (açılış/saat dilimi), AdhanService, WorkManager
    audio/          Media3 ExoPlayer + MediaSessionService (Kur'an oynatıcı)
    download/       WorkManager tabanlı devam ettirilebilir indirme merkezi
    location/       Fused Location, Geocoder, çevrimdışı ülke/il/ilçe arama, Nominatim
    qibla/          SensorManager pusula (gerçek kuzey, manyetik sapma)
    data/           Room, DataStore, içerik deposu
    ui/             Compose ekranları
web/                iPhone ve PC için PWA (aynı veri ve aynı hesaplama motoru: web/js/praytimes.js)
  data/             ORTAK veri: Kur'an, dualar, esma, hadis, konumlar, ses katalogları, i18n (Android'e de gömülür)
tools/              Veri üretimi, CI testleri, ezan ses işleme
shared/tests/       Diyanet resmî vakit test verisi
docs/               Play Store hazırlığı, lisanslar, imzalama, test raporu
.github/workflows/  APK derleme + testler + emülatör testi + Releases + GitHub Pages
```

## Otomatik derleme
`main` dalına her gönderimde GitHub Actions:
1. Namaz vakti testleri (Diyanet resmî tablosu ±1 dk, AlAdhan canlı karşılaştırma: İstanbul, Ankara, Londra, Berlin, New York; Hanefi/Şafii)
2. Lisanslı ezan kayıtlarını Wikimedia Commons'tan indirip SHA-1 doğrular
3. 13 hafızın 114 suresini sunucuda tek tek doğrular
4. Kotlin birim testleri + release/debug APK derlemesi
5. GitHub Release (`vX.Y.Z`) ve web sitesini (`gh-pages`) yayınlar
6. Android 14 emülatöründe gerçek test: tüm ekranlar, dil/tema, ekran kapalıyken Kur'an, gerçek ezan alarmı, yeniden başlatma, saat dilimi, çökme kontrolü → sonuçlar ve ekran görüntüleri `ci-results` dalına yazılır

Telefondan yeniden derlemek: **Actions → APK + Web derleme → Run workflow**.

## Bilgisayarda geliştirme
Android Studio ile `android/` klasörünü açın. Release imzalama için `docs/IMZALAMA.md`.

Lisanslar ve kaynaklar: `docs/LISANSLAR.md` · Play Store: `docs/PLAY_STORE.md` · Gizlilik: `web/gizlilik.html`
