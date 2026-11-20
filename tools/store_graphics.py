#!/usr/bin/env python3
"""Play Store görselleri: başlıklı telefon ekran görüntüleri (1080x1920) ve öne çıkan görsel (1024x500).
Girdi: emülatörde çekilen ham ekran görüntüleri (ci-out/store_raw). Çıktı: ci-out/store/..."""
import os, sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter
R = os.path.dirname(os.path.abspath(__file__))
RAW = sys.argv[1] if len(sys.argv) > 1 else 'ci-out/store_raw'
OUT = sys.argv[2] if len(sys.argv) > 2 else 'ci-out/store'
B = lambda s: ImageFont.truetype(f'{R}/fonts/DejaVuSans-Bold.ttf', s)
N = lambda s: ImageFont.truetype(f'{R}/fonts/DejaVuSans.ttf', s)
EM, NAVY, GOLD, GOLDL = (11, 110, 79), (11, 31, 58), (201, 162, 39), (240, 210, 122)

def bg(w, h):
    im = Image.new('RGB', (w, h)); d = ImageDraw.Draw(im)
    for y in range(h):
        t = y / h; c = tuple(int(EM[i] * (1 - t) + NAVY[i] * t) for i in range(3)); d.line([(0, y), (w, y)], fill=c)
    import random; random.seed(7)
    for _ in range(70):  # yıldızlar
        x, y, r = random.randint(0, w), random.randint(0, int(h * .35)), random.choice([1, 1, 2, 3])
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 240, 200))
    return im

def center(d, y, text, font, fill, w):
    tw = d.textlength(text, font=font); d.text(((w - tw) / 2, y), text, font=font, fill=fill)

def wrap(d, text, font, maxw):
    words, lines, cur = text.split(), [], ''
    for wd in words:
        t = (cur + ' ' + wd).strip()
        if d.textlength(t, font=font) <= maxw: cur = t
        else: lines.append(cur); cur = wd
    lines.append(cur); return lines

def phone(shot, H, maxw=900, crop=None):
    s = Image.open(shot).convert('RGB')
    if crop: s = s.crop((0, 0, s.width, int(s.height * crop)))
    w = int(s.width * H / s.height)
    if w > maxw: H = int(H * maxw / w); w = maxw
    s = s.resize((w, H), Image.LANCZOS)
    rad = 48; m = Image.new('L', s.size, 0); ImageDraw.Draw(m).rounded_rectangle([0, 0, w - 1, H - 1], rad, fill=255)
    fr = Image.new('RGBA', (w + 24, H + 24), (0, 0, 0, 0)); ImageDraw.Draw(fr).rounded_rectangle([0, 0, w + 23, H + 23], rad + 12, fill=GOLD + (255,))
    fr.paste(s, (12, 12), m); return fr

def screenshot(shot, title, sub, out, W=1080, Hh=1920, crop=None):
    im = bg(W, Hh).convert('RGBA'); d = ImageDraw.Draw(im)
    center(d, 70, title, B(70), GOLDL, W)
    y = 170
    for ln in wrap(d, sub, N(40), W - 140): center(d, y, ln, N(40), (255, 255, 255), W); y += 52
    p = phone(shot, Hh - y - 80, crop=crop)
    sh = Image.new('RGBA', im.size, (0, 0, 0, 0)); ImageDraw.Draw(sh).rounded_rectangle([(W - p.width) // 2 + 14, y + 44, (W + p.width) // 2 + 14, y + 44 + p.height], 60, fill=(0, 0, 0, 120))
    im = Image.alpha_composite(im, sh.filter(ImageFilter.GaussianBlur(18)))
    im.alpha_composite(p, ((W - p.width) // 2, y + 30))
    im.convert('RGB').save(out, quality=95)

def feature(icon, out):
    W, H = 1024, 500; im = bg(W, H).convert('RGBA'); d = ImageDraw.Draw(im)
    ic = Image.open(icon).convert('RGBA').resize((400, 400), Image.LANCZOS); im.alpha_composite(ic, (50, 50))
    d.text((490, 120), 'Namaz Vakti', font=B(66), fill=GOLDL)
    d.text((492, 205), 'Ezan ve Kıble', font=B(50), fill=(255, 255, 255))
    d.text((494, 290), 'Ezan · Kıble · Kur\'an · Yasin', font=N(30), fill=(235, 240, 245))
    d.text((494, 335), 'Reklamsız · İnternetsiz çalışır', font=N(30), fill=(235, 240, 245))
    d.text((494, 410), 'By-Hkn', font=B(28), fill=GOLD)
    im.convert('RGB').save(out, quality=95)

SHOTS = [  # (ham dosya, TR başlık, TR alt yazı, EN başlık, EN alt yazı)
    ('home', 'Namaz Vakitleri', 'Sonraki vakte canlı geri sayım, Hicri tarih ve beş vakit', 'Prayer Times', 'Live countdown to the next prayer, Hijri date and all five prayers'),
    ('ezan', 'Gerçek Ezan', 'Her vakit için tam ezan, kısa ezan, bildirim veya titreşim', 'Real Adhan', 'Full adhan, short adhan, notification or vibration for each prayer'),
    ('yasin', 'Yasin-i Şerif Dinle', 'Arapça metin, Latin okunuş ve Türkçe meal ile', 'Listen to Surah Yasin', 'Arabic text, transliteration and translation'),
    ('reciters', '13 Hafızdan Kur\'an', 'Sudeys, Afasi, Husari, Minşavi… İndir, internetsiz dinle', 'Quran by 13 Reciters', 'Sudais, Alafasy, Husary, Minshawi… Download and listen offline'),
    ('qibla', 'Kıble Pusulası', 'Telefon sensörüyle Kâbe yönü ve mesafesi; harita desteği', 'Qibla Compass', 'Kaaba direction and distance with your phone sensors, plus map'),
    ('notif', 'Namazını Kıldın mı?', 'Vakitten sonra hatırlatma; "Kıldım ✓" ile tek dokunuşta işaretle', 'Did You Pray?', 'A reminder after each prayer; mark it with one tap'),
    ('ramazan', 'Ramazan ve İmsakiye', 'İftara kalan süre, imsakiye ve oruç takibi', 'Ramadan Timetable', 'Time to iftar, imsakiye and fasting tracker'),
    ('dualar', 'Dualar ve Zikirmatik', 'Kaynaklı dualar, Esmâü\'l-Hüsnâ, Ramazan imsakiyesi', 'Duas & Tasbih', 'Duas with sources, 99 Names, Ramadan timetable'),
    ('dark', 'Gece Teması', 'Açık, koyu ve otomatik tema · Türkçe, English, العربية', 'Dark Theme', 'Light, dark and automatic theme · Turkish, English, Arabic'),
]
if __name__ == '__main__':
    for lang in ('tr-TR', 'en-US'):
        os.makedirs(f'{OUT}/{lang}/phoneScreenshots', exist_ok=True)
        i = 0
        for key, tt, ts, et, es in SHOTS:
            src = f'{RAW}/{key}.png'
            if not os.path.exists(src): print('yok:', src); continue
            i += 1
            screenshot(src, tt if lang == 'tr-TR' else et, ts if lang == 'tr-TR' else es, f'{OUT}/{lang}/phoneScreenshots/{i}.png', crop=.52 if key == 'notif' else None)
        print(lang, i, 'ekran görüntüsü')
    icon = os.path.join(R, '..', 'docs', 'play-icon-512.png')
    feature(icon, f'{OUT}/featureGraphic.png')
    print('öne çıkan görsel hazır')
