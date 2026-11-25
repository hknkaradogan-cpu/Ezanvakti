// NUR VAKTİ GLOBAL — Namaz vakti hesaplama motoru (cihaz üzerinde, internetsiz).
// Standart astronomik formüller (NOAA / USNO güneş konumu yaklaşımı). Android tarafında
// PrayerCalc.kt bu dosyanın birebir karşılığıdır; ikisi aynı test dosyasıyla doğrulanır.
export const METHODS = {
  DIYANET:   { name: 'Diyanet İşleri Başkanlığı (Türkiye)', fajr: 18,   isha: 17,   tune: { sunrise: -7, dhuhr: 5, asr: 4.6, maghrib: 8.1, isha: 1.5, fajr: 0 } },
  MWL:       { name: 'Muslim World League',               fajr: 18,   isha: 17 },
  ISNA:      { name: 'ISNA (Kuzey Amerika)',               fajr: 15,   isha: 15 },
  EGYPT:     { name: 'Egyptian General Authority',         fajr: 19.5, isha: 17.5 },
  MAKKAH:    { name: 'Umm Al-Qura (Mekke)',                fajr: 18.5, ishaMin: 90 },
  KARACHI:   { name: 'University of Islamic Sciences, Karachi', fajr: 18, isha: 18 },
  TEHRAN:    { name: 'Institute of Geophysics, Tehran',    fajr: 17.7, isha: 14, maghribAngle: 4.5, jafari: true },
  JAKIM:     { name: 'JAKIM (Malezya)',                    fajr: 20,   isha: 18 },
  SINGAPORE: { name: 'MUIS (Singapur)',                    fajr: 20,   isha: 18 },
  KEMENAG:   { name: 'Kemenag (Endonezya)',                fajr: 20,   isha: 18 },
  KUWAIT:    { name: 'Kuwait',                             fajr: 18,   isha: 17.5 },
  QATAR:     { name: 'Qatar',                              fajr: 18,   ishaMin: 90 },
  DUBAI:     { name: 'Dubai (BAE)',                        fajr: 18.2, isha: 18.2 },
  GULF:      { name: 'Gulf Region',                        fajr: 19.5, ishaMin: 90 },
  FRANCE:    { name: 'UOIF (Fransa)',                      fajr: 12,   isha: 12 },
  RUSSIA:    { name: 'Rusya Müslümanları Dini İdaresi',     fajr: 16,   isha: 15 },
};
// AlAdhan API'deki karşılık gelen yöntem numaraları (çevrimiçi doğrulama için)
export const ALADHAN_ID = { DIYANET: 13, MWL: 3, ISNA: 2, EGYPT: 5, MAKKAH: 4, KARACHI: 1, TEHRAN: 7, JAKIM: 17, SINGAPORE: 11, KEMENAG: 20, KUWAIT: 9, QATAR: 10, DUBAI: 16, GULF: 8, FRANCE: 12, RUSSIA: 14 };
export const PRAYERS = ['imsak', 'fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'];

const D2R = Math.PI / 180, R2D = 180 / Math.PI;
const sin = d => Math.sin(d * D2R), cos = d => Math.cos(d * D2R), tan = d => Math.tan(d * D2R);
const asin = x => R2D * Math.asin(x), acos = x => R2D * Math.acos(x), atan2 = (y, x) => R2D * Math.atan2(y, x);
const acot = x => R2D * Math.atan(1 / x);
const fix = (a, b) => { a = a - b * Math.floor(a / b); return a < 0 ? a + b : a; };

function julian(y, m, d) {
  if (m <= 2) { y -= 1; m += 12; }
  const A = Math.floor(y / 100), B = 2 - A + Math.floor(A / 4);
  return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + B - 1524.5;
}
function sunPosition(jd) {
  const D = jd - 2451545.0;
  const g = fix(357.529 + 0.98560028 * D, 360);
  const q = fix(280.459 + 0.98564736 * D, 360);
  const L = fix(q + 1.915 * sin(g) + 0.020 * sin(2 * g), 360);
  const e = 23.439 - 0.00000036 * D;
  const RA = atan2(cos(e) * sin(L), cos(L)) / 15;
  const eqt = q / 15 - fix(RA, 24);
  const decl = asin(sin(e) * sin(L));
  return { decl, eqt };
}

/**
 * @param {Date|{y,m,d}} date  yerel takvim günü
 * @param {number} lat @param {number} lng
 * @param {number} tz  saat farkı (saat, ör. +3)
 * @param {object} opt { method, asr:'STANDARD'|'HANAFI', highLat:'NONE'|'MIDDLE'|'SEVENTH'|'ANGLE', elevation, offsets:{fajr..} }
 * @returns {object} her vakit için gün başından itibaren dakika (ondalık) değerleri
 */
