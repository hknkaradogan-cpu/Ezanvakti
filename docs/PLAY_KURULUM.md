# Google Play'e yükleme — hazır paket ve adım adım kurulum

Her şey depoda hazır. Senin yapacağın yalnızca Google hesabıyla yapılabilen kısımlar (hesap, ödeme, kimlik, formlar). Gerisini GitHub Actions'taki **"Play Store'a gönder"** iş akışı yapar.

## Hazır olanlar
| Ne | Nerede |
|---|---|
| Mağaza başlığı, kısa ve uzun açıklama (TR / EN / AR) | `fastlane/metadata/android/<dil>/` |
| Sürüm notları | `fastlane/metadata/android/<dil>/changelogs/default.txt` |
| Uygulama simgesi 512×512 | `docs/play-icon-512.png` |
| Öne çıkan görsel 1024×500 | `fastlane/metadata/android/<dil>/images/featureGraphic.png` |
| 9 telefon ekran görüntüsü (başlıklı, TR ve EN) | `fastlane/metadata/android/<dil>/images/phoneScreenshots/` |
| Play'e yüklenecek paket (.aab) | Her GitHub sürümünde `NamazVakti-…-playstore.aab` |
| Gizlilik politikası | https://hknkaradogan-cpu.github.io/Ezanvakti/gizlilik.html (GitHub Pages açıldığında) |
| Otomatik yükleme | `.github/workflows/play.yml` |

Ekran görüntüleri her derlemede emülatörde yeniden çekilir; uygulama değişince mağaza görselleri de güncellenir.

## 1. Geliştirici hesabı (bir kez, 25 $)
play.google.com/console → **Kişisel hesap** → kimlik doğrulama (nüfus cüzdanı) ve telefon doğrulaması. Onay birkaç gün sürebilir.

> **Önemli:** 13 Kasım 2023'ten sonra açılan kişisel hesaplarda, uygulamayı herkese açmadan önce **en az 12 test kullanıcısıyla 14 gün kesintisiz kapalı test** yapılması zorunludur. 12 kişinin Gmail adresini topla (aile, arkadaş); test boyunca uygulamayı silmemeleri yeterli.

## 2. Uygulamayı oluştur
Play Console → **Uygulama oluştur**
- Uygulama adı: `Namaz Vakti: Ezan ve Kıble` · Varsayılan dil: **Türkçe – tr-TR**
- Uygulama · **Ücretsiz**
- Paket adı ilk yüklemede otomatik gelir: `com.nurvakti.global`

## 3. İmza anahtarı
`docs/IMZALAMA.md`'deki 4 GitHub Secret eklenmiş olmalı. Play Console'da **Play Uygulama İmzalama** açık kalsın (varsayılan); GitHub'daki anahtar "yükleme anahtarı" olur.

## 4. Otomatik yükleme izni (hizmet hesabı) — bir kez
1. console.cloud.google.com → yeni proje: `namaz-vakti`
2. **API'ler ve Hizmetler → Kitaplık** → "Google Play Android Developer API" → **Etkinleştir**
3. **IAM → Hizmet hesapları → Oluştur**: ad `github-play` → Bitti
4. Hesaba tıkla → **Anahtarlar → Anahtar ekle → JSON** → dosya iner
5. Play Console → **Kullanıcılar ve izinler → Yeni kullanıcı davet et** → hizmet hesabının e-postası (`github-play@…iam.gserviceaccount.com`) → Uygulama izinleri: *Namaz Vakti* → "Sürümleri yayınla", "Mağaza girişini düzenle" → Davet et
6. GitHub → Settings → Secrets → Actions → **PLAY_SERVICE_ACCOUNT_JSON** → JSON dosyasının **tüm içeriğini** yapıştır

> Google, yeni uygulamanın **ilk paketinin Play Console'dan elle** yüklenmesini ister: GitHub Releases'tan `…-playstore.aab` dosyasını indir → Play Console → Test → Dahili test → Yeni sürüm → yükle. Sonraki tüm sürümleri iş akışı yükler.

## 5. Formlar (Uygulama içeriği bölümü) — hazır cevaplar
| Form | Cevap |
|---|---|
| Gizlilik politikası | https://hknkaradogan-cpu.github.io/Ezanvakti/gizlilik.html |
| Uygulama erişimi | Tüm işlevler özel erişim olmadan kullanılabilir |
| Reklamlar | **Hayır**, uygulamada reklam yok (reklam eklenirse güncellenecek) |
| İçerik derecelendirmesi | Kategori: "Referans, Haber veya Eğitim" → şiddet / cinsellik / küfür / uyuşturucu / kumar: **Hayır**; kullanıcılar etkileşim kuramaz, içerik paylaşamaz: **Hayır**; konum paylaşımı başkalarıyla: **Hayır** → Sonuç: PEGI 3 / Herkes |
| Hedef kitle | 13 yaş ve üzeri (13-15, 16-17, 18+) · Çocuklara yönelik değil |
| Haber uygulaması | Hayır |
| Veri güvenliği | Toplanan veri: **Yok**. Paylaşılan veri: **Yok**. Açıklama: "Konum yalnızca cihazda vakit ve kıble hesabı için kullanılır." Veriler aktarımda şifrelenir: Evet (HTTPS). Kullanıcı veri silme isteyebilir: Evet (uygulama içinden). |
| Devlet uygulaması / Finansal / Sağlık | Hayır |
| Kesin alarm (Alarmlar ve hatırlatıcılar) | Uygulamanın temel işlevi: kullanıcının seçtiği namaz vaktinde ezan alarmı çalmak |
| Ön plan hizmeti | `mediaPlayback`: ezanın ve Kur'an'ın ekran kapalıyken çalınması · `dataSync`: kullanıcının başlattığı ses indirmeleri. Kısa ekran videosu istenirse: ezan bildirimini ve Kur'an oynatıcı bildirimini gösteren 20-30 sn'lik kayıt |
| Konum | Yalnızca ön planda (arka plan konumu kullanılmıyor) |
| Paket yükleme izni (REQUEST_INSTALL_PACKAGES) | Play sürümünde GitHub güncellemesi kapalıdır; Play bu izni reddederse Play sürümünden kaldırılacak |
| Mağaza: Kategori | Yaşam tarzı · Etiketler: Din, Takvim |
| İletişim e-postası | (kendi e-postan) |

## 6. Kapalı test → yayın
1. GitHub → **Actions → Play Store'a gönder → Run workflow** → track: **alpha** (kapalı test)
2. Play Console → Test → Kapalı test → Test kullanıcıları: 12 Gmail adresini ekle → katılım bağlantısını onlara gönder
3. 14 gün sonra Play Console → **Üretim erişimi için başvur** (birkaç soruya cevap; test sırasında neyi düzelttiğimizi ben yazarım)
4. Onaydan sonra: **Run workflow** → track: **production**

Mağaza yazısını veya görselleri sürüm yüklemeden güncellemek için: Run workflow → **Yalnızca mağaza yazıları ve görselleri** işaretli.
