# Emülatör testi (14 / API 34)

- ✅ APK kuruldu

## Kur'an dinleme (Alafasy, Yasin) ve ekran kapalıyken çalma
  MediaSession durumu (ekran kapalı): state=PlaybackState {state=PLAYING
- ✅ Ekran kapalıyken Kur'an çalmaya devam ediyor (PLAYING)
  Media button session is com.nurvakti.global/androidx.media3.session.id. (userId=0)
  Sessions Stack - have 8 sessions:
    androidx.media3.session.id. com.nurvakti.global/androidx.media3.session.id. (userId=0)
      ownerPid=3334, ownerUid=10192, userId=0
      package=com.nurvakti.global
      launchIntent=PendingIntent{869a9bf: PendingIntentRecord{5a2727a com.nurvakti.global startActivity (allowlist: 68f48f9:+30s0ms/0/NOTIFICATION_SERVICE/NotificationManagerService)}}
      mediaButtonReceiver=null
      active=true
      flags=7
      rating type=0
      controllers: 8
      state=PlaybackState {state=PLAYING(3), position=36217, buffered position=130638, speed=1.0, updated=249440, actions=7339995, custom actions=[], active item id=0, error=null}
      audioAttrs=AudioAttributes: usage=USAGE_MEDIA content=CONTENT_TYPE_MUSIC flags=0x800 tags= bundle=null
      volumeType=LOCAL, controlType=ABSOLUTE, max=0, current=0, volumeControlId=null

## İndirme merkezi ve çevrimdışı oynatma
    files/media/ezan:
    total 2164
    -rw------- 1 u0_a192 u0_a192 2208716 2026-11-25 23:32 mekke.mp3
    
    files/media/quran/sudais:
    total 12428
    -rw------- 1 u0_a192 u0_a192 12718649 2026-11-25 23:32 36.mp3
- ✅ Yasin (Sudeys) telefona indirildi
- ✅ Mekke ezanı telefona indirildi
- ✅ İnternet kapalıyken indirilen Yasin (Sudeys) çalıyor
    --------- beginning of main
    11-25 23:34:12.162  3334  3334 I NurAdhan: playing file:///data/user/0/com.nurvakti.global/files/media/ezan/mekke.mp3
- ✅ İnternet kapalı + ekran kapalı: indirilen Mekke ezanı dosyadan çaldı

## Farklı hafızlar
- ✅ husary ile İhlâs suresi çalıyor
- ✅ minshawi ile İhlâs suresi çalıyor
- ✅ sudais ile İhlâs suresi çalıyor

## Konum değişikliği
- ✅ Konum Londra'ya değiştirildi (ekran görüntüsü: place_london.png, times_london.png)

## Gerçek ezan alarmı (AlarmManager, ekran kapalı)
        RTC_WAKEUP #5: Alarm{360019f type 0 origWhen 1795649809345 whenElapsed 535409 com.nurvakti.global}
          type=RTC_WAKEUP origWhen=2026-11-25 23:36:49.345 window=0 exactAllowReason=permission repeatInterval=0 count=0 flags=0x3
        ELAPSED #6: Alarm{5feb1d8 type 3 origWhen 546065 whenElapsed 546065 android}
- ✅ Alarm tetiklendi, AdhanService (ön plan) ekran kapalıyken çalışıyor

## Planlanan vakit alarmları
  Yeniden başlatma öncesi kayıtlı alarm satırı: 129
- ✅ Vakit alarmları AlarmManager'a kuruldu

## Telefon yeniden başlatma
  Yeniden başlatma sonrası kayıtlı alarm satırı: 96
- ✅ Telefon yeniden başladıktan sonra alarmlar yeniden kuruldu (BootReceiver)

## Saat dilimi değişikliği
- ✅ Saat dilimi değişince alarmlar yeniden kuruldu (106)

## Hadis ve "kıldın mı?" bildirimleri
- ✅ "Öğle namazını kıldın mı?" bildirimi (Kıldım ✓ düğmeli) gösterildi

## Ana ekran widget'ları
- ✅ 2 widget sağlayıcısı sistemde kayıtlı (Geri sayım, Günün vakitleri)

## Kilit ekranında hadis/ayet
- ✅ Hadis bildirimi kilit ekranında tam içerikle gösteriliyor (vis=PUBLIC)

## Çökme kontrolü
- ✅ Tüm ekran gezintisi, dil/tema değişimi, oynatma ve alarm boyunca çökme yok

**Sonuç: 18 başarılı, 0 başarısız**
Play Store görselleri üretildi: ci-out/store
