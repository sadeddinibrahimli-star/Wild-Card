# Wild-Card
The final project for Peerstack Academy, as well as a project I would like to work on in the future.

---

## Running locally (step by step)

### Requirements
- **Java 21+** and **Maven 3.9+** (backend)
- **Node.js 18+** and npm (frontend; tested against Node 26)
- **Python 3** + `requests` (QA smoke test only)
- (optional) **Docker** + `docker compose`
- (optional) **Chromium/Chrome** — browser E2E test only

### 1) Configuration (.env)
```bash
cd Wild-Card            # repo root
cp .env.example .env
```
Values required in `.env`:
| Value | Purpose |
|---|---|
| `JWT_SECRET` | JWT signing secret, **min 32 chars** (if empty a dev key is generated + a warning is logged) |
| `ADMIN_SEED_ENABLED=true` | Creates the admin account (default `true`) |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Admin credentials (example: `admin@wildcard.com` / `admin123`) |
| `SEED_ENABLED=true` + `SEED_USER_PASSWORD=demo123456` | Seeds demo users |
| `TMDB_API_KEY` | Movie search; if empty the feature is disabled and manual title entry works |
| `MAIL_*` | If empty, the reset link is written to the console / dev mailbox |

### 2) Backend — port 8080
```bash
set -a; . ./.env; set +a        # load .env into the environment
cd Backend && mvn spring-boot:run
```
Ready? → `curl http://localhost:8080/v3/api-docs` should return **200**.

> **Profile:** the default `dev` uses an **H2 in-memory** database. This means every restart **wipes all data and re-runs the seed** (the admin always gets id=1). Use `docker compose` (PostgreSQL) for a persistent database.

### 3) Frontend — port 5173
```bash
cd Frontend
npm install          # first run only
npm run dev
```
Open **http://localhost:5173**

> The Vite proxy forwards everything to the backend: `/api` → `:8080`, `/ws` → `:8080` (WebSocket included). So no CORS or extra configuration is needed.

### 4) Sign-in
- **Admin:** `admin@wildcard.com` / `admin123` (use the values from `.env` if you changed them)
- **Demo users:** password = `SEED_USER_PASSWORD` (example: `demo123456`)
- **Password reset:** with no SMTP configured, "Forgot password" still works and the link lands in the **dev mailbox**: `GET http://localhost:8080/api/v1/dev/mailbox` (requires an admin token). The UI shows this automatically.
- **Real-time notifications:** on login the frontend automatically opens `ws://localhost:5173/ws` (STOMP + JWT). Header `Authorization: Bearer <token>` (case sensitive).

### 5) Checks / tests
```bash
cd Backend && mvn test                          # 22 unit/integration tests
cd Frontend && npm run build                    # production build
npx @biomejs/biome check --config-path=QA/biome.json Frontend/src   # lint (33 files)

cd QA && npm install                            # first run only (puppeteer-core, ws)
python3 smoke.py                                # 98 API checks (backend must be up on 8080)
node wsprobe.js                                 # 9 STOMP/WebSocket checks
node e2e.js                                     # 28 browser E2E checks (chromium at /usr/bin/chromium)
```
Each prints `N/N PASS` at the end; exit code `0` means everything passed.
If chromium lives elsewhere: `CHROME=/path/to/chrome node e2e.js`.

### 6) Docker alternative (everything with one command)
```bash
cp .env.example .env    # fill in the values (don't leave DB_PASSWORD empty)
docker compose up --build
```
- Frontend: **http://localhost:5173**
- Backend API: **http://localhost:8080**
- PostgreSQL: `localhost:5432` (profile: `docker`, persistent data)

### 7) FAQ
| Question | Answer |
|---|---|
| Backend returns 401/403 | A token is required: `POST /api/v1/auth/login` → `data.accessToken` → `Authorization: Bearer <token>` |
| Data lost after a restart | H2 is an in-RAM database (expected). Use docker compose (Postgres) for persistence |
| WebSocket won't connect | `/ws` works through the Vite proxy with `ws:true`; a stale browser cache → refresh the page |
| Movie search doesn't work | `TMDB_API_KEY` is missing from `.env` (AniList/iTunes need no key) |
| Frontend shows "Network error" | The backend isn't running on 8080 — check the backend log |

### 8) Project structure
```
Backend/    Spring Boot 3 (Java 21) — REST + STOMP WebSocket, H2(dev)/Postgres(docker)
Frontend/   React + Vite (hash routing: #feed, #profile, #admin, ...)
QA/         automated checks: smoke.py, wsprobe.js, e2e.js, biome.json
```