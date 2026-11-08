const CACHE_NAME = 'ravex-v1'
const PRECACHE = ['/', '/favicon.png', '/ravexv2.png']

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(PRECACHE)).then(() => self.skipWaiting())
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  )
})

self.addEventListener('fetch', (event) => {
  const req = event.request
  if (req.method !== 'GET') return
  const url = new URL(req.url)
  // Network-first for API calls
  if (url.hostname === 'api.github.com') {
    event.respondWith(
      fetch(req).catch(() => caches.match(req))
    )
    return
  }
  // Cache-first for static assets
  event.respondWith(
    caches.match(req).then((cached) => cached || fetch(req).then((res) => {
      const clone = res.clone()
      caches.open(CACHE_NAME).then((cache) => cache.put(req, clone))
      return res
    }))
  )
})