#!/usr/bin/env python3
"""Ezan ve hafız kataloglarını üretir. --verify verilirse (CI'da) her hafızın 114 sure dosyası
HTTP HEAD ile tek tek kontrol edilir; yalnızca gerçekten erişilebilen sureler 'surahs' listesine yazılır."""
import json, os, sys, concurrent.futures, urllib.request
B = os.path.join(os.path.dirname(__file__), '..', 'web')
CDN = 'https://cdn.islamic.network/quran/audio-surah/{br}/{ed}/{n}.mp3'
RECITERS = [
 ('alafasy','Mişari Raşid el-Afasi','Mishary Rashid Alafasy','Kuveyt · Hafs an Âsım · Murattal',[('ar.alafasy',128),('ar.alafasy',64)]),
 ('husary','Mahmud Halil el-Husari','Mahmoud Khalil Al-Husary','Mısır · Hafs an Âsım · Murattal',[('ar.husary',128),('ar.husary',64)]),
 ('minshawi','Muhammed Sıddık el-Minşavi','Mohamed Siddiq Al-Minshawi','Mısır · Murattal',[('ar.minshawi',128),('ar.minshawi',64)]),
 ('abdulbasit','Abdülbasit Abdüssamed','Abdul Basit Abdus Samad','Mısır · Murattal',[('ar.abdulbasitmurattal',192),('ar.abdulbasit',192),('ar.abdulbasitmurattal',64)]),
 ('sudais','Abdurrahman es-Südeys','Abdurrahman As-Sudais','Suudi Arabistan · Mescid-i Haram imamı',[('ar.abdurrahmaansudais',192),('ar.sudais',192),('ar.abdurrahmaansudais',64)]),
 ('shuraim','Suud eş-Şüreym','Saud Ash-Shuraim','Suudi Arabistan · Mescid-i Haram imamı',[('ar.saoodshuraym',128),('ar.shuraim',128),('ar.saoodshuraym',64)]),
 ('ajamy','Ahmed el-Acemi','Ahmed Al-Ajamy','Suudi Arabistan',[('ar.ahmedajamy',128),('ar.ajamy',128),('ar.ahmedajamy',64)]),
 ('maher','Mahir el-Muaykıli','Maher Al-Muaiqly','Suudi Arabistan · Mescid-i Haram imamı',[('ar.mahermuaiqly',128),('ar.mahermuaiqly',64)]),
 ('hudhaify','Ali el-Huzeyfi','Ali Al-Hudhaify','Suudi Arabistan · Mescid-i Nebevî imamı',[('ar.hudhaify',128),('ar.hudhaify',64)]),
 ('ayyoub','Muhammed Eyyub','Muhammad Ayyoub','Suudi Arabistan · Mescid-i Nebevî imamı',[('ar.muhammadayyoub',128),('ar.muhammadayoub',128)]),
 ('jibreel','Muhammed Cibril','Muhammad Jibreel','Mısır',[('ar.muhammadjibreel',128),('ar.muhammadjibreel',64)]),
 ('shaatree','Ebu Bekir eş-Şatri','Abu Bakr Ash-Shaatree','Suudi Arabistan',[('ar.shaatree',128),('ar.shaatree',64)]),
 ('rifai','Hani er-Rifai','Hani Ar-Rifai','Suudi Arabistan',[('ar.hanirifai',192),('ar.hanirifai',64)]),
]
def head(url):
    try:
        req = urllib.request.Request(url, method='HEAD', headers={'User-Agent': 'NurVaktiGlobal-CI/1.0'})
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status == 200 and int(r.headers.get('Content-Length') or 1) > 1000, int(r.headers.get('Content-Length') or 0)
    except Exception:
        return False, 0
