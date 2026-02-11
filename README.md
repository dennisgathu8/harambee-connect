# 🇰🇪 Harambee Stars Connect

![CI](https://github.com/dennisgathu8/harambee-connect/actions/workflows/ci.yml/badge.svg)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**The Kenyan Premier League Digital Platform**

> *"Kila mashabiki ameunganishwa. Kila mechi moja kwa moja. Kila wakati hauwezi kubadilishwa."*
>
> *"Every fan connected. Every match live. Every moment immutable."*

---

## 🌍 What Is This?

Harambee Stars Connect is a **world-class, mobile-first football platform** for the FKF Premier League — built for African connectivity challenges, mobile-first usage, and local community engagement.

Unlike generic football apps, this platform is:
- **Swahili-first** — native language support, not just translated
- **Offline-first** — works on 2G, syncs when connected
- **M-Pesa native** — buy tickets in 3 taps, no credit card needed
- **Community-centered** — built by Kenyans, for Kenyans

## 🏗️ Architecture

```
harambee-stars-connect/
├── src/
│   ├── clj/harambee/          # Clojure API Backend
│   │   ├── core.clj           # Application entry point
│   │   ├── routes.clj         # HTTP API routes (Reitit)
│   │   ├── middleware.clj     # Security middleware stack
│   │   ├── db.clj             # XTDB database layer
│   │   ├── match.clj          # Match domain + standings
│   │   ├── club.clj           # Club profiles + stats
│   │   ├── payments.clj       # M-Pesa Daraja integration
│   │   └── sse.clj            # Server-Sent Events (live)
│   └── cljs/harambee/         # ClojureScript PWA Frontend
│       ├── app.cljs           # Main app entry + routing
│       ├── i18n.cljs          # Swahili/English bilingual
│       ├── offline.cljs       # Offline-first data layer
│       ├── components.cljs    # Shared UI components
│       └── views/             # Page views
│           ├── home.cljs      # Home + live matches
│           ├── match.cljs     # Live Match Centre
│           ├── clubs.cljs     # Club profiles
│           ├── standings.cljs # League table
│           └── tickets.cljs   # M-Pesa ticket purchase
├── resources/
│   ├── public/                # PWA static assets
│   │   ├── index.html         # PWA shell
│   │   ├── css/style.css      # Premium design system
│   │   ├── sw.js              # Service Worker
│   │   └── manifest.json      # PWA manifest
│   └── data/                  # EDN data files
│       ├── clubs.edn          # 18 FKF Premier League clubs
│       ├── fixtures.edn       # Sample match data
│       └── i18n.edn           # Translation dictionaries
├── project.clj                # Leiningen (backend)
├── shadow-cljs.edn            # Shadow-cljs (frontend)
└── package.json               # npm dependencies
```

## 🛠️ Tech Stack

| Layer | Technology | Why |
|-------|-----------|-----|
| **Backend** | Clojure + Ring + Reitit | Composable, immutable, fast |
| **Database** | XTDB (bitemporal) | Every moment immutable — perfect for match data |
| **Frontend** | ClojureScript + Reagent | Reactive UI, compiles to tiny JS bundles |
| **Build** | shadow-cljs | Best ClojureScript build tool, npm interop |
| **Real-time** | Server-Sent Events + core.async | Simpler than WebSockets, works on 2G |
| **Offline** | Service Worker + localStorage | PWA standard, works on all Android |
| **Payments** | M-Pesa Daraja API | Kenya's dominant payment rail |
| **i18n** | Custom EDN dictionaries | Lightweight, no heavy library |

## 🚀 Getting Started

### Prerequisites
- **Java** 11+ (OpenJDK recommended)
- **Leiningen** 2.9+
- **Node.js** 18+ and npm

### Setup
```bash
# Clone
git clone https://github.com/dennisgathu8/harambee-connect.git
cd harambee-connect

# Install frontend dependencies
npm install

# Build ClojureScript frontend
npx shadow-cljs release app

# Start the server
lein run
```

### Development Mode
```bash
# Terminal 1: Start backend
lein run

# Terminal 2: Start frontend dev server with hot reload
npx shadow-cljs watch app
```

Open **http://localhost:3000** in your browser.

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/health` | Health check |
| GET | `/api/matches` | All matches |
| GET | `/api/matches?status=live` | Live matches only |
| GET | `/api/matches/:id` | Match detail |
| GET | `/api/matches/:id/events` | SSE live events stream |
| GET | `/api/clubs` | All 18 FKF clubs |
| GET | `/api/clubs/:id` | Club detail |
| GET | `/api/standings` | League table |
| GET | `/api/tickets/tiers` | Ticket pricing |
| POST | `/api/tickets/purchase` | Initiate M-Pesa payment |

## 💳 M-Pesa Configuration

The platform runs in **sandbox mode** by default (no real payments). To enable production M-Pesa:

```bash
export MPESA_CONSUMER_KEY="your_key"
export MPESA_CONSUMER_SECRET="your_secret"
export MPESA_SHORTCODE="174379"
export MPESA_CALLBACK_URL="https://your-domain.com/api/tickets/callback"
```

## 🔒 Security

- ✅ **No `eval`** — zero dynamic code execution
- ✅ **Input sanitization** — all inputs stripped of HTML/script tags
- ✅ **Rate limiting** — 60 requests/minute per IP
- ✅ **Security headers** — CSP, XSS protection, nosniff
- ✅ **M-Pesa security** — never stores PINs, tokenized only
- ✅ **CORS configured** — controlled cross-origin access

## 🌐 Offline-First

The platform works fully offline:
1. **Service Worker** caches all static assets on install
2. **API responses** cached in localStorage with timestamps
3. **Network-first** strategy for API with cache fallback
4. **Action queue** stores offline actions, replays when connected
5. **Connectivity detection** with automatic sync on reconnect

## 🇰🇪 The Clojure Advantage

| Challenge | Mainstream | Clojure Solution |
|-----------|-----------|------------------|
| Offline sync | Complex conflict resolution | Immutable data + CRDTs |
| Real-time updates | WebSocket complexity | SSE + `core.async` backpressure |
| Feature phones | Separate basic site | ClojureScript → ES3 output |
| Payment integration | Multiple SDKs | Unified M-Pesa in pure Clojure |
| Low bandwidth | JSON bloat | EDN, structural sharing, delta updates |

## 📱 Features

### For Fans
- 📺 **Live Match Centre** — real-time scores, events, lineups
- 🇰🇪 **Swahili-first** — toggle between Swahili and English
- 📡 **Offline Mode** — works without internet
- 💳 **M-Pesa Ticketing** — buy tickets in 3 taps
- 🏟️ **Club Profiles** — all 18 FKF Premier League clubs

### For Clubs
- 📊 **League Standings** — computed live from match data
- 👥 **Squad Management** — player profiles and stats

---

**Built by Kenyans, for Kenyans, in Clojure.** 🇰🇪⚽

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details on how to submit pull requests, report issues, and contribute to the project.

## License

MIT License. See [LICENSE](LICENSE) for details.
