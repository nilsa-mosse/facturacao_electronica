const CACHE_NAME = 'efacturacao-v2';
const urlsToCache = [
  '/',
  '/login',
  '/css/premium-style.css',
  '/plugins/adminlte/css/adminlte.min.css',
  '/plugins/fontawesome/css/all.min.css',
  '/plugins/jquery/jquery-3.6.0.min.js',
  '/plugins/bootstrap/css/bootstrap.min.css',
  '/plugins/bootstrap/js/bootstrap.bundle.min.js',
  '/manifest.json'
];

// Instalação: Cacheia ativos estáticos
self.addEventListener('install', event => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('Cacheando ativos fundamentais...');
        return cache.addAll(urlsToCache);
      })
  );
});

// Ativação: Limpa caches antigos
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.filter(name => name !== CACHE_NAME)
          .map(name => caches.delete(name))
      );
    })
  );
  return self.clients.claim();
});

// Fetch: Estratégia de Rede com Fallback para Cache
self.addEventListener('fetch', event => {
  // Ignorar pedidos que não sejam GET ou sejam de extensões
  if (event.request.method !== 'GET' || !event.request.url.startsWith('http')) {
    return;
  }

  event.respondWith(
    fetch(event.request)
      .then(response => {
        // Se for uma navegação e houver redirecionamento, tratamos para evitar erro de segurança
        if (event.request.mode === 'navigate' && response.redirected) {
          return Response.redirect(response.url, response.status);
        }
        
        // Opcional: Cachear dinamicamente recursos válidos
        if (response.status === 200 && response.type === 'basic') {
          const responseToCache = response.clone();
          caches.open(CACHE_NAME).then(cache => {
            cache.put(event.request, responseToCache);
          });
        }

        return response;
      })
      .catch(error => {
        console.log('Rede indisponível, recorrendo ao cache:', event.request.url);
        
        return caches.match(event.request)
          .then(cachedResponse => {
            if (cachedResponse) {
              return cachedResponse;
            }

            // Fallback especial para navegação offline
            if (event.request.mode === 'navigate') {
              return caches.match('/login') || caches.match('/');
            }
            
            return new Response('Offline e recurso não cacheado.', {
              status: 503,
              statusText: 'Service Unavailable',
              headers: new Headers({ 'Content-Type': 'text/plain' })
            });
          });
      })
  );
});
