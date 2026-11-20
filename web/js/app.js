// NUR VAKTİ GLOBAL — iPhone ve bilgisayar için web uygulaması (PWA). Android sürümüyle aynı veri ve hesaplama motorunu kullanır.
import { METHODS, ALADHAN_ID, computeMinutes, fmt, qiblaBearing, distanceKm, KAABA } from './praytimes.js';

// ------------------------------------------------------------------ durum
const DEF = {
  onboarded: false, lang: 'tr', theme: 'auto',
  place: { name: 'İstanbul', region: 'Türkiye', lat: 41.0082, lng: 28.9784, tz: 'Europe/Istanbul', country: 'TR', gps: false },
  method: 'DIYANET', autoMethod: true, asr: 'STANDARD', highLat: 'ANGLE', offsets: {}, hijriAdjust: 0,
  alerts: { fajr: { mode: 'FULL', sound: 'klasik', pre: 0 }, dhuhr: { mode: 'FULL', sound: 'klasik', pre: 0 }, asr: { mode: 'FULL', sound: 'klasik', pre: 0 },
    maghrib: { mode: 'FULL', sound: 'klasik', pre: 0 }, isha: { mode: 'FULL', sound: 'klasik', pre: 0 }, imsak: { mode: 'OFF', sound: 'klasik', pre: 0 }, sunrise: { mode: 'OFF', sound: 'klasik', pre: 0 } },
  reciter: 'alafasy', translation: 'tr', translit: false, font: 26, last: [1, 1], zikirGoal: 100, prayerGoal: 5, gold: 0, silver: 0, cur: 'TRY',
};
const load = (k, d) => { try { const v = JSON.parse(localStorage.getItem(k)); return v ?? d; } catch { return d; } };
const save = (k, v) => { try { localStorage.setItem(k, JSON.stringify(v)); } catch {} };
let S = Object.assign(structuredClone(DEF), load('nv_settings', {}));
S.alerts = Object.assign(structuredClone(DEF.alerts), S.alerts || {});
const DB = { logs: load('nv_logs', {}), kaza: load('nv_kaza', {}), fasts: load('nv_fasts', {}), zikir: load('nv_zikir', []), customZikir: load('nv_czikir', []),
  bookmarks: load('nv_bm', []), favs: load('nv_favs', []), places: load('nv_places', []) };
const saveDB = k => save('nv_' + ({ logs: 'logs', kaza: 'kaza', fasts: 'fasts', zikir: 'zikir', customZikir: 'czikir', bookmarks: 'bm', favs: 'favs', places: 'places' })[k], DB[k]);
const setS = (f) => { S = Object.assign(S, typeof f === 'function' ? f(S) : f); save('nv_settings', S); applyTheme(); };

// ------------------------------------------------------------------ veri
const cache = {};
const data = async p => cache[p] ??= fetch('data/' + p).then(r => { if (!r.ok) throw new Error(p + ' ' + r.status); return r.json(); });
let I18N = {};
const t = s => (S.lang === 'tr' ? s : (I18N[s]?.[S.lang] || s));
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);
const $ = s => document.querySelector(s);
const PN = { imsak: 'İmsak', fajr: 'Sabah', sunrise: 'Güneş', dhuhr: 'Öğle', asr: 'İkindi', maghrib: 'Akşam', isha: 'Yatsı' };
const pn = k => t(PN[k]);
const norm = s => String(s).toLocaleLowerCase('tr').normalize('NFD').replace(/\p{M}/gu, '').replace(/ı/g, 'i');
function toast(m) { const d = document.createElement('div'); d.className = 'toast'; d.textContent = t(m); document.body.appendChild(d); setTimeout(() => d.remove(), 3200); }
const locale = () => ({ tr: 'tr-TR', en: 'en-GB', ar: 'ar' })[S.lang];

// ------------------------------------------------------------------ vakit hesabı (Android ile aynı motor)
const defaultMethod = cc => ({ TR: 'DIYANET', AZ: 'DIYANET', CY: 'DIYANET', BA: 'DIYANET', MK: 'DIYANET', XK: 'DIYANET', AL: 'DIYANET', BG: 'DIYANET', US: 'ISNA', CA: 'ISNA', MX: 'ISNA',
  SA: 'MAKKAH', YE: 'MAKKAH', EG: 'EGYPT', SD: 'EGYPT', LY: 'EGYPT', SY: 'EGYPT', LB: 'EGYPT', IQ: 'EGYPT', JO: 'EGYPT', PS: 'EGYPT', PK: 'KARACHI', IN: 'KARACHI', BD: 'KARACHI', AF: 'KARACHI',
  IR: 'TEHRAN', MY: 'JAKIM', BN: 'JAKIM', SG: 'SINGAPORE', ID: 'KEMENAG', KW: 'KUWAIT', QA: 'QATAR', AE: 'DUBAI', BH: 'GULF', OM: 'GULF', FR: 'FRANCE', RU: 'RUSSIA', KZ: 'RUSSIA', UZ: 'RUSSIA', TJ: 'RUSSIA', KG: 'RUSSIA', TM: 'RUSSIA' })[cc] || 'MWL';
