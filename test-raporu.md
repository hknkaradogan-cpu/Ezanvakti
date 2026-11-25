# Namaz vakti testleri

Tarih: 2026-11-25T23:20:40.362Z

## 1. Diyanet resmî vakitleriyle karşılaştırma (tolerans ±1 dk)

| Yer | Tarih | Hesap | Resmî | En büyük fark |
|---|---|---|---|---|
| İstanbul | 2026-08-04 | 04:13 05:56 13:15 17:09 20:25 22:01 | 04:13 05:56 13:15 17:09 20:25 22:01 | 0 dk ✅ |
| İstanbul | 2026-08-27 | 04:46 06:19 13:11 16:54 19:53 21:19 | 04:46 06:19 13:11 16:54 19:53 21:19 | 0 dk ✅ |
| İstanbul | 2026-09-15 | 05:11 06:37 13:04 16:35 19:22 20:43 | 05:11 06:37 13:04 16:35 19:22 20:43 | 0 dk ✅ |
| İstanbul | 2026-09-24 | 05:21 06:46 13:01 16:25 19:06 20:26 | 05:21 06:46 13:01 16:25 19:06 20:26 | 0 dk ✅ |
| Artvin/Borçka | 2026-08-27 | 03:54 05:27 12:20 16:03 19:03 20:29 | 03:55 05:27 12:20 16:03 19:03 20:29 | 1 dk ✅ |
| Mersin/Erdemli | 2026-08-27 | 04:38 06:03 12:49 16:30 19:26 20:45 | 04:38 06:03 12:49 16:30 19:26 20:45 | 0 dk ✅ |
| Çorum/Kargı | 2026-08-27 | 04:24 05:56 12:49 16:32 19:31 20:57 | 04:24 05:56 12:49 16:32 19:31 20:57 | 0 dk ✅ |
| Sivas/Doğanşar | 2026-08-27 | 04:15 05:45 12:36 16:19 19:18 20:42 | 04:15 05:45 12:37 16:19 19:18 20:42 | 1 dk ✅ |
| Mersin/Silifke | 2026-09-22 | 05:06 06:25 12:42 16:09 18:50 20:04 | 05:06 06:25 12:42 16:09 18:49 20:04 | 1 dk ✅ |
| Yalova/Termal | 2026-09-22 | 05:19 06:44 13:01 16:26 19:09 20:28 | 05:19 06:43 13:01 16:26 19:09 20:28 | 1 dk ✅ |
| Giresun/Keşap | 2026-09-22 | 04:41 06:06 12:24 15:49 18:32 19:52 | 04:41 06:06 12:24 15:49 18:32 19:51 | 1 dk ✅ |

## 2. AlAdhan API ile canlı karşılaştırma

Tolerans ±2 dk. Not: AlAdhan'ın Diyanet yöntemi (13) Diyanet'in temkin sürelerini birebir uygulamaz; Diyanet doğruluğu 1. bölümde resmî tabloyla ölçülür, buradaki Diyanet satırları bilgi amaçlıdır.

| Şehir | Yöntem | Tarih | Uygulama (imsak güneş öğle ikindi akşam yatsı) | AlAdhan | En büyük fark | Sonuç |
|---|---|---|---|---|---|---|
| İstanbul | DIYANET/Şafii | 25-11-2026 | 06:26 07:56 12:56 15:24 17:47 19:12 | 06:26 07:56 12:56 15:24 17:46 19:10 | 2 dk | ℹ️ bilgi (Diyanet doğruluğu 1. bölümde resmî tabloyla ölçülür) |
| İstanbul | DIYANET/Hanefi | 25-11-2026 | 06:26 07:56 12:56 16:03 17:47 19:12 | 06:26 07:56 12:56 16:02 17:46 19:10 | 2 dk | ℹ️ bilgi (Diyanet doğruluğu 1. bölümde resmî tabloyla ölçülür) |
| Ankara | DIYANET/Şafii | 25-11-2026 | 06:09 07:37 12:41 15:12 17:34 18:58 | 06:09 07:37 12:41 15:12 17:33 18:56 | 2 dk | ℹ️ bilgi (Diyanet doğruluğu 1. bölümde resmî tabloyla ölçülür) |
| Ankara | DIYANET/Hanefi | 25-11-2026 | 06:09 07:37 12:41 15:51 17:34 18:58 | 06:09 07:37 12:41 15:50 17:33 18:56 | 2 dk | ℹ️ bilgi (Diyanet doğruluğu 1. bölümde resmî tabloyla ölçülür) |
| Londra | MWL/Şafii | 25-11-2026 | 05:35 07:35 11:47 13:40 16:00 17:53 | 05:35 07:35 11:47 13:42 16:00 17:53 | 2 dk | ✅ |
| Londra | MWL/Hanefi | 25-11-2026 | 05:35 07:35 11:47 14:13 16:00 17:53 | 05:35 07:35 11:47 14:13 16:00 17:53 | 0 dk | ✅ |
| Berlin | MWL/Şafii | 25-11-2026 | 05:42 07:45 11:53 13:42 16:01 17:57 | 05:42 07:45 11:53 13:43 16:01 17:57 | 1 dk | ✅ |
| Berlin | MWL/Hanefi | 25-11-2026 | 05:42 07:45 11:53 14:13 16:01 17:57 | 05:42 07:45 11:53 14:14 16:01 17:57 | 1 dk | ✅ |
| New York | ISNA/Şafii | 25-11-2026 | 05:34 06:54 11:43 14:12 16:31 17:52 | 05:34 06:54 11:43 14:13 16:31 17:52 | 1 dk | ✅ |
| New York | ISNA/Hanefi | 25-11-2026 | 05:34 06:54 11:43 14:51 16:31 17:52 | 05:34 06:54 11:43 14:51 16:31 17:52 | 0 dk | ✅ |

## 3. Hanefi / Şafii ikindi farkı (İstanbul, Diyanet)

- 15.01.2026: Şafii 15:46 · Hanefi 16:24 · fark 38 dk ✅
- 15.04.2026: Şafii 16:52 · Hanefi 17:52 · fark 59 dk ✅
- 15.07.2026: Şafii 17:14 · Hanefi 18:25 · fark 71 dk ✅
- 15.10.2026: Şafii 15:59 · Hanefi 16:45 · fark 46 dk ✅

## 4. Kıble

- İstanbul kıble açısı 151.62° (beklenen ≈151.6°), Kâbe'ye 2405 km
- New York kıble açısı 58.48° (beklenen ≈58.5°)

**Sonuç: TÜM TESTLER GEÇTİ** (AlAdhan canlı test sayısı: 10)
