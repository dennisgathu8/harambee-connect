// ============================================
// Harambee Stars Connect — Service Worker
// Offline-first: works on 2G, syncs when connected
// ============================================

const CACHE_NAME = 'harambee-v1';
const API_CACHE_NAME = 'harambee-api-v1';

// Static assets to cache immediately
const STATIC_ASSETS = [
    '/',
    '/index.html',
    '/css/style.css',
    '/js/compiled/main.js',
    '/manifest.json'
];

// API endpoints to cache for offline use
const API_ENDPOINTS = [
    '/api/clubs',
    '/api/matches',
    '/api/standings',
    '/api/tickets/tiers'
];

// ---- Install: Cache static assets ----
self.addEventListener('install', (event) => {
    console.log('[SW] Installing Harambee Stars Connect Service Worker');
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then((cache) => {
                console.log('[SW] Caching static assets');
                return cache.addAll(STATIC_ASSETS);
            })
            .then(() => self.skipWaiting())
            .catch((err) => {
                console.log('[SW] Cache failed, continuing:', err);
                return self.skipWaiting();
            })
    );
});

// ---- Activate: Clean old caches ----
self.addEventListener('activate', (event) => {
    console.log('[SW] Activating');
    event.waitUntil(
        caches.keys()
            .then((cacheNames) => {
                return Promise.all(
                    cacheNames
                        .filter((name) => name !== CACHE_NAME && name !== API_CACHE_NAME)
                        .map((name) => caches.delete(name))
                );
            })
            .then(() => self.clients.claim())
    );
});

// ---- Fetch Strategy ----
self.addEventListener('fetch', (event) => {
    const url = new URL(event.request.url);

    // Skip non-GET requests
    if (event.request.method !== 'GET') return;

    // Skip SSE/EventSource requests
    if (url.pathname.includes('/events')) return;

    // API requests: Network-first with cache fallback
    if (url.pathname.startsWith('/api/')) {
        event.respondWith(
            fetch(event.request)
                .then((response) => {
                    // Clone and cache successful responses
                    if (response.ok) {
                        const responseClone = response.clone();
                        caches.open(API_CACHE_NAME).then((cache) => {
                            cache.put(event.request, responseClone);
                        });
                    }
                    return response;
                })
                .catch(() => {
                    // Offline: serve from cache
                    return caches.match(event.request).then((cached) => {
                        if (cached) {
                            return cached;
                        }
                        // Return offline JSON response
                        return new Response(
                            JSON.stringify({
                                offline: true,
                                error: 'Uko nje ya mtandao. Data iliyohifadhiwa inaonyeshwa.',
                                error_en: 'You are offline. Showing cached data.'
                            }),
                            {
                                status: 503,
                                headers: { 'Content-Type': 'application/json' }
                            }
                        );
                    });
                })
        );
        return;
    }

    // Static assets: Cache-first
    event.respondWith(
        caches.match(event.request)
            .then((cached) => {
                if (cached) return cached;
                return fetch(event.request).then((response) => {
                    // Cache new static resources
                    if (response.ok && !url.pathname.startsWith('/api/')) {
                        const responseClone = response.clone();
                        caches.open(CACHE_NAME).then((cache) => {
                            cache.put(event.request, responseClone);
                        });
                    }
                    return response;
                });
            })
            .catch(() => {
                // Ultimate fallback
                if (event.request.destination === 'document') {
                    return caches.match('/index.html');
                }
                return new Response('Offline', { status: 503 });
            })
    );
});

// ---- Background Sync for queued actions ----
self.addEventListener('sync', (event) => {
    if (event.tag === 'sync-actions') {
        console.log('[SW] Background sync triggered');
        // Process queued offline actions when back online
        event.waitUntil(processOfflineQueue());
    }
});

async function processOfflineQueue() {
    // This would read from IndexedDB and replay queued requests
    console.log('[SW] Processing offline queue');
}

// ---- Push Notifications (future) ----
self.addEventListener('push', (event) => {
    if (event.data) {
        const data = event.data.json();
        event.waitUntil(
            self.registration.showNotification(data.title || 'Harambee Stars Connect', {
                body: data.body || 'Mechi mpya!',
                icon: '/icon-192.png',
                badge: '/badge-72.png',
                tag: data.tag || 'match-update',
                data: data
            })
        );
    }
});