const methodId = () => S.autoMethod ? defaultMethod(S.place.country) : S.method;
function tzOffsetH(tz, ms) {
  try { const p = new Intl.DateTimeFormat('en-US', { timeZone: tz, timeZoneName: 'longOffset' }).formatToParts(new Date(ms)).find(x => x.type === 'timeZoneName').value;
    const m = p.match(/GMT([+-])(\d{1,2}):?(\d{2})?/); return m ? (m[1] === '-' ? -1 : 1) * (+m[2] + (+(m[3] || 0)) / 60) : 0; } catch { return -new Date(ms).getTimezoneOffset() / 60; }
}
/** Bulunulan saat dilimindeki takvim günü (y,m,d) */
function zonedYMD(ms, tz) { const p = new Intl.DateTimeFormat('en-CA', { timeZone: tz, year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date(ms)).split('-').map(Number); return { y: p[0], m: p[1], d: p[2] }; }
const ymdStr = o => `${o.y}-${String(o.m).padStart(2, '0')}-${String(o.d).padStart(2, '0')}`;
const addDays = (o, n) => { const d = new Date(Date.UTC(o.y, o.m - 1, o.d + n)); return { y: d.getUTCFullYear(), m: d.getUTCMonth() + 1, d: d.getUTCDate() }; };
function dayTimes(o) {
  const tz = S.place.tz; const off = tzOffsetH(tz, Date.UTC(o.y, o.m - 1, o.d, 12));
  const mins = computeMinutes(o, S.place.lat, S.place.lng, off, { method: methodId(), asr: S.asr, highLat: S.highLat, offsets: S.offsets });
  const base = Date.UTC(o.y, o.m - 1, o.d) - off * 3600000; const res = { date: o, mins };
  for (const k in mins) res[k] = base + Math.round(mins[k]) * 60000;
  return res;
}
const hhmm = (d, k) => fmt(d.mins[k]);
const todayYMD = () => zonedYMD(Date.now(), S.place.tz);
function nextPrayer(now = Date.now()) {
  const keys = ['fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'];
  for (let i = 0; i < 2; i++) { const d = dayTimes(addDays(todayYMD(), i)); const k = keys.find(k => d[k] > now); if (k) return [k, d[k]]; }
  return ['fajr', dayTimes(addDays(todayYMD(), 1)).fajr];
}
function currentPrayer(now = Date.now()) { const d = dayTimes(todayYMD()); return ['isha', 'maghrib', 'asr', 'dhuhr', 'sunrise', 'fajr'].find(k => d[k] <= now) || null; }
const sourceLabel = () => (t('Cihaz üzerinde hesaplama') + ' · ' + METHODS[methodId()].name);

// ------------------------------------------------------------------ Hicri takvim (Ümmü'l-Kurâ)
const HM = { tr: ['Muharrem', 'Safer', 'Rebîülevvel', 'Rebîülâhir', 'Cemâziyelevvel', 'Cemâziyelâhir', 'Recep', 'Şaban', 'Ramazan', 'Şevval', 'Zilkade', 'Zilhicce'],
  en: ['Muharram', 'Safar', "Rabi' al-Awwal", "Rabi' al-Thani", 'Jumada al-Ula', 'Jumada al-Akhirah', 'Rajab', "Sha'ban", 'Ramadan', 'Shawwal', "Dhu al-Qa'dah", 'Dhu al-Hijjah'],
  ar: ['مُحَرَّم', 'صَفَر', 'رَبِيع الأَوَّل', 'رَبِيع الآخِر', 'جُمَادَى الأُولَى', 'جُمَادَى الآخِرَة', 'رَجَب', 'شَعْبَان', 'رَمَضَان', 'شَوَّال', 'ذُو القَعْدَة', 'ذُو الحِجَّة'] };
const hfmt = new Intl.DateTimeFormat('en-u-ca-islamic-umalqura-nu-latn', { timeZone: 'UTC', day: 'numeric', month: 'numeric', year: 'numeric' });
function hijri(o, adj = S.hijriAdjust) { const d = new Date(Date.UTC(o.y, o.m - 1, o.d + adj, 12)); const p = Object.fromEntries(hfmt.formatToParts(d).map(x => [x.type, x.value]));
  return { d: +p.day, m: +p.month, y: parseInt(p.year) }; }
const hijriStr = h => `${h.d} ${HM[S.lang][h.m - 1]} ${h.y}`;
function religiousDays(year) {
  const res = []; let o = { y: year, m: 1, d: 1 }; o = addDays(o, -1); let reg = -1;
  while (o.y <= year) {
    const n = addDays(o, 1), h = hijri(o), hn = hijri(n); const add = (key, tr, night, hd) => { if (o.y === year) res.push({ date: ymdStr(o), key, tr, night, h: hd }); };
    if (h.m === 1 && h.d === 1) add('hijri_new', 'Hicri Yılbaşı', false, h);
    if (h.m === 1 && h.d === 10) add('ashura', 'Aşure Günü', false, h);
    if (h.m === 7 && h.d === 1) add('uc', 'Üç Ayların Başlangıcı', false, h);
    if (h.m === 9 && h.d === 1) add('ram', 'Ramazan Başlangıcı', false, h);
    if (hn.m === 10 && hn.d === 1) add('arefe_r', 'Ramazan Bayramı Arefesi', false, h);
    if (h.m === 10 && h.d <= 3) add('fitr', `Ramazan Bayramı ${h.d}. Gün`, false, h);
    if (h.m === 12 && h.d === 9) add('arefe', 'Kurban Bayramı Arefesi', false, h);
    if (h.m === 12 && h.d >= 10 && h.d <= 13) add('adha', `Kurban Bayramı ${h.d - 9}. Gün`, false, h);
    if (hn.m === 3 && hn.d === 12) add('mevlid', 'Mevlid Kandili', true, hn);
    if (hn.m === 7 && hn.d <= 7 && new Date(Date.UTC(n.y, n.m - 1, n.d)).getUTCDay() === 5 && reg !== hn.y) { add('regaib', 'Regaib Kandili', true, hn); reg = hn.y; }
    if (hn.m === 7 && hn.d === 27) add('mirac', 'Miraç Kandili', true, hn);
    if (hn.m === 8 && hn.d === 15) add('berat', 'Berat Kandili', true, hn);
    if (hn.m === 9 && hn.d === 27) add('kadir', 'Kadir Gecesi', true, hn);
    o = n;
  }
  return res;
}

// ------------------------------------------------------------------ ses: Kur'an oynatıcı + ezan
const audio = new Audio(); audio.preload = 'none';
const P = { list: [], i: 0, reciter: null, repeat: 'off' };
async function audioUrl(url) { // indirildiyse (Cache Storage) internetsiz çal
  try { const r = await caches.match(url, { ignoreSearch: true }); if (r && r.status === 200 && r.type !== 'opaque') return URL.createObjectURL(await r.blob()); } catch {}
  return url;
}
async function playIndex(i) {
  P.i = i; const it = P.list[i]; if (!it) return;
  audio.src = await audioUrl(it.url); audio.play().catch(e => toast('Ses başlatılamadı: ' + e.message));
  if ('mediaSession' in navigator) {
    navigator.mediaSession.metadata = new MediaMetadata({ title: it.title, artist: it.artist, album: "Kur'an-ı Kerim", artwork: [{ src: 'icons/icon-512.png', sizes: '512x512', type: 'image/png' }] });
  }
  renderPlayer();
}
async function playSurahs(reciterId, surahs, start) {
  const rs = (await data('media/reciters.json')).reciters; const r = rs.find(x => x.id === reciterId) || rs[0]; const su = await data('quran/surahs.json');
  P.reciter = r; P.list = surahs.filter(n => r.surahs.includes(n)).map(n => ({ n, url: r.url.replace('{n}', n), title: `${n}. ${su[n - 1].tr} ${t('Suresi')}`, artist: r.tr }));
  if (!P.list.length) return toast('Bu hafızın bu sure kaydı mevcut değil');
  playIndex(Math.max(0, P.list.findIndex(x => x.n === start)));
}
audio.addEventListener('ended', () => { if (P.repeat === 'one') return playIndex(P.i); if (P.i + 1 < P.list.length) return playIndex(P.i + 1); if (P.repeat === 'all') return playIndex(0); renderPlayer(); });
audio.addEventListener('error', () => { if (audio.src) { toast("Ses açılamadı. İnternet bağlantınızı veya indirmeyi kontrol edin."); renderPlayer(); } });
['play', 'pause', 'timeupdate', 'loadedmetadata'].forEach(e => audio.addEventListener(e, () => renderPlayer(e === 'timeupdate')));
if ('mediaSession' in navigator) {
  const ms = navigator.mediaSession;
  ms.setActionHandler('play', () => audio.play()); ms.setActionHandler('pause', () => audio.pause());
  ms.setActionHandler('nexttrack', () => P.i + 1 < P.list.length && playIndex(P.i + 1)); ms.setActionHandler('previoustrack', () => P.i > 0 && playIndex(P.i - 1));
  ms.setActionHandler('seekbackward', () => audio.currentTime -= 10); ms.setActionHandler('seekforward', () => audio.currentTime += 10);
}
const mmss = s => { s = Math.max(0, Math.floor(s || 0)); return s >= 3600 ? `${Math.floor(s / 3600)}:${String(Math.floor(s / 60) % 60).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}` : `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`; };
function renderPlayer(onlyTime) {
  const el = $('#player'); const it = P.list[P.i];
  el.classList.toggle('on', !!it);
  if (!it) return;
  if (onlyTime && el.querySelector('#pt')) { el.querySelector('#pt').textContent = `${mmss(audio.currentTime)} / ${mmss(audio.duration)}`; const r = el.querySelector('#pr'); if (r && !r.matches(':active')) { r.max = audio.duration || 1; r.value = audio.currentTime; } return; }
  el.innerHTML = `<div class="row"><button class="ib" data-a="pprev" title="${t('Önceki sure')}">⏮</button><button class="ib" data-a="pback">↺</button>
   <button class="ib" data-a="ptoggle" style="font-size:28px">${audio.paused ? '▶' : '⏸'}</button><button class="ib" data-a="pfwd">↻</button><button class="ib" data-a="pnext" title="${t('Sonraki sure')}">⏭</button>
   <div class="grow"><div style="font-weight:600;white-space:nowrap;overflow:hidden;text-overflow:ellipsis">${esc(it.title)}</div><div class="mut" style="color:#cfd8e6">${esc(it.artist)} · <span id="pt">${mmss(audio.currentTime)} / ${mmss(audio.duration)}</span></div></div>
   <button class="ib" data-a="prepeat" title="${t('Tekrar')}">${P.repeat === 'one' ? '🔂' : P.repeat === 'all' ? '🔁' : '➡️'}</button><button class="ib" data-a="pclose">✕</button></div>
   <div class="row"><input id="pr" type="range" min="0" step="1" max="${audio.duration || 1}" value="${audio.currentTime}" style="flex:1"><span>🔊</span><input id="pv" type="range" min="0" max="1" step="0.05" value="${audio.volume}" style="width:90px"></div>`;
}

const ezAudio = new Audio();
async function ezanSrc(id) { const e = (await data('media/ezan.json')).ezan.find(x => x.id === id) || { file: 'klasik.mp3' }; const url = new URL('audio/ezan/' + e.file, location.href).href; return audioUrl(url); }
async function playEzan(id) { ezAudio.src = await ezanSrc(id); ezAudio.play().catch(() => toast('Tarayıcı sesi engelledi. Ezanın çalabilmesi için sayfaya bir kez dokunun.')); }

// ------------------------------------------------------------------ uygulama açıkken ezan/bildirim zamanlayıcısı (web sınırlaması açıklanır)
const fired = new Set(load('nv_fired', []));
function alarmTick() {
  const now = Date.now(); const d = dayTimes(todayYMD());
  for (const k of ['imsak', 'fajr', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha']) {
    const a = S.alerts[k]; if (!a) continue;
    const ev = [[d[k], 0]]; if (a.pre) ev.push([d[k] - a.pre * 60000, a.pre]);
    for (const [at, pre] of ev) {
      const key = `${k}-${pre}-${at}`;
      if (now >= at && now - at < 120000 && !fired.has(key)) {
        fired.add(key); save('nv_fired', [...fired].slice(-80));
        if (pre) notifyUser(`${pn(k)} ${t('vaktine')} ${pre} ${t('dakika')}`, S.place.name);
        else if (a.mode !== 'OFF') {
          notifyUser(`${pn(k)} ${t('vakti girdi')}`, `${S.place.name} · ${hhmm(d, k)}`);
          if (a.mode === 'FULL') playEzan(a.sound); else if (a.mode === 'SHORT') playEzan('kisa'); else if (a.mode === 'VIBRATE' && navigator.vibrate) navigator.vibrate([600, 300, 600]);
        }
      }
    }
  }
}
function notifyUser(title, body) {
  if ('Notification' in window && Notification.permission === 'granted') {
    navigator.serviceWorker?.ready.then(r => r.showNotification(title, { body, icon: 'icons/icon-192.png', badge: 'icons/icon-192.png', tag: title })).catch(() => new Notification(title, { body }));
  } else toast(title + ' — ' + body);
}

// ------------------------------------------------------------------ yönlendirme
const routes = {};
const go = h => { location.hash = h; };
function nav(active) {
  const items = [['home', '🏠', 'Ana sayfa'], ['times', '🕰️', 'Vakitler'], ['quran', '📖', "Kur'an"], ['qibla', '🧭', 'Kıble'], ['menu', '☰', 'Menü']];
  return `<nav class="nav">${items.map(([r, i, l]) => `<a href="#/${r}" class="${active === r ? 'on' : ''}"><span>${i}</span>${t(l)}</a>`).join('')}</nav>`;
}
const top = (title, back = true, extra = '') => `<div class="top">${back ? `<button class="ib" data-a="back" aria-label="${t('Geri')}">${S.lang === 'ar' ? '→' : '←'}</button>` : ''}<h1>${esc(title)}</h1>${extra}</div>`;
let cleanup = null;
async function render() {
  if (cleanup) { cleanup(); cleanup = null; }
  const [r, ...args] = (location.hash.replace(/^#\/?/, '') || 'home').split('/');
  const fn = routes[r] || routes.home;
  document.documentElement.lang = S.lang; document.documentElement.dir = S.lang === 'ar' ? 'rtl' : 'ltr';
  const main = S.onboarded && ['home', 'times', 'quran', 'qibla', 'menu'].includes(r) ? r : null;
  try { $('#app').innerHTML = (await fn(...args)) + (main ? nav(main) : ''); }
  catch (e) { $('#app').innerHTML = top(t('Hata')) + `<div class="view"><div class="card err">${esc(e.message)}</div></div>`; console.error(e); }
  window.scrollTo(0, 0);
}
function applyTheme() { document.documentElement.dataset.theme = S.theme; }

// ------------------------------------------------------------------ EKRANLAR
const MODES = { FULL: 'Tam ezan', SHORT: 'Kısa ezan', NOTIFY: 'Normal bildirim', VIBRATE: 'Titreşim', SILENT: 'Sessiz', OFF: 'Kapalı' };
const MODE_IC = { FULL: '🔊', SHORT: '📢', NOTIFY: '🔔', VIBRATE: '📳', SILENT: '🔕', OFF: '🔇' };
const dayIdx = () => Math.floor(Date.now() / 86400000);
const dfmt = (o, opt = { day: 'numeric', month: 'long', year: 'numeric', weekday: 'long' }) => new Date(Date.UTC(o.y, o.m - 1, o.d, 12)).toLocaleDateString(locale(), { ...opt, timeZone: 'UTC' });

routes.onboard = async () => `<div class="view" style="padding-top:40px">
  <div class="hero center"><img src="icons/icon-192.png" width="96" height="96" alt="" style="border-radius:24px"><div class="brand" style="font-size:20px;margin-top:8px">NUR VAKTİ GLOBAL</div><div>${t('Her vakitte, her yerde, huzurla.')}</div></div>
  <div class="card"><h3>${t('Dil / Language / اللغة')}</h3><div class="chips">${[['tr', 'Türkçe'], ['en', 'English'], ['ar', 'العربية']].map(([c, n]) => `<button class="chip ${S.lang === c ? 'on' : ''}" data-a="lang" data-v="${c}">${n}</button>`).join('')}</div>
  <h3>${t('Konum izni')}</h3><p>${t('Namaz vakitlerini ve kıble yönünü bulunduğunuz yere göre hesaplamak için konumunuz kullanılır. Konumunuz yalnızca telefonunuzda işlenir; hiçbir sunucuya gönderilmez.')}</p>
  <button class="btn w" data-a="gps">${t('Konumumu kullan')}</button><p></p><button class="btn o w" data-a="manualloc">${t('Şehri elle seçeceğim')}</button>
  <p class="mut">${t('Web sürümü (iPhone/PC): ezan ve bildirimler yalnızca uygulama açıkken çalar. Ekran kapalıyken ezan için Android uygulamasını kullanın.')}</p>
  <button class="btn w" data-a="startapp">${t('Başla')}</button></div></div>`;

routes.home = async () => {
  if (!S.onboarded) return routes.onboard();
  const [ayet, hadis, duas, esma, zikir] = await Promise.all(['content/ayet.json', 'content/hadis.json', 'content/dualar.json', 'content/esma.json', 'content/zikir.json'].map(data));
  const d = dayTimes(todayYMD()); const cur = currentPrayer(); const i = dayIdx();
  const a = ayet[i % ayet.length], h = hadis[i % hadis.length], du = duas.items[i % duas.items.length], e = esma[i % esma.length], z = zikir[i % zikir.length];
  const tr = S.lang === 'tr';
  const rows = ['imsak', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'].map(k => { const act = k === cur || (k === 'imsak' && cur === 'fajr'); const mk = k === 'imsak' ? 'fajr' : k;
    return `<div class="prow ${act ? 'act' : ''}"><span class="nm">${k === 'imsak' ? pn('imsak') + ' / ' + pn('fajr') : pn(k)}</span><a href="#/ezan" style="text-decoration:none;margin:0 10px">${MODE_IC[S.alerts[mk]?.mode || 'OFF']}</a><b>${hhmm(d, k)}</b></div>`; }).join('');
  const tiles = [['🎧', "Kur'an dinle", 'reciters'], ['📖', 'Yasin dinle', 'reader/36/1/yasin'], ['🧭', 'Kıble bul', 'qibla'], ['📿', 'Zikirmatik', 'zikir'], ['🌙', 'Ramazan', 'ramazan'], ['✅', 'Namaz takibi', 'tracker']];
  setTimeout(tickHome, 0);
  return `${top('', false)}<div class="view">
  <div class="hero"><div class="row"><img src="icons/icon-192.png" width="40" height="40" alt="" style="border-radius:10px"><div class="grow"><div class="brand">NUR VAKTİ GLOBAL</div>
   <div class="city" data-a="nav" data-v="location">📍 ${esc(S.place.name)}</div></div><button class="ib" data-a="nav" data-v="settings" style="color:#fff" aria-label="${t('Ayarlar')}">⚙️</button></div>
   <div>${dfmt(todayYMD())}</div><div class="hijri">${hijriStr(hijri(todayYMD()))}</div>
   <div style="margin-top:16px" id="nextl"></div><div class="count" id="cd">--:--:--</div><div class="center gold" id="curl"></div></div>
  <div class="card">${rows}</div>
  <button class="big" data-a="nav" data-v="reader/36/1/yasin">🎧 ${t('YASİN SURESİ DİNLE')}</button>
  <div class="grid">${tiles.map(([ic, l, r]) => `<button class="tile" data-a="nav" data-v="${r}"><i>${ic}</i>${t(l)}</button>`).join('')}</div>
  <div class="card" data-a="nav" data-v="reader/${a.s}/${a.a}"><h3>${t('Günün ayeti')}</h3><div class="ar" style="font-size:22px">${esc(a.ar)}</div><p>${esc(tr ? a.tr : a.en)}</p><div class="mut">${t('Kaynak')}: ${esc(a.name)} ${a.ref}</div></div>
  <div class="card"><h3>${t('Günün hadisi')}</h3><p>${esc(tr ? h.tr : h.en)}</p><div class="mut">${t('Kaynak')}: ${esc(h.src)}</div></div>
  <div class="card" data-a="nav" data-v="dualar"><h3>${t('Günün duası')}</h3><b>${esc(du.title)}</b><div class="ar" style="font-size:20px">${esc(du.ar)}</div><div class="mut">${esc(du.okunus)}</div><p>${esc(tr ? du.tr : du.en)}</p><div class="mut">${t('Kaynak')}: ${esc(du.src)}</div></div>
  <div class="card" data-a="nav" data-v="esma"><h3>${t('Günün esması')}</h3><div class="ar" style="font-size:32px">${esc(e.ar)}</div><p>${esc(e.okunus)} — ${esc(tr ? e.tr : e.en)}</p><div class="mut">${t('Kaynak')}: Tirmizî, Deavât 82</div></div>
  <div class="card" data-a="nav" data-v="zikir"><h3>${t('Günün zikir önerisi')}</h3><div class="ar">${esc(z.ar)}</div><p>${esc(z.okunus)} × ${z.count} — ${esc(tr ? z.tr : z.en)}</p><div class="mut">${t('Kaynak')}: ${esc(z.src)}</div></div>
  <p class="mut">${t('Veri kaynağı')}: ${esc(sourceLabel())} · ${t(S.asr === 'HANAFI' ? 'Hanefi ikindi' : 'Şafii/Standart ikindi')}</p></div>`;
};
function tickHome() {
  const cd = $('#cd'); if (!cd) return;
  const [k, at] = nextPrayer(); const left = Math.max(0, Math.floor((at - Date.now()) / 1000)); const cur = currentPrayer();
  cd.textContent = `${String(Math.floor(left / 3600)).padStart(2, '0')}:${String(Math.floor(left / 60) % 60).padStart(2, '0')}:${String(left % 60).padStart(2, '0')}`;
  $('#nextl').textContent = `${t('Sonraki vakit')}: ${pn(k)} · ${new Date(at).toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit', timeZone: S.place.tz })}`;
  $('#curl').textContent = cur ? `${t('Şu anki vakit')}: ${pn(cur)}` : t('Gece (yatsı vakti devam ediyor)');
}

routes.times = async (tab = '0', a = '', b = '') => {
  const T = +tab; const now = todayYMD(); let days = [], head = '';
  if (T === 0) days = [dayTimes(now)];
  else if (T === 1) days = [...Array(7)].map((_, i) => dayTimes(addDays(now, i)));
  else if (T === 2) { const y = +a || now.y, m = +b || now.m; const n = new Date(Date.UTC(y, m, 0)).getUTCDate(); days = [...Array(n)].map((_, i) => dayTimes({ y, m, d: i + 1 }));
    const pm = m === 1 ? [y - 1, 12] : [y, m - 1], nm = m === 12 ? [y + 1, 1] : [y, m + 1];
    head = `<div class="row" style="justify-content:space-between"><a class="btn o s" href="#/times/2/${pm[0]}/${pm[1]}">‹</a><b>${new Date(Date.UTC(y, m - 1, 1)).toLocaleDateString(locale(), { month: 'long', year: 'numeric', timeZone: 'UTC' })}</b><a class="btn o s" href="#/times/2/${nm[0]}/${nm[1]}">›</a></div>`; }
  else { const y = +a || now.y; const n = (y % 4 === 0 && y % 100 !== 0) || y % 400 === 0 ? 366 : 365; days = [...Array(n)].map((_, i) => dayTimes(addDays({ y, m: 1, d: 1 }, i)));
    head = `<div class="row" style="justify-content:space-between"><a class="btn o s" href="#/times/3/${y - 1}">‹</a><b>${y}</b><a class="btn o s" href="#/times/3/${y + 1}">›</a></div>`; }
  const tabs = ['Günlük', 'Haftalık', 'Aylık', 'Yıllık'].map((l, i) => `<a class="chip ${i === T ? 'on' : ''}" href="#/times/${i}" style="text-decoration:none">${t(l)}</a>`).join('');
  let body;
  if (T === 0) { const d = days[0]; const m = METHODS[methodId()];
    body = `<div class="card"><h3>${dfmt(now)}</h3>${Object.keys(PN).map(k => `<div class="kv"><span>${pn(k)}</span><b>${hhmm(d, k)}</b></div>`).join('')}</div>
    <div class="card"><h3>${t('Hesaplama')}</h3><div class="kv"><span>${t('Konum')}</span><span>${esc(S.place.name)} (${S.place.lat.toFixed(4)}, ${S.place.lng.toFixed(4)})</span></div>
    <div class="kv"><span>${t('Saat dilimi')}</span><span>${esc(S.place.tz)}</span></div><div class="kv"><span>${t('Yöntem')}</span><span>${esc(m.name)}</span></div>
    <div class="kv"><span>${t('İkindi')}</span><span>${t(S.asr === 'HANAFI' ? 'Hanefi (gölge 2 misli)' : 'Standart / Şafii (gölge 1 misli)')}</span></div>
    <div class="kv"><span>${t('Kaynak')}</span><span>${esc(sourceLabel())}</span></div>
    ${methodId() === 'DIYANET' ? `<p class="mut">${t("Not: Diyanet yöntemi (18°/17° ve temkin süreleri) cihazda hesaplanır ve Diyanet'in yayımladığı resmî vakitlerle test edilmiştir (±1 dk). Resmî Diyanet API'si kullanılmamaktadır; bu API kurumdan alınan kullanıcı adı/şifre gerektirir.")}</p>` : ''}
    <a class="btn o s" href="#/settings">${t('Hesaplama ayarları')}</a></div>`; }
  else { const td = ymdStr(now);
    body = `${head}<table><thead><tr><th>${t('Tarih')}</th>${['imsak', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'].map(k => `<th>${pn(k)}</th>`).join('')}</tr></thead><tbody>${days.map(d => `<tr class="${ymdStr(d.date) === td ? 'today' : ''}"><td>${dfmt(d.date, { day: '2-digit', month: '2-digit', weekday: 'short' })}</td>${['imsak', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'].map(k => `<td>${hhmm(d, k)}</td>`).join('')}</tr>`).join('')}</tbody></table>`; }
  return `${top(t('Namaz vakitleri'), false)}<div class="view"><div class="chips">${tabs}</div>${body}</div>`;
};

routes.menu = async () => {
  const items = [['🎧', 'Hafızlar ve dinleme', 'reciters'], ['📖', 'Yasin-i Şerif', 'reader/36/1/yasin'], ['🔖', 'Yer imleri', 'bookmarks'], ['🔊', 'Ezan ayarları', 'ezan'],
    ['⬇️', 'İndirme merkezi', 'downloads'], ['🤲', 'Dualar', 'dualar'], ['📿', 'Zikirmatik', 'zikir'], ['✨', "Esmâü'l-Hüsnâ", 'esma'], ['🌙', 'Ramazan', 'ramazan'],
    ['📅', 'Dini takvim', 'takvim'], ['✅', 'Namaz takibi', 'tracker'], ['🧮', 'Zekât hesapla', 'zekat'], ['🕌', 'Yakındaki camiler', 'camiler'], ['🎓', 'Namaz rehberi', 'rehber'],
    ['📍', 'Konum', 'location'], ['⚙️', 'Ayarlar', 'settings'], ['📱', 'Android uygulaması', 'android'], ['ℹ️', 'Hakkında ve lisanslar', 'about']];
  return `${top(t('Menü'), false)}<div class="view"><div class="grid">${items.map(([i, l, r]) => `<button class="tile" data-a="nav" data-v="${r}"><i>${i}</i>${t(l)}</button>`).join('')}</div></div>`;
};
routes.android = async () => { location.href = 'indir.html'; return ''; };

// ---- Kur'an
routes.quran = async (q = '') => {
  q = decodeURIComponent(q);
  const su = await data('quran/surahs.json'); const n = norm(q);
  const list = q ? su.filter(s => norm(s.tr).includes(n) || norm(s.tl).includes(n) || s.ar.includes(q) || String(s.n) === q.trim()) : su;
  let hits = '';
  if (q.length >= 3) { const tr = await data(`quran/${S.lang === 'tr' ? 'tr' : 'en'}.json`); const res = [];
    outer: for (let i = 0; i < tr.length; i++) for (let j = 0; j < tr[i].length; j++) if (norm(tr[i][j]).includes(n)) { res.push([i + 1, j + 1, tr[i][j]]); if (res.length >= 100) break outer; }
    hits = `<h3>${res.length} ${t('ayet bulundu')}</h3>` + res.map(([s, a, x]) => `<a class="item" href="#/reader/${s}/${a}"><div class="grow"><b style="color:var(--em)">${esc(su[s - 1].tr)} ${s}:${a}</b><div>${esc(x.slice(0, 220))}</div></div></a>`).join(''); }
  return `${top(t("Kur'an-ı Kerim"), false, `<a class="ib" href="#/bookmarks">🔖</a><a class="ib" href="#/reciters">🎧</a>`)}<div class="view">
   <input id="qs" placeholder="${t('Sure adı veya ayet içinde kelime ara')}" value="${esc(q)}" data-enter="quran">
   <p></p><div class="card" data-a="nav" data-v="reader/${S.last[0]}/${S.last[1]}"><h3>${t('Kaldığın yerden devam')}</h3><b>${esc(su[S.last[0] - 1].tr)} · ${t('Ayet')} ${S.last[1]}</b></div>
   ${hits}<div class="list">${list.map(s => `<a class="item" href="#/reader/${s.n}/1"><span class="num">${s.n}</span><div class="grow"><b>${esc(S.lang === 'tr' ? s.tr : s.tl)}</b><div class="mut">${esc(s.en)} · ${s.ayahs} ${t('ayet')} · ${t(s.type === 'meccan' ? 'Mekkî' : 'Medenî')}</div></div><span class="ar" style="font-size:22px">${s.ar}</span></a>`).join('')}</div>
   <p class="mut">${t("Arapça metin: Kral Fahd Kur'an Basım Kompleksi Uthmani metni (QuranEnc). Türkçe meal: Diyanet İşleri Başkanlığı (tanzil.net). İngilizce: Saheeh International.")}</p></div>`;
};
routes.reader = async (sn = '1', an = '1', mode = '') => {
  const s = +sn, startA = +an, yasin = mode === 'yasin';
  const [su, ar, rs] = await Promise.all([data('quran/surahs.json'), data('quran/ar.json'), data('media/reciters.json')]);
  const tr = S.translation === 'none' ? null : await data(`quran/${S.translation}.json`);
  const tl = (S.translit || yasin) ? await data('quran/translit.json') : null;
  const r = rs.reciters.find(x => x.id === S.reciter) || rs.reciters[0]; const info = su[s - 1];
  S.last = [s, startA]; save('nv_settings', S);
  const bm = (a, k) => DB.bookmarks.some(b => b.s === s && b.a === a && b.k === k);
  setTimeout(() => { const el = document.getElementById('a' + startA); if (el && startA > 1) el.scrollIntoView(); }, 50);
  const obs = new IntersectionObserver(es => es.forEach(e => { if (e.isIntersecting) { S.last = [s, +e.target.id.slice(1)]; save('nv_settings', S); } }), { threshold: .6 });
  setTimeout(() => document.querySelectorAll('[id^=a]').forEach(x => obs.observe(x)), 100); cleanup = () => obs.disconnect();
  return `${top(yasin ? t('Yasin-i Şerif') : `${s}. ${info.tr}`, true, `<button class="ib" data-a="font" data-v="-2">A-</button><button class="ib" data-a="font" data-v="2">A+</button><button class="ib" data-a="night">🌙</button>`)}
  <div class="view"><div class="card" style="background:var(--pc)"><div class="row"><button class="btn" data-a="playsurah" data-v="${s}" data-y="${yasin ? 1 : 0}">▶ ${t('Dinle')}</button>
   <div class="grow"><select data-a="reciter">${rs.reciters.map(x => `<option value="${x.id}" ${x.id === r.id ? 'selected' : ''} ${x.surahs.includes(s) ? '' : 'disabled'}>${esc(x.tr)} (${x.surahs.length}/114)</option>`).join('')}</select></div>
   <button class="btn o s" data-a="dlsurah" data-v="${s}">⬇️ ${t('İndir')}</button></div>
   ${r.surahs.includes(s) ? '' : `<p class="err">${t('Bu hafızın bu sure kaydı yok')}</p>`}</div>
   <div class="center"><div class="ar" style="text-align:center;font-size:32px">${info.ar}</div><b>${esc(info.tr)} · ${info.ayahs} ${t('ayet')}</b>
   ${s !== 1 && s !== 9 ? `<div class="ar" style="text-align:center;font-size:${S.font}px">بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ</div>` : ''}
   ${yasin ? `<p class="mut">${t('Okunuş: Latin harfli transliterasyon (tanzil.net). Meal: Diyanet İşleri Başkanlığı.')}</p>` : ''}</div>
   ${ar[s - 1].map((x, i) => `<div class="card" id="a${i + 1}"><div class="row"><b class="grow" style="color:var(--em)">${s}:${i + 1}</b>
    <button class="ib" data-a="bm" data-s="${s}" data-n="${i + 1}" data-k="b">${bm(i + 1, 'b') ? '🔖' : '📑'}</button><button class="ib" data-a="bm" data-s="${s}" data-n="${i + 1}" data-k="f">${bm(i + 1, 'f') ? '❤️' : '🤍'}</button></div>
    <div class="ar" style="font-size:${S.font}px">${esc(x)}</div>${tl ? `<div style="color:var(--gold)">${esc(tl[s - 1][i])}</div>` : ''}${tr ? `<p>${esc(tr[s - 1][i])}</p>` : ''}</div>`).join('')}
   <div class="row" style="justify-content:space-between">${s > 1 ? `<a class="btn o" href="#/reader/${s - 1}/1">‹ ${esc(su[s - 2].tr)}</a>` : '<span></span>'}${s < 114 ? `<a class="btn o" href="#/reader/${s + 1}/1">${esc(su[s].tr)} ›</a>` : ''}</div></div>`;
};
routes.reciters = async (open = '') => {
  const [rs, su] = await Promise.all([data('media/reciters.json'), data('quran/surahs.json')]);
  const cached = await cachedUrls();
  return `${top(t("Kur'an hafızları"))}<div class="view"><p class="mut">${t('Kayıtlar AlQuran Cloud CDN (cdn.islamic.network) üzerinden yayınlanır. Her hafız için yalnızca sunucuda gerçekten bulunan sureler listelenir; eksik sure varsa sayı belirtilir.')}</p>
  ${rs.reciters.map(r => { const fav = DB.favs.includes('reciter:' + r.id);
    return `<div class="card"><div class="row"><div class="grow"><b>${esc(r.tr)}</b><div class="mut">${esc(r.info)}</div><div class="${r.surahs.length === 114 ? '' : 'err'}" style="font-size:13px">${r.surahs.length}/114 ${t('sure')}${r.verified ? '' : ' · ' + t('sunucu doğrulaması bekliyor')}</div></div>
     <button class="ib" data-a="fav" data-v="reciter:${r.id}">${fav ? '⭐' : '☆'}</button></div>
     <div class="row" style="flex-wrap:wrap"><button class="btn s ${S.reciter === r.id ? '' : 'o'}" data-a="setreciter" data-v="${r.id}">${S.reciter === r.id ? t('Seçili') : t('Seç')}</button>
     <button class="btn o s" data-a="play" data-r="${r.id}" data-v="1">▶ ${t('Örnek: Fâtiha')}</button><a class="btn o s" href="#/reciters/${open === r.id ? '' : r.id}">${t('Sureler')}</a></div>
     ${open === r.id ? `<p></p><button class="btn o s w" data-a="dlall" data-v="${r.id}">⬇️ ${t('Tüm sureleri indir')}</button>${r.surahs.map(n => `<div class="kv"><span>${n}. ${esc(su[n - 1].tr)} ${cached.has(r.url.replace('{n}', n)) ? '✅' : ''}</span><span><button class="btn o s" data-a="play" data-r="${r.id}" data-v="${n}">▶</button> <button class="btn o s" data-a="dl" data-r="${r.id}" data-v="${n}">⬇️</button></span></div>`).join('')}` : ''}</div>`; }).join('')}</div>`;
};
routes.bookmarks = async () => {
  const [su, tr] = await Promise.all([data('quran/surahs.json'), data('quran/tr.json')]);
  return `${top(t('Yer imleri ve favoriler'))}<div class="view">${DB.bookmarks.length ? DB.bookmarks.slice().reverse().map(b => `<a class="item" href="#/reader/${b.s}/${b.a}"><div class="grow"><b>${b.k === 'f' ? '❤️' : '🔖'} ${esc(su[b.s - 1].tr)} ${b.s}:${b.a}</b><div>${esc(tr[b.s - 1][b.a - 1].slice(0, 200))}</div></div></a>`).join('') : `<p>${t('Henüz yer imi yok. Okurken ayetin yanındaki işarete dokunun.')}</p>`}</div>`;
};

// ---- indirmeler (Cache Storage)
async function cachedUrls() { try { const c = await caches.open('nv-audio'); return new Set((await c.keys()).map(r => r.url)); } catch { return new Set(); } }
const DL = new Map(); // url -> {title, done, total, status, ctrl}
async function download(url, title) {
  if (DL.get(url)?.status === 'running') return;
  const st = { title, done: 0, total: 0, status: 'running', ctrl: new AbortController() }; DL.set(url, st); refreshDl();
  try {
    let res; try { res = await fetch(url, { signal: st.ctrl.signal, mode: 'cors' }); } catch (e) { if (e.name === 'AbortError') throw e; res = null; }
    const c = await caches.open('nv-audio');
    if (res && res.ok && res.body) {
      st.total = +res.headers.get('content-length') || 0; const rd = res.body.getReader(); const parts = [];
      for (;;) { const { done, value } = await rd.read(); if (done) break; parts.push(value); st.done += value.length; refreshDl(); }
      await c.put(url, new Response(new Blob(parts, { type: 'audio/mpeg' }), { headers: { 'Content-Type': 'audio/mpeg' } }));
    } else throw new Error(t('Sunucu çevrimdışı kullanım için dosya indirmeye izin vermedi (CORS). Bu kayıt yalnızca çevrimiçi dinlenebilir.'));
    st.status = 'done';
  } catch (e) { st.status = e.name === 'AbortError' ? 'paused' : 'failed'; st.error = e.message; }
  refreshDl();
}
function refreshDl() { if (location.hash.startsWith('#/downloads')) { clearTimeout(refreshDl.t); refreshDl.t = setTimeout(render, 300); } }
routes.downloads = async () => {
  const cached = [...await cachedUrls()]; let est = null; try { est = await navigator.storage.estimate(); } catch {}
  const running = [...DL.entries()].filter(([, v]) => v.status !== 'done');
  return `${top(t('İndirme merkezi'))}<div class="view"><div class="card"><h3>${t('Depolama')}</h3>${est ? `<div class="kv"><span>${t('Kullanılan')}</span><b>${(est.usage / 1048576).toFixed(1)} MB</b></div><div class="kv"><span>${t('Kullanılabilir')}</span><span>${(est.quota / 1048576).toFixed(0)} MB</span></div>` : ''}
   <p class="mut">${t('Web sürümünde indirilen sesler tarayıcının depolama alanına kaydedilir ve internet yokken çalınır. iPhone, uzun süre açılmayan sitelerin verilerini silebilir.')}</p></div>
   ${running.map(([u, v]) => `<div class="card"><b>${esc(v.title)}</b><div class="bar"><div style="width:${v.total ? v.done * 100 / v.total : 5}%"></div></div><div class="mut">${t({ running: 'İndiriliyor', paused: 'Duraklatıldı', failed: 'Başarısız' }[v.status])} · ${(v.done / 1048576).toFixed(1)} MB${v.total ? ' / ' + (v.total / 1048576).toFixed(1) + ' MB' : ''}</div>${v.error ? `<div class="err">${esc(v.error)}</div>` : ''}
    <div class="row">${v.status === 'running' ? `<button class="btn o s" data-a="dlpause" data-v="${u}">${t('Duraklat')}</button>` : `<button class="btn o s" data-a="dlresume" data-v="${u}">${t('Devam et')}</button>`}<button class="btn o s" data-a="dlcancel" data-v="${u}">${t('İptal')}</button></div></div>`).join('')}
   <h3>${t('İndirilenler')} (${cached.length})</h3>${cached.map(u => `<div class="item"><div class="grow" style="word-break:break-all;font-size:13px">${esc(decodeURIComponent(u.split('/').slice(-2).join(' / ')))}</div><button class="btn o s" data-a="dldel" data-v="${u}">🗑️</button></div>`).join('') || `<p>${t('Henüz indirme yok.')}</p>`}</div>`;
};

// ---- Kıble
let leafletP = null;
const leaflet = () => leafletP ??= new Promise((ok, no) => { const c = document.createElement('link'); c.rel = 'stylesheet'; c.href = 'vendor/leaflet/leaflet.css'; document.head.appendChild(c);
  const s = document.createElement('script'); s.src = 'vendor/leaflet/leaflet.js'; s.onload = () => ok(window.L); s.onerror = no; document.head.appendChild(s); });
function gc(a, b, n) { const r = Math.PI / 180, p1 = a[0] * r, l1 = a[1] * r, p2 = b[0] * r, l2 = b[1] * r; const d = 2 * Math.asin(Math.sqrt(Math.sin((p2 - p1) / 2) ** 2 + Math.cos(p1) * Math.cos(p2) * Math.sin((l2 - l1) / 2) ** 2)); const o = [];
  for (let i = 0; i <= n; i++) { const f = i / n, A = Math.sin((1 - f) * d) / Math.sin(d), B = Math.sin(f * d) / Math.sin(d); const x = A * Math.cos(p1) * Math.cos(l1) + B * Math.cos(p2) * Math.cos(l2), y = A * Math.cos(p1) * Math.sin(l1) + B * Math.cos(p2) * Math.sin(l2), z = A * Math.sin(p1) + B * Math.sin(p2);
    o.push([Math.atan2(z, Math.sqrt(x * x + y * y)) / r, Math.atan2(y, x) / r]); } return o; }
async function showMap(mode, items = []) {
  const L = await leaflet(); const el = $('#map'); if (!el) return; const me = [S.place.lat, S.place.lng];
  const m = L.map(el); L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19, attribution: '© OpenStreetMap' }).addTo(m);
  L.circleMarker(me, { radius: 8, color: '#0B1F3A', fillColor: '#C9A227', fillOpacity: 1 }).addTo(m).bindTooltip(t('Konumunuz'));
  if (mode === 'qibla') { const line = gc(me, [KAABA.lat, KAABA.lng], 64); L.polyline(line, { color: '#0B6E4F', weight: 4 }).addTo(m); L.marker([KAABA.lat, KAABA.lng]).addTo(m).bindTooltip('Kâbe'); m.setView(me, 15); }
  else { items.forEach(i => L.marker([i.lat, i.lng]).addTo(m).bindPopup(esc(i.name))); m.setView(me, 14); }
}
routes.qibla = async (tab = '0') => {
  const q = qiblaBearing(S.place.lat, S.place.lng), dist = distanceKm(S.place.lat, S.place.lng);
  const tabs = `<div class="chips"><a class="chip ${tab === '0' ? 'on' : ''}" href="#/qibla/0" style="text-decoration:none">${t('Pusula')}</a><a class="chip ${tab === '1' ? 'on' : ''}" href="#/qibla/1" style="text-decoration:none">${t('Harita')}</a></div>`;
  const info = `<p class="center"><b>${t('Kıble açısı')}: ${q.toFixed(1)}° (${t('gerçek kuzeyden')})</b><br>${t("Kâbe'ye mesafe")}: ${Math.round(dist).toLocaleString(locale())} km · ${esc(S.place.name)}</p>`;
  if (tab === '1') { setTimeout(() => showMap('qibla'), 30); return `${top(t('Kıble'), false)}<div class="view">${tabs}${info}<div id="map"></div><p class="mut">${t("Yeşil çizgi, bulunduğunuz yerden Kâbe'ye en kısa (büyük daire) yoldur. Haritadaki bir bina veya yol ile hizalanarak yön bulabilirsiniz. Harita için internet gerekir.")}</p></div>`; }
  let got = false;
  const onOri = e => {
    let h = null;
    if (typeof e.webkitCompassHeading === 'number') h = e.webkitCompassHeading; // iOS: manyetik kuzey
    else if (e.absolute && e.alpha != null) h = (360 - e.alpha) % 360;
    if (h == null) return; got = true;
    const scr = (screen.orientation?.angle || 0); h = (h + scr) % 360;
    const dial = $('#dial'); if (!dial) return;
    dial.style.transform = `rotate(${-h}deg)`;
    const diff = ((q - h + 540) % 360) - 180; const ok = Math.abs(diff) < 4;
    $('#qmsg').textContent = ok ? t('Kıbleye yöneldiniz ✓') : `${t(diff > 0 ? 'Sağa dönün' : 'Sola dönün')} ${Math.abs(diff).toFixed(0)}°`;
    $('#qmsg').style.color = ok ? 'var(--em)' : ''; $('#arrow').style.background = ok ? 'var(--em)' : 'var(--gold)';
    $('#hd').textContent = `${t('Telefon yönü')}: ${h.toFixed(0)}°${e.webkitCompassAccuracy > 25 ? ' · ' + t('Pusula kalibrasyonu gerekli') : ''}`;
  };
  const start = () => { window.addEventListener('deviceorientationabsolute', onOri); window.addEventListener('deviceorientation', onOri);
    setTimeout(() => { if (!got && $('#qmsg')) $('#qmsg').textContent = t('Bu cihazda/tarayıcıda pusula sensörü okunamadı. Harita sekmesini kullanın.'); }, 4000); };
  const needPerm = typeof DeviceOrientationEvent !== 'undefined' && typeof DeviceOrientationEvent.requestPermission === 'function';
  window._qstart = async () => { try { if (needPerm && (await DeviceOrientationEvent.requestPermission()) !== 'granted') return toast('Pusula izni verilmedi'); start(); $('#qperm')?.remove(); } catch (e) { toast(e.message); } };
  if (!needPerm) setTimeout(start, 50);
  cleanup = () => { window.removeEventListener('deviceorientationabsolute', onOri); window.removeEventListener('deviceorientation', onOri); };
  return `${top(t('Kıble'), false, `<button class="ib" data-a="gps" title="${t('Konumu yenile')}">📍</button>`)}<div class="view">${tabs}
   <h2 class="center" id="qmsg">${t('Sensör bekleniyor…')}</h2>${needPerm ? `<button id="qperm" class="btn w" onclick="_qstart()">${t('Pusulayı başlat')}</button>` : ''}
   <div class="compass"><div class="mark"></div><div class="dial" id="dial"><span class="n">N</span><div class="arrow" id="arrow" style="transform:rotate(${q + 180}deg)"></div></div></div>
   ${info}<p class="center mut" id="hd"></p>
   <div class="card"><h3>${t('Nasıl kullanılır?')}</h3><p>${t("1. Telefonu düz (yere paralel) tutun.\n2. Altın renkli ok telefonun üst kenarındaki çizgiyle hizalanana kadar kendi etrafınızda dönün.\n3. Hizalandığında ok yeşile döner.\nPusula sensörü yoksa veya güvenilmezse 'Harita' sekmesini kullanın.").replace(/\n/g, '<br>')}</p>
   <p class="mut">${t('Sensör doğruluğu düşük. Telefonu havada 8 rakamı çizer gibi birkaç kez yavaşça hareket ettirin. Mıknatıs, hoparlör, metal masa ve araç içinden uzak durun.')}</p><p class="mut">${t('Bilgisayarlarda pusula sensörü bulunmaz; harita sekmesini kullanın.')}</p></div></div>`;
};

// ---- Ayarlar
const opt = (v, cur, l) => `<option value="${v}" ${v === cur ? 'selected' : ''}>${esc(l)}</option>`;
routes.settings = async () => {
  return `${top(t('Ayarlar'))}<div class="view">
  <div class="card"><h3>${t('Genel')}</h3><label>${t('Dil')}</label><select data-s="lang">${opt('tr', S.lang, 'Türkçe')}${opt('en', S.lang, 'English')}${opt('ar', S.lang, 'العربية')}</select>
   <label>${t('Tema')}</label><select data-s="theme">${opt('auto', S.theme, t('Otomatik'))}${opt('light', S.theme, t('Açık'))}${opt('dark', S.theme, t('Koyu'))}</select></div>
  <div class="card"><h3>${t('Konum')}</h3><div class="kv"><b>${esc(S.place.name)}</b><span>${esc(S.place.tz)}</span></div><a class="btn w" href="#/location" style="display:block;text-align:center;text-decoration:none">${t('Konumu değiştir')}</a></div>
  <div class="card"><h3>${t('Hesaplama yöntemi')}</h3><label><input type="checkbox" data-s="autoMethod" ${S.autoMethod ? 'checked' : ''} style="width:auto"> ${t('Ülkeye göre otomatik seç')} (${esc(METHODS[methodId()].name)})</label>
   ${S.autoMethod ? '' : `<label>${t('Yöntem')}</label><select data-s="method">${Object.entries(METHODS).map(([k, m]) => opt(k, S.method, m.name)).join('')}</select>`}
   <label>${t('İkindi hesabı')}</label><select data-s="asr">${opt('STANDARD', S.asr, t('Standart (Şafii, Mâlikî, Hanbelî)'))}${opt('HANAFI', S.asr, t('Hanefi'))}</select>
   <label>${t('Yüksek enlem kuralı')}</label><select data-s="highLat">${opt('ANGLE', S.highLat, t('Açı tabanlı (önerilen)'))}${opt('MIDDLE', S.highLat, t('Gecenin yarısı'))}${opt('SEVENTH', S.highLat, t('Gecenin yedide biri'))}${opt('NONE', S.highLat, t('Yok'))}</select>
   <p class="mut">${t("Diyanet'in resmî 'Awqat Salah' API'si kurumdan başvuru ile alınan kullanıcı adı ve şifre gerektirir; bu sürümde kullanılmamaktadır. Diyanet yöntemi cihazda hesaplanır ve gösterilen kaynak her zaman yazılır.")}</p>
   <button class="btn o s" data-a="aladhan">${t('AlAdhan API ile karşılaştır')}</button><div id="alres" class="mut"></div></div>
  <div class="card"><h3>${t('Vakit düzeltmeleri (dakika)')}</h3>${['imsak', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'].map(k => `<div class="kv"><span>${pn(k)}</span><span><button class="btn o s" data-a="off" data-k="${k}" data-v="-1">−</button> <b style="display:inline-block;width:36px;text-align:center">${S.offsets[k] || 0}</b> <button class="btn o s" data-a="off" data-k="${k}" data-v="1">+</button></span></div>`).join('')}</div>
  <div class="card"><h3>${t('Hicri takvim')}</h3><select data-s="hijriAdjust" data-num="1">${[-2, -1, 0, 1, 2].map(v => opt(String(v), String(S.hijriAdjust), (v > 0 ? '+' : '') + v + ' ' + t('gün'))).join('')}</select>
   <p class="mut">${t("Hicri tarih Ümmü'l-Kurâ takvimiyle hesaplanır. Ülkenizdeki resmî ilanla 1-2 gün fark varsa buradan düzeltebilirsiniz. Türkiye'de dini günler Diyanet'in yayımladığı listeden gösterilir.")}</p></div>
  <div class="card"><h3>${t("Kur'an")}</h3><label>${t('Meal')}</label><select data-s="translation">${opt('tr', S.translation, 'Türkçe (Diyanet)')}${opt('en', S.translation, 'English (Saheeh Intl.)')}${opt('none', S.translation, t('Gösterme'))}</select>
   <label><input type="checkbox" data-s="translit" ${S.translit ? 'checked' : ''} style="width:auto"> ${t('Latin okunuşu göster')}</label><label>${t('Yazı boyutu')}: ${S.font}</label><input type="range" min="16" max="48" value="${S.font}" data-s="font" data-num="1"></div>
  <div class="card"><h3>${t('Gizlilik ve verilerim')}</h3><p>${t('Namaz takibi, zikir, yer imleri ve ayarlar yalnızca bu cihazda saklanır. Uygulamada reklam, hesap veya analiz aracı yoktur.')}</p>
   <button class="btn o w" data-a="export">${t('Verilerimi dışa aktar (JSON)')}</button><p></p><button class="btn o w" data-a="wipe" style="color:var(--err);border-color:var(--err)">${t('Tüm verilerimi sil')}</button><p><a href="gizlilik.html">${t('Gizlilik politikası')}</a></p></div></div>`;
};
routes.location = async (cc = '', q = '') => {
  cc = cc || S.place.country; q = decodeURIComponent(q);
  const countries = await data('loc/countries.json'); let places = [];
  try { const o = await data(`loc/${cc}.json`); const st = Object.fromEntries(o.s.map(s => [s[0], s[1]])); places = [...o.s.map(s => ({ name: s[1], region: '', lat: s[2], lng: s[3] })), ...o.c.map(c => ({ name: c[0], region: st[c[1]] || '', lat: c[2], lng: c[3] }))]; } catch {}
  const n = norm(q); const res = (q ? places.filter(p => norm(p.name).includes(n) || norm(p.region).startsWith(n)).sort((a, b) => (norm(a.name).startsWith(n) ? 0 : 1) - (norm(b.name).startsWith(n) ? 0 : 1)) : places.filter(p => !p.region)).slice(0, 60);
  const dn = new Intl.DisplayNames([locale()], { type: 'region' }); const cname = c => { try { return dn.of(c); } catch { return c; } };
  window._places = res.map(p => ({ ...p, country: cc }));
  return `${top(t('Konum'))}<div class="view"><div class="card"><h3>${t('Geçerli konum')}</h3><b>${esc(S.place.name)}${S.place.region ? ', ' + esc(S.place.region) : ''}</b><div class="mut">${S.place.lat.toFixed(4)}, ${S.place.lng.toFixed(4)} · ${esc(S.place.tz)}</div><p></p>
   <button class="btn w" data-a="gps">📍 ${t('GPS ile otomatik bul')}</button><p></p><button class="btn o w" data-a="saveplace">${t('Bu konumu kaydet')}</button></div>
   ${DB.places.length ? `<div class="card"><h3>${t('Kayıtlı konumlar')}</h3>${DB.places.map((p, i) => `<div class="kv"><a href="javascript:void 0" data-a="useplace" data-v="${i}">${esc(p.name)} · ${esc(p.tz)}</a><button class="btn o s" data-a="delplace" data-v="${i}">🗑️</button></div>`).join('')}</div>` : ''}
   <div class="card"><h3>${t('Ülke, il ve ilçe seç')}</h3><label>${t('Ülke')}</label><select data-a="country">${countries.map(c => [c.c, cname(c.c)]).sort((a, b) => a[1].localeCompare(b[1], locale())).map(([c, l]) => opt(c, cc, l)).join('')}</select>
   <label>${t('Şehir / ilçe ara')}</label><input id="ls" value="${esc(q)}" data-enter="loc" data-cc="${cc}"><p class="mut">${t('Liste internetsiz çalışır (il/eyalet ve ilçe/şehirler).')}</p></div>
   ${res.map((p, i) => `<a class="item" href="javascript:void 0" data-a="pickplace" data-v="${i}"><div class="grow"><b>${esc(p.name)}</b><div class="mut">${esc(p.region || t('İl / eyalet'))}</div></div></a>`).join('')}
   <div class="card"><h3>${t('Çevrimiçi arama (OpenStreetMap)')}</h3><p class="mut">${t("Listede bulamazsanız yazdığınız metin OpenStreetMap Nominatim'e gönderilerek aranır.")}</p><button class="btn o s" data-a="nominatim">${t('Çevrimiçi ara')}</button><div id="nomres"></div></div>
   <div class="card"><h3>${t('Koordinatla konum')}</h3><label>${t('Enlem (ör. 41.0082)')}</label><input id="clat" inputmode="decimal"><label>${t('Boylam (ör. 28.9784)')}</label><input id="clng" inputmode="decimal"><p></p><button class="btn" data-a="coords">${t('Uygula')}</button></div></div>`;
};
async function zoneFor(cc, lng) {
  const c = (await data('loc/countries.json')).find(x => x.c === cc); const tzs = c?.tz || [];
  if (tzs.length === 1) return tzs[0][0]; if (!tzs.length) return Intl.DateTimeFormat().resolvedOptions().timeZone;
  return tzs.slice().sort((a, b) => Math.abs(a[1] - lng / 15 * 3600) - Math.abs(b[1] - lng / 15 * 3600))[0][0];
}
async function setPlace(p, gps = false) {
  const tz = gps ? Intl.DateTimeFormat().resolvedOptions().timeZone : await zoneFor(p.country, p.lng);
  setS({ place: { name: p.name, region: p.region || '', lat: +p.lat, lng: +p.lng, tz, country: p.country, gps } }); toast(`${p.name} · ${tz}`);
}
async function useGps() {
  if (!navigator.geolocation) return toast('Bu cihaz konum desteklemiyor');
  navigator.geolocation.getCurrentPosition(async pos => {
    const { latitude: lat, longitude: lng } = pos.coords; let name = `${lat.toFixed(3)}, ${lng.toFixed(3)}`, region = '', cc = S.place.country;
    // En yakın şehir: cihazdaki (indirilen) ülke listesinden; ülke bilinmiyorsa ülke merkezlerine göre tahmin
    try { const cs = await data('loc/countries.json'); const tz = Intl.DateTimeFormat().resolvedOptions().timeZone;
      const cand = cs.filter(c => c.tz.some(z => z[0] === tz)); const pick = (cand.length ? cand : cs).slice().sort((a, b) => distanceKm(lat, lng, a.lat, a.lng) - distanceKm(lat, lng, b.lat, b.lng))[0]; cc = pick.c;
      const o = await data(`loc/${cc}.json`); const st = Object.fromEntries(o.s.map(s => [s[0], s[1]]));
      const near = o.c.map(c => [c, distanceKm(lat, lng, c[2], c[3])]).sort((a, b) => a[1] - b[1])[0]; if (near && near[1] < 40) { name = near[0][0]; region = st[near[0][1]] || ''; } } catch {}
    await setPlace({ name, region, lat, lng, country: cc }, true); render();
  }, e => toast('Konum alınamadı: ' + e.message), { enableHighAccuracy: true, timeout: 15000 });
}
routes.ezan = async () => {
  const ez = (await data('media/ezan.json')).ezan; const cached = await cachedUrls();
  const np = 'Notification' in window ? Notification.permission : 'unsupported';
  return `${top(t('Ezan ayarları'))}<div class="view"><div class="card" style="background:var(--sc)"><b>${t('Web sürümü sınırlaması')}</b><p>${t('iPhone ve bilgisayarda tarayıcılar, sayfa kapalıyken uygulamanın ses çalmasına izin vermez. Bu nedenle web sürümünde ezan yalnızca uygulama açıkken (ön planda veya bilgisayarda arka plan sekmesinde) çalar. Ekran kapalıyken kesin ezan için Android uygulamasını kullanın.')}</p>
   <div class="kv"><span>${t('Bildirim izni')}</span><b>${np === 'granted' ? '✓' : np === 'unsupported' ? t('desteklenmiyor') : '✗'}</b></div>${np === 'default' ? `<button class="btn s" data-a="notifperm">${t('Bildirim izni ver')}</button>` : ''}
   <p class="mut">${t("iPhone'da bildirim için önce siteyi Ana Ekrana ekleyin (iOS 16.4+).")}</p><button class="btn o s" data-a="stopezan">⏹ ${t('Çalan ezanı durdur')}</button></div>
  ${['fajr', 'dhuhr', 'asr', 'maghrib', 'isha', 'imsak', 'sunrise'].map(k => { const a = S.alerts[k];
    return `<div class="card"><h3>${pn(k)}</h3><label>${t('Uyarı türü')}</label><select data-al="${k}" data-f="mode">${Object.entries(MODES).map(([m, l]) => opt(m, a.mode, t(l))).join('')}</select>
     ${a.mode === 'FULL' ? `<label>${t('Ezan sesi')}</label><select data-al="${k}" data-f="sound">${ez.filter(e => e.available).map(e => opt(e.id, a.sound, t(e.tr))).join('')}</select>` : ''}
     <label>${t('Vakit öncesi hatırlatma')}</label><select data-al="${k}" data-f="pre" data-num="1">${[0, 5, 10, 15, 30].map(v => opt(String(v), String(a.pre), v ? `${v} ${t('dakika önce')}` : t('Kapalı'))).join('')}</select>
     ${a.mode === 'FULL' || a.mode === 'SHORT' ? `<p></p><button class="btn o s" data-a="preview" data-v="${a.mode === 'SHORT' ? 'kisa' : a.sound}">▶ ${t('Önizle')}</button>` : ''}</div>`; }).join('')}
  <h3>${t('Ezan sesleri')}</h3>${ez.map(e => { const url = new URL('audio/ezan/' + e.file, location.href).href; const off = cached.has(url);
    return `<div class="card"><b>${t(e.tr)}</b><div class="mut">${esc(e.author)} · ${esc(e.license)}</div><div class="mut">${esc(e.changes)}</div>
     <div class="row">${e.available ? `<button class="btn o s" data-a="preview" data-v="${e.id}">▶ ${t('Dinle')}</button>${off ? `<span class="badge">${t('İndirildi (internetsiz)')}</span>` : `<button class="btn o s" data-a="dlez" data-v="${url}" data-t="${esc(e.tr)}">⬇️ ${t('İndir')}</button>`}` : `<span class="badge">${t('Sunucuda henüz yok')}</span>`}</div></div>`; }).join('')}</div>`;
};

// ---- Dualar, zikir, esma
routes.dualar = async (cat = 'all') => {
  const d = await data('content/dualar.json'); const tr = S.lang === 'tr';
  const items = d.items.filter(x => cat === 'all' ? true : cat === 'fav' ? DB.favs.includes('dua:' + x.id) : x.cat === cat);
  return `${top(t('Dualar'))}<div class="view"><div class="chips"><a class="chip ${cat === 'all' ? 'on' : ''}" href="#/dualar/all" style="text-decoration:none">${t('Tümü')}</a><a class="chip ${cat === 'fav' ? 'on' : ''}" href="#/dualar/fav" style="text-decoration:none">★ ${t('Favoriler')}</a>
   ${d.cats.map(c => `<a class="chip ${cat === c.id ? 'on' : ''}" href="#/dualar/${c.id}" style="text-decoration:none">${esc(tr ? c.tr : c.en)}</a>`).join('')}</div>
   ${items.map(x => `<div class="card"><div class="row"><b class="grow">${esc(x.title)}</b><button class="ib" data-a="fav" data-v="dua:${x.id}">${DB.favs.includes('dua:' + x.id) ? '⭐' : '☆'}</button></div>
    <div class="ar">${esc(x.ar)}</div><div style="color:var(--gold)">${esc(x.okunus)}</div><p>${esc(x.tr)}</p><p class="mut">${esc(x.en)}</p><div class="mut">${t('Kaynak')}: ${esc(x.src)}</div></div>`).join('')}</div>`;
};
const Z = { name: null, target: 33, count: 0, vib: true };
routes.zikir = async () => {
  const zs = await data('content/zikir.json'); const names = [...zs.map(z => z.okunus), ...DB.customZikir.map(c => c.name)];
  Z.name ??= names[0]; const z = zs.find(x => x.okunus === Z.name); const today = ymdStr(todayYMD());
  const tToday = DB.zikir.filter(x => x.date === today).reduce((a, b) => a + b.count, 0), total = DB.zikir.reduce((a, b) => a + b.count, 0);
  return `${top(t('Zikirmatik'))}<div class="view"><select data-a="zname">${names.map(n => opt(n, Z.name, n)).join('')}</select>
   ${z ? `<div class="ar center" style="text-align:center">${esc(z.ar)}</div><p class="center">${esc(S.lang === 'tr' ? z.tr : z.en)}</p><p class="center mut">${t('Kaynak')}: ${esc(z.src)}</p>` : ''}
   <div class="chips" style="justify-content:center">${[33, 99, 100, 1000, 0].map(v => `<button class="chip ${Z.target === v ? 'on' : ''}" data-a="ztarget" data-v="${v}">${v || t('Sınırsız')}</button>`).join('')}</div>
   <button class="ring" data-a="zinc"><div><div id="zc">${Z.count}</div><div style="font-size:18px;color:var(--goldl)">${Z.target ? '/ ' + Z.target : '∞'}</div></div></button>
   <div class="row" style="justify-content:center;flex-wrap:wrap"><button class="btn o s" data-a="zreset">↺ ${t('Sıfırla')}</button><button class="btn s" data-a="zsave">💾 ${t('Kaydet')}</button><button class="btn o s" data-a="zadd">＋ ${t('Zikir ekle')}</button>
   <label style="width:auto"><input type="checkbox" data-a="zvib" ${Z.vib ? 'checked' : ''} style="width:auto"> ${t('Titreşim')}</label></div>
   <div class="card"><h3>${t('İstatistik')}</h3><div class="kv"><span>${t('Bugün')}</span><b>${tToday}</b></div><div class="kv"><span>${t('Toplam zikir')}</span><b>${total}</b></div>
   <div class="kv"><span>${t('Günlük hedef')}</span><span><input type="number" value="${S.zikirGoal}" data-s="zikirGoal" data-num="1" style="width:90px"></span></div><div class="bar"><div style="width:${Math.min(100, tToday * 100 / Math.max(1, S.zikirGoal))}%"></div></div></div>
   ${DB.customZikir.length ? `<div class="card"><h3>${t('Kişisel zikirlerim')}</h3>${DB.customZikir.map((c, i) => `<div class="kv"><span>${esc(c.name)} (${c.target})</span><button class="btn o s" data-a="zdel" data-v="${i}">🗑️</button></div>`).join('')}</div>` : ''}
   <div class="card"><h3>${t('Geçmiş kayıtlar')}</h3>${DB.zikir.slice(-30).reverse().map(h => `<div class="kv"><span>${h.date} · ${esc(h.name)}</span><b>${h.count}</b></div>`).join('') || t('Henüz kayıt yok')}</div></div>`;
};
function zsave() { if (Z.count > 0) { DB.zikir.push({ name: Z.name, count: Z.count, date: ymdStr(todayYMD()), ts: Date.now() }); saveDB('zikir'); Z.count = 0; } }
routes.esma = async (f = '') => {
  const es = await data('content/esma.json'); const tr = S.lang === 'tr'; const e0 = es[dayIdx() % es.length];
  return `${top(t("Esmâü'l-Hüsnâ"), true, `<a class="ib" href="#/esma/${f ? '' : 'fav'}">${f ? '⭐' : '☆'}</a>`)}<div class="view">
   <div class="card"><h3>${t('Günün esması')}</h3><div class="ar" style="font-size:34px">${e0.ar}</div><b>${e0.n}. ${esc(e0.okunus)}</b><p>${esc(tr ? e0.tr : e0.en)}</p></div>
   <p class="mut">${t('"Allah" lafza-i celâli bütün bu güzel isimleri kendinde toplayan özel isimdir. Liste: Tirmizî, Deavât 82 rivayeti.')}</p>
   ${es.filter(e => !f || DB.favs.includes('esma:' + e.n)).map(e => `<div class="item"><span class="num">${e.n}</span><div class="grow"><b>${esc(e.okunus)}</b><div>${esc(tr ? e.tr : e.en)}</div>${tr ? `<div class="mut">${esc(e.en)}</div>` : ''}</div><span class="ar" style="font-size:24px">${e.ar}</span><button class="ib" data-a="fav" data-v="esma:${e.n}">${DB.favs.includes('esma:' + e.n) ? '⭐' : '☆'}</button></div>`).join('')}</div>`;
};

// ---- Ramazan, takvim
const daysBetween = (a, b) => Math.round((Date.UTC(b.y, b.m - 1, b.d) - Date.UTC(a.y, a.m - 1, a.d)) / 86400000);
routes.ramazan = async () => {
  const now = todayYMD(); const h = hijri(now); const inR = h.m === 9;
  let start = now; if (inR) start = addDays(now, -(h.d - 1)); else { for (let i = 0; i < 400; i++) { const x = addDays(now, i), hx = hijri(x); if (hx.m === 9 && hx.d === 1) { start = x; break; } } }
  let eid = now, eidName = ''; for (let i = 0; i < 400; i++) { const x = addDays(now, i), hx = hijri(x); if (hx.m === 10 && hx.d === 1) { eid = x; eidName = 'Ramazan Bayramı'; break; } if (hx.m === 12 && hx.d === 10) { eid = x; eidName = 'Kurban Bayramı'; break; } }
  const imsakiye = [...Array(30)].map((_, i) => dayTimes(addDays(start, i))); const td = dayTimes(now); const tdS = ymdStr(now); const st = DB.fasts[tdS];
  window._ramTick = () => { const el = $('#rcd'); if (!el) return; const n = Date.now(); let target = td.maghrib, lbl = 'İftara kalan'; if (n < td.imsak) { target = td.imsak; lbl = 'Sahura (imsaka) kalan'; } else if (n > td.maghrib) { target = dayTimes(addDays(now, 1)).imsak; lbl = 'Sahura (imsaka) kalan'; }
    const s = Math.max(0, Math.floor((target - n) / 1000)); el.textContent = `${String(Math.floor(s / 3600)).padStart(2, '0')}:${String(Math.floor(s / 60) % 60).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`; $('#rlbl').textContent = t(lbl); };
  setTimeout(() => window._ramTick(), 0);
  return `${top(t('Ramazan'))}<div class="view"><div class="hero center">${inR ? `<div class="gold">${t("Ramazan'ın")} ${h.d}. ${t('günü')}</div><div id="rlbl"></div><div class="count" id="rcd"></div><div>${t('İmsak')} ${hhmm(td, 'imsak')} · ${t('İftar')} ${hhmm(td, 'maghrib')}</div>`
    : `<div>${t("Ramazan'a kalan")}</div><div class="count">${daysBetween(now, start)} ${t('gün')}</div><div class="gold">${dfmt(start, { day: 'numeric', month: 'long', year: 'numeric' })}</div>`}
    <p>${t(eidName)}: ${dfmt(eid, { day: 'numeric', month: 'long', year: 'numeric' })} · ${daysBetween(now, eid)} ${t('gün')}</p></div>
   <div class="row"><a class="btn o grow" href="#/dualar/ramazan" style="text-align:center;text-decoration:none">${t('Ramazan duaları')}</a><a class="btn o grow" href="#/takvim" style="text-align:center;text-decoration:none">${t('Kadir Gecesi')}</a></div><p></p>
   <div class="card"><h3>${t('Oruç takibi')}</h3><div class="chips"><button class="chip ${st === 'F' ? 'on' : ''}" data-a="fast" data-v="F">${t('Tuttum')}</button><button class="chip ${st === 'M' ? 'on' : ''}" data-a="fast" data-v="M">${t('Tutamadım (kaza)')}</button>${st ? `<button class="chip" data-a="fast" data-v="">${t('Temizle')}</button>` : ''}</div>
   <div class="kv"><span>${t('Tutulan oruç')}</span><b>${Object.values(DB.fasts).filter(v => v === 'F').length}</b></div><div class="kv"><span>${t('Kaza orucu borcu')}</span><b>${Object.values(DB.fasts).filter(v => v === 'M').length}</b></div></div>
   <h3>${t('İmsakiye')} · ${esc(S.place.name)}</h3><table><thead><tr><th>${t('Gün')}</th><th>${t('Tarih')}</th>${['İmsak', 'Güneş', 'Öğle', 'İkindi', 'İftar', 'Yatsı'].map(x => `<th>${t(x)}</th>`).join('')}</tr></thead>
   <tbody>${imsakiye.map((d, i) => `<tr class="${ymdStr(d.date) === tdS ? 'today' : ''}"><td>${i + 1}</td><td>${String(d.date.d).padStart(2, '0')}.${String(d.date.m).padStart(2, '0')}</td>${['imsak', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'].map(k => `<td>${hhmm(d, k)}</td>`).join('')}</tr>`).join('')}</tbody></table>
   <p class="mut">${t('İmsakiye, Hicri takvime göre hesaplanan Ramazan başlangıcından itibaren 30 gün gösterir; resmî ilanla 1 gün fark olabilir.')}</p></div>`;
};
routes.takvim = async (yy = '') => {
  const y = +yy || todayYMD().y; const off = await data('content/dini_gunler_tr.json');
  const useOff = S.place.country === 'TR' && off.days.some(d => d[0].startsWith(String(y)));
  const days = useOff ? off.days.filter(d => d[0].startsWith(String(y))).map(d => ({ date: d[0], tr: d[2], night: d[3] })) : religiousDays(y);
  const now = todayYMD(); const parse = s => { const [a, b, c] = s.split('-').map(Number); return { y: a, m: b, d: c }; };
  const fr = []; for (let o = { y, m: 1, d: 1 }; o.y === y; o = addDays(o, 1)) if (new Date(Date.UTC(o.y, o.m - 1, o.d)).getUTCDay() === 5) fr.push(`${String(o.d).padStart(2, '0')}.${String(o.m).padStart(2, '0')}`);
  return `${top(t('Dini takvim'))}<div class="view"><div class="row" style="justify-content:space-between"><a class="btn o s" href="#/takvim/${y - 1}">‹</a><b style="font-size:20px">${y}</b><a class="btn o s" href="#/takvim/${y + 1}">›</a></div>
   <p class="mut">${useOff ? t('Kaynak: Diyanet İşleri Başkanlığı dini günler takvimi') : t("Ümmü'l-Kurâ Hicri takvimine göre hesaplanmıştır. Ülkenizin resmî ilanıyla 1-2 gün fark olabilir (Ayarlar > Hicri düzeltme).")}</p>
   ${days.map(d => { const o = parse(d.date), left = daysBetween(now, o); return `<div class="card"><div class="row"><div class="grow"><b>${esc(t(d.tr))}</b><div class="mut">${dfmt(o)} · ${hijriStr(hijri(d.night ? addDays(o, 1) : o))}</div>${d.night ? `<div class="mut" style="color:var(--gold)">${t('Bu gecenin (akşamın) ihyası')}</div>` : ''}</div>${left >= 0 ? `<b style="color:var(--em)">${left} ${t('gün')}</b>` : ''}</div></div>`; }).join('')}
   <div class="card"><h3>${t('Cuma günleri')} (${y})</h3><p>${fr.join('  ')}</p></div></div>`;
};

// ---- Namaz takibi
routes.tracker = async (ds = '') => {
  const now = todayYMD(); const day = ds ? (([a, b, c]) => ({ y: a, m: b, d: c }))(ds.split('-').map(Number)) : now; const key = ymdStr(day);
  const five = ['fajr', 'dhuhr', 'asr', 'maghrib', 'isha']; const done = (k, p) => !!DB.logs[k]?.[p];
  const stat = from => { let c = 0, n = 0; for (let o = from; daysBetween(o, now) >= 0; o = addDays(o, 1)) { n += 5; c += five.filter(p => done(ymdStr(o), p)).length; } return [c, n]; };
  const wd = new Date(Date.UTC(now.y, now.m - 1, now.d)).getUTCDay(); const wk = addDays(now, -((wd + 6) % 7));
  const cnt = five.filter(p => done(key, p)).length;
  return `${top(t('Namaz takibi'))}<div class="view"><div class="card"><div class="row" style="justify-content:space-between"><a class="btn o s" href="#/tracker/${ymdStr(addDays(day, -1))}">‹</a><b>${dfmt(day)}</b>${daysBetween(day, now) > 0 ? `<a class="btn o s" href="#/tracker/${ymdStr(addDays(day, 1))}">›</a>` : '<span></span>'}</div>
   ${five.map(p => `<div class="prow" data-a="log" data-d="${key}" data-v="${p}" style="cursor:pointer"><span style="font-size:26px;margin-inline-end:10px">${done(key, p) ? '✅' : '⚪'}</span><span class="nm">${pn(p)}</span><span class="mut">${t(done(key, p) ? 'Kılındı' : 'Kılınmadı')}</span></div>`).join('')}
   <div class="kv"><span>${t('Günlük hedef')}</span><b>${cnt} / ${S.prayerGoal}</b></div><div class="bar"><div style="width:${Math.min(100, cnt * 100 / S.prayerGoal)}%"></div></div></div>
   <div class="card"><h3>${t('İstatistik')}</h3>${[['Bu hafta', wk], ['Bu ay', { ...now, d: 1 }], ['Bu yıl', { y: now.y, m: 1, d: 1 }]].map(([l, f]) => { const [c, n] = stat(f); return `<div class="kv"><span>${t(l)}</span><b>${c} / ${n} (%${n ? Math.round(c * 100 / n) : 0})</b></div>`; }).join('')}
   <div class="row" style="justify-content:space-between;align-items:flex-end;height:110px">${[6, 5, 4, 3, 2, 1, 0].map(i => { const o = addDays(now, -i), c = five.filter(p => done(ymdStr(o), p)).length; return `<div class="center"><div style="width:26px;height:${8 + c * 16}px;border-radius:6px;background:${c === 5 ? 'var(--em)' : 'var(--gold)'};margin:auto"></div><small>${o.d}</small></div>`; }).join('')}</div></div>
   <div class="card"><h3>${t('Kaza namazı sayacı')}</h3>${[...five, 'vitr'].map(p => `<div class="kv"><span>${p === 'vitr' ? t('Vitir') : pn(p)}</span><span><button class="btn o s" data-a="kaza" data-k="${p}" data-v="-1">−</button> <b style="display:inline-block;width:44px;text-align:center">${DB.kaza[p] || 0}</b> <button class="btn o s" data-a="kaza" data-k="${p}" data-v="1">+</button></span></div>`).join('')}
   <p class="mut">${t('Kaza borcunuzu (+) ile ekleyin, kıldıkça (−) ile azaltın. Veriler yalnızca telefonunuzda saklanır.')}</p></div>
   <div class="card"><h3>${t('Kişisel hedef')}</h3><select data-s="prayerGoal" data-num="1">${[1, 2, 3, 4, 5].map(v => opt(String(v), String(S.prayerGoal), String(v))).join('')}</select></div></div>`;
};

// ---- Zekât
routes.zekat = async () => `${top(t('Zekât hesaplama'))}<div class="view"><div class="card"><h3>${t('Güncel fiyatlar (gram)')}</h3>
  <label>${t('Para birimi')}</label><select id="zc_cur">${['TRY', 'USD', 'EUR', 'GBP', 'SAR', 'AED'].map(c => opt(c, S.cur, c)).join('')}</select><p></p><button class="btn s" data-a="metal">${t('Güncel fiyatı al')}</button><div id="zsrc" class="mut"></div>
  <label>${t('1 gram altın fiyatı')}</label><input id="z_gp" inputmode="decimal" value="${S.gold || ''}"><label>${t('1 gram gümüş fiyatı')}</label><input id="z_sp" inputmode="decimal" value="${S.silver || ''}"></div>
  <div class="card"><h3>${t('Varlıklarınız')}</h3>${[['z_g', 'Altın (gram, 24 ayar karşılığı)'], ['z_s', 'Gümüş (gram)'], ['z_c', 'Nakit'], ['z_b', 'Banka birikimi'], ['z_t', 'Ticari mal (satış değeri)'], ['z_d', 'Borçlar']].map(([i, l]) => `<label>${t(l)}</label><input id="${i}" inputmode="decimal" data-z="1">`).join('')}</div>
  <div class="card"><h3>${t('Sonuç')}</h3><div id="zres"></div><p class="mut">${t("Zekât, nisap miktarı mala bir kameri yıl (Hicri yıl) boyunca sahip olunması hâlinde verilir. Nisap Diyanet İşleri Başkanlığı'nın esas aldığı 80,18 gram altındır. Ayrıntılı durumlar için bir din görevlisine danışınız.")}</p></div></div>`;
function zcalc() {
  const v = id => parseFloat(($('#' + id)?.value || '').replace(',', '.')) || 0; const cur = $('#zc_cur')?.value || 'TRY';
  const gp = v('z_gp'), sp = v('z_sp'); const total = v('z_g') * gp + v('z_s') * sp + v('z_c') + v('z_b') + v('z_t') - v('z_d'); const nis = 80.18 * gp; const f = x => x.toLocaleString(locale(), { maximumFractionDigits: 2 }) + ' ' + cur;
  const el = $('#zres'); if (!el) return;
  el.innerHTML = `<div class="kv"><span>${t('Net zekâta tabi varlık')}</span><b>${f(total)}</b></div><div class="kv"><span>${t('Nisap (80,18 g altın)')}</span><span>${gp ? f(nis) : t('altın fiyatı girin')}</span></div>` +
    (gp ? (total >= nis ? `<div class="kv"><span>${t('Verilecek zekât (%2,5)')}</span><b style="color:var(--em)">${f(total * .025)}</b></div>` : `<p style="color:var(--em)">${t('Varlığınız nisap miktarının altında; zekât düşmez.')}</p>`) : '');
}
async function fetchMetal(cur) {
  const j = async u => { const r = await fetch(u); if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); };
  const [xau, xag] = await Promise.all([j('https://api.gold-api.com/price/XAU'), j('https://api.gold-api.com/price/XAG')]);
  const rate = cur === 'USD' ? 1 : (await j('https://open.er-api.com/v6/latest/USD')).rates[cur]; const oz = 31.1034768;
  return [xau.price / oz * rate, xag.price / oz * rate];
}

// ---- Camiler
routes.camiler = async (rad = '3000') => {
  setTimeout(async () => {
    const box = $('#mres'); if (!box) return;
    try {
      const q = `[out:json][timeout:25];(node["amenity"="place_of_worship"]["religion"="muslim"](around:${rad},${S.place.lat},${S.place.lng});way["amenity"="place_of_worship"]["religion"="muslim"](around:${rad},${S.place.lat},${S.place.lng}););out center 80;`;
      const r = await fetch('https://overpass-api.de/api/interpreter', { method: 'POST', body: 'data=' + encodeURIComponent(q), headers: { 'Content-Type': 'application/x-www-form-urlencoded' } });
      if (!r.ok) throw new Error('Overpass HTTP ' + r.status);
      const els = (await r.json()).elements.map(e => ({ name: e.tags?.['name:tr'] || e.tags?.name || t('Cami / Mescit'), lat: e.lat ?? e.center?.lat, lng: e.lon ?? e.center?.lon })).filter(e => e.lat)
        .map(e => ({ ...e, km: distanceKm(S.place.lat, S.place.lng, e.lat, e.lng) })).sort((a, b) => a.km - b.km);
      box.innerHTML = (els.length ? '' : `<p>${t("Bu yarıçapta OpenStreetMap'te kayıtlı cami bulunamadı. Yarıçapı büyütün.")}</p>`) + els.map(e => `<div class="item"><div class="grow"><b>${esc(e.name)}</b><div class="mut">${e.km.toFixed(2)} km</div></div>
        <a class="btn o s" target="_blank" rel="noopener" href="https://www.openstreetmap.org/directions?route=${S.place.lat},${S.place.lng};${e.lat},${e.lng}">🧭 ${t('Yol tarifi')}</a></div>`).join('') + `<p class="mut">© OpenStreetMap katkıcıları (ODbL) · Overpass API</p>`;
      showMap('mosque', els);
    } catch (e) { box.innerHTML = `<p class="err">${t('Cami verisi alınamadı')}: ${esc(e.message)}<br>${t('İnternet bağlantınızı kontrol edin.')}</p>`; }
  }, 30);
  return `${top(t('Yakındaki camiler'))}<div class="view"><div class="chips">${[1000, 3000, 10000].map(r => `<a class="chip ${+rad === r ? 'on' : ''}" href="#/camiler/${r}" style="text-decoration:none">${r / 1000} km</a>`).join('')}</div><div id="map"></div><p></p><div id="mres">⏳</div></div>`;
};

// ---- Rehber, hakkında
routes.rehber = async (open = '') => {
  const r = await data('content/rehber.json'); const tr = S.lang === 'tr';
  return `${top(t('Namaz rehberi'))}<div class="view"><p class="mut">${t('Hanefi mezhebi esas alınmış, Şafii mezhebindeki farklar ayrıca belirtilmiştir. Kaynak: Diyanet İşleri Başkanlığı, İlmihal.')}</p>
   ${r.map(x => `<div class="card" data-a="nav" data-v="rehber/${open === x.id ? '' : x.id}"><h3>${esc(tr ? x.tr : x.en)}</h3>${x.rekat ? `<b>${esc(x.rekat)}</b>` : ''}
    ${open === x.id ? `${tr ? '' : `<p>${esc(x.en_summary)}</p>`}<ol>${x.steps.map(s => `<li>${esc(s)}</li>`).join('')}</ol>${x.farz ? `<p>${esc(x.farz)}</p>` : ''}${x.safii ? `<p style="color:var(--gold)">${t('Şafii')}: ${esc(x.safii)}</p>` : ''}` : `<div class="mut">${t('Ayrıntılar için dokunun')}</div>`}</div>`).join('')}</div>`;
};
routes.about = async () => {
  const ez = (await data('media/ezan.json')).ezan; let v = {}; try { v = await (await fetch('version.json', { cache: 'no-store' })).json(); } catch {}
  return `${top(t('Hakkında ve lisanslar'))}<div class="view"><div class="card"><h3>NAMAZ VAKTİ: EZAN VE KIBLE</h3><p>NUR VAKTİ GLOBAL · ${esc(v.version || '__BUILD__')}<br>${t('Her vakitte, her yerde, huzurla.')}</p><p class="mut">${t('Reklamsız, hesap gerektirmeyen, verileri yalnızca telefonda saklayan namaz vakti uygulaması.')}</p></div>
   <div class="card"><h3>${t('Veri kaynakları')}</h3><ul>${['Namaz vakitleri: cihaz üzerinde astronomik hesaplama (16 yöntem); isteğe bağlı AlAdhan API (api.aladhan.com).', "Kur'an metni: Kral Fahd Kur'an Basım Kompleksi Uthmani metni, quran-json 3.1.2 paketi (CC BY 4.0).",
     'Türkçe meal: Diyanet İşleri Başkanlığı (tanzil.net); İngilizce: Saheeh International; Latin okunuş: tanzil.net.', "Kur'an sesleri: AlQuran Cloud CDN (cdn.islamic.network) — kayıtların telif hakları sahiplerine aittir; ücretsiz dağıtım için yayımlanmıştır.",
     'Konum listesi: countries-states-cities-database (ODbL 1.0). Harita ve camiler: © OpenStreetMap katkıcıları (ODbL), Overpass API, Nominatim.', 'Altın/gümüş: gold-api.com; döviz: open.er-api.com.'].map(x => `<li>${t(x)}</li>`).join('')}</ul></div>
   <div class="card"><h3>${t('Ezan kayıtları ve lisansları')}</h3>${ez.map(e => `<p>• ${t(e.tr)} — ${esc(e.author)}, ${esc(e.license)}<br><a href="${e.source}" target="_blank" rel="noopener" style="font-size:12px;word-break:break-all">${esc(e.source)}</a></p>`).join('')}</div>
   <div class="card"><h3>${t('Dini içerik')}</h3><p class="mut">${t("Hadis ve dualar Kütüb-i Sitte'deki (Buhârî, Müslim, Ebû Dâvûd, Tirmizî, Nesâî, İbn Mâce) rivayetlerden, kaynak numarasıyla birlikte verilmiştir. Kur'an'dan alınan dualar doğrudan Kur'an metninden gösterilir. Esmâü'l-Hüsnâ: Tirmizî, Deavât 82.")}</p></div>
   <div class="card"><h3>${t('Açık kaynak yazılım')}</h3><p class="mut">Leaflet (BSD-2) · <a href="https://github.com/hknkaradogan-cpu/Ezanvakti">GitHub</a> · <a href="test-raporu.md">${t('Test raporu')}</a> · <a href="gizlilik.html">${t('Gizlilik politikası')}</a></p></div></div>`;
};

// ------------------------------------------------------------------ olaylar
const A = {
  back: () => history.length > 1 ? history.back() : go('#/home'),
  nav: el => go('#/' + el.dataset.v),
  lang: el => { setS({ lang: el.dataset.v }); render(); },
  gps: () => useGps(), manualloc: () => { setS({ onboarded: true }); go('#/location'); }, startapp: () => { setS({ onboarded: true }); askNotif(); render(); },
  ptoggle: () => audio.paused ? audio.play() : audio.pause(), pprev: () => P.i > 0 && playIndex(P.i - 1), pnext: () => P.i + 1 < P.list.length && playIndex(P.i + 1),
  pback: () => audio.currentTime -= 10, pfwd: () => audio.currentTime += 10,
  prepeat: () => { P.repeat = { off: 'one', one: 'all', all: 'off' }[P.repeat]; toast({ one: 'Bu sureyi tekrarla', all: 'Listeyi tekrarla', off: 'Tekrar kapalı' }[P.repeat]); renderPlayer(); },
  pclose: () => { audio.pause(); audio.removeAttribute('src'); P.list = []; renderPlayer(); },
  font: el => { setS({ font: Math.min(48, Math.max(16, S.font + +el.dataset.v)) }); render(); },
  night: () => { setS({ theme: document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark' }); },
  playsurah: el => { const s = +el.dataset.v; playSurahs(S.reciter, el.dataset.y === '1' ? [s] : [...Array(115 - s)].map((_, i) => s + i), s); },
  play: el => playSurahs(el.dataset.r, [+el.dataset.v], +el.dataset.v),
  dlsurah: async el => { const r = (await data('media/reciters.json')).reciters.find(x => x.id === S.reciter); const n = +el.dataset.v; download(r.url.replace('{n}', n), `${n} · ${r.tr}`); toast('İndirme başladı — İndirme merkezinden takip edebilirsiniz'); },
  dl: async el => { const r = (await data('media/reciters.json')).reciters.find(x => x.id === el.dataset.r); const n = +el.dataset.v; download(r.url.replace('{n}', n), `${n} · ${r.tr}`); toast('İndirme başladı — İndirme merkezinden takip edebilirsiniz'); },
  dlall: async el => { const r = (await data('media/reciters.json')).reciters.find(x => x.id === el.dataset.v); toast(`${r.surahs.length} sure indirme kuyruğuna eklendi`);
    for (const n of r.surahs) await download(r.url.replace('{n}', n), `${n} · ${r.tr}`); },
  dlez: el => { download(el.dataset.v, el.dataset.t).then(render); },
  dlpause: el => DL.get(el.dataset.v)?.ctrl.abort(), dlresume: el => { const v = DL.get(el.dataset.v); download(el.dataset.v, v?.title || ''); },
  dlcancel: el => { DL.get(el.dataset.v)?.ctrl.abort(); DL.delete(el.dataset.v); render(); },
  dldel: async el => { (await caches.open('nv-audio')).delete(el.dataset.v); DL.delete(el.dataset.v); render(); },
  bm: el => { const s = +el.dataset.s, a = +el.dataset.n, k = el.dataset.k; const i = DB.bookmarks.findIndex(b => b.s === s && b.a === a && b.k === k);
    if (i >= 0) DB.bookmarks.splice(i, 1); else DB.bookmarks.push({ s, a, k, ts: Date.now() }); saveDB('bookmarks'); el.textContent = k === 'b' ? (i >= 0 ? '📑' : '🔖') : (i >= 0 ? '🤍' : '❤️'); },
  fav: el => { const v = el.dataset.v; const i = DB.favs.indexOf(v); if (i >= 0) DB.favs.splice(i, 1); else DB.favs.push(v); saveDB('favs'); el.textContent = i >= 0 ? '☆' : '⭐'; },
  setreciter: el => { setS({ reciter: el.dataset.v }); render(); },
  off: el => { const k = el.dataset.k; setS(s => ({ offsets: { ...s.offsets, [k]: (s.offsets[k] || 0) + +el.dataset.v } })); render(); },
  aladhan: async () => { const out = $('#alres'); out.textContent = '⏳'; try { const n = todayYMD();
      const u = `https://api.aladhan.com/v1/timings/${String(n.d).padStart(2, '0')}-${String(n.m).padStart(2, '0')}-${n.y}?latitude=${S.place.lat}&longitude=${S.place.lng}&method=${ALADHAN_ID[methodId()]}&school=${S.asr === 'HANAFI' ? 1 : 0}&timezonestring=${encodeURIComponent(S.place.tz)}`;
      const tm = (await (await fetch(u)).json()).data.timings; const d = dayTimes(n);
      out.innerHTML = `<table><tr><th></th>${['imsak', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'].map(k => `<th>${pn(k)}</th>`).join('')}</tr><tr><td>${t('Uygulama')}</td>${['imsak', 'sunrise', 'dhuhr', 'asr', 'maghrib', 'isha'].map(k => `<td>${hhmm(d, k)}</td>`).join('')}</tr><tr><td>AlAdhan</td>${['Fajr', 'Sunrise', 'Dhuhr', 'Asr', 'Maghrib', 'Isha'].map(k => `<td>${tm[k].slice(0, 5)}</td>`).join('')}</tr></table>`;
    } catch (e) { out.textContent = t("AlAdhan'a ulaşılamadı") + ': ' + e.message; } },
  export: () => { const b = new Blob([JSON.stringify({ app: 'NUR VAKTİ GLOBAL', exported: new Date().toISOString(), settings: S, ...DB }, null, 1)], { type: 'application/json' });
    const a = document.createElement('a'); a.href = URL.createObjectURL(b); a.download = 'nurvakti-verilerim.json'; a.click(); },
  wipe: async () => { if (!confirm(t('Namaz takibi, zikir geçmişi, yer imleri, indirilen sesler ve ayarlar kalıcı olarak silinir.'))) return;
    Object.keys(localStorage).filter(k => k.startsWith('nv_')).forEach(k => localStorage.removeItem(k)); try { await caches.delete('nv-audio'); } catch {} location.reload(); },
  saveplace: () => { DB.places.push({ ...S.place }); saveDB('places'); toast('Konum kaydedildi'); render(); },
  useplace: el => { setS({ place: { ...DB.places[+el.dataset.v] } }); go('#/home'); },
  delplace: el => { DB.places.splice(+el.dataset.v, 1); saveDB('places'); render(); },
  pickplace: async el => { await setPlace(window._places[+el.dataset.v]); go('#/home'); },
  nominatim: async () => { const q = $('#ls').value.trim(); if (q.length < 2) return; const box = $('#nomres'); box.textContent = '⏳';
    try { const r = await (await fetch('https://nominatim.openstreetmap.org/search?format=jsonv2&limit=12&addressdetails=1&accept-language=' + S.lang + '&q=' + encodeURIComponent(q))).json();
      window._nom = r.map(x => ({ name: x.display_name.split(',')[0], region: x.display_name.split(',').slice(1).join(',').trim(), lat: +x.lat, lng: +x.lon, country: (x.address?.country_code || '').toUpperCase() }));
      box.innerHTML = window._nom.map((p, i) => `<a class="item" href="javascript:void 0" data-a="picknom" data-v="${i}"><div class="grow"><b>${esc(p.name)}</b><div class="mut">${esc(p.region)}</div></div></a>`).join('') || t('Sonuç yok');
    } catch (e) { box.textContent = 'Arama başarısız: ' + e.message; } },
  picknom: async el => { await setPlace(window._nom[+el.dataset.v]); go('#/home'); },
  coords: async () => { const la = parseFloat($('#clat').value.replace(',', '.')), lo = parseFloat($('#clng').value.replace(',', '.'));
    if (!(la >= -90 && la <= 90 && lo >= -180 && lo <= 180)) return toast('Geçersiz koordinat');
    await setPlace({ name: `${la.toFixed(4)}, ${lo.toFixed(4)}`, lat: la, lng: lo, country: $('select[data-a=country]').value }); go('#/home'); },
  notifperm: async () => { await Notification.requestPermission(); render(); },
  stopezan: () => { ezAudio.pause(); ezAudio.currentTime = 0; },
  preview: el => playEzan(el.dataset.v),
  ztarget: el => { Z.target = +el.dataset.v; render(); },
  zinc: () => { Z.count++; $('#zc').textContent = Z.count; if (Z.vib && navigator.vibrate) navigator.vibrate(Z.target && Z.count % Z.target === 0 ? 400 : 20); if (Z.target && Z.count === Z.target) toast(t('Hedefe ulaşıldı') + ': ' + Z.target); },
  zreset: () => { Z.count = 0; render(); }, zsave: () => { zsave(); toast('Kaydedildi'); render(); },
  zadd: () => { const n = prompt(t('Zikir adı')); if (!n) return; const tg = parseInt(prompt(t('Hedef sayı'), '100')) || 100; DB.customZikir.push({ name: n.trim(), target: tg }); saveDB('customZikir'); render(); },
  zdel: el => { DB.customZikir.splice(+el.dataset.v, 1); saveDB('customZikir'); render(); },
  fast: el => { const k = ymdStr(todayYMD()); if (el.dataset.v) DB.fasts[k] = el.dataset.v; else delete DB.fasts[k]; saveDB('fasts'); render(); },
  log: el => { const d = el.dataset.d, p = el.dataset.v; DB.logs[d] ??= {}; DB.logs[d][p] = !DB.logs[d][p]; saveDB('logs'); render(); },
  kaza: el => { const k = el.dataset.k; DB.kaza[k] = Math.max(0, (DB.kaza[k] || 0) + +el.dataset.v); saveDB('kaza'); render(); },
  metal: async () => { const cur = $('#zc_cur').value; $('#zsrc').textContent = '⏳';
    try { const [g, s] = await fetchMetal(cur); $('#z_gp').value = g.toFixed(2); $('#z_sp').value = s.toFixed(2); setS({ gold: +g.toFixed(2), silver: +s.toFixed(2), cur });
      $('#zsrc').textContent = `${t('Kaynak')}: gold-api.com (ons) · open.er-api.com (kur) · ${new Date().toLocaleString(locale())}`; zcalc(); }
    catch (e) { $('#zsrc').textContent = `Fiyat alınamadı (${e.message}). Lütfen fiyatları elle girin.`; } },
};
document.addEventListener('click', e => { const el = e.target.closest('[data-a]'); if (!el || el.tagName === 'SELECT' || el.tagName === 'INPUT') return; const f = A[el.dataset.a]; if (f) { e.preventDefault(); f(el); } });
document.addEventListener('change', e => {
  const el = e.target;
  if (el.dataset.s) { const v = el.type === 'checkbox' ? el.checked : el.dataset.num ? +el.value : el.value; setS({ [el.dataset.s]: v }); if (el.dataset.s !== 'zikirGoal') render(); return; }
  if (el.dataset.al) { const k = el.dataset.al, f = el.dataset.f; setS(s => ({ alerts: { ...s.alerts, [k]: { ...s.alerts[k], [f]: el.dataset.num ? +el.value : el.value } } })); render(); return; }
  if (el.dataset.a === 'reciter') { setS({ reciter: el.value }); render(); }
  if (el.dataset.a === 'country') go('#/location/' + el.value);
  if (el.dataset.a === 'zname') { zsave(); Z.name = el.value; data('content/zikir.json').then(zs => { const z = zs.find(x => x.okunus === Z.name); const c = DB.customZikir.find(x => x.name === Z.name); Z.target = z?.count ?? c?.target ?? 33; render(); }); }
  if (el.dataset.a === 'zvib') Z.vib = el.checked;
  if (el.id === 'zc_cur') zcalc();
});
document.addEventListener('input', e => { const el = e.target; if (el.id === 'pr') audio.currentTime = +el.value; if (el.id === 'pv') audio.volume = +el.value; if (el.dataset.z || el.id === 'z_gp' || el.id === 'z_sp') zcalc(); });
document.addEventListener('keydown', e => { const el = e.target; if (e.key !== 'Enter' || !el.dataset.enter) return;
  if (el.dataset.enter === 'quran') go('#/quran/' + encodeURIComponent(el.value)); if (el.dataset.enter === 'loc') go(`#/location/${el.dataset.cc}/${encodeURIComponent(el.value)}`); });
function askNotif() { if ('Notification' in window && Notification.permission === 'default') Notification.requestPermission().catch(() => {}); }

// ------------------------------------------------------------------ başlat
(async () => {
  applyTheme();
  try { I18N = await data('i18n.json'); } catch {}
  window.addEventListener('hashchange', render); await render();
  setInterval(() => { tickHome(); window._ramTick?.(); }, 1000);
  setInterval(alarmTick, 15000); alarmTick();
  if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(() => {});
})();
