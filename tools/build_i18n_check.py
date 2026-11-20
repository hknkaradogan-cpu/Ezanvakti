#!/usr/bin/env python3
"""Kotlin ve web kaynaklarındaki t("...") metinlerini toplar; web/data/i18n.json'da eksik İngilizce/Arapça çevirileri raporlar."""
import re, json, os, glob
R = os.path.join(os.path.dirname(__file__), '..')
keys = set()
pat = re.compile(r'\bt\(\s*"((?:[^"\\]|\\.)*)"\s*\)')
pat_js = re.compile(r"\bt\(\s*'((?:[^'\\]|\\.)*)'\s*\)")
for f in glob.glob(f'{R}/android/app/src/main/java/**/*.kt', recursive=True):
    keys |= {k.encode().decode('unicode_escape').encode('latin1').decode('utf-8') if '\\' in k else k for k in pat.findall(open(f, encoding='utf-8').read())}
for f in glob.glob(f'{R}/web/js/*.js'):
    s = open(f, encoding='utf-8').read(); keys |= {k.replace('\\n', '\n') for k in set(pat_js.findall(s)) | set(pat.findall(s))}
# Kod içinde t() ile değil değişkenle çevrilen sabit metinler
extra = ["İmsak","Sabah","Güneş","Öğle","İkindi","Akşam","Yatsı","Ana sayfa","Vakitler","Kur'an","Kıble","Menü","Tam ezan","Kısa ezan","Normal bildirim","Titreşim","Sessiz","Kapalı",
         "Sırada","İndiriliyor","Duraklatıldı","İndirildi","Başarısız","Ramazan Bayramı","Kurban Bayramı","Bu hafta","Bu ay","Bu yıl","Gün","Tarih","İftar"]
keys |= set(extra)
d = json.load(open(f'{R}/web/data/i18n.json', encoding='utf-8')) if os.path.exists(f'{R}/web/data/i18n.json') else {}
missing = sorted(k for k in keys if k not in d or not d[k].get('en') or not d[k].get('ar'))
print(f'toplam {len(keys)} metin, eksik {len(missing)}')
for k in missing: print('  -', k)
if __name__ == '__main__' and os.environ.get('DUMP'): json.dump(sorted(keys), open(os.environ['DUMP'], 'w', encoding='utf-8'), ensure_ascii=False, indent=0)
