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
KEYS = {'alafasy': ['alafasy'], 'husary': ['husary'], 'minshawi': ['minshawi'], 'abdulbasit': ['abdulbasit'], 'sudais': ['sudais'],
        'shuraim': ['shuraim', 'shuraym'], 'ajamy': ['ajamy', 'ajmi'], 'maher': ['maher', 'muaiqly'], 'hudhaify': ['hudhaify', 'huthayfi'],
        'ayyoub': ['ayyoub', 'ayoub'], 'jibreel': ['jibreel', 'jibril'], 'shaatree': ['shaatree', 'shatri'], 'rifai': ['rifai']}
INFO = {}
if verify:
    # CDN'in resmî listesi: hangi kimlik (edition) hangi bit hızında sure dosyası sunuyor
    try:
        import re
        raw = urllib.request.urlopen(urllib.request.Request('https://cdn.islamic.network/quran/info/by-surah/info.json', headers={'User-Agent': 'NurVaktiGlobal-CI/1.0'}), timeout=30).read().decode()
        data = json.loads(raw)
        def walk(o, key=None):
            if isinstance(o, dict):
                ident = o.get('identifier') or key
                brs = o.get('bitrates') or o.get('bitrate')
                if ident and isinstance(ident, str) and ident.startswith('ar.') and brs is not None:
                    INFO[ident] = [int(b) for b in (brs if isinstance(brs, list) else [brs]) if str(b).isdigit()]
                for k, v in o.items(): walk(v, k)
            elif isinstance(o, list):
                for v in o: walk(v)
        walk(data)
        print('CDN listesi:', len(INFO), 'kimlik'); print(json.dumps(INFO)[:3000])
    except Exception as e:
        print('CDN listesi alınamadı:', e)
MP3Q = {}
if verify:
    try:
        rs = json.loads(urllib.request.urlopen(urllib.request.Request('https://www.mp3quran.net/api/v3/reciters?language=eng', headers={'User-Agent': 'NurVaktiGlobal-CI/1.0'}), timeout=30).read().decode())['reciters']
        for rid, keys in KEYS.items():
            for r in rs:
                nm = r['name'].lower().replace(' ', '')
                if any(k in nm for k in keys):
                    ms = [m for m in r['moshaf'] if int(m.get('surah_total', 0)) >= 100 and 'mujawwad' not in m['name'].lower()]
                    ms.sort(key=lambda m: (-int(m['surah_total']), 'murattal' not in m['name'].lower()))
                    if ms: MP3Q[rid] = {'server': ms[0]['server'].rstrip('/') + '/', 'name': r['name']}; break
        print('mp3quran eşleşmeleri:', {k: v['server'] for k, v in MP3Q.items()})
    except Exception as e:
        print('mp3quran listesi alınamadı:', e)
def candidates(rid, base):
    out = list(base)
    for ident, brs in INFO.items():
        if any(k in ident for k in KEYS.get(rid, [])) and 'mujawwad' not in ident:
            for br in sorted(brs, key=lambda b: (b != 128, -b)): out.append((ident, br))
    seen = set(); res = []
    for c in out:
        if c not in seen: seen.add(c); res.append(c)
    return res
out = []
prev = {}
p = f'{B}/data/media/reciters.json'
if os.path.exists(p): prev = {r['id']: r for r in json.load(open(p, encoding='utf-8'))['reciters']}
for rid, tr, en, info, cands in RECITERS:
    entry = {'id': rid, 'tr': tr, 'en': en, 'info': info, 'source': 'cdn.islamic.network (AlQuran Cloud CDN)'}
    if verify:
        chosen = None
        for ed, br in candidates(rid, cands):
            ok, _ = head(CDN.format(br=br, ed=ed, n=1))
            if ok: chosen = (ed, br); break
        tmpl = None
        if chosen:
            ed, br = chosen; tmpl = CDN.replace('{br}', str(br)).replace('{ed}', ed); src = 'cdn.islamic.network (AlQuran Cloud CDN)'
        else:
            # Yedek kaynak: mp3quran.net (ücretsiz, ticari olmayan kullanım)
            m = MP3Q.get(rid)
            if m and head(m['server'] + '001.mp3')[0]:
                ed, br = 'mp3quran:' + m['name'], 128; tmpl = m['server'] + '{nnn}.mp3'; src = 'mp3quran.net'
        if not tmpl:
            print('YOK:', rid); continue
        url_of = lambda n: tmpl.replace('{nnn}', '%03d' % n).replace('{n}', str(n))
        with concurrent.futures.ThreadPoolExecutor(16) as ex:
            res = list(ex.map(lambda n: (n, *head(url_of(n))), range(1, 115)))
        avail = [n for n, ok, _ in res if ok]
        sizes = {str(n): s for n, ok, s in res if ok}
        if not avail: print('YOK:', rid); continue
        entry.update({'edition': ed, 'bitrate': br, 'url': tmpl, 'surahs': avail, 'sizes': sizes, 'verified': True, 'source': src})
        print(rid, ed, br, len(avail), '/114', src)
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
