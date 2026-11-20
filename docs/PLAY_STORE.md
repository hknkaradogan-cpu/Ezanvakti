# Google Play Store hazırlığı — NAMAZ VAKTİ: EZAN VE KIBLE

## Mağaza kimliği
| Alan | Değer |
|---|---|
| Uygulama adı (en fazla 30 karakter) | **Namaz Vakti: Ezan ve Kıble** |
| Geliştirici / marka | NUR VAKTİ GLOBAL |
| Paket adı | `com.nurvakti.global` |
| Kategori | Yaşam tarzı (Lifestyle) |
| İçerik derecelendirmesi | Herkes (anket: şiddet/kumar/kullanıcı etkileşimi yok) |
| Reklam | Yok |
| Fiyat | Ücretsiz |
| Hedef API | 36 (Android 16) · min API 26 (Android 8.0) |

## Kısa açıklama (en fazla 80 karakter)
**TR:** Namaz vakitleri, ezan, kıble pusulası, Kur'an ve Yasin dinle — reklamsız.
**EN:** Prayer times, adhan, Qibla compass, Quran & Yasin audio — ad-free.

## Uzun açıklama (TR)
Namaz Vakti: Ezan ve Kıble, Türkiye'de ve dünyanın her yerinde namaz vakitlerini doğru hesaplayan, vakit girdiğinde gerçek ezan okuyan, reklamsız ve hesap gerektirmeyen bir namaz uygulamasıdır.

**Namaz vakitleri ve ezan vakti**
• İmsak, sabah, güneş, öğle, ikindi, akşam ve yatsı vakitleri; sonraki vakte canlı geri sayım
• Türkiye'de Diyanet İşleri Başkanlığı yöntemi (resmî vakitlerle ±1 dk test edildi); Muslim World League, ISNA, Umm Al-Qura, Mısır, Karaçi, Tahran, JAKIM, Singapur ve diğer yöntemler
• Hanefi ve Şafii ikindi, yüksek enlem ayarları, dakika düzeltmesi
• Günlük, haftalık, aylık ve yıllık ezan saatleri takvimi; internetsiz çalışır
• GPS ile otomatik konum, ülke–il–ilçe seçimi, koordinat, kayıtlı konumlar, seyahat modu

**Gerçek ezan**
• Beş vakit için ayrı ayrı: tam ezan, kısa ezan, bildirim, titreşim, sessiz veya kapalı
• Farklı ezan kayıtları (klasik, Mekke, Medine), önizleme, kendi ses dosyanızı seçme
• Vakitten 5/10/15/30 dk önce hatırlatma; ekran kapalıyken ve telefon yeniden başladıktan sonra da çalışır

**Kur'an-ı Kerim ve Yasin**
• 114 surenin tamamı: Arapça (Uthmani), Türkçe meal (Diyanet), İngilizce meal, Latin okunuş
• Birden fazla hafız (Mişari el-Afasi, Husari, Minşavi, Abdülbasit, Sudeys, Şüreym…); ekran kapalıyken ve arka planda dinleme, bildirimden ve Bluetooth'tan kontrol
• Büyük "Yasin Suresi Dinle" düğmesi; sure ve ayet arama, yer imi, favori, kaldığın yerden devam
• İndirme merkezi: sureleri ve ezanları indirip internetsiz dinleyin

**Kıble ve daha fazlası**
• Telefon sensörleriyle kıble pusulası, Kâbe'ye mesafe, kalibrasyon rehberi, harita üzerinde kıble
• Dualar (kaynaklı), Esmâü'l-Hüsnâ, zikirmatik, Ramazan imsakiyesi ve iftar sayacı, dini günler ve kandiller
• Namaz takibi ve kaza sayacı, zekât hesaplama, yakındaki camiler (OpenStreetMap), namaz rehberi
• Türkçe, İngilizce ve Arapça (sağdan sola) arayüz; açık, koyu ve otomatik tema

**Gizlilik:** Reklam, hesap ve analiz aracı yok. Konumunuz ve kişisel kayıtlarınız telefonunuzda kalır; dilediğiniz an dışa aktarabilir veya silebilirsiniz.

## Long description (EN) — özet
Accurate prayer times worldwide (Diyanet, MWL, ISNA, Umm al-Qura, Egypt, Karachi, Tehran, JAKIM, Singapore…), real adhan per prayer with offline sounds, Qibla compass with map fallback, full Quran with Turkish/English translation and multiple reciters, Surah Yasin, offline downloads, duas with sources, 99 Names, tasbih counter, Ramadan timetable, Hijri calendar, prayer tracker, zakat calculator, nearby mosques. No ads, no account, data stays on your phone.

## Anahtar kelime çalışması (ASO)
Başlık ve açıklamada **doğal biçimde** geçen terimler (anahtar kelime doldurma yapılmadı):
- Birincil (TR): namaz vakti, namaz vakitleri, ezan vakti, ezan saatleri, ezan, kıble, kıble pusulası, Kur'an, Yasin, imsakiye, Diyanet
- İkincil (TR): sabah ezanı, iftar vakti, sahur, zikirmatik, dualar, esmaül hüsna, kandil, hicri takvim, kaza namazı, zekat
- Uluslararası (EN, mağaza İngilizce listelemesi için): prayer times, adhan, azan, athan, salah times, qibla finder, quran audio, muslim prayer
Not: Play Console'da her dil için ayrı listeleme açın (tr-TR varsayılan, en-US, ar). Başlıkta yalnızca bir ana ifade kullanın; Google, başlıkta "en iyi, 1 numara, ücretsiz" gibi ifadeleri reddeder.

## Grafik varlıklar
- Uygulama simgesi 512×512: `web/icons/icon-512.png`
- Öne çıkan görsel 1024×500: hazırlanmalı (marka renkleri: zümrüt #0B6E4F, altın #C9A227, lacivert #0B1F3A)
- Ekran görüntüleri (en az 2, telefon): CI emülatör testi her derlemede `ci-results` dalına 30+ ekran görüntüsü yazar; bunlardan seçilebilir.

## Play Console beyanları
- **Veri güvenliği formu:** Toplanan veri yok. Konum: cihazda işlenir, paylaşılmaz (yalnızca kullanıcı çevrimiçi özellik kullanırsa üçüncü taraf hizmete koordinat gider — "isteğe bağlı" ve "geçici" olarak beyan edin). Şifreleme: HTTPS.
- **Kesin alarm (SCHEDULE_EXACT_ALARM):** Gerekçe: "Ezan, kullanıcının seçtiği namaz vaktinde tam dakikasında çalmalıdır." (USE_EXACT_ALARM kullanılmadı; izin kullanıcıdan istenir.)
- **Ön plan hizmeti türleri:** `mediaPlayback` (ezan ve Kur'an sesi), `dataSync` (indirmeler). Play Console'daki ön plan hizmeti beyanında kısa video/açıklama istenebilir.
- **Konum:** Yalnızca uygulama açıkken / ön planda (arka plan konumu istenmez).
- **Gizlilik politikası URL'si:** https://hknkaradogan-cpu.github.io/Ezanvakti/gizlilik.html
- **Hedef kitle:** 13 yaş ve üzeri genel kitle.

## Yükleme
Play Store **AAB** ister. CI'a şu adım eklenerek üretilebilir: `gradle bundleRelease` → `android/app/build/outputs/bundle/release/app-release.aab`. "Play App Signing" açın; GitHub Secrets'taki anahtar **yükleme anahtarı** olur.
