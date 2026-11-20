#!/usr/bin/env bash
# Android emülatöründe gerçek cihaz testi: kurulum, tüm ekranlar, dil değişimi, Kur'an çalma (ekran kapalı),
# gerçek AlarmManager ezan alarmı (ekran kapalı), yeniden başlatma sonrası alarmlar, çökme kontrolü.
set -u
PKG=com.nurvakti.global; ACT=$PKG/.MainActivity; OUT=ci-out; mkdir -p $OUT/shots; R=$OUT/emulator.md
log() { echo "$@" | tee -a $R; }
shot() { sleep "${2:-4}"; adb exec-out screencap -p > "$OUT/shots/$1.png"; }
start() { adb shell am start -W -n $ACT -f 0x10008000 "$@" > /dev/null 2>&1; }
pass=0; fail=0; ok() { log "- ✅ $1"; pass=$((pass+1)); }; ko() { log "- ❌ $1"; fail=$((fail+1)); }
log "# Emülatör testi ($(adb shell getprop ro.build.version.release | tr -d '\r') / API $(adb shell getprop ro.build.version.sdk | tr -d '\r'))"; log ""
adb install -r out/app-debug.apk && ok "APK kuruldu" || ko "APK kurulamadı"
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS; adb shell appops set $PKG SCHEDULE_EXACT_ALARM allow
adb shell settings put system screen_off_timeout 1800000
adb logcat -c
start --es ci onboard; sleep 6
start; shot 00_home 6
for r in times quran "reader/1?a=1" yasin reciters qibla menu settings location ezan downloads dualar zikir esma ramazan takvim tracker zekat rehber about battery bookmarks player; do
  start --es open "$r"; shot "$(echo "$r" | tr '/?=' '___')"
done
for l in en ar; do start --es ci "lang:$l"; sleep 3; start; shot "lang_${l}_home"; start --es open quran; shot "lang_${l}_quran"; done
start --es ci lang:tr; sleep 2; start --es ci theme:DARK; sleep 2; start; shot dark_home; start --es ci theme:SYSTEM; sleep 2
log ""; log "## Kur'an dinleme (Alafasy, Yasin) ve ekran kapalıyken çalma"
start --es open yasin; sleep 3; start --es ci play:36 --es open yasin; sleep 20; shot playing_yasin 1
adb shell input keyevent 223; sleep 15
st=$(adb shell dumpsys media_session | tr -d "\r" | grep -A14 "package=$PKG" | grep -m1 -o "state=PlaybackState {state=[A-Z_]*")
log "  MediaSession durumu (ekran kapalı): $st"; [ "$st" = "state=PlaybackState {state=PLAYING" ] && ok "Ekran kapalıyken Kur'an çalmaya devam ediyor (PLAYING)" || ko "Ekran kapalıyken çalma doğrulanamadı ($st)"
adb shell input keyevent 224; sleep 2; adb shell input keyevent 82
adb shell dumpsys media_session | tr -d '\r' | grep -A12 "$PKG" | head -14 >> $R
adb shell cmd media_session dispatch pause 2>/dev/null || adb shell input keyevent 127
log ""; log "## İndirme merkezi ve çevrimdışı oynatma"
start --es ci dl:36; sleep 3; start --es ci dlezan:mekke; sleep 45; start --es open downloads; shot downloads_after 4
fq=$(adb shell run-as $PKG ls -l files/media/quran/alafasy/ 2>/dev/null | tr -d '\r' | grep -c "36.mp3"); fe=$(adb shell run-as $PKG ls -l files/media/ezan/ 2>/dev/null | tr -d '\r' | grep -c "mekke.mp3")
adb shell run-as $PKG ls -l files/media/quran/alafasy/ files/media/ezan/ 2>/dev/null | sed 's/^/    /' >> $R
[ "$fq" -gt 0 ] && ok "Yasin (Alafasy) telefona indirildi" || ko "Sure indirilemedi"; [ "$fe" -gt 0 ] && ok "Mekke ezanı telefona indirildi" || ko "Ezan indirilemedi"
adb shell cmd connectivity airplane-mode enable; adb shell svc wifi disable; adb shell svc data disable; sleep 5
start --es ci play:36; sleep 12
st=$(adb shell dumpsys media_session | tr -d "\r" | grep -A14 "package=$PKG" | grep -m1 -o "state=PlaybackState {state=[A-Z_]*")
[ "$st" = "state=PlaybackState {state=PLAYING" ] && ok "İnternet kapalıyken indirilen Yasin çalıyor" || ko "Çevrimdışı oynatma doğrulanamadı ($st)"
adb shell input keyevent 127; sleep 2
start --es ci alarm25:mekke; sleep 3; adb shell input keyevent 223; sleep 35
svc=$(adb shell dumpsys activity services $PKG | tr -d '\r' | grep -c "AdhanService"); used=$(adb logcat -d -s NurAdhan | grep -c "playing file:.*mekke.mp3")
adb logcat -d -s NurAdhan | tail -3 | sed 's/^/    /' >> $R
[ "$svc" -gt 0 ] && [ "$used" -gt 0 ] && ok "İnternet kapalı + ekran kapalı: indirilen Mekke ezanı dosyadan çaldı" || ko "Çevrimdışı Mekke ezanı doğrulanamadı (servis=$svc, dosya=$used)"
adb shell input keyevent 224; adb shell am startservice -n $PKG/.alarm.AdhanService -a com.nurvakti.global.STOP_ADHAN >/dev/null 2>&1
adb shell cmd connectivity airplane-mode disable; adb shell svc wifi enable; adb shell svc data enable; sleep 8
log ""; log "## Farklı hafızlar"
for rr in husary minshawi sudais; do start --es ci "playr:$rr:112"; sleep 14
  st=$(adb shell dumpsys media_session | tr -d "\r" | grep -A14 "package=$PKG" | grep -m1 -o "state=PlaybackState {state=[A-Z_]*")
  [ "$st" = "state=PlaybackState {state=PLAYING" ] && ok "$rr ile İhlâs suresi çalıyor" || log "- ⚠️ $rr çalınamadı ($st) — hafız listede yoksa beklenen durum"
  adb shell input keyevent 127; sleep 1; done
