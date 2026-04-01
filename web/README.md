# Boro Rwjab Bilai Web (PWA)

This folder contains a Progressive Web App version of the Android app.

## Files

- `index.html` - App shell and UI structure
- `styles.css` - Responsive styles (mobile-first)
- `app.js` - Core logic, search/filter/favorites/recents/detail/install flow
- `manifest.json` - PWA metadata and icons
- `service-worker.js` - Offline caching + fallback
- `offline.html` - Offline fallback page
- `data/song.json` - Song data copied from Android assets

## Run locally

Use a local HTTP server (service workers do not work with plain file opening):

### Option A: Python

```bash
cd web
python3 -m http.server 8080
```

Open:

- `http://localhost:8080` on desktop
- `http://<your-lan-ip>:8080` from your phone on same Wi-Fi

### Option B: Node serve

```bash
npm i -g serve
cd web
serve -l 8080
```

## Test PWA install (Android Chrome)

1. Open the app URL in Chrome.
2. Wait for service worker registration.
3. Use the `Install` button in-app (from `beforeinstallprompt`) or browser menu install option.
4. Verify launch opens in standalone mode.

## Test iOS home screen install (Safari)

1. Open the app URL in Safari.
2. Tap Share icon.
3. Tap **Add to Home Screen**.
4. Launch from home screen and confirm standalone behavior.

## Offline test

1. Open app once online to warm cache.
2. Disable network.
3. Reload app; cached shell should load.
4. Navigate to an uncached route and confirm `offline.html` fallback appears.
