// Minimal offline-friendly service worker: cache the app shell, always go to network for the API.
const CACHE = "loglife-shell-v2";
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

    // App shell: network-first so UI updates reach the phone immediately; the cache is only the
    // offline fallback. (Cache-first froze the shell at install time — stale UI on every deploy.)
    event.respondWith(
        fetch(event.request)
            .then((response) => {
                const copy = response.clone();
                caches.open(CACHE).then((c) => c.put(event.request, copy)).catch(() => { /* best-effort */ });
                return response;
            })
            .catch(() => caches.match(event.request))
    );
});
