#!/usr/bin/env bash
# CI'da çalışır: Wikimedia Commons'taki açık lisanslı orijinal kayıtları indirir, SHA-1 ile doğrular ve MP3'e çevirir.
set -euo pipefail
D="$(cd "$(dirname "$0")/.." && pwd)/web/audio/ezan"; T=$(mktemp -d)
UA="NurVaktiGlobal-CI/1.0 (https://github.com/hknkaradogan-cpu/Ezanvakti)"
get() { curl -fsSL -A "$UA" --retry 3 -o "$2" "$1"; echo "$3  $2" | sha1sum -c -; }
if [ ! -s "$D/mekke.mp3" ]; then
  get "https://upload.wikimedia.org/wikipedia/commons/6/6d/Maghrib_Adhan_at_the_Masjid_al_Haram%2C_Mecca_-_25_Feb%2C_2012.webm" "$T/mekke.webm" 4482abb50e335b68bf02343958709e9d37dbeab0 \
  && ffmpeg -loglevel error -y -ss 112 -to 296 -i "$T/mekke.webm" -vn -ac 1 -b:a 96k -af "loudnorm=I=-16:TP=-1.5,afade=t=out:st=180:d=4" "$D/mekke.mp3" || echo "UYARI: Mekke kaydı alınamadı"
fi
if [ ! -s "$D/medine.mp3" ]; then
  get "https://upload.wikimedia.org/wikipedia/commons/7/7c/33937_ejaz215_call-to-prayer-from-the-prophet-s-mo.ogg" "$T/medine.ogg" 4aa432b9e44804e7d72af4429bd743aa5fa9e3a4 \
  && ffmpeg -loglevel error -y -i "$T/medine.ogg" -vn -ac 1 -b:a 96k -af "loudnorm=I=-16:TP=-1.5" "$D/medine.mp3" || echo "UYARI: Medine kaydı alınamadı"
fi
ls -la "$D"
