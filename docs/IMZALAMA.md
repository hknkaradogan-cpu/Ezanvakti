# Release imzalama anahtarı (GitHub Secrets)

APK'nın her sürümde aynı ve **gizli** bir anahtarla imzalanması için 4 gizli değer GitHub'a eklenir. Anahtar depoda (kodda) bulunmaz.

Telefondan:
1. `github.com/hknkaradogan-cpu/Ezanvakti` → **Settings** → **Secrets and variables** → **Actions** → **New repository secret**
2. Aşağıdaki 4 adı ve size ayrıca verilen dosyadaki değerleri tek tek ekleyin:
   - `SIGNING_KEYSTORE_BASE64`
   - `SIGNING_STORE_PASSWORD`
   - `SIGNING_KEY_ALIAS` (değer: `nurvakti`)
   - `SIGNING_KEY_PASSWORD`
3. **Actions** sekmesinden "APK + Web derleme" → **Run workflow**.

Gizli değerler eklenmeden üretilen APK'lar depodaki sabit *test anahtarıyla* imzalanır (her sürüm aynı test anahtarı olduğu için üstüne güncellenir). Gizli anahtara geçtikten sonra telefondaki test sürümünü **bir kez kaldırıp** yeni sürümü kurmanız gerekir (Android, farklı imzalı uygulamanın üstüne kurulmasına izin vermez).

Anahtar dosyasını (`nurvakti-release.jks`) ve şifresini güvenli bir yerde yedekleyin. Play Store'da "Play App Signing" kullanıldığında bu anahtar yükleme anahtarıdır; kaybolursa Play Console'dan sıfırlanabilir.
