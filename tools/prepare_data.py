#!/usr/bin/env python3
"""Kaynak veri dosyalarından uygulamanın kullandığı sıkıştırılmış JSON dosyalarını üretir.
Kaynaklar:
  - quran-json 3.1.2 (npm, CC-BY-4.0): Uthmani metin (King Fahd Complex / QuranEnc), Diyanet meali (tanzil tr.diyanet),
    Saheeh International (en), Latin transliterasyon (tanzil en.transliteration)
  - countries-states-cities-database (dr5hn, ODbL-1.0) – country-state-city 3.2.1 npm paketi içindeki veri
Kullanım: python3 tools/prepare_data.py <quran-json/package/dist> <country-state-city/package/lib/assets>
"""
import json, re, sys, os
qdir, cdir = sys.argv[1], sys.argv[2]
out = os.path.join(os.path.dirname(__file__), '..', 'web', 'data')
def dump(name, obj):
    p = os.path.join(out, name); os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8') as f: json.dump(obj, f, ensure_ascii=False, separators=(',', ':'))
    print(name, os.path.getsize(p))
strip = lambda s: re.sub(r'<[^>]+>', '', s).strip()

ar = json.load(open(f'{qdir}/quran.json', encoding='utf-8'))
tr = json.load(open(f'{qdir}/quran_tr.json', encoding='utf-8'))
en = json.load(open(f'{qdir}/quran_en.json', encoding='utf-8'))
tl = json.load(open(f'{qdir}/quran_transliteration.json', encoding='utf-8'))
chen = json.load(open(f'{qdir}/chapters/en/index.json', encoding='utf-8'))
assert len(ar) == 114 and sum(len(s['verses']) for s in ar) == 6236
dump('quran/ar.json', [[v['text'] for v in s['verses']] for s in ar])
dump('quran/tr.json', [[strip(v['translation']) for v in s['verses']] for s in tr])
dump('quran/en.json', [[strip(v['translation']) for v in s['verses']] for s in en])
dump('quran/translit.json', [[strip(v['transliteration']) for v in s['verses']] for s in tl])
surahs = []
for s, t, e in zip(ar, tr, chen):
    surahs.append({'n': s['id'], 'ar': s['name'], 'tl': s['transliteration'], 'tr': t['translation'], 'en': e['translation'],
                   'type': s['type'], 'ayahs': s['total_verses']})
dump('quran/surahs.json', surahs)

countries = json.load(open(f'{cdir}/country.json', encoding='utf-8'))
states = json.load(open(f'{cdir}/state.json', encoding='utf-8'))
cities = json.load(open(f'{cdir}/city.json', encoding='utf-8'))
r = lambda x: round(float(x), 4)
clist = []
for c in countries:
    tzs = [[z['zoneName'], z['gmtOffset']] for z in (c.get('timezones') or [])]
    clist.append({'c': c['isoCode'], 'n': c['name'], 'lat': r(c['latitude'] or 0), 'lng': r(c['longitude'] or 0), 'tz': tzs})
dump('loc/countries.json', clist)
by = {}
for s in states:
    if not s['latitude']: continue
    by.setdefault(s['countryCode'], {'s': [], 'c': []})['s'].append([s['isoCode'], s['name'], r(s['latitude']), r(s['longitude'])])
for c in cities:
    by.setdefault(c[1], {'s': [], 'c': []})['c'].append([c[0], c[2], r(c[3]), r(c[4])])
for cc, d in by.items(): dump(f'loc/{cc}.json', d)
