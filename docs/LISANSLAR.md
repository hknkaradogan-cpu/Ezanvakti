# Kullanılan kaynaklar ve lisanslar

## Ezan kayıtları (uygulamada / sitede dağıtılanlar)
| Kayıt | Kaynak | Sahibi | Lisans | Değişiklik |
|---|---|---|---|---|
| Klasik ezan (`klasik.mp3`, uygulamaya gömülü) | [Wikimedia Commons: Beautiful adhan.ogg](https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg) | Adam-synagda | CC0 1.0 (kamu malı) | MP3 96 kb/s, ses normalizasyonu ([marsathan-adhan v2](https://github.com/marwan313/marsathan-adhan)) |
| Kısa ezan (`kisa.mp3`, gömülü) | aynı kayıt | Adam-synagda | CC0 1.0 | İlk 42 sn, 5 sn azalan bitiş |
| Mekke tarzı (`mekke.mp3`, indirilebilir) | [Commons: Maghrib Adhan at the Masjid al Haram, 25 Feb 2012](https://commons.wikimedia.org/wiki/File:Maghrib_Adhan_at_the_Masjid_al_Haram,_Mecca_-_25_Feb,_2012.webm) | 3omar Faruq | CC BY 3.0 | Videodan ses çıkarıldı, 1:52–4:56 kesildi, normalize edildi (CI'da, SHA-1 doğrulamalı) |
| Medine tarzı (`medine.mp3`, indirilebilir) | [Commons: call to prayer from the Prophet's Mosque](https://commons.wikimedia.org/wiki/File:33937_ejaz215_call-to-prayer-from-the-prophet-s-mo.ogg) | ejaz215 (Freesound #33937) | CC BY 3.0 | MP3'e çevrildi, normalize edildi |

Lisansı belirsiz veya içeriği doğrulanamayan kayıtlar (ör. ünlü müezzinlerin ticari kayıtları, Şii ezan formu içerebilecek kayıtlar) **bilinçli olarak eklenmedi**. Lisanslı ve "es-salâtü hayrun mine'n-nevm" ifadesini içerdiği doğrulanmış ayrı bir *sabah ezanı* kaydı bulunamadı; kullanıcı Ezan ayarlarından telefonundaki kendi ses dosyasını seçebilir.

## Kur'an sesleri
AlQuran Cloud CDN (`cdn.islamic.network/quran/audio-surah/...`). Islamic Network şartlarına göre API/CDN ücretsiz ve anahtarsızdır; **ses kayıtlarının telif hakları sahiplerine aittir**. Kayıtlar okuyucular tarafından ücretsiz dağıtım için yayımlanmıştır; ticari kullanımda (ör. ücretli uygulama, reklamlı yayın) ilgili hak sahiplerinden izin gerekebilir. Uygulama ücretsiz ve reklamsızdır, sesleri değiştirmeden doğrudan CDN'den çalar/indirir. Her CI derlemesinde 13 hafızın 114 suresinin tamamı HTTP ile kontrol edilir; sunucuda bulunmayan sure listelenmez.

## Kur'an metni ve mealler
- Arapça Uthmani metin: Kral Fahd Kur'an Basım Kompleksi (QuranEnc) — `quran-json` 3.1.2 npm paketi (CC BY 4.0)
- Türkçe meal: Diyanet İşleri Başkanlığı (tanzil.net, `tr.diyanet`); İngilizce: Saheeh International; Latin transliterasyon: tanzil.net. Tanzil çevirileri ticari olmayan kullanım ve kaynak gösterilerek dağıtım için sunulur.

## Diğer veriler
- Ülke / il / ilçe listesi: [countries-states-cities-database](https://github.com/dr5hn/countries-states-cities-database) (ODbL 1.0)
- Harita karoları, cami verisi, adres arama: © OpenStreetMap katkıcıları (ODbL) — tile.openstreetmap.org, Overpass API, Nominatim (kullanım politikalarına uygun düşük hacim, User-Agent tanımlı)
- Namaz vakti karşılaştırma (isteğe bağlı): [AlAdhan API](https://aladhan.com/prayer-times-api) — ücretsiz, anahtarsız
- Zekât fiyatları: api.gold-api.com (altın/gümüş ons, ücretsiz), open.er-api.com (döviz, ücretsiz)
- Dini günler (Türkiye 2026-2027): Diyanet İşleri Başkanlığı dini günler takvimi (basında yayımlanan resmî liste)

## Diyanet resmî API
Diyanet'in "Awqat Salah" API'si (`awqatsalah.diyanet.gov.tr`) **kurumdan başvuruyla verilen kullanıcı adı ve şifre** gerektirir. Bu sürümde kullanılmamıştır ve uygulama Diyanet verisini kullanıyormuş gibi gösterilmez: vakitler "Cihaz üzerinde hesaplama · Diyanet İşleri Başkanlığı yöntemi" olarak etiketlenir. Erişim alındığında anahtar GitHub Secrets ile CI'a verilerek eklenebilir (kaynak koda yazılmaz).

## Yazılım
Jetpack Compose, Material 3, Media3 (ExoPlayer, Session), Room, WorkManager, DataStore, Kotlinx Serialization/Coroutines, Play Services Location — Apache 2.0. Leaflet 1.9.4 — BSD-2-Clause.

### Yedek ses kaynağı
Bir hafız AlQuran Cloud CDN'de bulunamazsa CI, [mp3quran.net API v3](https://www.mp3quran.net/api/v3/reciters) listesinden aynı hafızın *murattal* kaydını bulur ve 114 sureyi tek tek doğrular. mp3quran.net kayıtları ücretsiz ve ticari olmayan kullanım için sunulur; hangi kaynağın kullanıldığı `web/data/media/reciters.json` dosyasında her hafız için yazılıdır.