export function computeMinutes(date, lat, lng, tz, opt = {}) {
  const y = date.y ?? date.getFullYear(), m = date.m ?? date.getMonth() + 1, d = date.d ?? date.getDate();
  const method = METHODS[opt.method || 'MWL'];
  const asrFactor = opt.asr === 'HANAFI' ? 2 : 1;
  const highLat = opt.highLat || 'ANGLE';
  const jDate = julian(y, m, d) - lng / (15 * 24);
  const elev = opt.elevation || 0;
  const riseAngle = 0.833 + 0.0347 * Math.sqrt(Math.max(0, elev));

  const midDay = t => { const { eqt } = sunPosition(jDate + t); return fix(12 - eqt, 24); };
  const angleTime = (angle, t, ccw) => {
    const { decl } = sunPosition(jDate + t);
    const noon = midDay(t);
    const v = (-sin(angle) - sin(decl) * sin(lat)) / (cos(decl) * cos(lat));
    const T = acos(Math.max(-1, Math.min(1, v))) / 15;
    const invalid = v < -1 || v > 1;
    return { time: noon + (ccw ? -T : T), invalid };
  };
  const asrTime = (factor, t) => {
    const { decl } = sunPosition(jDate + t);
    const angle = -acot(factor + tan(Math.abs(lat - decl)));
    return angleTime(angle, t, false).time;
  };

  // iki geçişli hesap (ilk tahmin → gerçek gün kesri)
  let t = { fajr: 5, sunrise: 6, dhuhr: 12, asr: 13, sunset: 18, maghrib: 18, isha: 18 };
  let inv = {};
  for (let i = 0; i < 2; i++) {
    const p = k => t[k] / 24;
    const f = angleTime(method.fajr, p('fajr'), true);
    const sr = angleTime(riseAngle, p('sunrise'), true);
    const ss = angleTime(riseAngle, p('sunset'), false);
    const mg = method.maghribAngle ? angleTime(method.maghribAngle, p('maghrib'), false) : ss;
    const is = method.isha ? angleTime(method.isha, p('isha'), false) : { time: NaN, invalid: false };
    t = { fajr: f.time, sunrise: sr.time, dhuhr: midDay(p('dhuhr')), asr: asrTime(asrFactor, p('asr')), sunset: ss.time, maghrib: mg.time, isha: is.time };
    inv = { fajr: f.invalid, isha: is.invalid };
  }
  // saat dilimi
  for (const k in t) t[k] += tz - lng / 15;
  if (method.ishaMin) t.isha = t.maghrib + method.ishaMin / 60;

  // yüksek enlem düzeltmesi
  if (highLat !== 'NONE') {
    const night = fix(t.sunrise - t.sunset, 24);
    const portion = a => highLat === 'MIDDLE' ? 0.5 : highLat === 'SEVENTH' ? 1 / 7 : a / 60;
    const fp = portion(method.fajr) * night;
    if (inv.fajr || isNaN(t.fajr) || fix(t.sunrise - t.fajr, 24) > fp) t.fajr = t.sunrise - fp;
    if (!method.ishaMin) {
      const ip = portion(method.isha) * night;
      if (inv.isha || isNaN(t.isha) || fix(t.isha - t.sunset, 24) > ip) t.isha = t.sunset + ip;
    }
  }
  const tune = Object.assign({}, method.tune || {}, opt.offsets || {});
  const out = {
    imsak: t.fajr * 60 + (tune.imsak ?? tune.fajr ?? 0),
    fajr: t.fajr * 60 + (tune.fajr || 0) + (opt.fajrAfterImsak || 0),
    sunrise: t.sunrise * 60 + (tune.sunrise || 0),
    dhuhr: t.dhuhr * 60 + (tune.dhuhr || 0),
    asr: t.asr * 60 + (tune.asr || 0),
    maghrib: t.maghrib * 60 + (tune.maghrib || 0),
    isha: t.isha * 60 + (tune.isha || 0),
  };
  return out;
}

/** Dakikaları en yakın dakikaya yuvarlayıp "HH:MM" döndürür (Diyanet usulü yukarı yuvarlama yok, en yakın). */
export function fmt(min) {
  if (!isFinite(min)) return '--:--';
  let m = Math.round(min); m = ((m % 1440) + 1440) % 1440;
  return String(Math.floor(m / 60)).padStart(2, '0') + ':' + String(m % 60).padStart(2, '0');
}

/** Belirli bir gün için Date nesneleri (gerçek an) döndürür. tz saat cinsinden. */
export function computeDates(y, m, d, lat, lng, tz, opt) {
  const mins = computeMinutes({ y, m, d }, lat, lng, tz, opt);
  const base = Date.UTC(y, m - 1, d) - tz * 3600000;
  const res = {};
  for (const k of PRAYERS) res[k] = new Date(base + Math.round(mins[k]) * 60000);
  return res;
}

// ---------- Kıble ----------
export const KAABA = { lat: 21.4224779, lng: 39.8262136 };
export function qiblaBearing(lat, lng) {
  const dL = (KAABA.lng - lng) * D2R, p1 = lat * D2R, p2 = KAABA.lat * D2R;
  const b = Math.atan2(Math.sin(dL), Math.cos(p1) * Math.tan(p2) - Math.sin(p1) * Math.cos(dL));
  return (b * R2D + 360) % 360;
}
export function distanceKm(lat1, lng1, lat2 = KAABA.lat, lng2 = KAABA.lng) {
  const R = 6371.0088, dp = (lat2 - lat1) * D2R, dl = (lng2 - lng1) * D2R;
  const a = Math.sin(dp / 2) ** 2 + Math.cos(lat1 * D2R) * Math.cos(lat2 * D2R) * Math.sin(dl / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(a));
}