verify = '--verify' in sys.argv
out = []
prev = {}
p = f'{B}/data/media/reciters.json'
if os.path.exists(p): prev = {r['id']: r for r in json.load(open(p, encoding='utf-8'))['reciters']}
for rid, tr, en, info, cands in RECITERS:
    entry = {'id': rid, 'tr': tr, 'en': en, 'info': info, 'source': 'cdn.islamic.network (AlQuran Cloud CDN)'}
    if verify:
        chosen = None
        for ed, br in cands:
            ok, _ = head(CDN.format(br=br, ed=ed, n=1))
            if ok: chosen = (ed, br); break
        if not chosen:
            print('YOK:', rid); continue
        ed, br = chosen
        with concurrent.futures.ThreadPoolExecutor(16) as ex:
            res = list(ex.map(lambda n: (n, *head(CDN.format(br=br, ed=ed, n=n))), range(1, 115)))
        avail = [n for n, ok, _ in res if ok]
        sizes = {str(n): s for n, ok, s in res if ok}
        entry.update({'edition': ed, 'bitrate': br, 'url': CDN.replace('{br}', str(br)).replace('{ed}', ed), 'surahs': avail, 'sizes': sizes, 'verified': True})
        print(rid, ed, br, len(avail), '/114')
    else:
        old = prev.get(rid)
        if old: entry = old
        else:
            ed, br = cands[0]
            entry.update({'edition': ed, 'bitrate': br, 'url': CDN.replace('{br}', str(br)).replace('{ed}', ed), 'surahs': list(range(1, 115)), 'sizes': {}, 'verified': False})
    out.append(entry)
json.dump({'generated': 'ci' if verify else 'local', 'reciters': out}, open(p, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)

E = [
 {'id': 'klasik', 'tr': 'Klasik ezan', 'en': 'Classic adhan', 'file': 'klasik.mp3', 'bundled': True, 'author': 'Adam-synagda', 'license': 'CC0 1.0 (Kamu malı)',
  'source': 'https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg', 'changes': 'MP3 (96 kb/s) olarak dönüştürüldü, ses seviyesi normalize edildi (marsathan-adhan v2).'},
 {'id': 'kisa', 'tr': 'Kısa ezan (ilk 42 sn)', 'en': 'Short adhan (first 42 s)', 'file': 'kisa.mp3', 'bundled': True, 'author': 'Adam-synagda', 'license': 'CC0 1.0 (Kamu malı)',
  'source': 'https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg', 'changes': 'Klasik ezanın ilk 42 saniyesi, 5 sn sesi azalarak biten kısaltma.'},
 {'id': 'mekke', 'tr': 'Mekke tarzı ezan (Mescid-i Haram, akşam)', 'en': 'Makkah style (Masjid al-Haram, Maghrib)', 'file': 'mekke.mp3', 'bundled': False, 'author': '3omar Faruq', 'license': 'CC BY 3.0',
  'source': 'https://commons.wikimedia.org/wiki/File:Maghrib_Adhan_at_the_Masjid_al_Haram,_Mecca_-_25_Feb,_2012.webm', 'changes': 'Videodan ses çıkarıldı, 1:52–4:56 arası (ezan) kesildi, MP3 96 kb/s.'},
 {'id': 'medine', 'tr': 'Medine tarzı ezan (Mescid-i Nebevî)', 'en': 'Madinah style (Prophet\'s Mosque)', 'file': 'medine.mp3', 'bundled': False, 'author': 'ejaz215 (Freesound #33937)', 'license': 'CC BY 3.0',
  'source': 'https://commons.wikimedia.org/wiki/File:33937_ejaz215_call-to-prayer-from-the-prophet-s-mo.ogg', 'changes': 'MP3 96 kb/s olarak dönüştürüldü, ses seviyesi normalize edildi.'},
]
for e in E:
    f = f'{B}/audio/ezan/{e["file"]}'
    e['available'] = os.path.exists(f)
    e['size'] = os.path.getsize(f) if e['available'] else 0
json.dump({'ezan': E}, open(f'{B}/data/media/ezan.json', 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print('ezan', [(e['id'], e['available']) for e in E])