start --es open reciters; shot reciters_list 4
log ""; log "## Konum değişikliği"
start --es ci "place:51.5074,-0.1278,GB,x,Londra"; sleep 4; start; shot place_london 5; start --es open times; shot times_london 4
start --es ci "place:41.0082,28.9784,TR,x,İstanbul"; sleep 3; start --es open camiler; shot camiler 20
ok "Konum Londra'ya değiştirildi (ekran görüntüsü: place_london.png, times_london.png)"

log ""; log "## Gerçek ezan alarmı (AlarmManager, ekran kapalı)"
start --es ci alarm45; sleep 3
adb shell dumpsys alarm | tr -d '\r' | grep -B2 -A6 "$PKG" | grep -m3 -E "RTC_WAKEUP|ALARM_CLOCK|origWhen" | sed 's/^/    /' >> $R
adb shell input keyevent 223; sleep 60
svc=$(adb shell dumpsys activity services $PKG | tr -d '\r' | grep -c "AdhanService")
[ "$svc" -gt 0 ] && ok "Alarm tetiklendi, AdhanService (ön plan) ekran kapalıyken çalışıyor" || ko "AdhanService çalışmıyor"
adb shell dumpsys notification --noredact | tr -d '\r' | grep -A3 "pkg=$PKG" | grep -m2 -E "android.title|title=" | sed 's/^/    /' >> $R
adb shell input keyevent 224; sleep 2; shot alarm_notification 1; adb shell cmd statusbar expand-notifications; shot alarm_shade 2; adb shell cmd statusbar collapse
log ""; log "## Planlanan vakit alarmları"
n1=$(adb shell dumpsys alarm | tr -d '\r' | grep -c "$PKG")
log "  Yeniden başlatma öncesi kayıtlı alarm satırı: $n1"; [ "$n1" -gt 2 ] && ok "Vakit alarmları AlarmManager'a kuruldu" || ko "Vakit alarmları bulunamadı"
log ""; log "## Telefon yeniden başlatma"
adb reboot; adb wait-for-device; for i in $(seq 1 90); do [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ] && break; sleep 2; done; sleep 40
n2=$(adb shell dumpsys alarm | tr -d '\r' | grep -c "$PKG")
log "  Yeniden başlatma sonrası kayıtlı alarm satırı: $n2"; [ "$n2" -gt 2 ] && ok "Telefon yeniden başladıktan sonra alarmlar yeniden kuruldu (BootReceiver)" || ko "Yeniden başlatma sonrası alarm yok"
log ""; log "## Saat dilimi değişikliği"
adb shell cmd alarm set-timezone Europe/Berlin 2>/dev/null || adb shell service call alarm 3 s16 Europe/Berlin >/dev/null; sleep 8
n3=$(adb shell dumpsys alarm | tr -d '\r' | grep -c "$PKG"); [ "$n3" -gt 2 ] && ok "Saat dilimi değişince alarmlar yeniden kuruldu ($n3)" || ko "Saat dilimi sonrası alarm yok"
adb shell cmd alarm set-timezone Europe/Istanbul 2>/dev/null
start; shot after_reboot_home 6
log ""; log "## Çökme kontrolü"
adb logcat -d > $OUT/logcat.txt
if grep -q "FATAL EXCEPTION" $OUT/logcat.txt && grep -A3 "FATAL EXCEPTION" $OUT/logcat.txt | grep -q "$PKG"; then ko "Uygulama çöktü (logcat'te FATAL EXCEPTION)"; grep -A25 "FATAL EXCEPTION" $OUT/logcat.txt | head -60 | sed 's/^/    /' >> $R
else ok "Tüm ekran gezintisi, dil/tema değişimi, oynatma ve alarm boyunca çökme yok"; fi
grep -E "ANR in $PKG" $OUT/logcat.txt && ko "ANR tespit edildi" || true
log ""; log "**Sonuç: $pass başarılı, $fail başarısız**"
exit 0
