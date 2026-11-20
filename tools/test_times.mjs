// CI testi: (1) Diyanet resmî vakitleri, (2) AlAdhan API ile canlı karşılaştırma (İstanbul, Ankara, Londra, Berlin, New York),
// (3) Hanefi/Şafii ikindi farkı, (4) Kotlin testleri için golden dosya üretimi. Sonuç: test-results/times.md
import { computeMinutes, fmt, ALADHAN_ID, qiblaBearing, distanceKm } from '../web/js/praytimes.js';
import fs from 'node:fs';
const out = []; let fail = 0;
const toM = s => { const [a, b] = s.split(':').map(Number); return a * 60 + b; };
const diffMin = (a, b) => { let d = Math.abs(a - b); return Math.min(d, 1440 - d); };
const K6 = ['imsak', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'];

out.push('## 1. Diyanet resmî vakitleriyle karşılaştırma (tolerans ±1 dk)\n', '| Yer | Tarih | Hesap | Resmî | En büyük fark |', '|---|---|---|---|---|');
const ref = JSON.parse(fs.readFileSync(new URL('../shared/tests/diyanet_reference.json', import.meta.url)));
for (const [n, lat, lng, ds, r] of ref.cases) {
  const [y, m, d] = ds.split('-').map(Number);
  const t = computeMinutes({ y, m, d }, lat, lng, ref.tz, { method: 'DIYANET', highLat: 'NONE' });
  const calc = K6.map(k => fmt(t[k])); const worst = Math.max(...r.split(' ').map((v, i) => diffMin(Math.round(t[K6[i]]), toM(v))));
  if (worst > 1) fail++;
  out.push(`| ${n} | ${ds} | ${calc.join(' ')} | ${r} | ${worst} dk ${worst > 1 ? '❌' : '✅'} |`);
}

const cities = [
  ['İstanbul', 41.0082, 28.9784, 'Europe/Istanbul', 'DIYANET'], ['Ankara', 39.9334, 32.8597, 'Europe/Istanbul', 'DIYANET'],
  ['Londra', 51.5074, -0.1278, 'Europe/London', 'MWL'], ['Berlin', 52.52, 13.405, 'Europe/Berlin', 'MWL'],
  ['New York', 40.7128, -74.006, 'America/New_York', 'ISNA'],
];
const tzOffset = (tz, date) => { const s = new Date(date).toLocaleString('en-US', { timeZone: tz, timeZoneName: 'longOffset' }); const m = s.match(/GMT([+-]\d{2}):?(\d{2})?/); return m ? (+m[1] + Math.sign(+m[1] || 1) * (+(m[2] || 0)) / 60) : 0; };
out.push('\n## 2. AlAdhan API ile canlı karşılaştırma\n', 'Tolerans ±2 dk. Not: AlAdhan'ın Diyanet yöntemi (13) Diyanet'in temkin sürelerini birebir uygulamaz; Diyanet doğruluğu 1. bölümde resmî tabloyla ölçülür, buradaki Diyanet satırları bilgi amaçlıdır.\n',
  '| Şehir | Yöntem | Tarih | Uygulama (imsak güneş öğle ikindi akşam yatsı) | AlAdhan | En büyük fark | Sonuç |', '|---|---|---|---|---|---|---|');
const today = new Date(); const ds = `${String(today.getUTCDate()).padStart(2, '0')}-${String(today.getUTCMonth() + 1).padStart(2, '0')}-${today.getUTCFullYear()}`;
let aladhanOk = 0;
for (const [n, lat, lng, tz, method] of cities) {
  for (const asr of ['STANDARD', 'HANAFI']) {
    const url = `https://api.aladhan.com/v1/timings/${ds}?latitude=${lat}&longitude=${lng}&method=${ALADHAN_ID[method]}&school=${asr === 'HANAFI' ? 1 : 0}&timezonestring=${encodeURIComponent(tz)}`;
    try {
      const j = await (await fetch(url, { headers: { 'User-Agent': 'NurVaktiGlobal-CI' } })).json();
      const at = j.data.timings;
      const off = tzOffset(tz, Date.now());
      const t = computeMinutes({ y: today.getUTCFullYear(), m: today.getUTCMonth() + 1, d: today.getUTCDate() }, lat, lng, off, { method, asr, highLat: 'ANGLE' });
      const theirs = [at.Fajr, at.Sunrise, at.Dhuhr, at.Asr, at.Maghrib, at.Isha].map(s => s.slice(0, 5));
      const mine = K6.map(k => fmt(t[k]));
      const worst = Math.max(...theirs.map((v, i) => diffMin(Math.round(t[K6[i]]), toM(v))));
      const info = method === 'DIYANET'; const tol = 2; if (!info && worst > tol) fail++;
      aladhanOk++;
      out.push(`| ${n} | ${method}/${asr === 'HANAFI' ? 'Hanefi' : 'Şafii'} | ${ds} | ${mine.join(' ')} | ${theirs.join(' ')} | ${worst} dk | ${info ? 'ℹ️ bilgi (Diyanet doğruluğu 1. bölümde resmî tabloyla ölçülür)' : worst <= tol ? '✅' : '❌'} |`);
    } catch (e) { out.push(`| ${n} | ${method} | ${ds} | — | AlAdhan'a ulaşılamadı: ${e.message} | — | ⚠️ test edilemedi |`); }
  }
}

out.push('\n## 3. Hanefi / Şafii ikindi farkı (İstanbul, Diyanet)\n');
for (const mm of [1, 4, 7, 10]) {
  const a = computeMinutes({ y: 2026, m: mm, d: 15 }, 41.0082, 28.9784, 3, { method: 'DIYANET' });
  const b = computeMinutes({ y: 2026, m: mm, d: 15 }, 41.0082, 28.9784, 3, { method: 'DIYANET', asr: 'HANAFI' });
  const d = b.asr - a.asr; if (d < 25 || d > 90) fail++;
  out.push(`- 15.${String(mm).padStart(2, '0')}.2026: Şafii ${fmt(a.asr)} · Hanefi ${fmt(b.asr)} · fark ${d.toFixed(0)} dk ${d >= 25 && d <= 90 ? '✅' : '❌'}`);
}
out.push('\n## 4. Kıble\n', `- İstanbul kıble açısı ${qiblaBearing(41.0082, 28.9784).toFixed(2)}° (beklenen ≈151.6°), Kâbe'ye ${distanceKm(41.0082, 28.9784).toFixed(0)} km`,
  `- New York kıble açısı ${qiblaBearing(40.7128, -74.006).toFixed(2)}° (beklenen ≈58.5°)`);
if (Math.abs(qiblaBearing(41.0082, 28.9784) - 151.6) > 0.5 || Math.abs(qiblaBearing(40.7128, -74.006) - 58.5) > 0.7) fail++;

// Kotlin karşılaştırması için golden dosya
const golden = [];
for (const [n, lat, lng, tz, method] of [...cities, ['Oslo', 59.9139, 10.7522, 'Europe/Oslo', 'MWL'], ['Mekke', 21.4225, 39.8262, 'Asia/Riyadh', 'MAKKAH'], ['Cakarta', -6.2088, 106.8456, 'Asia/Jakarta', 'KEMENAG'], ['Tahran', 35.6892, 51.389, 'Asia/Tehran', 'TEHRAN']]) {
  for (const date of ['2026-01-15', '2026-06-21', '2026-11-20']) for (const asr of ['STANDARD', 'HANAFI']) {
    const [y, m, d] = date.split('-').map(Number); const off = tzOffset(tz, Date.UTC(y, m - 1, d, 12));
    const t = computeMinutes({ y, m, d }, lat, lng, off, { method, asr, highLat: 'ANGLE' });
    golden.push({ name: n, lat, lng, tz: off, date, method, asr, highLat: 'ANGLE', times: Object.fromEntries(Object.entries(t).map(([k, v]) => [k, fmt(v)])) });
  }
}
fs.mkdirSync(new URL('../shared/tests/', import.meta.url), { recursive: true });
fs.writeFileSync(new URL('../shared/tests/golden_js.json', import.meta.url), JSON.stringify(golden));
fs.mkdirSync('test-results', { recursive: true });
fs.writeFileSync('test-results/times.md', `# Namaz vakti testleri\n\nTarih: ${new Date().toISOString()}\n\n` + out.join('\n') + `\n\n**Sonuç: ${fail === 0 ? 'TÜM TESTLER GEÇTİ' : fail + ' HATA'}** (AlAdhan canlı test sayısı: ${aladhanOk})\n`);
console.log(out.join('\n')); console.log('FAIL', fail);
process.exit(fail ? 1 : 0);
