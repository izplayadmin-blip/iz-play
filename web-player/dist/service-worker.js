const CACHE_NAME = 'iz-play-web-v2.1.4'
const APP_SHELL = [
  '/player/',
  '/player/index.html',
  '/player/manifest.json',
  '/player/assets/izplay-logo-login.png',
  '/player/icons/icon-192.svg',
  '/player/icons/icon-512.svg',
  '/player/vendor/swarmcloud-hls.min.js'
]

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(APP_SHELL))
      .then(() => self.skipWaiting())
  )
})

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(keys.filter(key => key !== CACHE_NAME).map(key => caches.delete(key))))
      .then(() => self.clients.claim())
  )
})

self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return
  const url = new URL(event.request.url)
  if (url.origin !== self.location.origin) return
  if (url.pathname.startsWith('/control/') ||
      url.pathname.startsWith('/gateway/') ||
      url.pathname.startsWith('/video-gateway/') ||
      url.pathname.startsWith('/protected-gateway/')) return

  event.respondWith(
    fetch(event.request)
      .then(response => {
        if (response.ok && url.pathname.startsWith('/player/')) {
          const copy = response.clone()
          caches.open(CACHE_NAME).then(cache => cache.put(event.request, copy))
        }
        return response
      })
      .catch(() => caches.match(event.request).then(cached => cached || caches.match('/player/index.html')))
  )
})
