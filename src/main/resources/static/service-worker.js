// Minimal offline-friendly service worker: cache the app shell, always go to network for the API.
const CACHE = "loglife-shell-v1";
const SHELL = [
    ".",
    "index.html",
    "styles.css",
    "app.js",
    "manifest.webmanifest",
    "icons/icon.svg",
];

self.addEventListener("install", (event) => {
    event.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (event) => {
    event.waitUntil(
        caches.keys().then((keys) =>
            Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))
        ).then(() => self.clients.claim())
    );
});

self.addEventListener("fetch", (event) => {
    const url = new URL(event.request.url);

    // Never cache API calls — nutrition data must always be live.
    if (url.pathname.startsWith("/api/")) {
        return; // default network behaviour
    }

    // App shell: cache-first, fall back to network.
    event.respondWith(
        caches.match(event.request).then((cached) => cached || fetch(event.request))
    );
});
