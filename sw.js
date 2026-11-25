// Nur Vakti Global service worker — uygulama kabuğu ve veriler internetsiz çalışır.
const V = 'nv-shell-1.0.21';
const SHELL = ['./', 'index.html', 'css/app.css?v=1.0.21', 'js/app.js?v=1.0.21', 'js/praytimes.js', 'manifest.webmanifest', 'icons/icon-192.png', 'icons/icon-512.png', 'icons/favicon.png',
  'data/i18n.json', 'data/quran/surahs.json', 'data/content/ayet.json', 'data/content/hadis.json', 'data/content/dualar.json', 'data/content/esma.json', 'data/content/zikir.json',
  'data/content/rehber.json', 'data/content/dini_gunler_tr.json', 'data/media/reciters.json', 'data/media/ezan.json', 'data/loc/countries.json', 'audio/ezan/klasik.mp3', 'audio/ezan/kisa.mp3'];
self.addEventListener('install', e => e.waitUntil(caches.open(V).then(c => Promise.allSettled(SHELL.map(u => c.add(u)))).then(() => self.skipWaiting())));
self.addEventListener('activate', e => e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k.startsWith('nv-shell-') && k !== V).map(k => caches.delete(k)))).then(() => self.clients.claim())));
self.addEventListener('fetch', e => {
  const u = new URL(e.request.url);
  if (e.request.method !== 'GET') return;
  if (u.pathname.endsWith('.mp3') || u.pathname.endsWith('.apk')) return; // sesler uygulama içinde blob olarak önbellekten çalınır
  if (u.origin === location.origin) {
    // Uygulama dosyaları: önce önbellek, arka planda güncelle; veri dosyaları ilk kullanımda önbelleğe alınır
    e.respondWith(caches.open(V).then(async c => {
      const hit = await c.match(e.request, { ignoreSearch: u.pathname.endsWith('.json') || u.pathname.endsWith('.mp3') });
      const net = fetch(e.request).then(r => { if (r.status === 200) c.put(e.request, r.clone()).catch(() => {}); return r; }).catch(() => hit || Response.error());
      return hit || net;
    }));
  } else if (u.hostname === 'tile.openstreetmap.org') {
    e.respondWith(caches.open('nv-tiles').then(async c => (await c.match(e.request)) || fetch(e.request).then(r => { c.put(e.request, r.clone()); return r; })));
  }
});
self.addEventListener('notificationclick', e => { e.notification.close(); e.waitUntil(self.clients.matchAll({ type: 'window' }).then(cs => cs[0] ? cs[0].focus() : self.clients.openWindow('./'))); });
