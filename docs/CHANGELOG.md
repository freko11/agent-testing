# Changelog

Full per-story build history for this project: design-gate rationale, bugs
found and fixed, and live-verification notes, in the order the stories were
built (E1-F1-S1 → E6-F2-S1 so far). For current status, commands, and
architecture, see `CLAUDE.md` instead — this file is historical detail, not
a reference doc, and isn't loaded by default the way CLAUDE.md is.

## E1-F1-S1 — local Oracle XE via Docker Compose

E1-F1-S1 (local Oracle XE via Docker Compose) is done. `docker-compose.yml` runs
`gvenzl/oracle-xe:21-slim`, with credentials and host port sourced from `.env` (see
`.env.example` — never commit `.env`). Local data persists to `oracle-data/`
(gitignored). Connect from SQL Developer / `sqlplus` with:
- Host: `localhost`, Port: `1522` (not the default 1521 — this dev machine already
  runs a native Oracle listener on 1521, so the container's host port defaults to
  1522 via `ORACLE_HOST_PORT` in `.env`; override if your machine doesn't conflict)
- Service name: `XEPDB1`
- App user/password: `ORACLE_APP_USER` / `ORACLE_APP_USER_PASSWORD` from `.env`
- SYS password: `ORACLE_PASSWORD` from `.env` (SYSDBA role required for SYS)

Bring the DB up with `docker compose up -d`; first boot takes ~60-90s before the
healthcheck reports healthy (`docker compose ps`).

## E1-F1-S2 — Spring Boot backend skeleton

E1-F1-S2 (Spring Boot backend skeleton) is done. `backend/` is a Maven project
(Java 21, Spring Boot 4.1, package `com.autotrade.dashboard`) with `web`, `actuator`,
`data-jpa`, `validation` starters plus the `ojdbc11` Oracle driver. Run it with
`./mvnw spring-boot:run` from `backend/` (requires `ORACLE_APP_USER_PASSWORD` in the
environment, matching `.env`'s app-user password — Spring Boot reads env vars via
relaxed binding, no extra wiring needed). `/health` (actuator remapped to web
base-path `/`) returns 200 once the Oracle XE container from S1 is up, since the
datasource is already pointed at it (`jdbc:oracle:thin:@//localhost:${ORACLE_HOST_PORT:1522}/XEPDB1`)
— no JPA entities/repositories yet, that's F1.2. Build/test: `./mvnw verify`.

## E1-F1-S3 — React app skeleton

E1-F1-S3 (React app skeleton) is done. `frontend/` is a Vite + React 19 + TypeScript
app with `react-router-dom`, one placeholder route (`/` → `DashboardPage`). Run with
`npm install && npm run dev` from `frontend/`; build with `npm run build`.

## E1-F1-S4 — CI pipeline

E1-F1-S4 (CI pipeline) is done. `.github/workflows/ci.yml` runs on every push and PR:
a `backend` job (`./mvnw -B verify`, Temurin 21) and a `frontend` job (`npm ci && npm run build`,
Node 22). Backend tests run against an in-memory H2 datasource
(`backend/src/test/resources/application.properties` overrides the main Oracle
datasource for the test classpath only) so CI and local `mvn verify` don't need Docker
or `.env` — Oracle is still what the running app uses in dev/paper/prod.
GitHub branch protection on `master` ("failure blocks merge") is now enabled via
`gh api .../branches/master/protection`, requiring the `backend` and `frontend` CI
jobs to pass (strict mode — branch must be up to date) before a PR can merge. This
required making `github.com/freko11/agent-testing` **public** first — branch
protection on private repos needs a paid GitHub plan (Pro/Team), which this account
doesn't have; the user explicitly chose public visibility over upgrading. This
completes E1-F1-S4's acceptance criteria in full.

## E1-F1-S5 — env/config profiles

E1-F1-S5 (env/config profiles) is done. Three Spring profiles in `backend/src/main/resources/`:
- `local` (default — activates automatically if `SPRING_PROFILES_ACTIVE` is unset):
  Docker Compose Oracle XE via `ORACLE_HOST_PORT`/`ORACLE_APP_USER`/`ORACLE_APP_USER_PASSWORD`.
- `paper`: reads `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` (defaults fall back to the same
  local XE instance until a real paper-environment DB exists); this is also where E4's
  Alpaca-paper/Binance-testnet base URLs will be added.
- `prod`: same `DB_*` vars, no defaults — every value must come from the environment,
  so it can't silently start on placeholder config. Not usable until E6's live-mode
  gate exists.

Switch profiles via `SPRING_PROFILES_ACTIVE=paper` (env var) or
`-Dspring-boot.run.profiles=paper` (mvnw) — no code changes needed; verified both
`local` (default) and `paper` bring the app up against the same Docker XE instance
using their respective env-var names.

This completes E1-F1 (local dev environment) — DB, backend, frontend, CI, and profiles
all in place.

## E1-F2 — core data model

E1-F2 (core data model) is done — all three stories:
- **E1-F2-S1**: Oracle tables for `tickers`, `indicator_snapshots`, `broker_credentials`,
  and `orders` (`backend/src/main/resources/db/migration/V1__init_core_schema.sql`),
  with PK/FK/CHECK/UNIQUE constraints — including a DB-level defense-in-depth check
  that stock orders can never carry leverage, ahead of E6's guardrails.
- **E1-F2-S2**: A JPA entity + Spring Data repository per table (`broker`, `indicator`,
  `order`, `ticker` packages under `com.autotrade.dashboard`), with a `CredentialCipherConverter`
  (AES/GCM, key from `CREDENTIAL_ENC_KEY` env var — see `.env.example`) encrypting
  `broker_credentials`' key/secret columns at rest. This is F1.2's minimal safeguard
  only; full key rotation/keystore-backed encryption is separate F1.3 work.
  `CoreDataModelIntegrationTest` proves CRUD round-trips through all four repositories
  plus unique/FK/check-constraint violations.
- **E1-F2-S3**: Flyway (`V1__init_core_schema.sql` + `V2__add_supporting_indexes.sql`)
  is the single source of schema truth; `spring.jpa.hibernate.ddl-auto=validate` on
  every profile (local/paper/prod/test) so Hibernate only checks entity mappings
  against what Flyway already created, never generates DDL itself.

Two real bugs were found and fixed while bringing this into `master` (this code was
originally produced by a background agent run that never committed and skipped the
`Plan`-agent design gate the story called for; it was reviewed, debugged, and verified
against a live Oracle XE container — not just the H2 test profile — before merging):
`BrokerCredential.isActive` needed `@Convert(NumericBooleanConverter.class)` +
`@JdbcTypeCode(SqlTypes.NUMERIC)` to bind a Java `boolean` against an Oracle
`NUMBER(1)` column (plain `boolean` defaults to native `BOOLEAN`, which Oracle XE
21c doesn't have); and the migration's `NOT NULL DEFAULT x` column clauses had to be
reordered to `DEFAULT x NOT NULL` (Oracle requires `DEFAULT` before inline
constraints — H2's Oracle-compatibility mode silently accepted the wrong order, real
Oracle rejected it with `ORA-00907`). Backend tests (`./mvnw verify`, H2 in Oracle
mode) and a live run against the Docker Oracle XE container (`./mvnw spring-boot:run`,
`local` profile — Flyway migrations applied, `/health` returned `UP`) both pass.

A third bug surfaced in CI itself, on the push that landed E1-F2: `backend/mvnw` had
been committed with mode `100644` instead of `100755`, so the `backend` CI job failed
with exit code 126 ("permission denied") the first time GitHub Actions actually
executed it — this is a known Windows/git gotcha, since Windows has no executable
bit, so `mvnw` silently loses it unless explicitly `chmod +x`'d and staged before
commit. Fixed via `git update-index --chmod=+x backend/mvnw`. If a future wrapper
script (`gradlew`, a shell script, etc.) is added from a Windows checkout, check its
mode with `git ls-files -s <path>` before committing — it should read `100755`, not
`100644`.

`.github/workflows/ci.yml` now pins `actions/checkout@v5` and `actions/setup-java@v5`/
`actions/setup-node@v5` (bumped from `@v4`), clearing the "forced to run on Node.js 24"
deprecation warning GitHub surfaced on the first successful CI run (v4 actions still
targeted the deprecated Node 20 runner).

## E1-F3-S1 — broker-credential key rotation

E1-F3-S1 (broker-credential key rotation) is done. `CredentialCipherConverter` (a
transparent JPA converter, single fixed key, no rotation) is replaced by
`CredentialEncryptionService` (`backend/.../broker/CredentialEncryptionService.java`) —
a keyring built at startup from every `CREDENTIAL_ENC_KEY_<ID>` env var (e.g.
`CREDENTIAL_ENC_KEY_V1`, `CREDENTIAL_ENC_KEY_V2`), with `CREDENTIAL_ENC_ACTIVE_KEY_ID`
naming which key new writes use; old and new keys can coexist so rotation never needs
a Big Bang re-encrypt. `BrokerCredentialService` is now the sole entry point for
plaintext broker keys (`store`, `readDecrypted`, `rotateAll`) — `BrokerCredential`'s
ciphertext columns are plain (no `@Convert`), since a transparent converter can't see
the sibling `encryption_key_version` column rotation depends on. `rotateAll()` is
idempotent: re-encrypts every row not on the active key, safe to run repeatedly during
a rotation window. The full operational procedure (generate key → add alongside old →
flip active id → run `rotateAll()` → verify → remove old key) is documented in
`docs/runbooks/credential-key-rotation.md`. `V3__rename_legacy_encryption_key_version.sql`
renames the schema default from `'v1-basic'` to `'v1'` to match the new
`CREDENTIAL_ENC_KEY_V1` naming (no real credentials existed yet, so this was a safe
no-op). `.env.example` updated accordingly. Falls back to a single insecure dev-only
key (logged warning) if no `CREDENTIAL_ENC_KEY_*` vars are set, same posture as before.
Tested via `CredentialEncryptionServiceTest` (keyring/rotation logic, unit) and
`BrokerCredentialServiceRotationTest` (end-to-end rotation against the real H2/Oracle-mode
repository) — both new, plus the existing `CoreDataModelIntegrationTest` updated to go
through `BrokerCredentialService` instead of relying on transparent converter round-trips.

A real-Oracle-only bug surfaced while verifying E1-F3-S1 against the Docker XE
container (H2's Oracle-compatibility mode didn't catch it): `V3`'s
`ALTER TABLE ... MODIFY encryption_key_version VARCHAR2(20) DEFAULT 'v1' NOT NULL`
passed on H2 but real Oracle rejected it with `ORA-01442` (column to be modified to
NOT NULL is already NOT NULL) — real Oracle refuses to re-declare a `NOT NULL` a
column already has, which H2's Oracle mode silently allows. Fixed by dropping the
redundant `NOT NULL` clause (`MODIFY (encryption_key_version VARCHAR2(20) DEFAULT 'v1')`),
which changes only the default and leaves the existing constraint untouched on both
databases. The one failed-migration row this left in `flyway_schema_history` on the
local XE container was deleted manually before re-running — a real Oracle instance,
unlike H2, persists that state across app restarts. If a future migration ever fails
against the local container, check `flyway_schema_history` for a `success=0` row
before assuming the migration file itself is still broken.

## E1-F3-S2 — dashboard login

E1-F3-S2 (dashboard login) is done, closing out F1.3. `SecurityConfig`
(`backend/.../security/`) adds Spring Security with a single in-memory operator
account (`DASHBOARD_USERNAME`/`DASHBOARD_PASSWORD_HASH`, or `DASHBOARD_PASSWORD`
in local — bcrypt-encoded at startup with a logged warning, same posture as the
`CREDENTIAL_ENC_KEY` dev fallback) — no user table, since this is a single-user tool.
Session-cookie auth (`formLogin` at `POST /api/auth/login`, `POST /api/auth/logout`),
CSRF via `CookieCsrfTokenRepository` + a custom `SpaCsrfTokenRequestHandler` (the
default `XorCsrfTokenRequestAttributeHandler` XOR-masks tokens for server-rendered
forms, which breaks a plain cookie-reading SPA — this is the pattern Spring
Security's own docs recommend for that case) plus a `CsrfCookieWriteFilter` that
forces the deferred CSRF token to materialize into a cookie on every request (a pure
REST API never renders `_csrf`, so nothing else triggers that write). `AuthController`
exposes `GET /api/auth/csrf` (primes the cookie) and `GET /api/auth/me` (session
check on page load). `/health` (not `/actuator/health` — this app's actuator base
path is remapped to `/`, per E1-F1-S2) stays `permitAll` for the Docker/CI healthcheck;
`management.endpoint.health.show-details` is now explicitly `never` so it can't leak
DB details unauthenticated. Session timeout is set per profile: `local` 2h, `paper`
30m, `prod` 15m. Frontend: `frontend/src/auth/AuthContext.tsx` (session state +
login/logout), `RequireAuth.tsx` (route guard), `pages/LoginPage.tsx`; `vite.config.ts`
proxies `/api` and `/actuator` to `localhost:8080` in dev so the browser sees
same-origin (no CORS needed) — how `paper`/`prod` will serve the built frontend
(bundled vs. separate origin, needing real CORS config) is still undecided, flagged
for whenever a real deploy story picks this up. Verified via `SecurityConfigTest`
(MockMvc: unauthenticated → 401, `/health` public, wrong password → 401, login →
session → logout → 401 again) and manually end-to-end — `run` skill, real Oracle XE
container, `curl` through the full CSRF/login/session/logout cycle, then clicked
through the same flow in a browser (login redirect, dashboard, logout redirect).
`security-review` skill run against both F1.3 stories before landing; one finding
fixed: `BrokerCredentialService.DecryptedCredential` is a record, so its default
`toString()` would have printed both plaintext fields if anything ever accidentally
logged it — overridden to redact, ahead of E4 adapters that will be the first real
callers of `readDecrypted()`.

This closes out F1.3 (secrets & config management) and E1-F1 through E1-F4 except
E1-F4 itself (testing strategy — E1-F4-S1's mock-broker E2E test and E1-F4-S2's
indicator fixtures are still open, deferred since they're more naturally written once
E2's signal engine and E4.1's mock adapter exist to exercise).

## E2-F1-S1 — ticker price-history ingestion

E2-F1-S1 (ticker price-history ingestion) is done, starting E2 (Signal Engine). A
`Plan`-agent design gate resolved the two assumptions `docs/agile-plan.md` had
deliberately left open for this story: **a ticker must be explicitly registered with
its asset type via `POST /api/tickers` before price history can be fetched for it**
(no symbol-shape heuristic — `Ticker.assetType` is already a mandatory schema column,
and this app favors deterministic rules over fuzzy inference everywhere else); and
Alpaca market-data credentials are simple env-var config (`ALPACA_API_KEY`/
`ALPACA_API_SECRET`, new in `.env.example`), deliberately *not* routed through
`BrokerCredentialService` — that service is scoped to E4's trading credentials, tied
to a "connect my account" flow that doesn't exist yet.

New `com.autotrade.dashboard.marketdata` package: `MarketDataClient` interface
(intentionally separate from E4's future `BrokerAdapter` — reading public candles and
placing money-moving orders don't share enough concerns to justify one interface),
`AlpacaMarketDataClient` (`GET /v2/stocks/bars`, auth required even for read-only
data) and `BinanceMarketDataClient` (`GET /api/v3/klines`, fully public) implement it;
`MarketDataService` routes by `Ticker.assetType` to the right client; `RetryHelper`
gives both clients one bounded retry (I/O errors, 5xx, 429) — deliberately not a
pluggable resilience framework, since E4-F1-S2 owns the trading-path retry/backoff
contract with different requirements (idempotency). `GET
/api/tickers/{symbol}/price-history?limit=N` (default 200, bounded 1–1000, daily
candles only for v1) returns `{ticker, source, candles}`; `MarketDataExceptionHandler`
(the app's first `@RestControllerAdvice`) maps ticker/market-data failures to
structured JSON errors: 404 `TICKER_NOT_REGISTERED`/`NO_PRICE_DATA`, 409
`ASSET_TYPE_CONFLICT`, 429 `MARKET_DATA_RATE_LIMITED` (echoes `Retry-After`), 503
`MARKET_DATA_UNAVAILABLE`. New `com.autotrade.dashboard.ticker.TickerService`/
`TickerController` handle registration (idempotent re-registration, 409 on asset-type
conflict, symbols normalized to uppercase). Every profile (`local`/`paper`/`prod`/
test) got `marketdata.alpaca.*`/`marketdata.binance.*` properties — `paper` uses the
real public Binance API (not testnet) for indicator accuracy, an intentional,
flagged divergence from E4/E5's Binance *testnet* trading prices. Tested via
`AlpacaMarketDataClientTest`/`BinanceMarketDataClientTest` (`MockRestServiceServer`
against checked-in fixture JSON, no live HTTP in CI), `MarketDataServiceTest`
(Mockito, asset-type routing), `TickerServiceTest` (H2, find-or-register/conflict),
and `MarketDataControllerTest`/`TickerControllerTest` (`@WebMvcTest`+`MockMvc`,
status codes and error bodies) — 41 backend tests total, `./mvnw verify` green.

Verified live end-to-end via the `run` skill against the real Docker Oracle XE
container and the real public Binance API (no mocking): registered `AAPL`
(STOCK) and `BTCUSDT` (CRYPTO) via `POST /api/tickers`, then `GET
.../price-history` — `BTCUSDT` returned real live daily candles from Binance
(200), `AAPL` correctly 503'd with `MARKET_DATA_UNAVAILABLE` since no real
`ALPACA_API_KEY` is configured on this dev machine (expected — Alpaca can't be
verified live without real paper-trading credentials), and an unregistered
symbol correctly 404'd with `TICKER_NOT_REGISTERED`.

A real-Oracle-only bug surfaced during that live verification, affecting every
`Instant`-typed column in the schema, not just this story's new code: Hibernate 7
maps `java.time.Instant` fields to a JDBC type that requests `OffsetDateTime` from
the driver, but every timestamp column in `V1__init_core_schema.sql` is a plain
`TIMESTAMP(6)` with **no** timezone — Oracle's JDBC driver (ojdbc11) refused with
`ORA-18716: {0} not in any time zone` the moment anything actually read such a
column back (H2's Oracle-compatibility mode never caught this, since this is the
first story to read an `Instant` column back through a live query in normal app
flow — earlier live-Oracle checks only exercised `/health` and the login flow, never
a repository read touching one of these columns). Fixed by adding
`@JdbcTypeCode(SqlTypes.TIMESTAMP)` to all nine affected fields across four entities
(`Ticker.createdAt`; `IndicatorSnapshot.snapshotAt`/`createdAt`;
`BrokerCredential.createdAt`/`updatedAt`; `Order.submittedAt`/`filledAt`/
`createdAt`/`updatedAt`), which tells Hibernate to read them as plain
non-timezone-aware timestamps instead. If a future entity adds an `Instant` column
mapped to a plain `TIMESTAMP` (not `TIMESTAMP WITH [LOCAL] TIME ZONE`), it needs
this same annotation — verified live-Oracle re-read of `Ticker.createdAt` succeeds
after the fix; full test suite (41 tests, H2) still green since H2's Oracle-mode
never reproduced this in the first place.

## E2-F1-S2 — clear error for an unregistered ticker

E2-F1-S2 (clear error for an unregistered ticker) is done — as anticipated, this was
entirely a frontend story, since S1 already produced the `TICKER_NOT_REGISTERED` 404
this consumes. New `frontend/src/marketdata/` package: `api.ts` (`fetchPriceHistory`,
wrapping the existing `apiFetch` helper; throws a typed `MarketDataError` carrying the
backend's structured `error` code so callers branch on it instead of pattern-matching
strings) and `TickerLookup.tsx` (a form wired into `DashboardPage`, mapping each of
`MarketDataExceptionHandler`'s error codes — `TICKER_NOT_REGISTERED`, `NO_PRICE_DATA`,
`MARKET_DATA_RATE_LIMITED`, `MARKET_DATA_UNAVAILABLE` — to a specific rendered message
rather than one generic failure string; codes without a mapping fall back to the
backend's own `message`). This is intentionally minimal — a symbol input, a lookup
button, and a one-line result/error — not the full metrics display, which is E3-F1-S1's
job. One build-time fix: `MarketDataError extends Error` couldn't use a constructor
parameter property (`public readonly code: ...`) under this project's
`erasableSyntaxOnly` TS setting (same class of restriction as decorators/enums —
TS1294); rewritten as a plain field assignment in the constructor body. Verified live
via the `run` skill against the real running stack (Oracle XE + Spring Boot + Vite dev
server, browser click-through, not just `npm run build`): looking up an unregistered
symbol (`NOTREAL`) rendered `"NOTREAL" isn't a registered ticker yet. Register it
before looking up price history.`; looking up the already-registered `BTCUSDT`
immediately after rendered `BTCUSDT: 200 candles from BINANCE.` and correctly cleared
the prior error. No backend changes were needed for this story.

## E2-F1-S3 — market-hours handling

E2-F1-S3 (market-hours handling) is done, closing out F2.1. A `Plan`-agent design gate
resolved the detection approach: a new `MarketHoursService`
(`backend/.../marketdata/`) implements a hardcoded NYSE/NASDAQ regular-hours calendar
(09:30-16:00 `America/New_York`, Mon-Fri, both boundaries inclusive on open/exclusive
on close) rather than calling Alpaca's separate `/v2/clock` endpoint (a new network
dependency and outage mode for a fact that's actually deterministic) or pulling in a
market-hours library (unnecessary dependency for ~15 lines of `java.time` logic, same
bias as `RetryHelper`'s "deliberately not a pluggable resilience framework"). Holiday
and early-close awareness are explicitly out of scope for v1 — flagged, not silently
decided. `MarketHoursService` takes a constructor-injected `java.time.Clock` (new bean
in `MarketDataClientConfig`, `Clock.systemUTC()` in production) so tests supply
`Clock.fixed(...)` instead of depending on wall-clock time — the app's first `Clock`
consumer. `MarketDataService.getPriceHistory` checks it only for `AssetType.STOCK`,
after the ticker-registration check but before calling `AlpacaMarketDataClient` — so
an unregistered stock symbol still 404s regardless of hours, Alpaca is never called
overnight/weekends (a free reliability win), and crypto's routing path never touches
the check at all (no bypass flag needed). New `MarketClosedException` maps to a new
`MARKET_CLOSED` error code, HTTP 409 (`MarketDataExceptionHandler`) — 409 fits this
codebase's existing "conflicts with current system state" precedent
(`ASSET_TYPE_CONFLICT`), not reused 503 since that means a genuine provider outage,
not an expected/scheduled closed state. Frontend: one new entry each in
`api.ts`'s `MarketDataErrorCode` union and `TickerLookup.tsx`'s error-message map,
following the exact existing pattern. Tested via new `MarketHoursServiceTest` (9 pure
unit tests, no Spring context, fixed clocks covering open/closed boundaries, weekends,
and both EDT/EST to prove DST is handled via `ZoneId` rules rather than a hand-rolled
offset), updated `MarketDataServiceTest` (proves crypto never calls
`marketHoursService`, and that an unregistered stock still short-circuits to
`TICKER_NOT_REGISTERED` before any hours check), and a new `MarketDataControllerTest`
case (409 + `MARKET_CLOSED` body). All 52 backend tests pass; frontend builds clean.

Verified live via the `run` skill against the real running stack: at the time of
verification (~22:23 ET, after regular hours), `GET /api/tickers/AAPL/price-history`
correctly returned 409 `MARKET_CLOSED` (both via `curl` and clicking through
`TickerLookup` in the browser, rendering the new message), while `BTCUSDT` was
completely unaffected (200, live Binance candles) in both checks. One environment
gotcha hit during this verification, not a code bug: a stale `java.exe` process from
an earlier dev session was still holding port 8080 with pre-fix code, so the first
verification round silently tested against old behavior (`AAPL` returned a live 503
from a real Alpaca call instead of 409, since the old code lacked the hours check).
Caught via `Web server failed to start. Port 8080 was already in use` in the new
process's own log after `mvnw spring-boot:run` — worth checking for a leftover backend
process (`netstat -ano | grep :8080`) before trusting a live verification's result if
`/health` was already returning 200 suspiciously fast on startup.

## E2-F2-S1 — RSI, MACD, and moving-average crossover computed for a ticker

E2-F2-S1 (RSI, MACD, and moving-average crossover computed for a ticker) is done,
starting F2.2 (technical indicator calculation). The `docs/agile-plan.md` "Assumptions
to confirm" indicator-library choice was resolved first via the `Explore` agent:
hand-roll RSI/MACD/MA-crossover rather than pull in `ta4j` — its `BarSeries`/`Num`
data model doesn't map cleanly onto this app's existing `Candle` DTO, and this
codebase already has two precedents (`RetryHelper`, `MarketHoursService`) for
hand-rolling small deterministic logic rather than taking on a library's adapter
cost. A `Plan`-agent design gate then fixed every formula/parameter choice before
implementation: RSI-14 (Wilder smoothing), MACD(12,26,9) (SMA-seeded EMA
convention), and a **SMA10/SMA30** crossover — deliberately not the golden-cross
50/200 pair, since the product vision's hold-term example ("3-10 days", E2-F3-S2)
calls for something responsive enough to matter within a multi-day hold.

New flat `com.autotrade.dashboard.indicator` package (alongside the existing
`IndicatorSnapshot` entity/repository from E1-F2, which already anticipated every
field this story populates — no Flyway migration needed): `RsiCalculator`,
`MacdCalculator`/`MacdResult`, `MovingAverageCrossoverCalculator`/
`MovingAverageResult`/`MovingAverageRelation` are pure static calculators over
`List<Candle>` (each enforces its own minimum-candle-count precondition,
ascending-timestamp order assumed, never re-sorted). `IndicatorService` calls
`MarketDataService.getPriceHistory` directly (a service-to-service handoff, not a
second HTTP hop) — this means ticker-registration and market-hours checks are
inherited for free, and an unregistered/closed-market ticker fails exactly as
`GET .../price-history` already does. `MIN_CANDLES_FOR_INDICATORS = 34` (MACD's
`slowPeriod + signalPeriod - 1` is the binding constraint across all three
indicators); fewer candles throws a new `InsufficientPriceHistoryException` → 422
`INSUFFICIENT_PRICE_HISTORY` (distinct from `NO_PRICE_DATA`'s zero-data case).
Every computation persists an `IndicatorSnapshot` row (append-only, no
unique constraint on ticker+day — matching this codebase's other append-only
audit-log-style tables; revisit only if a periodic-computation story arrives).
`GET /api/tickers/{symbol}/indicators?limit=N` (default 200, bounded
34-1000) returns ticker/source/asOf/price plus `rsi`, `macd` (line/signal/
histogram), and `movingAverage` (shortMa/longMa/relation). Error handling extends
the existing `MarketDataExceptionHandler` rather than adding a parallel handler,
per its own Javadoc's stated extension point — reused as-is: `TICKER_NOT_REGISTERED`
(404), `MARKET_CLOSED` (409), `NO_PRICE_DATA` (404), rate-limit/unavailable (429/503);
newly added: `INSUFFICIENT_PRICE_HISTORY` (422) and `INVALID_REQUEST` (400, limit
out of bounds) for indicator-specific failures. `TickerSummary.from` was bumped
from package-private to `public static` (one-line visibility change) so the
`indicator` package can reuse it in the response DTO.

Reference values for RSI/MACD/SMA were computed independently via a Python/Decimal
script (50-digit precision) before writing any Java, then cross-checked against the
Plan agent's own computed values — both matched exactly, including the RSI edge
cases most likely to be implemented wrong (`avgGain==0 && avgLoss==0` → neutral 50,
not the all-gains 100 case) and the exact MACD signal-line seed boundary at the
34-candle minimum. `IndicatorTestFixtures` (40 hardcoded daily closes, degenerate
OHLC, no JSON/HTTP involved unlike the Alpaca/Binance client fixtures) also
substantially fulfills the still-open **E1-F4-S2** ("deterministic fixture data for
indicator math") for this indicator set — only E2-F2-S2's volatility/volume fixture
(needing real high/low spread) remains open there. Tested via
`RsiCalculatorTest`/`MacdCalculatorTest`/`MovingAverageCrossoverCalculatorTest`
(exact reference-value + edge-case + minimum-candle-count assertions),
`IndicatorServiceTest` (Mockito: calculator wiring, snapshot persistence, and that
`MarketDataService` failures propagate unmodified), and `IndicatorControllerTest`
(`@WebMvcTest`, status codes and error bodies) — 20 new tests, 72 backend tests
total, `./mvnw verify` green.

Verified live via the `run` skill against the real running stack (Docker Oracle XE
+ real public Binance API, no mocking) — one gotcha hit before verification even
started: a stale `java.exe` (PID from an earlier session, started before this
story's code existed) was still holding port 8080, caught via the same
`netstat -ano | grep :8080` check this file's E2-F1-S3 entry already recommends;
killed and restarted clean. `GET /api/tickers/BTCUSDT/indicators?limit=200`
returned live 200 with real numbers (`rsi: 45.3816`, `macd.line: 191.31058207`,
`movingAverage.relation: "SHORT_ABOVE_LONG"`); a direct `sqlplus` query against
the live Oracle XE container confirmed the persisted `indicator_snapshots` row
matched the API response exactly. `GET .../NOTREAL/indicators` correctly 404'd
`TICKER_NOT_REGISTERED`, `limit=10` correctly 400'd `INVALID_REQUEST` ("limit must
be between 34 and 1000"), and `GET .../AAPL/indicators` (a stock, checked
after-hours) correctly 409'd `MARKET_CLOSED` — proving the inherited
ticker-registration/market-hours checks work through the new endpoint exactly as
through `price-history`. No frontend changes in this story (backend-only, same
split as E2-F1-S1/S2 — indicator consumption in the UI is a later story).

## E2-F2-S2 — volatility/volume-trend metric

E2-F2-S2 (volatility/volume-trend metric) is done, closing out F2.2. A `Plan`-agent
design gate picked the exact formulas before implementation: **ATR-14, normalized as
a percentage of the latest close** (`ATR% = ATR / close * 100`) for volatility —
Wilder-smoothed, reusing RSI-14's exact seed-then-smooth recursion shape, and
percentage-normalized (rather than raw ATR) so volatility is comparable across
tickers of very different price scales (a stock vs. a crypto pair), matching RSI's
own bounded-percentage precedent; and a **10/30 volume-SMA ratio**
(`SMA(volume,10) / SMA(volume,30)`) for volume-trend — reusing the same short/long
period pair `MovingAverageCrossoverCalculator` already established for price,
generalized to run over `Candle.volume()` instead of `Candle.close()`. Both were
weighed against alternatives (stddev-of-returns, which needs `BigDecimal` to
approximate `ln()`; OBV, an unbounded cumulative running total needing a separate
slope step) and rejected as materially more complex for no accuracy gain over data
this codebase already has. Both fit comfortably under the existing 34-candle
minimum (ATR-14 needs 15, volume-trend needs 30) — no change to
`IndicatorService.MIN_CANDLES_FOR_INDICATORS`, and no new Flyway migration, since
`indicator_snapshots.volatility`/`volume`/`volume_trend` were already added nullable
in `V1__init_core_schema.sql` anticipating exactly this story.

New `com.autotrade.dashboard.indicator.VolatilityCalculator`/`VolumeTrendCalculator`
(pure static, `List<Candle>` in, same precondition-then-throw pattern as
RSI/MACD/MA). `MovingAverageCrossoverCalculator`'s private `sma` helper was widened
to package-private and generalized with a `Function<Candle, BigDecimal>` value
extractor so `VolumeTrendCalculator` reuses it against `Candle::volume` rather than
duplicating the loop a third time. `VolumeTrendCalculator.calculate` returns `null`
(not an exception) when the long-period volume SMA is zero — a genuinely dead/illiquid
ticker is valid market data this story exists to surface, not a caller error, and the
column/DTO field was already nullable for this reason; it's the first calculator in
this package with a legitimate null-return contract, documented in its Javadoc since
RSI/MACD/MA never return null. `IndicatorService.compute`/`computeIndicators` and
`IndicatorResponse` both extended with `volatility`/`volume`/`volumeTrend` fields; no
changes needed to `IndicatorController` or `MarketDataExceptionHandler` — no new
failure mode is introduced beyond the existing 34-candle gate.

Reference values were computed the same way as E2-F2-S1: an independent Python/Decimal
script (50-digit precision) before writing any Java. `IndicatorTestFixtures` gained an
**OHLCV_40** dataset (`HIGHS_40`/`LOWS_40`, ~1.6% daily range around each of the
existing degenerate `CLOSES_40` closes) plus three volume variants — a step-up (last
10 candles 3x the first 30's volume, ratio clearly >1), a mirrored decline (ratio
clearly <1), and an all-zero window (exercises the null-return path) — finally closing
E1-F4-S2's one remaining gap (the RSI/MACD/MA fixture set's degenerate high=low=close
data couldn't exercise real ATR spread). The existing degenerate `candles40()` also
got new reference values (`ATR_PCT_DEGENERATE_FULL`, `VOLUME_TREND_DEGENERATE_FULL`,
`VOLUME_DEGENERATE_FULL`) since it's shared with `IndicatorServiceTest`'s RSI/MACD/MA
assertions, and that test's degenerate candle set now also produces real (non-null)
volatility/volume/volumeTrend values instead of the nulls it asserted before this
story. Tested via `VolatilityCalculatorTest`/`VolumeTrendCalculatorTest` (reference-value,
boundary, degenerate-data, and null-path cases) plus updated `IndicatorServiceTest`/
`IndicatorControllerTest` — 10 new tests, 82 backend tests total, `./mvnw verify` green.

Verified live via the `run` skill against the real running stack (Docker Oracle XE +
real public Binance API, no mocking) — `GET /api/tickers/BTCUSDT/indicators?limit=200`
returned live 200 with real numbers (`volatility: 2.6121`, `volume: 2484.1459`,
`volumeTrend: 0.8414`); a direct `sqlplus` query against the live Oracle XE container
confirmed the persisted `indicator_snapshots` row matched the API response exactly.
`GET /api/tickers/AAPL/indicators` (a stock, checked after-hours) still correctly
409'd `MARKET_CLOSED`, confirming no regression to E2-F1-S3's market-hours check.
No frontend changes in this story (backend-only, same split as E2-F2-S1).

This closes out E2-F2 (technical indicator calculation) in full.

## E2-F3-S1 — indicators combined into a single Buy/Sell/Hold call

E2-F3-S1 (indicators combined into a single Buy/Sell/Hold call) is done, starting
F2.3. A `Plan`-agent design gate fixed the rule table before implementation: three
**safety gates** run first and can only ever force HOLD (`NO_VOLUME_DATA` when
volume-trend is null, `VOLUME_DRIED_UP` when the 10/30-day volume ratio is
below 0.20, `VOLATILITY_TOO_EXTREME` when ATR% is above 8.0), then a **2-of-3
directional vote** across RSI (oversold `<30`/overbought `>70`), MACD histogram
sign, and MA-crossover relation decides BUY/SELL — any bullish/bearish split
(at least one of each) falls through to `CONFLICTING_SIGNALS` (HOLD), and fewer
than two agreeing indicators falls through to `NO_STRONG_SIGNAL` (HOLD). RSI
30/70 are the conventional thresholds; the two new gate thresholds (ATR% > 8.0,
volume ratio < 0.20) are provisional engineering estimates, explicitly **not
yet validated against real price history** — flagged for revisit once
E2-F4-S1's backtest harness exists, per `signal-rule-review.md`'s own "don't
ship a rule-table change on unit tests alone" checklist item. Volume-trend and
volatility are gates, not votes — they only ever suppress a call, never
produce one, matching E2-F2-S2's stated "spot dead or erratic tickers" purpose.

New `com.autotrade.dashboard.signal` package (split from `indicator`, mirroring
the existing `marketdata`/`indicator` split even though one calls the other
directly): `SignalRuleId` (an enum of all 9 rule-table branches, each carrying
its `SignalCall` and a human-readable rationale string — this enum *is* the
documented threshold table) and `SignalRuleEngine` (pure static `evaluate`,
plus `RULE_TABLE_VERSION = "v1"` so a future threshold revision is an
auditable, versioned change feeding E6-F3-S2's rule-provenance requirement).
`SignalCallEntry` (a new JPA entity, named "Entry" to avoid colliding with the
`SignalCall` enum) persists every call, FK'd to both `Ticker` and the specific
`IndicatorSnapshot` that produced it — append-only, no unique constraint on
ticker+day, the same audit-log-style pattern as `indicator_snapshots` (whose
own comment had anticipated exactly this table). `IndicatorService` gained a
public `computeForSignal` (returning both the response DTO and the persisted
`IndicatorSnapshot`) so `SignalService` reuses the *same* snapshot instead of
computing/persisting a second one per request; `computeIndicators` is now a
thin wrapper over it, unchanged behavior. `GET /api/tickers/{symbol}/signal`
(new `SignalController`, same limit bounds/validation as `/indicators`) returns
`{ticker, call, matchedRule, ruleRationale, ruleTableVersion, indicators}` —
nesting the existing `IndicatorResponse` rather than flattening its fields.
No new error codes: every failure mode (`TICKER_NOT_REGISTERED`,
`MARKET_CLOSED`, `NO_PRICE_DATA`, rate-limit/unavailable,
`INSUFFICIENT_PRICE_HISTORY`, `INVALID_REQUEST`) is inherited unmodified
through the existing `MarketDataExceptionHandler`, since `SignalService`
delegates straight to `IndicatorService.computeForSignal`.
`V4__add_signal_calls.sql` adds the `signal_calls` table/sequence/indexes.

Tested via `SignalRuleEngineTest` (17 tests: one per rule-table branch, plus
RSI/volatility/volume-trend boundary values proving thresholds are exclusive
where documented, plus priority-ordering tests proving a gate wins even when
a later condition would also match), `SignalServiceTest` (Mockito: engine
invoked with the right fields, persisted `SignalCallEntry` carries the correct
snapshot FK/ticker/version/matchedRule, `IndicatorService` failures propagate
without persisting), and `SignalControllerTest` (`@WebMvcTest`, happy-path
shape plus representative inherited-error cases) — 25 new tests, 107 backend
tests total, `./mvnw verify` green. `simplify` and `signal-rule-review` skills
both run clean: the rule table stayed a plain enum-driven mapping (no generic
rule-engine framework), determinism holds (pure function of already-computed
indicator values, no wall-clock/randomness), every branch has its own test,
and the rule-table version is in place for E6-F3-S2 ahead of need.

Verified live via the `run` skill against the real running stack (Docker
Oracle XE + real public Binance API, no mocking; logged in through E1-F3-S2's
session-cookie auth first, since guardrail testing now requires it) — `V4`
applied cleanly against real Oracle (schema version 3 → 4). `GET
/api/tickers/BTCUSDT/signal` returned live 200: `call: "HOLD"`, `matchedRule:
"CONFLICTING_SIGNALS"` (RSI 45.97 neutral, MACD histogram negative/bearish,
MA relation SHORT_ABOVE_LONG/bullish — one dissenting vote each way, correctly
suppressed rather than guessing a direction). A direct `sqlplus` query against
the live container confirmed the persisted `signal_calls` row matched the API
response exactly (`call=HOLD`, `matched_rule=CONFLICTING_SIGNALS`,
`rule_table_version=v1`, correct ticker/snapshot FKs). `GET
.../NOTREAL/signal` correctly 404'd `TICKER_NOT_REGISTERED`, `limit=10`
correctly 400'd `INVALID_REQUEST`, and `GET .../AAPL/signal` (a stock, checked
after-hours) correctly 409'd `MARKET_CLOSED` — proving inherited
error-handling works through the new endpoint exactly as through
`/indicators`. No frontend changes in this story (backend-only; the call/badge
UI is E3-F1-S2's job). E2-F3-S2 (hold-term) is next, and can derive its range
from `SignalResponse.indicators().volatility()` without further plumbing
changes.

## E2-F3-S2 — suggested hold-term alongside the call

E2-F3-S2 (suggested hold-term alongside the call) is done, closing out F2.3
(Buy/Sell/Hold signal & hold-term). A `Plan`-agent design gate picked the exact
formula before implementation: a **6-branch table** cross-producting
**trend strength** (derived from the matched `SignalRuleId` itself — UNANIMOUS
→ `STRONG`, MAJORITY → `MODERATE` — not recomputed from raw indicator values a
second time) against a **volatility band** (`LOW` <2.0%, `MEDIUM` <5.0%, `HIGH`
≥5.0% ATR%, reusing the same field `SignalRuleEngine`'s extreme-volatility gate
already reads) into a day range: STRONG/LOW 5-15, STRONG/MEDIUM 3-10,
STRONG/HIGH 2-6, MODERATE/LOW 3-10, MODERATE/MEDIUM 2-7, MODERATE/HIGH 1-4.
**HOLD calls get no hold-term at all** (`null`) — every HOLD reason (the three
safety gates, `CONFLICTING_SIGNALS`, `NO_STRONG_SIGNAL`) is equivalent from
hold-term's perspective, since there's no entry to size a horizon for. Like
`SignalRuleEngine`'s own thresholds, this table is explicitly provisional
engineering estimate, not yet backtest-validated — revisit once E2-F4-S1's
backtest harness exists (it currently only reports call win/loss stats per its
AC; extending it to check realized hold-term accuracy is a natural follow-on,
flagged not scoped).

New `com.autotrade.dashboard.signal` members: `TrendStrength`/`VolatilityBand`
(plain enums), `HoldTermRule` (the 6-branch table, same documented-enum shape
as `SignalRuleId`), `HoldTerm` (record: `minDays`/`maxDays`/`label`/`rationale`/
`tableVersion`; `label` is `"3-10 days"` — ASCII hyphen, not the agile-plan
prose example's en dash, to avoid encoding footguns across JSON/DB/frontend),
and `HoldTermCalculator` (pure static `calculate(SignalRuleId, BigDecimal
volatility)`, returns `null` for any non-BUY/SELL rule; its own
`HOLD_TERM_TABLE_VERSION = "v1"`, versioned independently of
`SignalRuleEngine.RULE_TABLE_VERSION` since a day-range revision shouldn't
force reinterpreting historical BUY/SELL calls and vice versa). `SignalService`
calls it right after `SignalRuleEngine.evaluate`; `SignalResponse` gained a
`holdTerm` field (`null` when HOLD); `SignalCallEntry` gained three nullable
columns (`hold_term_min_days`/`hold_term_max_days`/`hold_term_table_version`)
persisted alongside the existing rule-table version, for the same reason
`rule_table_version` itself is stored rather than recomputed — a later table
revision must not silently reinterpret a past audit row. `V5__add_hold_term_to_signal_calls.sql`
adds the three columns plus a check constraint (all-null-or-all-populated,
`min_days > 0 AND min_days <= max_days`) — the same DB-level defense-in-depth
pattern as `V1`'s stock/leverage check.

A real-Oracle-only bug (H2's Oracle-compatibility mode didn't catch it) surfaced
during `./mvnw verify`, not just live verification: a plain `Integer` field
with no `@JdbcTypeCode` mapped to `NUMBER(4)` failed Hibernate schema
validation (`found [numeric], but expecting [integer]`) — this codebase had no
prior plain-`Integer`-column precedent to check against (only `Long` PK/FK
columns, which already carry `@JdbcTypeCode(SqlTypes.NUMERIC)`). Fixed by
adding the same `@JdbcTypeCode(SqlTypes.NUMERIC)` + `precision = 4, scale = 0`
to `holdTermMinDays`/`holdTermMaxDays`. If a future entity adds a plain
`Integer`/`int` column, apply this same annotation up front rather than
discovering it at schema-validation time.

Tested via `HoldTermCalculatorTest` (14 tests: one per `HoldTermRule` branch,
volatility-band boundary values at 2.0/5.0 proving the exclusive-upper
convention, and a null-return case for every HOLD-producing `SignalRuleId`),
updated `SignalServiceTest` (persisted `SignalCallEntry` carries the right
hold-term columns for a BUY/SELL fixture, all three null for a HOLD fixture),
and updated `SignalControllerTest` (`holdTerm` JSON shape present for BUY,
absent/null for HOLD) — 14 new tests, 123 backend tests total, `./mvnw verify`
green. `simplify` and `signal-rule-review` skills both run clean: the table
stayed a plain documented mapping (no generic framework), hold-term is
genuinely derived from per-ticker volatility/trend strength (not a hardcoded
constant), every branch has its own test, and the new table version is
independently versioned ahead of any future revision.

Verified live via the `run` skill against the real running stack (Docker
Oracle XE + real public Binance API, no mocking; logged in through E1-F3-S2's
session-cookie auth) — `V5` applied cleanly against real Oracle (schema
version 4 → 5). `GET /api/tickers/BTCUSDT/signal` returned live `HOLD`/
`CONFLICTING_SIGNALS` with `holdTerm: null`; newly registered `SOLUSDT` and
`ADAUSDT` returned live `SELL`/`BEARISH_MAJORITY` with real, distinct
hold-terms (`"2-7 days"` at moderate volatility, `"1-4 days"` at high
volatility) — proving both the null-HOLD path and a real populated path work
end-to-end, not just against mocks. A direct `sqlplus` query against the live
container confirmed both persisted `signal_calls` rows (SOLUSDT, ADAUSDT)
matched their API responses exactly (`hold_term_min_days`/`max_days`/
`table_version`), and that both `BTCUSDT` HOLD rows persisted all three
columns as `NULL`. No frontend changes in this story (backend-only, same
split as every other E2 story — hold-term display in the UI is E3-F1-S2's
job, which can now pull `SignalResponse.holdTerm()` with no further backend
plumbing).

This closes out F2.3 in full. E2-F4-S1 (backtest harness) is the last story in
E2 (Signal Engine) — and, per both this story's and E2-F3-S1's own flagged
caveats, it's now doing double duty: validating the rule-table thresholds
*and* the hold-term day ranges, not just call win/loss.

## E2-F4-S1 — backtest harness

E2-F4-S1 (backtest harness) is done, closing out E2 (Signal Engine) entirely.
A `Plan`-agent design gate fixed every open design question before
implementation: the harness is a **JUnit test that doubles as the "script"**
the AC calls for (`BacktestHarnessTest`, rerunnable via
`./mvnw test -Dtest=BacktestHarnessTest`) rather than a new CLI-runner class —
no new build tooling, matching this codebase's existing bias (`RetryHelper`,
`MarketHoursService`) against infrastructure for something this small;
historical input is **real candles, not hand-authored fixtures** — hand-rolled
data (like `IndicatorTestFixtures`) is right for exact-reference-value unit
tests but wrong for asking "are these thresholds actually good," which needs
real bull/bear/choppy/volatile-spike regimes; and the walk-forward replay
recomputes every indicator over a **growing window anchored at index 0**
(`candles.subList(0, i + 1)`), not a fixed trailing slice — MACD's EMA seed
and RSI's Wilder seed are both anchored at wherever the passed-in list starts,
so a fixed trailing window would silently compute different numbers than
`IndicatorService.compute()` (which always passes the entire fetched candle
list) ever would have on that historical day.

New test-only `com.autotrade.dashboard.backtest` package (`src/test/java`,
never ships in the production jar, consistent with "not part of the live
API"): `BacktestCandleCsvLoader` (parses a checked-in CSV into an
ascending-by-timestamp `List<Candle>`), `BacktestConfig` (harness-only
diagnostic thresholds — 0.25% win/loss deadband, 5-day HOLD reference
horizon, 3.0% large-move threshold — deliberately *not* versioned with
`SignalRuleEngine.RULE_TABLE_VERSION`/`HoldTermCalculator.HOLD_TERM_TABLE_VERSION`,
since these measure outcomes rather than define the rule table under test),
`Checkpoint`/`DirectionalOutcome`/`HoldGateOutcome` enums, `BacktestDecisionPoint`/
`CheckpointStats`/`DirectionalOutcomeStats`/`HoldGateStats` records, `BacktestReport`
(aggregate + `printTo(PrintStream)`), and `BacktestHarness` (pure static
`run(String, List<Candle>)` — the walk-forward loop itself, calling
`RsiCalculator`/`MacdCalculator`/`MovingAverageCrossoverCalculator`/
`VolatilityCalculator`/`VolumeTrendCalculator`, `SignalRuleEngine.evaluate`,
and `HoldTermCalculator.calculate` directly as pure functions — never
`MarketDataService`/`IndicatorService`/persistence, so a backtest run never
writes a synthetic row into the real `signal_calls` audit table E6-F3-S2
depends on for provenance). For each BUY/SELL decision point, win/loss is
scored at **three checkpoints** (the hold-term's min/mid/max day) against the
deadband — scoring all three, not just one, is what lets the hold-term range
itself be evaluated, not just the call's direction. HOLD decision points are
tracked separately (not excluded) via a fixed 5-day/3.0% "large move"
reference check, broken down per matched `SignalRuleId`, since validating the
three safety gates is this story's other explicit purpose.
`BacktestHarnessTest`'s own assertions are structural only (every decision
point lands in exactly one of the 9 `SignalRuleId` buckets) — the win rate
itself is the evidence under review, not a fixed expectation to
regress-test.

Fixture data is **real, live-fetched, checked in**: 1000 daily candles each
for `BTCUSDT` and `DOGEUSDT`, fetched once from Binance's public (no-auth)
klines endpoint and converted to plain
`timestamp,open,high,low,close,volume` CSV under
`backend/src/test/resources/backtest/` (Nov 2023–Jul 2026, ~2.7 years, 967
decision points per series after the 34-candle minimum). Crypto-only for v1,
a scope call flagged by the Plan agent and accepted: no real `ALPACA_API_KEY`
exists on this dev machine to fetch a genuine historical stock series (per
E2-F1-S1's own note), and neither `SignalRuleEngine` nor `HoldTermCalculator`
branch on asset type, so real crypto history is sufficient evidence for the
shared rule logic — a stock series is a non-blocking follow-up once Alpaca
paper credentials exist (E4-F2-S1). `DOGEUSDT` was picked as the second
series (over `BTCUSDT` alone) after an offline check confirmed it actually
crosses `VOLATILITY_EXTREME_THRESHOLD` (316 of 986 candles, ~32%) while
`BTCUSDT` never does over the same window — needed to get any real evidence
on that gate at all.

Running the harness against this real data (not just asserting it runs
without exception) surfaced genuine findings, per `signal-rule-review`'s
explicit "don't ship a rule-table change on unit tests alone — run the
backtest and look at the win/loss stats" instruction: **`BULLISH_UNANIMOUS`/
`BEARISH_UNANIMOUS` never fired on either series** in ~2.7 years of real
daily data — only the `MAJORITY` (2-of-3) branches ever matched in practice;
**`MAJORITY` win rates cluster near a coin flip** (42.8–57.9% across
BTC/DOGE at all three min/mid/max checkpoints) — no strong edge is visible
yet in this data; **`VOLUME_DRIED_UP` and `NO_STRONG_SIGNAL` never fired on
either real series** (0 hits each), matching the earlier offline check that
neither pair's 10/30-day volume ratio ever dropped under 0.20; and
**`VOLATILITY_TOO_EXTREME` looks justified on `DOGEUSDT`** — 74.7% of the
time it fired, a >3% move followed within 5 days, real evidence the gate
suppressed calls in genuinely erratic conditions rather than over-suppressing
calm ones. Per this story's own scope (and `SignalRuleEngine`/
`HoldTermCalculator`'s existing discipline that a threshold revision is its
own auditable, versioned change), **no threshold was changed as part of this
story** — these findings are flagged as a candidate follow-up story, not
silently acted on here.

Tested via `BacktestHarnessTest` (2 tests: `BTCUSDT`, `DOGEUSDT`, running the
full harness against real fixture data and printing the report — structural
assertions only) — 2 new tests, 125 backend tests total, `./mvnw verify`
green. `simplify` and `signal-rule-review` skills both run clean: the harness
stayed test-scoped diagnostic tooling (no CLI runner, no generic framework),
it never touches `SignalRuleEngine`/`HoldTermCalculator` themselves or writes
to real audit tables, determinism holds (pure function of checked-in fixture
data, no wall-clock/randomness), and this story's real backtest evidence
requirement is now satisfied for the first time since both rule tables
shipped.

This closes out E2 (Signal Engine) in full. E3 (Dashboard/Frontend) is next —
E3-F1-S1 (ticker lookup + metrics display) and E3-F1-S2 (Buy/Sell/Hold badge
+ hold-term) can both pull straight from the existing `GET
/api/tickers/{symbol}/signal` response with no further backend plumbing.

## E3-F1-S1 — ticker lookup + metrics display

E3-F1-S1 (ticker lookup + metrics display) is done, starting E3 (Dashboard).
No `Plan`-agent design gate was run — unlike E2's rule-table/backtest stories,
this one had no open design question: the backend already returns everything
the AC needs via `GET /api/tickers/{symbol}/signal`, so the only work was
consuming it. The `dataviz` skill was consulted for stat-tile layout guidance
per its own routing table entry for this story.

New `frontend/src/signal/` package (mirrors the backend's `marketdata`/
`indicator`/`signal` package split): `api.ts` (`fetchSignal`, typed
`SignalResponse`/`IndicatorResponse`/`MacdResult`/`MovingAverageResult`/
`HoldTerm` DTOs matching the backend records field-for-field, reusing
`MarketDataError`/`MarketDataErrorCode` from the existing `marketdata/api.ts`
rather than duplicating the error-parsing branch — `MarketDataErrorCode` grew
one new member, `INSUFFICIENT_PRICE_HISTORY`, since `/signal` can return that
422 and `/price-history` can't) and `TickerMetrics.tsx` (ticker input + a
7-tile stat grid: Price, RSI, MACD, MA crossover, Volatility, Volume, Volume
trend — each null-safe via a `formatOrDash` helper for the two indicator
fields that can legitimately be null). This **replaces** `marketdata/
TickerLookup.tsx` on `DashboardPage` (deleted — fully superseded, nothing
else referenced it); `marketdata/api.ts`'s `fetchPriceHistory` and its error
types are untouched and still live, since E3-F2-S1's price chart will need
raw candles from that same endpoint. The call/hold-term (`"Call: SELL
(BEARISH_MAJORITY) · Suggested hold-term: 2-7 days"`) renders as a plain
unstyled line above the tiles — deliberately not prominent or color-coded
yet, since that visual treatment is explicitly E3-F1-S2's job, not this
story's.

`index.css` gained `.stat-tile`/`.stat-tile-grid` rules (a responsive
`auto-fit` grid, `light-dark()` CSS values for the border/background/text
colors — safe to use since `:root` already declares `color-scheme: light
dark`) — the app's first real stylesheet beyond the bare reset E1-F1-S3 left
in place, per this story being the one the `dataviz` skill's routing table
flags for "follow the generic skill's stat-tile guidance rather than ad hoc
cards." No component tests — this project still has no frontend test runner
configured (same gap as every prior frontend story since E2-F1-S2); `npm run
build` (typecheck + Vite build) and `npm run lint` (oxlint) both pass clean,
the only lint warning being a pre-existing unrelated one in `AuthContext.tsx`.

Verified live via the `run` skill against the real running stack (Docker
Oracle XE + real public Binance API, no mocking; logged in through E1-F3-S2's
session-cookie auth) — clicked through in an actual browser, not just
`npm run build`: `BTCUSDT` rendered all 7 tiles with live numbers and `Call:
HOLD (CONFLICTING_SIGNALS)` with no hold-term text; `SOLUSDT` rendered `Call:
SELL (BEARISH_MAJORITY) · Suggested hold-term: 2-7 days`, proving the
holdTerm-present path; `NOTREAL` correctly rendered the existing
`TICKER_NOT_REGISTERED` message and cleared the prior result; `AAPL` (a
stock, checked after-hours) correctly rendered the existing `MARKET_CLOSED`
message. No backend changes in this story.

## E3-F1-S2 — Buy/Sell/Hold call and hold-term shown prominently and color-coded

E3-F1-S2 (Buy/Sell/Hold call and hold-term shown prominently and color-coded)
is done, closing out F3.1. The `dataviz` skill was consulted first: rather
than the conventional red/green/amber traffic-light mapping, the badge uses
**teal (BUY) / orange (SELL) / slate (HOLD)** — a hue axis that stays
distinguishable under red-green color blindness (the most common form),
while the call word itself (`BUY`/`SELL`/`HOLD`) remains the primary signal
so color is never the sole means of conveying the call, per the project
constraint against relying on raw red/green/gray. `TickerMetrics.tsx` gained
a `SignalBadge` component (`role="status"`) rendering the call, matched rule,
and — only when non-null — the hold-term label; it replaces the plain
unstyled `Call: ...` line E3-F1-S1 deliberately left as a placeholder for
this story. New `index.css` rules (`.signal-badge` + `--buy`/`--sell`/
`--hold` variants) follow the same `light-dark()` pattern as the existing
`.stat-tile` rules from E3-F1-S1, so both light and dark system themes are
covered without a themeable palette system (this is a single-user tool, not
a multi-tenant product, per the `dataviz` skill's stated project constraint).
No backend changes — this story only consumes fields (`call`, `matchedRule`,
`holdTerm`) `SignalResponse` already returned since E2-F3-S1/S2.

No `Plan`-agent design gate — same reasoning as E3-F1-S1: no open design
question beyond the color mapping, which the `dataviz` skill's routing table
already answers directly. `npm run build` (typecheck + Vite build) and
`npm run lint` (oxlint) both pass clean, same pre-existing unrelated
`AuthContext.tsx` warning as before. No component tests — same gap noted in
every prior frontend story (no frontend test runner configured yet).

Verified live via the `run` skill against the real running stack (Docker
Oracle XE + real public Binance API, no mocking; logged in through E1-F3-S2's
session-cookie auth): `BTCUSDT`, `ETHUSDT`, `DOGEUSDT`, and `XRPUSDT` all
rendered the slate `HOLD` badge with no hold-term text; `SOLUSDT` rendered
the orange `SELL` badge with `Suggested hold-term: 2-7 days` shown inline,
proving both the hold-term-present and hold-term-absent paths render
correctly with visually distinct colors. No live ticker happened to be in a
BUY state during this verification session (market-dependent, not
reachable by construction) — the `signal-badge--buy` CSS class and the
`SignalBadge` component's conditional logic are identical in shape to the
verified `SELL` path, just parameterized on `call`, so this is a structural
rather than a live-observed gap; flagged rather than silently assumed.

This closes out F3.1 in full.

## E3-F2-S1 — price chart with MA/RSI overlays

E3-F2-S1 (price chart with MA/RSI overlays) is done, closing out F3.2 — the
last story before E3-F3-S1's stretch watchlist and E4's broker adapter layer.
An Explore agent first compared charting options (`lightweight-charts` v5 vs.
`recharts` vs. `visx` vs. hand-rolled SVG): `recharts` has no real candlestick
support (would need a hacked custom shape renderer) and no synced multi-pane
layout; `visx`/hand-rolled SVG would mean re-deriving scale/axis/resize logic
this codebase has no reason to reinvent, unlike the genuinely-simple
`RetryHelper`/`MarketHoursService`/hand-rolled-indicator precedents — a full
candlestick-plus-subplot chart crosses into real reinvented-wheel territory.
`lightweight-charts` v5 won: native multi-pane support (`chart.addPane()`)
covers the RSI-subplot-with-its-own-axis requirement directly, smallest
bundle cost of the real alternatives, and no React wrapper dependency needed
since its imperative API drives cleanly from a plain `useRef`/`useEffect`
component. A `Plan`-agent design gate then resolved the story's real crux:
where do historical MA/RSI *series* come from, given `/indicators`/`/signal`
only ever return the latest snapshot. Decision: a new backend endpoint
(`GET /api/tickers/{symbol}/chart-data`), not client-side reimplementation of
Wilder RSI/SMA in TypeScript (would risk the chart silently disagreeing with
the stat tiles directly above it) and not three independent frontend fetches
(real rate-limit exposure per E4-F1-S2's concern, plus a race between two
independently-timed candle fetches).

New backend: `IndicatorService.getChartData(symbol, limit)` calls
`MarketDataService.getPriceHistory` once, returns **all** fetched candles
always (unlike `/indicators`/`/signal`, this never throws
`InsufficientPriceHistoryException` — a candles-only chart is still a
meaningful render), and walks forward from index 33 computing
`RsiCalculator`/`MovingAverageCrossoverCalculator` over each growing
`candles.subList(0, i+1)` window (the same anchored-at-index-0 pattern
`BacktestHarness`, E2-F4-S1, already established/validated at ~1000-candle
scale) to build one `ChartIndicatorPoint(timestamp, rsi, maShort, maLong)`
per index — empty below 34 candles. **No persistence** — this is a read-only
diagnostic view, not the audited signal-computation path, matching
`BacktestHarness`'s precedent of never touching the real audit trail. New
`ChartDataResponse`/`ChartIndicatorPoint` records; `IndicatorController`'s
new route validates `limit` 1-1000 (not `/indicators`' 34-1000 floor) — no
Flyway migration, no `MarketDataExceptionHandler` changes (chart-data can
only ever surface the same `TICKER_NOT_REGISTERED`/`MARKET_CLOSED`/
`NO_PRICE_DATA`/rate-limit/unavailable failures every other endpoint does).
MACD/volatility/volume were deliberately excluded from this endpoint — the
AC says "MA lines, RSI subplot" only, and adding more would be unrequested
scope creep.

New frontend `frontend/src/chart/` package (mirrors the `marketdata`/
`indicator`/`signal` split): `api.ts` (`fetchChartData`, reusing
`MarketDataError`/`MarketDataErrorCode` rather than duplicating the
error-parsing branch — chart-data can never throw `INSUFFICIENT_PRICE_HISTORY`
so no new error code was needed), `mergeIndicators.ts` (pure functions
converting candles/indicator points into `lightweight-charts`' `{time, value}`/
OHLC shapes — `time` as a business-day string sliced from the midnight-UTC ISO
timestamp, since daily bars need no truer time-of-day resolution), `palette.ts`
(plain hex constants for light/dark, since `lightweight-charts` needs concrete
JS colors and can't consume the app's `light-dark()` CSS — candle up/down
colors intentionally match the existing `--buy`/`--sell` badge hues, plus two
new colorblind-safe blue/violet hues for the non-directional MA-10/MA-30
overlay lines, picked via `window.matchMedia('(prefers-color-scheme: dark)')`),
and `PriceChart.tsx` (the `useRef`/`useEffect` wrapper: candlestick + 2 line
series on the price pane, `chart.addPane()` for RSI with 30/70 dashed
reference price-lines, `autoSize: true` for width-responsiveness against a
CSS-fixed-height container — deliberately width-responsive/height-fixed, since
squeezing the RSI pane arbitrarily short on a narrow window would make it
illegible; full teardown-and-rebuild via `chart.remove()` on every prop
change, acceptable since a new ticker lookup has no zoom/pan state worth
preserving). `TickerMetrics.tsx` now fires `fetchSignal` and `fetchChartData`
concurrently via `Promise.allSettled` (not `Promise.all`) so the chart and the
badge/stat-tiles fail independently — a chart-data-specific transient failure
(rate-limited/unavailable) doesn't blank out a working signal call and vice
versa; a supplementary "Showing price only — not enough history yet for
indicator overlays" note renders under the chart when `indicators` comes back
empty. `index.css` gained `.price-chart-container`/`.price-chart` (fixed
420px height, `light-dark()` border matching `.stat-tile`)/`.chart-note`
rules. `layout.attributionLogo` was left at its default (enabled) to satisfy
`lightweight-charts`' Apache-2.0-plus-attribution license clause the Explore
agent flagged, at zero extra UI cost.

This story also introduced **Vitest** as the frontend's first test runner
(`vitest.config.ts`, `npm test`) — a small, narrowly-scoped addition the Plan
agent flagged as a recommendation rather than a silent scope add: this is the
first genuinely non-trivial, non-DOM frontend logic in the app (unlike
`TickerMetrics`' straightforward fetch+render), and a real canvas-rendering
chart can't be regression-tested without a real browser (jsdom doesn't fully
implement the 2D canvas context) — the `run` skill's live browser click-through
remains the verification method for the chart's actual visual rendering, same
as every prior E3 story. `mergeIndicators.test.ts` (4 tests) covers the OHLC
string-to-number conversion, business-day timestamp formatting, and the
empty-indicator-series edge case. Backend: `IndicatorServiceTest`/
`IndicatorControllerTest` gained 8 new tests (aligned-series correctness
against the exact indices `IndicatorService.MIN_CANDLES_FOR_INDICATORS`
implies, empty-series-below-34-candles, no-persistence assertion, and
inherited-error-propagation cases) — 133 backend tests total, `./mvnw verify`
green; `npm run build`/`npm run lint`/`npm test` all pass clean, same
pre-existing unrelated `AuthContext.tsx` lint warning as every prior story.

Verified live via the `run` skill against the real running stack (Docker
Oracle XE + real public Binance API, no mocking; logged in through E1-F3-S2's
session-cookie auth) — one gotcha hit before verification, a repeat of the
pattern this file has flagged since E2-F1-S3/E2-F2-S1: **two** stale `java.exe`
processes in a row were still holding port 8080 from earlier sessions (the
first `netstat`/`taskkill` cleared one, but a second had also bound the port —
likely from an earlier improperly-backgrounded `nohup` attempt in this same
session whose child process outlived the shell it was launched from); both
killed and the backend restarted clean before trusting `/health`. `BTCUSDT`
rendered the HOLD badge alongside a chart showing real live candles, both MA
lines (10-day blue, 30-day violet), and the RSI subplot with its own 0-100
axis and dashed 30/70 reference lines — all internally consistent with the
stat tiles above it. `SOLUSDT` rendered the SELL/orange badge with a
matching orange-down-candle chart, proving the palette-sync between badge
and chart. `NOTREAL` and after-hours `AAPL` both correctly rendered the
*same* error message twice (once from the signal fetch, once from the
independent chart fetch) rather than a blank chart area — a minor visual
redundancy from the two-independent-fetches design, flagged as acceptable
rather than a bug, since each fetch failing identically for the same
underlying reason is expected, not a fault. Browser console showed no
errors, confirming the chart's create/cleanup `useEffect` cycle is idempotent
under React 19 `StrictMode`'s dev-mode double-invoke. The narrow-viewport
`autoSize` resize behavior could not be independently observed in this
session's browser-automation environment (window resize didn't visibly
affect the captured viewport) — relies on `lightweight-charts`' own
documented `ResizeObserver`-backed `autoSize` behavior plus the container's
`width: 100%` CSS, flagged as a structural rather than live-observed check,
same disclosure style as E3-F1-S2's un-observed BUY-badge case.

This closes out E3 (Dashboard/Frontend) except E3-F3-S1's stretch watchlist.
E4 (Broker Adapter Layer) is next.

## E3-F3-S1 — watchlist

E3-F3-S1 (watchlist) is done, closing out F3.3 and E3 entirely. No `Plan`-agent
design gate — the only real design question (schema shape, idempotent-add/remove
semantics, response shape) was small enough to resolve directly against this
codebase's own precedents rather than warranting a separate gate: a ticker must
already be registered before it can be watchlisted, reusing the exact
"explicit registration first" pattern market data/indicators/signal all
already follow, including `TICKER_NOT_REGISTERED` (404) rather than a new
error code.

New `V6__add_watchlist.sql`: a single `watchlist_entries` table (`ticker_id`
FK, `UNIQUE` — a ticker can only appear once, single-user app with no account
table per E1-F3-S2, so no per-user scoping needed) plus `created_at`. New
backend `com.autotrade.dashboard.watchlist` package: `WatchlistEntry` entity,
`WatchlistEntryRepository` (`findAllByOrderByCreatedAtDesc`,
`findByTicker_Id`/`existsByTicker_Id`), `WatchlistService` (`list`/`add`/
`remove`, delegating ticker resolution to `TickerService.findRegistered` —
`add` is idempotent exactly like `TickerService.resolveOrRegister`, `remove`
is a no-op instead of an error if the ticker was never watchlisted, matching
DELETE's idempotent-by-convention semantics), and `WatchlistController`
(`GET`/`POST`/`DELETE /api/watchlist`, the `POST` 200-vs-201 distinction
mirroring `TickerController`'s own existed-before check). No new error codes
or `MarketDataExceptionHandler` changes — every failure this story can
produce (`TICKER_NOT_REGISTERED`, `INVALID_REQUEST`) was already wired.
`SecurityConfig`'s existing `anyRequest().authenticated()` covers the new
routes with no config change.

Frontend: new `frontend/src/watchlist/` package (`api.ts`: `fetchWatchlist`/
`addToWatchlist`/`removeFromWatchlist`; `Watchlist.tsx`: a list of saved
tickers, each with a clickable symbol to revisit it and a Remove button).
While wiring this in, a third call site for the marketdata/signal/chart
error-parsing branch appeared — past the "three similar lines" threshold this
codebase's own `simplify` guidance flags for extraction — so
`marketdata/api.ts` gained a shared `parseMarketDataError(response)` helper,
and `fetchPriceHistory`/`fetchSignal`/`fetchChartData` were all refactored to
call it instead of each re-parsing the response body inline; behavior is
unchanged, this is pure de-duplication. `TickerMetrics.tsx` gained an
`AddToWatchlistButton` (idle/saving/saved/error states, keyed by ticker
symbol so switching tickers resets it) rendered under a successful lookup,
and now accepts a `lookupRequest` prop (`{symbol, nonce}` — a fresh object
per watchlist click, so re-selecting the same symbol still re-runs the
lookup) plus an `onWatchlistChanged` callback. `DashboardPage` lifts just
enough state to connect the two: a `watchlistRefreshKey` bumped whenever
`TickerMetrics` reports a successful add (triggering `Watchlist` to refetch),
and a `lookupRequest` set whenever `Watchlist` reports a click
(triggering `TickerMetrics` to look that symbol up). New `index.css` rules
(`.watchlist-list`/`.watchlist-item`) follow the existing `.stat-tile`
`light-dark()` pattern. New tests: `WatchlistServiceTest` (H2/Oracle-mode,
add/remove/idempotency/ordering/not-registered cases) and
`WatchlistControllerTest` (`@WebMvcTest`, status codes and error bodies) — 13
new backend tests, 140 backend tests total, `./mvnw verify` green;
`mergeIndicators.test.ts` unaffected (4 tests), `npm run build`/`npm run
lint` both pass clean (same pre-existing unrelated `AuthContext.tsx` lint
warning as every prior story).

Verified live via the `run` skill against the real running stack (Docker
Oracle XE + real public Binance API, no mocking) — one instance of this
file's own recurring gotcha hit again: the backend process from the prior
session was still holding port 8080 with pre-story code, caught and killed
via the same `netstat`/`taskkill` check this file has flagged since
E2-F1-S3. `V6` applied cleanly against real Oracle (schema version 5 → 6).
Exercised the full API via `curl` first (registered `BTCUSDT`/`SOLUSDT`,
added both, confirmed idempotent re-add returns 200 not 201, confirmed
`NOTREAL` 404s `TICKER_NOT_REGISTERED`, removed `BTCUSDT` and confirmed a
second remove is a no-op 204 not an error), then **restarted the backend
process entirely** and confirmed `GET /api/watchlist` still returned the
surviving `SOLUSDT` entry from a fresh process — the AC's actual "survives
app restart" requirement, not just "the row is in the table." Then clicked
through the same flow in a real browser: the watchlist rendered the
restart-surviving `SOLUSDT` entry on page load; clicking its symbol
populated the lookup field and ran a full signal+chart fetch (the "revisit"
path); registering and looking up `ETHUSDT` then clicking "Add to
watchlist" showed the button flip to "Added to watchlist" and the list
update to show both tickers, most-recently-added first; clicking "Remove" on
`SOLUSDT` updated the list immediately to just `ETHUSDT`; a full page reload
still showed the persisted `ETHUSDT` entry with an empty lookup field (no
stale form state leaking across reloads). Browser console showed no errors
at any point.

This closes out E3 (Dashboard/Frontend) in full. E4 (Broker Adapter Layer) is next.

## E4-F1-S1 — `BrokerAdapter` interface + mock implementation + shared contract test suite

E4-F1-S1 (`BrokerAdapter` interface + mock implementation + shared contract
test suite) is done, starting E4 (Broker Adapter Layer). A `Plan`-agent
design gate fixed every method signature and DTO shape before implementation,
grounded in the `Order` entity's existing bracket-order fields (already
anticipated by E1-F2-S1's schema) and mirroring `MarketDataClient`'s own
`supportedAssetType()`/`broker()` pattern. One deliberate addition beyond the
story's literally-named four methods was flagged and kept:
**`getOrderStatus(clientOrderId, mode)`** — F4.2-S2's "status is pollable" AC
needs a way to poll a specific order that neither `getPosition` (can't
distinguish "my order didn't fill" from "another order already built this
position") nor `cancelOrder` (has side effects) can safely double as; adding
it now avoids a breaking interface change one story later.

New **top-level** `com.autotrade.dashboard.brokeradapter` package (not
nested under `broker` — every existing concern in this codebase is a flat
top-level package; `broker` itself stays scoped to credential storage/
rotation). Main source: `BrokerAdapter` interface (`supportedAssetType`,
`broker`, `placeOrder`, `getOrderStatus`, `getPosition`, `cancelOrder`,
`getAccountStatus` — every method takes an explicit `TradingMode` so E6.1's
paper/live switch changes which credentials/base URL an adapter uses without
re-instantiating anything), `BrokerOrderRequest`/`BrokerOrderResult`/
`BrokerPosition`/`BrokerAccountStatus` (records, reusing `OrderSide`/
`EntryOrderType`/`OrderStatus`/`AssetType`/`Broker`/`TradingMode` from their
existing packages rather than duplicating them — `brokeradapter` never
imports the `Order` JPA entity itself, only its sibling enums), and
`BrokerAdapterException` (unchecked, mirrors `MarketDataUnavailableException`'s
exact shape). Expected business outcomes (broker rejected an order, an
order/position not found) are always returned as normal result values
(`REJECTED`/`FAILED` status, `Optional.empty()`) — never exceptions; only
transport/infrastructure faults throw `BrokerAdapterException`. Retry/
backoff/rate-limit handling (E4-F1-S2) and outage/duplicate-prevention
semantics (E4-F1-S3) are deliberately not designed here — `clientOrderId`
being the sole required identifier everywhere and `BrokerAdapterException`
being the single seam a future retry wrapper would catch are what keep this
interface from needing a breaking change once those stories land. No
`BrokerAdapterRouter`/asset-type-routing service either — no second adapter
exists yet to route between (YAGNI until F4.2 gives it one, mirroring
`MarketDataService`'s existing routing-map precedent).

Test-only source (`backend/src/test/java/.../brokeradapter/`, never ships in
the production jar, same "test-only infrastructure" precedent as
`com.autotrade.dashboard.backtest`): `MockBrokerAdapter` (deterministic
in-memory implementation, never a Spring bean/`@Component` — must never be
autodiscoverable in any profile) with an `autoFill` flag (immediately fills a
placed order at a placeholder price — `entryLimitPrice` for LIMIT orders, the
TP/SL midpoint for MARKET — not a market-price simulation) and three
test-scripting hooks not part of the `BrokerAdapter` contract itself:
`rejectNextOrderWith(reason)`, `failNextCallWith(exception)`,
`simulateFill(clientOrderId, price)` — these exist so E4-F1-S2/S3's future
tests, and E1-F4-S1's still-open mock-broker E2E test, have something
concrete to script against. `placeOrder` replays the stored result unchanged
for an already-known `clientOrderId` (idempotent, no duplicate state) —
worth nailing down now even though full dedup semantics are E4-F1-S3's job,
since it's cheap and gives the contract suite something concrete to assert.
`cancelOrder` on a `FILLED` order or an already-`CANCELLED`/`REJECTED`/
`FAILED` one is an idempotent no-op (same DELETE-idempotent convention as
`WatchlistService.remove`); on an unknown `clientOrderId` it returns
`FAILED`/"Unknown clientOrderId" rather than throwing. `getPosition` derives
a net per-symbol position purely from `FILLED` transitions (BUY adds, SELL
reduces, weighted-average entry price recomputed on BUY fills only).

`BrokerAdapterContractTest` (abstract JUnit 5, no Spring context assumed) is
the shared suite every adapter must pass — plumbing/shape correctness only
(clientOrderId round-trips, idempotent replay, empty-`Optional` cases,
non-throwing cancel/status paths, non-empty account balances); no rate-limit
or outage-simulation assertions, left for E4-F1-S2/S3 to add once designed.
`MockBrokerAdapterContractTest` supplies a concrete `MockBrokerAdapter` and
proves the suite runs; F4.2/F4.3 add their own subclasses against the real
Alpaca/Binance adapters later (may need to skip assertions that don't apply
against a live paper API — that story's own call). `MockBrokerAdapterTest`
covers mock-specific behaviors the shared suite doesn't (manual-fill vs.
auto-fill, reject/fail scripting hooks and their one-shot reset, position
accumulation across BUY/SELL fills).

Tested via 16 new tests (`MockBrokerAdapterContractTest`: 7,
`MockBrokerAdapterTest`: 9) — 164 backend tests total, `./mvnw verify`
green. No frontend or database changes in this story — this is a pure
backend interface/contract addition with no consumer wired in yet (F4.2's
Alpaca adapter is the first real implementation; E5's order-submission flow
is the first real caller).

## E4-F1-S2 — rate-limit/retry/backoff built into the adapter contract

E4-F1-S2 (rate-limit/retry/backoff built into the adapter contract) is done.
A `Plan`-agent design gate resolved the story's central question — since no
real Alpaca/Binance adapter exists yet (F4.2/F4.3 are still future stories),
there was nothing to attach a per-adapter retry helper to (the read-only
`marketdata.RetryHelper` pattern), so the plan chose a **decorator** instead:
`RetryingBrokerAdapter implements BrokerAdapter`, wraps any delegate adapter,
and applies retry/backoff uniformly around every interface method — this
means retry code is written exactly once, never duplicated per future
adapter, and it's fully testable today by wrapping `MockBrokerAdapter`. This
is explicitly not the adapter-router E4-F1-S1 deliberately deferred (YAGNI)
— it wraps a single adapter instance, no asset-type routing involved.

Two new exception subtypes classify *why* a `BrokerAdapter` call failed
(`BrokerAdapterException` itself stays the fatal, non-retryable base — same
as before, so nothing about E4-F1-S1's existing usage changed):
`BrokerAdapterTransientException` (ambiguous transport fault — timeout,
connection reset, 5xx) and `BrokerAdapterRateLimitedException` (broker
explicitly rejected the call for throttling; carries a nullable
`retryAfterSeconds`, mirroring `MarketDataRateLimitedException`'s shape). A
concrete future adapter's only retry-related responsibility is to throw the
right subtype.

The one decision flagged for explicit sign-off (money-safety implications,
not silently assumed) was confirmed directly with the user before
implementation: **`placeOrder` retries a rate-limit failure but never a
generic transient one** — a rate-limit response is unambiguous (the broker
definitely rejected the call, and `clientOrderId` replay is already
idempotent per E4-F1-S1), but a transient failure is ambiguous (the broker
may have already received the order before the connection dropped), and
guaranteeing no duplicate submission under that ambiguity is **E4-F1-S3's**
explicit scope, not this story's. `getOrderStatus`/`getPosition`/
`getAccountStatus`/`cancelOrder` retry both transient and rate-limited
failures freely, since none of them submit money-moving state. A plain
(non-subtype) `BrokerAdapterException` is never retried on any method.
Backoff is exponential (`baseDelay * 2^(attempt-1)`, capped at `maxDelay`);
`BrokerAdapterRetryPolicy.defaultPolicy()` is 3 attempts/250ms base/2s cap —
provisional engineering defaults, not sourced from Alpaca's/Binance's actual
rate-limit windows, flagged to revisit once F4.2/F4.3 research those. A
rate-limited failure's `retryAfterSeconds`, when present, overrides the
computed backoff — but if it would exceed `maxDelay`, the wrapper stops
retrying immediately rather than blocking the calling thread for minutes
(no async job queue exists yet to defer to). An `InterruptedException`
during a backoff sleep sets the interrupt flag and rethrows the last-caught
failure immediately, rather than looping again — a deliberate deviation from
`RetryHelper`'s swallow-and-continue, since this loop can span several
attempts where that one was a single retry.

`MockBrokerAdapter` (test-only) gained `failNextCallsWith(exception, times)`
— its existing `failNextCallWith` is now a one-time convenience wrapper over
it — so tests can script retry-exhaustion (N failures in a row), not just a
single fail-then-succeed case. New `RetryingBrokerAdapterTest` (8 tests):
transient retry-then-succeed on a read, transient exhausted on a read,
rate-limit retry-then-succeed on both a read and `placeOrder`, rate-limit
exhausted (asserts the exact exception type and preserved
`retryAfterSeconds`), a rate-limit whose `retryAfterSeconds` exceeds
`maxDelay` stopping immediately, `placeOrder` *not* retrying a transient
failure (proven by scripting only one failure — if it had retried, the
unscripted second call would have silently succeeded instead of the test
observing a thrown exception), and a fatal exception never being retried on
a read (same one-shot-failure proof technique). New
`RetryingMockBrokerAdapterContractTest` runs the existing shared
`BrokerAdapterContractTest` suite against a `MockBrokerAdapter` wrapped in
`RetryingBrokerAdapter`, proving the decorator is fully transparent on the
happy path — and giving F4.2/F4.3 a ready template ("wrap your adapter and
run the shared suite against the wrapped instance too"), which
`BrokerAdapterContractTest`'s own Javadoc now recommends explicitly.
`BrokerAdapter`/`BrokerAdapterException`'s Javadoc were updated to point at
`RetryingBrokerAdapter` and its two exception subtypes instead of describing
retry/backoff as "not part of the contract yet."

Tested via 15 new tests (`RetryingBrokerAdapterTest`: 8,
`RetryingMockBrokerAdapterContractTest`: 7 inherited) — 179 backend tests
total, `./mvnw verify` green. No frontend or database changes — same
backend-only scope as E4-F1-S1. Outage handling and duplicate-order
prevention under an ambiguous transient failure remain E4-F1-S3's explicit,
not-yet-built scope.

## E4-F1-S3 — broker/data-provider outage fails visibly and safely; retries never duplicate an already-submitted order

E4-F1-S3 (broker/data-provider outage fails visibly and safely; retries never
duplicate an already-submitted order) is done, closing out F4.1. A
`Plan`-agent design gate fixed the approach before implementation: two new
exception types, both synthesized exclusively by `RetryingBrokerAdapter` —
never thrown directly by a concrete adapter, since doing so would bypass
reconciliation entirely. **`BrokerAdapterUnavailableException`** replaces the
raw `BrokerAdapterTransientException` whenever retries are exhausted on any
method (or immediately on `placeOrder`, which never auto-retries a transient
failure) — this is the AC's distinct "broker unavailable" state, mirroring
`MarketDataUnavailableException`'s existing precedent, for a future
order-submission controller to map to a UI state. **`BrokerAdapterAmbiguousOrderException`**
is thrown only from `placeOrder`, only when reconciliation itself also fails
— genuinely ambiguous whether the order reached the broker — carrying the
`clientOrderId` and an explicit message telling the caller to retry with the
*same* id (idempotent) rather than resubmit under a new one.

The actual duplicate-prevention mechanism: `RetryingBrokerAdapter.placeOrder`
now catches its own `BrokerAdapterUnavailableException` and reconciles via a
single call to **its own `getOrderStatus`** (not the delegate's — so the
reconciliation probe itself gets the same retry/backoff) keyed on the same
`clientOrderId`. If the broker confirms the order was actually recorded, that
real `BrokerOrderResult` is returned normally (no exception, no duplicate —
same identity resolves to the same order); if the broker confirms nothing
exists, `BrokerAdapterUnavailableException` propagates (confirmed-not-submitted,
safe to retry later); if the reconciliation call itself throws, that's
genuine ambiguity → `BrokerAdapterAmbiguousOrderException` (original failure
as cause, reconciliation failure attached via `addSuppressed`). Rate-limit
exhaustion is deliberately untouched — still throws the raw
`BrokerAdapterRateLimitedException`, since that's an unambiguous rejection,
a different concept from unreachability that this story doesn't conflate.
No `BrokerAdapter` interface change — `clientOrderId` already being the sole
required identifier is what keeps reconciliation possible without a breaking
change, per E4-F1-S1's own design note.

`MockBrokerAdapter` (test-only) gained one new scripting hook,
`simulateLostResponseOnNextPlaceOrder` — models "the broker actually
processed the order but the response back was lost" (state recorded,
auto-fill applied if enabled, *then* throws) — distinct from the existing
`failNextCallWith`/`failNextCallsWith` hooks, which throw before any state
mutation and could therefore only ever model "never reached the broker."
`BrokerAdapterContractTest` (the shared cross-adapter suite) needed no
changes — both new exception types are decorator-only, never part of what a
bare adapter must satisfy. Two existing `RetryingBrokerAdapterTest` cases
were updated to assert the new wrapping behavior instead of the old raw
exception type. New `RetryingBrokerAdapterOutageTest` (6 tests) covers:
read-call transient exhaustion wrapping with cause preserved, `placeOrder`
confirmed-not-submitted via reconciliation, `placeOrder` where the broker
actually succeeded (real result returned, exactly one position recorded —
the concrete no-duplicate proof), genuine reconciliation-failure ambiguity,
a same-`clientOrderId` retry after an ambiguous outcome resolving
idempotently with no duplicate order, and a regression guard proving
rate-limit exhaustion is never wrapped into either new type. Two new
`MockBrokerAdapterTest` cases cover the new hook directly. 187 backend tests
total, `./mvnw verify` green. `adapter-contract-check` skill run: interface
conformance and retry/backoff unaffected (full suite green); outage handling
now has a concrete, tested dedup mechanism (clientOrderId-keyed idempotent
replay plus reconciliation), not just "retry carefully"; leverage-bounds and
paper-vs-live-key checklist items remain out of scope until F4.2/F4.3 build
real adapters, same deferred status as E4-F1-S1/S2.

No frontend or database changes — same backend-only scope as E4-F1-S1/S2.
"The UI shows a clear broker unavailable state" remains a flagged, deferred
consumer requirement: no order-submission flow or UI exists yet (E5 hasn't
been built), so there's nothing to wire a real UI state into today. This
story provides the two distinct exception types a future order-submission
controller needs to satisfy that half of the AC without further adapter-layer
plumbing — `BrokerAdapterUnavailableException` for a "broker unavailable,
try again" state and `BrokerAdapterAmbiguousOrderException` for a stronger
"status unknown, do not resubmit" state — which HTTP status/error codes they
map to is E5's decision when that controller is actually built.

This closes out F4.1 (adapter interface) and E4-F1 in full. F4.2 (Alpaca
adapter) is next.

## E1-F4-S1 — mock-broker E2E test covering ticker → signal → order

E1-F4-S1 (mock-broker E2E test covering ticker → signal → order) is done,
closing the one remaining gap in F1.4 (testing strategy) that had been open
since early E1 — deferred back then because it needed both E2's signal engine
and E4-F1-S1's `MockBrokerAdapter` to exist first, both now do. A `Plan`-agent
design gate resolved the one real open question: E5 (Auto-Trade Execution —
the trade-input form, bracket-order construction, guardrails) hasn't been
built yet, so there's no production code that turns a `SignalResponse` into a
`BrokerOrderRequest`. Decision: the test itself contains a minimal, explicitly
commented test-only stand-in for that translation (no amount sizing, no
leverage rules, no guardrails — E5-F2-S1's actual scope), proving the plumbing
wires together end-to-end without pretending to be the real trade-execution
feature. This mirrors `com.autotrade.dashboard.backtest`'s existing precedent
of test-only logic standing in for a step production code doesn't own yet.

New test-only `com.autotrade.dashboard.e2e` package (parallels `backtest` —
also test-only, also cross-cutting, not owned by one feature package):
`TickerSignalOrderE2ETest` (`@SpringBootTest`, `@Transactional`, real H2, same
shape as `CoreDataModelIntegrationTest`) and `E2ECandleFixtures` (a
purpose-built 40-daily-candle series, deliberately not reused from
`IndicatorTestFixtures` — that package-private fixture's degenerate
`CLOSES_40` series actually evaluates to `CONFLICTING_SIGNALS` under
`SignalRuleEngine`, not a usable directional call for this test). Asset type
is CRYPTO, not STOCK, specifically to sidestep `MarketHoursService`'s
open/closed gate — a stock ticker would make the test's pass/fail depend on
what time CI happens to run. The fixture's 40 candles chop without net drift
for the first 26, then add a gentle uptrend for the last 14 so MACD's
histogram and the SMA10/SMA30 crossover both read bullish while RSI-14 stays
inside its neutral 30-70 band — a naive straight-line uptrend instead pushes
RSI toward overbought/bearish, colliding with MACD/MA's bullish read and
producing `CONFLICTING_SIGNALS` rather than a clean `BULLISH_MAJORITY`, a real
trap confirmed via an independent Python/Decimal script (mirroring
`RsiCalculator`/`MacdCalculator`/`MovingAverageCrossoverCalculator`/
`VolatilityCalculator`/`VolumeTrendCalculator` exactly) before the numbers
were finalized, same discipline as E2-F2-S1/S2's reference values. Final
series: RSI-14 = 57.7870, MACD histogram = +0.08595536, SMA10 100.787 > SMA30
100.278, ATR% = 1.5920 (LOW band), volume-trend ratio = 1.0000 (constant
volume) → `BULLISH_MAJORITY` / `HoldTermRule.MODERATE_LOW` ("3-10 days").

The test drives real `TickerService.resolveOrRegister` →
`SignalService.computeSignal` (real `IndicatorService`/`MarketDataService`/
calculators/`SignalRuleEngine`/`HoldTermCalculator`, real persisted
`IndicatorSnapshot`/`SignalCallEntry`) → a manually-instantiated
`MockBrokerAdapter.placeOrder` (never a Spring bean, per its own documented
contract) → a real persisted `Order` FK'd to both the ticker and the exact
`IndicatorSnapshot` the signal computation produced → a `getPosition` read
proving the fill is reflected in the adapter's own state, not just the
immediate return value. One real Spring Boot 4.1 wiring subtlety, flagged by
the `Plan` agent and confirmed necessary: `MarketDataService`'s constructor
eagerly calls `supportedAssetType()` on every injected `MarketDataClient` bean
at context-refresh time, before any test stubbing runs — a plain
`@MockitoBean` on `BinanceMarketDataClient` would return null from that
unstubbed call and silently corrupt the CRYPTO routing entry for the whole
test class. Fixed by using `@MockitoSpyBean` instead (wraps the real,
fully-constructed bean; only `fetchRecentCandles` is stubbed, via
`doReturn(...).when(spy)...` rather than `when(spy...).thenReturn(...)` —
the latter would trigger one real Binance HTTP call during stub setup on a
spy). `AlpacaMarketDataClient` needed no stubbing at all, since CRYPTO routing
never reaches it.

Tested via 1 new test (`TickerSignalOrderE2ETest`) — 188 backend tests total,
`./mvnw verify` green, no live network calls or wall-clock dependence (same
CI-safety posture as every other backend test). `simplify` skill run clean:
no generic "order builder" abstraction was introduced — the test-only
translation stays inline in the test method, explicitly commented as a stand-
in rather than real E5 logic. No frontend or database changes.

## E1 — Platform Foundation

E1 (Platform Foundation) is now fully closed out — every story across F1.1
through F1.4 is done.

## E4-F2-S1 — Alpaca paper account connected; `getAccountStatus` returns balance

E4-F2-S1 (Alpaca paper account connected; `getAccountStatus` returns
balance) is done, starting F4.2. A `Plan`-agent design gate resolved the
one real open question up front — confirmed directly with the user before
implementation, per its security-review weight: where do this adapter's
*trading* credentials come from, given `BrokerCredentialService` (E1-F3-S1)
exists specifically for this but no "connect my account" UI exists yet.
Decision: **bootstrap `BrokerCredentialService` from env vars at startup**
rather than reading plain env vars directly the way
`AlpacaMarketDataClient` does — the adapter always reads through the
encrypted, rotation-eligible store, and the real UI-driven connect flow
stays a separate future story. New `broker.AlpacaTradingCredentialBootstrap`
(`ApplicationRunner`) reads `ALPACA_TRADING_API_KEY`/
`ALPACA_TRADING_API_SECRET` (deliberately separate from
`ALPACA_API_KEY`/`ALPACA_API_SECRET`, which stay scoped to E2-F1-S1's
read-only market data) and seeds a `(ALPACA, PAPER)` row via
`BrokerCredentialService.store` **only if one doesn't already exist** —
re-seeding every restart would fight the audited `rotateAll()` rotation
flow that owns updating it from here on. `BrokerCredentialService` gained a
`find(Broker, TradingMode)` lookup (thin wrapper over the existing
repository method), the sole path both the bootstrap and the adapter use.
Scoped to `PAPER` only — no `LIVE` seeding, so `getAccountStatus`/etc.
called under `LIVE` fail closed with no credential configured until a
future story seeds one (a free backstop, not a substitute for E6's real
live-mode consent gate).

New `com.autotrade.dashboard.brokeradapter.AlpacaTradingAdapter` implements
the full `BrokerAdapter` interface for real against Alpaca's actual
paper/live trading API (`https://paper-api.alpaca.markets` /
`https://api.alpaca.markets` — distinct from `AlpacaMarketDataClient`'s
read-only `data.alpaca.markets`), not just this story's narrow
`getAccountStatus` AC: `BrokerAdapterContractTest`'s shared suite requires a
fully-working adapter, and E4-F2-S2 (place a market order) needs
`placeOrder`/`getOrderStatus`/`getPosition`/`cancelOrder` immediately next.
`placeOrder` submits Alpaca's native bracket-order shape
(`order_class=bracket`, `take_profit.limit_price`/`stop_loss.stop_price`,
`time_in_force=day`); a 403 (insufficient buying power/trading blocked) is
mapped to a normal `REJECTED` result rather than thrown, per
`BrokerAdapter`'s existing "business outcomes are return values, not
exceptions" contract; a 422 with Alpaca's `client_order_id`-uniqueness code
triggers a replay via `GET /v2/orders:by_client_order_id` (idempotent, no
duplicate order) rather than throwing; any other 422 is a fatal,
non-retried `BrokerAdapterException` (malformed request, a caller bug, not
a broker business decision); 429 throws `BrokerAdapterRateLimitedException`
with the parsed `Retry-After`; 5xx/connectivity throws
`BrokerAdapterTransientException`; everything else unexpected is a fatal
`BrokerAdapterException` — the same three-way classification
`RetryingBrokerAdapter` (E4-F1-S2/S3) already expects from a concrete
adapter. `cancelOrder` resolves the current state via `getOrderStatus`
first (idempotent no-op if already terminal, `FAILED` result — not an
exception — for an unknown `clientOrderId`), then `DELETE`s and re-fetches
the authoritative status afterward rather than trusting the 204 as "done."
Alpaca's order-status vocabulary maps onto this codebase's `OrderStatus`
enum; `expired`/`done_for_day`/`suspended` all provisionally map to
`CANCELLED` (no matching enum value), flagged to revisit if a later story
needs to distinguish them. Stock-order leverage is defense-in-depth
validated (fatal, no HTTP call) before every `placeOrder`, mirroring the
DB-level CHECK constraint from E1-F2-S1.

`AlpacaTradingAdapter` itself is a plain, non-`@Component` class — like
`MockBrokerAdapter`'s positioning but for a different reason (it's real,
just not the thing that should be directly injectable). New
`brokeradapter.BrokerAdapterConfig` wraps it in `RetryingBrokerAdapter` for
the actual exposed `BrokerAdapter` bean, per
`RetryingMockBrokerAdapterContractTest`'s own documented "wrap your
adapter" template — nothing consumes this bean yet (E5 doesn't exist),
same "bean nothing wires up yet" situation E4-F1-S1's interface addition
already accepted. New `brokeradapter.AlpacaTradingProperties`
(`broker.alpaca.paper-base-url`/`live-base-url`) deliberately has no
`apiKey`/`apiSecret` fields, unlike `AlpacaMarketDataProperties` — credentials
come from `BrokerCredentialService`, not config. The shared app-wide `Clock`
bean moved from `marketdata.MarketDataClientConfig` to a new
`common.ClockConfig`, per that bean's own comment instructing exactly this
once a second, unrelated consumer (this adapter) showed up.

Tested via `AlpacaTradingAdapterTest` (19 tests: JSON<->DTO mapping,
auth-header wiring, every error-classification branch, no-HTTP-call proofs
for the no-credential/leverage-validation fatal paths — no live HTTP, no
Spring context, same posture as `AlpacaMarketDataClientTest`),
`AlpacaTradingAdapterContractTest`/`RetryingAlpacaTradingAdapterContractTest`
(7 tests each, running the existing shared `BrokerAdapterContractTest`
suite — bare and `RetryingBrokerAdapter`-wrapped — against a new
test-only `FakeAlpacaTradingServer`: a minimal in-memory fake of Alpaca's
trading API wired in via `MockRestServiceServer`'s custom-`ResponseCreator`
escape hatch rather than a fixed `.expect()` sequence, since the shared
suite's call order isn't fixed), `AlpacaTradingCredentialBootstrapTest` (3
tests: unset env vars, already-stored no-overwrite, first-time seed), and
`BrokerCredentialServiceFindTest` (2 tests, real H2 repository) — 38 new
tests, 226 backend tests total, `./mvnw verify` green.

Not live-verified against a real Alpaca paper account in this session — no
real `ALPACA_TRADING_API_KEY`/`ALPACA_TRADING_API_SECRET` exist on this dev
machine (same gap flagged for `ALPACA_API_KEY` back in E2-F1-S1), so
`getAccountStatus`'s real response shape against Alpaca's live paper API is
unconfirmed beyond the fetched API docs and `FakeAlpacaTradingServer`'s
fixtures. `.env.example` documents `ALPACA_TRADING_API_KEY`/
`ALPACA_TRADING_API_SECRET` alongside the existing market-data keys. No
frontend changes — same backend-only scope as every other E4 story.

## E4-F2-S2 — place a market order via Alpaca; order ID returned, status pollable

E4-F2-S2 (place a market order via Alpaca; order ID returned, status
pollable) is done, closing out F4.2. No new code was needed: E4-F2-S1
already built `AlpacaTradingAdapter.placeOrder`/`getOrderStatus` ahead of its
own narrow AC (flagged explicitly in that story's own entry above —
"needs placeOrder/getOrderStatus/getPosition/cancelOrder immediately next"),
and both were written and tested against `EntryOrderType.MARKET` requests
from the start (`AlpacaTradingAdapterTest.sampleBuyRequest`,
`AlpacaTradingAdapterContractTest.sampleBuyOrderRequest`) — this story's AC
was a proper subset of what S1 already covered, not new scope. Re-ran the
targeted suite to confirm before closing the story rather than trusting the
prior session's summary: `AlpacaTradingAdapterTest` (19, including
`placeOrder_success_sendsBracketBodyAndAuthHeaders` with `"type":"market"`),
`AlpacaTradingAdapterContractTest`/`RetryingAlpacaTradingAdapterContractTest`
(7 each, `placeOrderReturnsResultMatchingRequestedClientOrderId` and
`getOrderStatusForAnUnknownClientOrderIdReturnsEmpty` — bare and
`RetryingBrokerAdapter`-wrapped) — 33 tests, all green, no code changes.
The AC's "dashboard button" framing is narrative, not literal: no HTTP
endpoint or controller exists yet for placing an order, and none was needed
here — the trade-input form/submit endpoint is E5's job (E5-F1-S1 onward),
which can now call the already-wired `BrokerAdapter` bean directly.

Same live-verification gap as E4-F2-S1, unchanged: no real
`ALPACA_TRADING_API_KEY`/`ALPACA_TRADING_API_SECRET` exist on this dev
machine, so a market order has not been placed against Alpaca's real paper
API in this session — only against `FakeAlpacaTradingServer` and
`MockRestServiceServer` fixtures. Flagged, not silently assumed; revisit
once real paper-trading credentials are available.

This closes out F4.2 (Alpaca adapter) in full. F4.3 (Binance adapter,
crypto) is next — E4-F3-S1 (Binance testnet account connected,
`getAccountStatus` returns balances).

## E4-F3-S1 — Binance testnet account connected; `getAccountStatus` returns balances

E4-F3-S1 (Binance testnet account connected; `getAccountStatus` returns
balances) is done, starting F4.3. A `Plan`-agent design gate resolved the
two open design questions before implementation, both then confirmed
directly with the user per this epic's money-safety-adjacent-decision
precedent (mirroring E4-F2-S1's credential-source confirmation): (1) this
adapter targets Binance's **USDⓈ-M Futures Testnet**
(`testnet.binancefuture.com`), not Spot Testnet — Spot has no reliable
leverage support, and E4-F3-S2 (leveraged orders, next) needs real margin,
so building S1 against Spot would have forced a base-API change one story
later; this intentionally diverges from `marketdata.BinanceMarketDataClient`
reading real spot prices, the same accepted paper/live price divergence
`application-paper.properties` already documents for Alpaca, extended one
step further to trading itself. (2) `BrokerAdapter.getOrderStatus`/
`cancelOrder` gained a mandatory `symbol` parameter (`(String symbol, String
clientOrderId, TradingMode mode)`) — a real breaking interface change,
confirmed before implementation — because Binance's per-order endpoints
require `symbol` (no global client-order-id lookup exists the way Alpaca's
`/v2/orders:by_client_order_id` does). The ripple was small and mechanical:
`AlpacaTradingAdapter` and the test-only `MockBrokerAdapter` both simply
ignore the new parameter (their own lookups stay global/keyed-by-id), and
`RetryingBrokerAdapter`'s reconciliation flow threads `request.symbol()`
through automatically. (3) Also confirmed: S1 implements only
`getAccountStatus`/`getPosition` for real — `placeOrder`/`getOrderStatus`/
`cancelOrder` throw a clear, documented `BrokerAdapterException` pointing at
E4-F3-S2, a deliberately narrower scope than Alpaca's S1 (which built the
full interface ahead of need). Binance Futures has no single-call bracket
order the way Alpaca does; building `placeOrder` now would have meant either
rushing E4-F3-S2's leverage/TP-SL design or risking an order with no
protective stop-loss attached.

Before implementation, an `Explore` agent verified the exact Binance Futures
Testnet API details live (docs fetch plus real authenticated-with-garbage-key
requests against the actual testnet) rather than trusting training-data
recall: `GET /fapi/v3/account` and `GET /fapi/v3/positionRisk` (both SIGNED,
HMAC-SHA256 over the query string, `X-MBX-APIKEY` header) are the endpoints
used; `-2014`/`-2015` (bad/invalid API key) empirically confirmed as HTTP 401
(not documented anywhere in Binance's own docs — a real, non-obvious finding);
429/418 (IP auto-ban after repeated 429s) both need rate-limit handling, and
`Retry-After` is **not guaranteed** on Futures endpoints (unlike Alpaca) —
Binance instead exposes `X-MBX-USED-WEIGHT-*` headers this codebase doesn't
yet track, flagged not implemented; and the unrealized-PnL field is spelled
`unRealizedProfit` (capital R) on `/positionRisk` but `unrealizedProfit`
(lowercase r) on `/account` — a real, historically-confirmed Binance
inconsistency between the two endpoint families, defended against via
`@JsonAlias` accepting both.

New `com.autotrade.dashboard.brokeradapter.BinanceFuturesTradingAdapter`
implements `BrokerAdapter` (`supportedAssetType() = CRYPTO`,
`broker() = BINANCE`), plain non-`@Component` class matching
`AlpacaTradingAdapter`'s positioning. Every signed request builds a
canonical `key=value&...` query string (including `timestamp`/`recvWindow`),
HMAC-SHA256-signs it with the API secret, and appends the hex signature —
sent via `RestClient`'s literal-string `uri(String)` overload, **never**
rebuilt through a `UriBuilder`/`UriComponentsBuilder` lambda, since
re-encoding an already-signed query string (so the signed string no longer
matches what's actually sent) is the single most common real-world
Binance-signing bug; this is safe here specifically because every param
value this adapter sends (symbols, timestamps, the hex signature itself) is
plain alphanumeric, nothing that would ever need percent-encoding. Error
classification mirrors Alpaca's three-way split (business outcome / `BrokerAdapterTransientException`
/ `BrokerAdapterRateLimitedException`): 429 and 418 both throw
`BrokerAdapterRateLimitedException` (418 treated the same as 429 for retry
purposes); 5xx/connectivity throws `BrokerAdapterTransientException`;
everything else (401 key errors, 400 signature/timestamp errors) is a fatal,
non-retried `BrokerAdapterException`. `getPosition` treats Binance's
`positionAmt: "0"` row as "no position" (`Optional.empty()`) rather than
relying on an absent row, since Binance always reports a row per symbol.
New `BinanceFuturesTradingProperties` (base URLs only, no credential fields,
same posture as `AlpacaTradingProperties`) and `BinanceFuturesAdapterConfig`
(sibling to `BrokerAdapterConfig`, wraps the adapter in
`RetryingBrokerAdapter` the same way) — both configs' `Map<TradingMode,
RestClient>` beans are now disambiguated via `@Qualifier` on the *consuming*
constructor parameter only (the producing `@Bean` method's own name already
serves as its bean name, so annotating the producer too would have been
redundant — caught and fixed during this story's own `simplify` pass),
since a second bean of that same generic type now exists.

New `com.autotrade.dashboard.broker.BinanceTradingCredentialBootstrap`
(`ApplicationRunner`) mirrors `AlpacaTradingCredentialBootstrap` exactly:
seeds `(BINANCE, PAPER)` into `BrokerCredentialService` from
`BINANCE_TRADING_API_KEY`/`BINANCE_TRADING_API_SECRET` at startup, only if no
credential already exists (idempotent, never fights the audited rotation
flow), `PAPER`-only (no `LIVE` seeding). These must be a **Futures Testnet**
key pair generated at `testnet.binancefuture.com` — a completely separate
testnet-only account from any real Binance.com login, and distinct from any
future Binance market-data credential (`marketdata.BinanceMarketDataClient`
needs no credentials at all today, since it only reads Binance's public
klines endpoint). `.env.example` and all three profile property files
(`application-{local,paper,prod}.properties`) updated accordingly —
`broker.binance.paper-base-url=https://testnet.binancefuture.com`,
`broker.binance.live-base-url=https://fapi.binance.com`.

Tested via `BinanceFuturesTradingAdapterTest` (12 tests: HMAC-SHA256 signing
verified against independently-computed reference signatures — Python's
`hmac`/`hashlib`, not copied from adapter output, same "compute reference
values independently" discipline as E2's indicator tests — plus JSON↔DTO
mapping, every error-classification branch including the 401/`-2015` and
400/`-1022` cases, the zero-`positionAmt` empty-Optional path, the
no-credential fatal path, and all three deferred methods' exception
messages), `FakeBinanceFuturesTradingServer` (test-only, `MockRestServiceServer`
custom-`ResponseCreator`, asserts every request carries `X-MBX-APIKEY` and a
`signature` query param), `BinanceFuturesTradingAdapterContractTest`/
`RetryingBinanceFuturesTradingAdapterContractTest` (7 shared tests each, 5
`@Disabled` with explicit reasons pointing at E4-F3-S2 — this codebase's own
checklist of exactly which tests to re-enable once `placeOrder`/
`getOrderStatus`/`cancelOrder` are real — leaving only
`getAccountStatusReturnsNonEmptyBalances`/
`getPositionForASymbolWithNoActivityReturnsEmpty` actually exercised, per
`BrokerAdapterContractTest`'s own documented allowance for a real-adapter
subclass to skip inapplicable assertions), and
`BinanceTradingCredentialBootstrapTest` (3 tests, mirrors
`AlpacaTradingCredentialBootstrapTest`) — 22 new tests, 255 backend tests
total, `./mvnw verify` green. `simplify` skill run clean after one fix (the
redundant producer-side `@Qualifier` noted above).

Not live-verified against a real Binance Futures Testnet account in this
session — no real `BINANCE_TRADING_API_KEY`/`BINANCE_TRADING_API_SECRET`
exist on this dev machine, so this adapter has only been tested against
`FakeBinanceFuturesTradingServer`/`MockRestServiceServer` fixtures, not a
live testnet call. The `Explore` agent's live (non-authenticated) probes
against the real testnet confirmed the base URL and 401 behavior are
correct, but the exact live shape of a real authenticated `/account`/
`/positionRisk` response — including the `unRealizedProfit` casing
ambiguity noted above — remains unconfirmed beyond docs/secondary sources;
flagged, not silently assumed, same gap pattern as E4-F2-S1/S2's Alpaca
adapter. No frontend changes — same backend-only scope as every other E4
story.

## E4-F3-S2 — place a leveraged order via Binance testnet

E4-F3-S2 (place a leveraged order via Binance testnet) is done, closing out
F4.3 (Binance adapter) and E4 (Broker Adapter Layer) in full. A `Plan`-agent
design gate resolved the story's real crux — Binance Futures has no
single-call bracket order the way Alpaca does — and four money-safety
decisions from that plan were confirmed directly with the user before
implementation, per this epic's established precedent (E4-F2-S1's
credential-source confirmation, E4-F3-S1's Spot-vs-Futures confirmation):
(1) **no auto-flatten** of the position if a take-profit/stop-loss leg fails
to place after entry fills — surfaced as a new `OrderStatus.PARTIALLY_PROTECTED`
result instead, since closing is itself another order action with the same
failure modes and could realize a worse loss than surfacing the state;
(2) the **stop-loss leg is attempted before the take-profit leg** when only
one fits in the bounded retry budget — the protective leg gets priority;
(3) **MARKET entries only for v1** — `LIMIT` is rejected with a clear,
documented `BrokerAdapterException`, since a resting LIMIT entry might not
fill before the call returns and the exit legs' `closePosition=true` needs
an already-open position; and (4) a hardcoded **`MAX_LEVERAGE = 20`**
adapter-intrinsic ceiling, distinct from E6-F2-S1's later user-configurable
risk cap, consistent with this codebase's bias toward hardcoded simple
values (`MarketHoursService`'s calendar, `SignalRuleEngine`'s thresholds)
over an extra live-data dependency on Binance's real per-symbol
`/fapi/v1/leverageBracket` (1x-125x).

`BinanceFuturesTradingAdapter.placeOrder`/`getOrderStatus`/`cancelOrder` are
now real. A "bracket" is three separate Binance orders — `POST
/fapi/v1/leverage` then a MARKET entry, then a `STOP_MARKET` stop-loss and a
`TAKE_PROFIT_MARKET` take-profit (both `closePosition=true`, so they close
the whole position regardless of fills, avoiding separate quantity
tracking). All three orders' `newClientOrderId`s are derived deterministically
from the app's single `clientOrderId` via SHA-256 (truncated to 30 hex chars
plus a `-E`/`-T`/`-S` suffix) rather than suffixed directly onto it — Binance's
36-char id limit is incompatible with this app's own `orders.client_order_id
VARCHAR2(64)` column. This determinism is what makes every leg idempotent on
retry: `placeOrder` always **checks first** (`GET /fapi/v1/order` by the
derived id) before ever creating a leg, rather than "POST then catch a
duplicate-id error" the way `AlpacaTradingAdapter` does — a full replay of
`placeOrder`, whether from a caller retry or `RetryingBrokerAdapter`'s own
outage reconciliation, safely re-discovers whatever already exists instead
of risking a duplicate order. Once the entry is confirmed `FILLED`/
`PARTIALLY_FILLED`, `placeOrder` never throws again: each exit leg is
placed with a small **bounded local retry** (2 attempts, 200ms pause —
same "one bounded retry" bias as `marketdata.RetryHelper`, deliberately not
`RetryingBrokerAdapter`'s multi-second exponential backoff), and every
failure (transient, rate-limited, or a fatal business rejection) is caught
uniformly and never rethrown post-fill — the caller only learns via the
returned `PARTIALLY_PROTECTED` result which leg(s) are missing. A business
rejection *before* the entry fills (bad leverage, insufficient margin) is
still returned as a normal `REJECTED` result, never thrown, per
`BrokerAdapter`'s existing contract.

`getOrderStatus` reports a **composite status** across all three legs: an
entry that hasn't filled yet is reported as-is; once filled, a missing or
terminated-without-filling exit leg downgrades the report to
`PARTIALLY_PROTECTED`, and an exit leg that itself shows `FILLED` (it
triggered, closing the position) reports as `CANCELLED` — the closest
existing vocabulary for "no longer live, no rejection" (which leg fired and
at what price isn't preserved by this interface today, a flagged, accepted
gap). `cancelOrder` cancels only the entry leg — same "cancel the order, not
the position" scope `AlpacaTradingAdapter` already has for its own bracket
legs — and is an idempotent no-op on any terminal composite status,
including the new `PARTIALLY_PROTECTED`.

`OrderStatus` gained `PARTIALLY_PROTECTED`; `V7__widen_orders_status_check_partially_protected.sql`
widens `ck_orders_status` to allow it (Oracle requires drop-then-recreate to
widen a CHECK constraint's value list). No other entity/enum needed a
change — a grep confirmed no exhaustive `switch` over `OrderStatus` exists
anywhere in the codebase yet (`AlpacaTradingAdapter.mapStatus` switches over
Alpaca's own status *strings*, not this enum), so `AlpacaTradingAdapter`/
`MockBrokerAdapter` both compile and behave unchanged despite never
producing the new value. `BrokerOrderResult`'s Javadoc was broadened to
document the new status's `filledPrice`/`rejectionReason` semantics.

One security-review finding, fixed before this story closed: every signed
Binance request builds a **literal, non-re-encoded query string** (see
`BinanceFuturesTradingAdapter`'s own class Javadoc on why — re-encoding
would break the signature), and `symbol` is the one caller-supplied value
built directly into it. `TickerController`'s own validation is only
`@NotBlank @Size(max = 20)`, no character-class restriction, so an
unvalidated ticker symbol like `"BTC&leverage=125"` could inject extra query
parameters into a real money-moving Binance request — a latent gap that
existed since E4-F3-S1's `getPosition`/`getAccountStatus` but that this
story's new `placeOrder`/`cancelOrder` calls meaningfully widened the blast
radius of. Fixed with a new `validateSymbol` fatal, pre-HTTP check (`^[A-Z0-9-]{1,20}$`
— hyphen allowed only for this codebase's own `"-NOACTIVITY"` test-symbol
convention from `BrokerAdapterContractTest`) applied to all four methods
that take a raw `symbol`: `placeOrder`, `getOrderStatus`, `getPosition`
(retrofitted), and transitively `cancelOrder` (which calls `getOrderStatus`
first). Two new tests prove the guard rejects an injection-shaped symbol
before any HTTP call.

Tested via 24 new/changed tests in `BinanceFuturesTradingAdapterTest`
(validation-fatal cases for asset type/entry type/leverage bounds/symbol
injection, full bracket-success mapping, leverage-rejected and
entry-rejected short-circuits, a take-profit-leg-fails-after-retry ->
`PARTIALLY_PROTECTED` case, a full-replay idempotency case proving zero POST
calls on a repeated `clientOrderId`, `getOrderStatus` unknown/filled/missing-leg/
triggered-leg cases, and `cancelOrder` idempotent-on-terminal vs.
cancels-a-resting-entry cases) plus the 5 previously-`@Disabled` shared
`BrokerAdapterContractTest` cases now enabled and passing in both
`BinanceFuturesTradingAdapterContractTest` and
`RetryingBinanceFuturesTradingAdapterContractTest` — 271 backend tests
total, `./mvnw verify` green. `adapter-contract-check` and `simplify` skills
both run clean (two throwaway/unused fields removed during the simplify
pass: an unused `BinanceLeverageResponse` DTO whose only field was never
read, and `BinanceOrderResponse.clientOrderId`, which this adapter never
reads since — unlike Alpaca — it always returns the app's own `clientOrderId`
in results, never Binance's derived leg id).

Not live-verified against a real Binance Futures Testnet account in this
session — same gap as E4-F3-S1, no real `BINANCE_TRADING_API_KEY`/
`BINANCE_TRADING_API_SECRET` exist on this dev machine, so the full
leverage-set/entry/exit-leg order flow has only been proven against
`FakeBinanceFuturesTradingServer`/`MockRestServiceServer` fixtures, not a
live testnet call. The exact Binance error code for a leverage-set rejection
(the Plan agent's own research guessed `-4028` but couldn't confirm it
live) remains unconfirmed — the adapter's classification doesn't depend on
that specific code (any non-429/418 4xx during leverage-set maps to a
`REJECTED` result), so this doesn't block correctness, only the precision
of matching Binance's exact documented error taxonomy. No frontend changes
— same backend-only scope as every other E4 story.

This closes out F4.3 (Binance adapter) and E4 (Broker Adapter Layer) in
full. E5 (Auto-Trade Execution) is next — E5-F1-S1 (trade input form) and
E5-F2-S1 (bracket order construction/submission) can now build on top of
both `BrokerAdapter` implementations being complete.

## E5-F1-S1 — trade input form: amount, leverage, take-profit, stop-loss

E5-F1-S1 (trade input form: amount, leverage, take-profit, stop-loss) is
done, starting E5 (Auto-Trade Execution). No `Plan`-agent design gate —
frontend-only, no new architectural decision, same reasoning as E3-F1-S1/S2.
The two open numeric-bound questions were resolved directly against this
codebase's own existing precedents rather than left to guesswork: leverage's
upper bound mirrors `BinanceFuturesTradingAdapter.MAX_LEVERAGE` (20), and
stock orders are validated to exactly 1x, mirroring the DB-level check
constraint from E1-F2-S1 that stock orders can never carry leverage. This is
deliberately narrower than E5-F1-S2's scope: leverage is still always
*shown* here (no asset-type-conditional hiding yet — that's next), just
tightly validated per asset type. Take-profit/stop-loss are validated not
just as positive numbers but directionally against the signal's own call
and current price (BUY: TP above price, SL below; SELL: TP below price, SL
above) — a real "broker limits" check, not just numeric-range busywork,
since a TP/SL on the wrong side of price is nonsensical for a bracket order.
The form only renders for a BUY/SELL call; a HOLD has no direction to size
an entry for, so nothing is shown (matches `SignalBadge`'s own no-hold-term
precedent for HOLD).

New `frontend/src/trade/` package (mirrors `chart`/`signal`/`watchlist`):
`validation.ts` (pure `validateTradeForm`, no React) and `TradeForm.tsx`
(the form itself, wired into `TickerMetrics.tsx` right after
`AddToWatchlistButton`, keyed by ticker symbol so switching tickers resets
its state same as the watchlist button). Submitting doesn't call a broker
yet — bracket-order construction and adapter routing are E5-F2-S1's
explicit scope — so a valid submit just renders "Order details captured —
submitting this to the broker lands in E5-F2-S1" rather than pretending to
place a real order, the same kind of explicit test-only/not-yet-wired
stand-in this codebase already used in E1-F4-S1's E2E test and E4-F1-S1's
unconsumed interface methods. New `index.css` `.trade-form` rules follow
the existing `.stat-tile`/`.signal-badge` `light-dark()` pattern.

Tested via `validation.test.ts` (9 Vitest cases: valid BUY/SELL payloads,
non-positive/non-numeric amount, stock-leverage-must-be-1x, crypto leverage
bounds and fractional-leverage rejection, and directional TP/SL validation
both ways) — 13 frontend tests total (4 existing + 9 new), `npm run
build`/`npm run lint`/`npm test` all pass clean (same pre-existing unrelated
`AuthContext.tsx` lint warning as every prior story). No backend changes —
this story is entirely frontend, same split as E3's stories.

Verified live via the `run` skill against the real running stack (Docker
Oracle XE + real public Binance API, no mocking; logged in through E1-F3-S2's
session-cookie auth) — Docker Desktop wasn't running at the start of this
session and had to be launched first, then Oracle XE brought up fresh and
polled for healthy before the backend would connect. `BTCUSDT` (a live HOLD)
correctly rendered no trade form at all; `ETHUSDT` (a live BUY) rendered the
full form, showed all four live validation errors on empty submission,
cleared them once valid amount/leverage/TP/SL were entered, enabled the
previously-disabled "Trade" button, and clicking it rendered the
not-yet-wired confirmation note. Typing leverage `25` against `ETHUSDT`
(crypto) correctly rendered "Leverage must be between 1x and 20x." live.
`AAPL` correctly still 409'd `MARKET_CLOSED` (checked outside market hours),
confirming no regression to E2-F1-S3 — this also means the STOCK-leverage-
capped-at-1x path could only be verified via `validation.test.ts`, not live
in this session (no stock ticker had a live BUY/SELL signal available
during market-closed hours); flagged, not silently assumed, same disclosure
style as prior stories' un-observed paths (e.g. E3-F1-S2's BUY badge).

## E5-F1-S2 — hide/default fields by asset type

E5-F1-S2 (hide/default fields by asset type) is done, closing out F5.1. No
`Plan`-agent design gate — frontend-only, no open design question, same
reasoning as every other small E3/E5 frontend story. The AC's two halves were
already partially true after E5-F1-S1: leverage was always numerically
validated per asset type, just not conditionally rendered. This story is the
rendering half only — `TradeForm.tsx`'s leverage `<label>`/`<input>` block
now renders only when `ticker.assetType === 'CRYPTO'` (a plain-number
`<input type="number" min={1} max={MAX_CRYPTO_LEVERAGE} step={1}>`, replacing
the prior freeform text input that only complained after the fact); for a
stock, the field is omitted entirely and `values.leverage` simply never
leaves its `DEFAULT_VALUES` default of `'1'`, still checked defense-in-depth
by the unchanged `validateTradeForm` stock branch. No changes to
`validation.ts`'s logic — only its doc-comment, since asset-type-aware
visibility now genuinely lives in `TradeForm.tsx` rather than being a
still-open TODO the comment had flagged since E5-F1-S1.

Tested via the existing `validation.test.ts` (13 tests, unchanged — the
validation function's signature/behavior didn't change, only what
`TradeForm.tsx` chooses to render) — `npm run build`/`npm run lint`/`npm
test` all pass clean, same pre-existing unrelated `AuthContext.tsx` lint
warning as every prior story. No backend changes.

Verified live via the `run` skill against the real running stack (Docker
Oracle XE + real public Binance API, no mocking; already running from a
prior session, backend/frontend both confirmed healthy on their expected
ports before reuse) — `ETHUSDT` (a live BUY) rendered the trade form with a
bounded `Leverage (1x-20x)` number-spinner input (spinner arrows visible,
defaulted to `1`); typing `25` correctly rendered "Leverage must be between
1x and 20x." live, matching the crypto validation path. `AAPL` (checked
outside market hours) still correctly 409'd `MARKET_CLOSED`, confirming no
regression to E2-F1-S3 — this also means the stock-hides-leverage-entirely
render path could only be confirmed by reading the conditional (`ticker.assetType
=== 'CRYPTO'`) plus `validation.test.ts`'s existing stock-1x-only case, not
observed live in this session, same gap E5-F1-S1 itself already flagged for
this exact path.

## E5-F2-S1 — bracket-order construction and submission through the `BrokerAdapter` layer

E5-F2-S1 (bracket-order construction and submission through the
`BrokerAdapter` layer) is done, starting F5.2. A `Plan`-agent design gate
(mandatory for bracket-order construction per this repo's own convention)
fixed every open design question up front, and two of its money-safety
decisions were confirmed directly with the user before implementation, per
this epic's established pattern (E4-F2-S1/E4-F3-S1/E4-F3-S2's own
check-ins): (1) **the USD amount is notional trade size, not margin** —
`quantity = amountUsd / price`, independent of leverage; leverage only
affects margin efficiency at the broker (via Binance's set-leverage call),
so a fat-fingered leverage value can't silently balloon exposure beyond the
dollar amount entered; and (2) a genuinely ambiguous `placeOrder` outcome
(`BrokerAdapterAmbiguousOrderException` — both the original call and
`RetryingBrokerAdapter`'s own reconciliation probe failed) gets a new
**`OrderStatus.SUBMISSION_UNKNOWN`** rather than being folded into `FAILED`,
since `FAILED` would wrongly imply it's safe to retry — `SUBMISSION_UNKNOWN`
honestly encodes "don't know, don't resubmit" in the audit trail.
`V8__widen_orders_status_check_submission_unknown.sql` widens
`ck_orders_status` for it, same drop-and-recreate pattern as `V7`.

The other design-gate decision: **the backend never trusts the client's
cached `SignalResponse`.** `POST /api/tickers/{symbol}/orders` accepts only
`amountUsd`/`leverage`/`takeProfitPrice`/`stopLossPrice` — direction and
price are always re-derived by recomputing the signal server-side
(`SignalService.computeSignalWithProvenance`, a new method returning a
`SignalComputation(SignalResponse, IndicatorSnapshot)` record; the existing
`computeSignal` is now a thin wrapper over it, same refactor shape
`IndicatorService.computeForSignal`/`computeIndicators` already
established). A call that's flipped to HOLD between the user's lookup and
their click throws a new `SignalNotActionableException` (409
`SIGNAL_NOT_ACTIONABLE`) — nothing to trade, no `Order` row created.

New `com.autotrade.dashboard.brokeradapter.BrokerAdapterRouter` — the
routing component `BrokerAdapterConfig`/`BinanceFuturesAdapterConfig` (E4)
deliberately deferred as YAGNI until a second adapter existed to route
between. Both real `@Bean BrokerAdapter`s are already plain-typed (wrapped
in `RetryingBrokerAdapter`), so a `List<BrokerAdapter>` injection picks up
both with no `@Qualifier` needed; an `EnumMap<AssetType, BrokerAdapter>`
keyed by each adapter's own `supportedAssetType()` — same pattern
`MarketDataService` already established for market-data clients.

New `com.autotrade.dashboard.order.OrderService.submitOrder` is the actual
bracket-order flow: recompute the signal → reject HOLD → re-validate
leverage/TP/SL server-side against the *fresh* price (mirrors
`trade/validation.ts` almost line-for-line, an accepted flagged triple
duplication of `MAX_CRYPTO_LEVERAGE=20` across FE/BE-adapter/BE-service,
same posture `validation.ts`'s own comment already accepted for the
FE/adapter pair) → convert `amountUsd` to a quantity
(`amountUsd.divide(price, 8, RoundingMode.DOWN)` — rounds down, never lets
the computed quantity imply spending more than requested) → route to the
right `BrokerAdapter` via the new router → resolve the `(broker, PAPER)`
`BrokerCredential` via `BrokerCredentialService.find` (`TradingMode` is
hardcoded `PAPER` in `OrderService`, never taken from the request — `LIVE`
isn't seeded by either credential bootstrap yet, so live trading is
structurally unreachable through this endpoint until E6's gate exists;
absence throws a new `BrokerCredentialNotConfiguredException`, 503
`BROKER_CREDENTIAL_NOT_CONFIGURED`) → generate `clientOrderId` and persist
the `Order` row as `PENDING` in its own short write (so the idempotency key
survives an app crash mid-call) → call `adapter.placeOrder` with **no open
transaction** (an external HTTP round-trip, including
`RetryingBrokerAdapter`'s own multi-attempt backoff, must never hold a DB
connection) → finalize the same row in a second short write. Every outcome —
filled, a business rejection, `PARTIALLY_PROTECTED`, `SUBMISSION_UNKNOWN`, or
a plain infra `FAILED` — is written onto that row as a normal value, never a
second HTTP exception; only pre-flight failures (bad ticker, HOLD, invalid
request, no credential) skip creating an `Order` row at all. New
`PlaceOrderRequest`/`TradeOrderResponse` records, `OrderController`
(`POST /api/tickers/{symbol}/orders`, always 201), and
`OrderExceptionHandler` (a separate `@RestControllerAdvice` from
`MarketDataExceptionHandler`, per that class's own documented invitation for
an unrelated — here, money-moving — error domain to decide for itself).

Tested via `BrokerAdapterRouterTest` (2), `OrderServiceTest` (11: every
pre-flight rejection, a filled/rejected/ambiguous/unavailable/rate-limited/
fatal outcome each mapped onto the right persisted status, and a stock
leverage-forced-to-1x case), and `OrderControllerTest` (6, status codes and
error bodies) — 19 new tests, 290 backend tests total, `./mvnw verify`
green.

Frontend: new `frontend/src/trade/api.ts` (`placeOrder`, reusing
`parseMarketDataError`; two new `MarketDataErrorCode` members,
`SIGNAL_NOT_ACTIONABLE`/`BROKER_CREDENTIAL_NOT_CONFIGURED`).
`TradeForm.tsx`'s `handleSubmit` now actually calls the broker: a
`SubmitState` union (`idle`/`submitting`/`result`/`error`) replaces the old
placeholder boolean, `describeResult` maps each `OrderStatus` to a
tone-and-message pair — `success` (FILLED/PARTIALLY_FILLED), `warning`
(PARTIALLY_PROTECTED/SUBMISSION_UNKNOWN — deliberately not styled as a
routine success or failure, since both mean a real position needs manual
attention), or `error` (REJECTED/FAILED) — rendered via new
`.trade-form__result--{success,warning,error}` CSS rules (same
`light-dark()` pattern as `.stat-tile`/`.signal-badge`, warning using a new
amber hue distinct from both). No confirmation step — per the AC's "in one
click" framing and this being the story immediately superseded by
E5-F2-S2, `handleSubmit` fires the real order as soon as client-side
validation passes. `npm run build`/`lint`/`test` all pass clean (13 tests,
unchanged — no new frontend logic worth a Vitest case beyond what
`validation.test.ts` already covers; the real order-submission behavior is
verified via backend tests and live browser verification instead).

Verified live via the `run` skill against the real running stack (Docker
Oracle XE + real public Binance API, no mocking; logged in through E1-F3-S2's
session-cookie auth) — a repeat of this file's own recurring gotcha: stale
`java`/`node` processes from an earlier session were still holding
8080/5173 with pre-story code, killed and both restarted clean before
trusting `/health`. `V8` applied cleanly against real Oracle (schema version
7 → 8). Exercised every pre-flight path via `curl` against real data first:
a HOLD ticker (`BTCUSDT`, `CONFLICTING_SIGNALS`) correctly 409'd
`SIGNAL_NOT_ACTIONABLE`; 25x leverage against a live `SELL` (`SOLUSDT`)
correctly 400'd `INVALID_REQUEST` ("Leverage must be between 1x and 20x.");
a take-profit on the wrong side of a live SELL price correctly 400'd;
`NOTREAL` correctly 404'd `TICKER_NOT_REGISTERED`; a negative amount
correctly 400'd via bean validation; and a fully valid `SOLUSDT` SELL order
correctly 503'd `BROKER_CREDENTIAL_NOT_CONFIGURED` — the actual reachable
terminus on this dev machine, since no real `BINANCE_TRADING_API_KEY`/
`BINANCE_TRADING_API_SECRET` exist here (same flagged gap as every E4-F2/F3
story). A direct `sqlplus` query confirmed **zero** rows were ever written
to `orders` across all six of these attempts — proving every pre-flight
failure, including the credential check, short-circuits before the `Order`
row is created, exactly as designed. Then clicked through the identical
`SOLUSDT` SELL flow in a real browser: the trade form rendered correctly
keyed to the live SELL signal, client-side validation errors appeared and
cleared correctly, and submitting rendered "No broker credentials are
configured for this asset type yet — the order was not submitted." in the
new error-toned result box, with no browser console errors. A real
FILLED/REJECTED/PARTIALLY_PROTECTED/SUBMISSION_UNKNOWN render, and the
`SIGNAL_NOT_ACTIONABLE` race window itself (the call flipping to HOLD
between lookup and click), were **not** observed live in this session — the
former needs real broker credentials this dev machine doesn't have, the
latter needs a live signal flip during a manual click, neither
practically reproducible here; flagged, not silently assumed, same
disclosure style as this file's other un-observed paths (e.g. E3-F1-S2's
BUY badge, E3-F2-S1's `autoSize` resize).

## E5-F2-S2 — explicit confirmation step before the order fires

E5-F2-S2 (explicit confirmation step before the order fires) is done,
closing out F5.2. No `Plan`-agent design gate — frontend-only, no open
design question, same reasoning as every other small E3/E5 frontend story.
`TradeForm.tsx`'s `SubmitState` union gained a `confirming` variant carrying
a snapshotted `PlaceOrderPayload`; `handleSubmit` (the form's `onSubmit`)
now only validates and transitions to `confirming` — it no longer calls
`placeOrder` directly. A native `<dialog>` element (`useRef`+`useEffect`
opening it via `showModal()`/closing via `close()` whenever
`submitState.kind` toggles in/out of `confirming`) renders the order
summary (amount, leverage — only for crypto, matching the field's own
asset-type-conditional visibility from E5-F1-S2 — take-profit, stop-loss)
with two buttons: "Cancel" (`handleCancelConfirm`, resets straight to
`idle`) and "Confirm trade" (`handleConfirm`, the only path that now calls
`placeOrder`, using the snapshotted payload rather than re-reading the
form's live values). Dismissing via Esc fires the dialog's native `cancel`
event, wired to the same `handleCancelConfirm` — so every dismissal path
(button or Esc) is one function with no API call. Deliberately no `onClose`
handler: the dialog is only ever closed by the effect reacting to
`submitState.kind`, so listening for the browser's own `close` event would
double-fire the same reset and risk clobbering a `submitting`/`result`
state the effect had just transitioned to. New `.trade-confirm-dialog`
CSS rules (border/background/`::backdrop` dimming, a `dl`-based summary
grid, right-aligned actions) follow the same `light-dark()` pattern as
`.trade-form`/`.stat-tile`/`.signal-badge`.

No backend changes — this story only inserts a client-side gate in front
of the existing `POST /api/tickers/{symbol}/orders` call E5-F2-S1 already
built; the backend still re-derives direction/price from a fresh signal
computation regardless of what the confirmation dialog displayed, so a
`SIGNAL_NOT_ACTIONABLE` race is still possible if the call flips between
opening the dialog and confirming — unchanged, pre-existing behavior. No
new Vitest cases — `vitest.config.ts` runs in a plain `node` environment
(no jsdom), so this project has no DOM-testing infrastructure to exercise
`<dialog>`'s `showModal`/`close`/`cancel` behavior; `npm run
build`/`lint`/`test` all pass clean (13 tests, unchanged), and the dialog's
actual behavior was verified live instead, per this story's own real
money-safety stakes.

Verified live via the `run` skill against the real running stack (Docker
Oracle XE + real public Binance API, no mocking; logged in through E1-F3-S2's
session-cookie auth) — stale `java`/`node` processes from an earlier
session were again holding 8080/5173 (this file's own recurring gotcha),
killed and both restarted clean before trusting `/health`. Looked up the
live `SOLUSDT` SELL signal, filled a valid amount/TP/SL, and clicked
"Trade": the confirmation dialog rendered with the exact summary values
entered, the page dimmed via `::backdrop`, and — confirmed via
`read_network_requests` — **zero** requests fired while the dialog was
open. Clicking "Cancel" closed the dialog with the form values still
intact and the Trade button re-enabled, still zero requests. Reopening and
pressing **Escape** produced the identical result (dialog closed, zero
requests) — proving the native `cancel` event path behaves the same as the
explicit Cancel button. Reopening a third time and clicking "Confirm
trade" fired exactly one `POST /api/tickers/SOLUSDT/orders`
(`read_network_requests` showed a single matching entry), which correctly
503'd `BROKER_CREDENTIAL_NOT_CONFIGURED` — the same reachable terminus
E5-F2-S1 already hit live, since no real `BINANCE_TRADING_API_KEY`/
`BINANCE_TRADING_API_SECRET` exist on this dev machine — and the dialog
closed back to the form with that error rendered. Browser console showed
no errors at any point. This closes out F5.2 (order construction &
submission) in full. E5-F3-S1 (order status/history) is next.

## E5-F3-S1 — order status/history

E5-F3-S1 (order status/history) is done, starting F5.3. A `Plan`-agent design
gate resolved the story's real question: given `OrderService.submitOrder`
(E5-F2-S1) already writes every order's final synchronous outcome to the row
before returning, is there ongoing "pending" state worth polling for? Decision:
**no automatic/background polling anywhere** — only `SUBMISSION_UNKNOWN` and
`PARTIALLY_PROTECTED` (plus, in principle, a `PENDING` row orphaned by an app
crash mid-submission) can ever go stale after that write, and those are
resolved by an explicit, manual per-order **"Refresh" button**, not a
scheduler or `setInterval` — matching this codebase's established bias
against automated background action on money-moving state (E5-F2-S2's
explicit confirm step, E4-F3-S2's no-auto-flatten decision). One decision
had real money-safety implications and was confirmed with the user before
implementation: when a refresh's `getOrderStatus` call returns
`Optional.empty()` (broker has no record of the `clientOrderId`), the row is
marked `FAILED` with *"Broker confirmed no record of this order — safe to
retry."* — mirroring `OrderService`'s existing outage-reconciliation wording
elsewhere. If the refresh call instead **throws**, the stored row is left
completely untouched (no status overwrite on a failed read) and a distinct
503 is surfaced instead.

New `OrderService.listOrders(limit)` (reads straight from
`OrderRepository.findAllByOrderByCreatedAtDesc`, a new derived query using
`PageRequest` — this codebase's first use of Spring Data `Pageable`) and
`OrderService.refreshOrder(orderId)` (loads the order, resolves the adapter
via the existing `BrokerAdapterRouter`, calls `adapter.getOrderStatus(...)`
with no open transaction — same short-write discipline `submitOrder` already
documents — and reuses the existing private `applyOutcome` helper for the
persisted update). New `OrderResponse` record (deliberately separate from
`TradeOrderResponse`, which is scoped to "the result of a click-Trade
submission" and is missing fields — ticker symbol, TP/SL, leverage — a status
page needs; same DTO shape serves both the list and the refresh response).
New `OrderNotFoundException` (404 `ORDER_NOT_FOUND`) and
`OrderRefreshUnavailableException` (503 `ORDER_REFRESH_UNAVAILABLE`), wired
into the existing `OrderExceptionHandler`. New `OrderQueryController`
(`GET /api/orders?limit=N` default 50/max 500, `POST /api/orders/{id}/refresh`)
— a separate top-level `/api/orders` resource mirroring `/api/watchlist`'s
precedent, kept apart from the existing submission-only `OrderController`
(`/api/tickers/{symbol}/orders`). No Flyway migration — every field
`OrderResponse` needs already exists on `Order` from E1-F2-S1/E5-F2-S1;
`updatedAt` (already bumped by `@PreUpdate` on every save) doubles as "last
confirmed against broker at" with no new column.

New `frontend/src/order/` package (mirrors `chart`/`signal`/`watchlist`):
`api.ts` (`OrderSummary`, `fetchOrders`, `refreshOrder` — reusing
`Broker`/`OrderSide`/`OrderStatus` from `trade/api.ts` and
`parseMarketDataError` from `marketdata/api.ts` rather than duplicating
either) and `OrderHistory.tsx` (a `<table>` — genuinely tabular data,
appropriate departure from `Watchlist.tsx`'s `<ul>` — one row per order with
a per-row "Refresh" button, disabling just that row and patching just that
row's data on success, with a per-row inline error on failure so one broker
hiccup doesn't blank the rest of the table; no `setInterval` anywhere, per
the design gate). Two new `MarketDataErrorCode` members
(`ORDER_NOT_FOUND`/`ORDER_REFRESH_UNAVAILABLE`). `TradeForm.tsx` gained an
`onOrderPlaced?: () => void` prop, called right after a `placeOrder` call
resolves (regardless of the resulting status — even a REJECTED/FAILED order
is a real row worth showing; pre-flight failures that never create an
`Order` row correctly don't trigger it, since `placeOrder` throws for those
instead of resolving), threaded through `TickerMetrics.tsx` the same way
`onWatchlistChanged` already is. `DashboardPage.tsx` gained an
`orderHistoryRefreshKey` bumped on that callback, rendering `<OrderHistory
refreshKey={orderHistoryRefreshKey} />` below `TickerMetrics`. New
`index.css` rules (`.order-history-table`, `.order-status--{success,warning,
error,neutral}` reusing the same tone split `TradeForm.tsx`'s
`describeResult` already established for `OrderStatus`) inside a
`.order-history-table-wrap` (`overflow-x: auto`, since a wide table must
scroll in its own container rather than the page).

Tested via 11 new backend tests (`OrderServiceTest`: `listOrders` mapping,
unknown-id 404, broker-confirms-a-status refresh, broker-has-no-record →
`FAILED`/safe-to-retry, and broker-throws → row left completely untouched
(asserted via the same mutable `Order` object, plus `verify(orderRepository,
never()).save(any())`); `OrderQueryControllerTest`: limit bounds, refresh
success/404/503) — 301 backend tests total, `./mvnw verify` green. No new
frontend Vitest cases — `OrderHistory.tsx` is a view-only DOM component with
no pure logic to unit test, the same gap already accepted for
`Watchlist.tsx`; `npm run build`/`lint`/`test` all pass clean (13 tests,
unchanged, same pre-existing unrelated `AuthContext.tsx` lint warning as
every prior story). `simplify` skill run clean — no new abstraction beyond
what the story needed (limit validation reuses the existing
`InvalidTradeRequestException`/`INVALID_REQUEST` code rather than adding a
parallel exception type just for a bounds check).

Verified live via the `run` skill against the real running stack — Docker
Desktop wasn't running at the start of this session and had to be launched
first, then Oracle XE brought up fresh and polled healthy (schema version
confirmed at 8, no migration needed, matching this story's own no-migration
design) before the backend would connect; logged in through E1-F3-S2's
session-cookie auth. Exercised the new endpoints via `curl` against real
Oracle first: `GET /api/orders` on a fresh container returned `200 []`;
`limit=0`/`limit=501` both correctly 400'd `INVALID_REQUEST`; `POST
/api/orders/999999/refresh` correctly 404'd `ORDER_NOT_FOUND` against a real
repository lookup. Then clicked through the real UI: `DashboardPage`
rendered a new "Order history" section reading "No orders placed yet." on
load; looked up the live `SOLUSDT` SELL signal, filled a valid amount/TP/SL,
confirmed the trade through the E5-F2-S2 dialog, and the attempt correctly
503'd `BROKER_CREDENTIAL_NOT_CONFIGURED` (same reachable terminus every
E4-F2/F3/E5-F2 story has hit on this dev machine, since no real
`BINANCE_TRADING_API_KEY`/`BINANCE_TRADING_API_SECRET` exist here) — and,
critically, the Order History section still read "No orders placed yet."
afterward, confirming `onOrderPlaced` is correctly *not* fired for a
pre-flight failure that never created a row. A direct `sqlplus` query
against the live container confirmed `orders` held exactly 0 rows throughout,
matching the UI. A real order actually appearing in the history table, and
the Refresh button's own re-poll of an existing row (both the
broker-confirms-a-new-status and the broker-has-no-record paths), were **not**
observed live in this session — same structural gap as every E4-F2/F3/E5-F2
story before it, since creating a real `Order` row on this dev machine needs
broker credentials that don't exist; flagged, not silently assumed, and
covered instead by `OrderServiceTest`'s mocked-adapter cases above.

## E5-F3-S2 — CSV export of trade history

E5-F3-S2 (CSV export of trade history) is done, closing out F5.3 and E5
(Auto-Trade Execution) in full. A `Plan`-agent design gate resolved the
open questions before implementation: **date-range semantics** — `start`/
`end` are `LocalDate` query params treated as **UTC calendar days**
(inclusive both ends), since this codebase has no established "user local
time" concept anywhere (every timestamp is UTC `Instant`; the one existing
timezone, `MarketHoursService`'s `America/New_York`, is NYSE-specific, not
user-local) — flagged, not silently assumed, as a real (if narrow)
discrepancy for a Singapore-local user, not worth a first local-time concept
for a 2-point story; **mode filter** — an optional `mode` param, defaulting
to *all* modes when omitted (matching `GET /api/orders`'s own no-filter
default), not defaulting to `PAPER` even though that's the only reachable
mode today, so the export stays consistent with the list endpoint rather
than needing a silent behavior change once E6 seeds `LIVE` credentials; and
**signal-snapshot reference** — resolved as human-readable `SignalCallEntry`
fields (call, matched rule, rule table version, hold-term), not a bare
`indicator_snapshot_id`, since a numeric FK alone isn't a useful "record for
my own tracking" when `SignalRuleId` is already surfaced everywhere else in
this app as "the documented threshold table."

`OrderRepository.findByOrderModeAndCreatedAtBetween` (added ahead of need
back in E5-F2-S1 anticipating this story) gained an explicit sort —
renamed to `findByOrderModeAndCreatedAtBetweenOrderByCreatedAtAsc` — since
an export reads naturally chronologically (oldest first), the opposite of
the on-screen table's newest-first order; a new mode-agnostic sibling,
`findByCreatedAtBetweenOrderByCreatedAtAsc`, backs the no-mode-filter
default. New `SignalCallEntryRepository.findByIndicatorSnapshot_IdIn`
(batched, avoiding N+1 across however many orders land in the range) feeds
a `Map<Long, SignalCallEntry>` that `OrderService.exportOrdersCsv` builds
once per request and hands to the new `OrderCsvExporter` (pure static,
package-private, mirrors `RsiCalculator`'s static-utility shape) — hand-
rolled CSV generation rather than a library, consistent with this
codebase's stated bias against a dependency for a small, deterministic task
(`RetryHelper`, `MarketHoursService`, hand-rolled indicators); RFC 4180
escaping (quote-wrap on comma/quote/CR/LF, embedded quotes doubled, CRLF
line endings) covers the one genuinely free-text column, `rejectionReason`.
25 columns, identity → trade economics → execution/status → timestamps →
signal provenance last. `GET /api/orders/export?start=...&end=...[&mode=...]`
(new route on the existing `OrderQueryController`) returns
`text/csv;charset=UTF-8` with a `Content-Disposition: attachment` header
carrying a `trade-history-<start>-to-<end>.csv` filename — a real file
download, not JSON-wrapped, the app's first deliberate content-type switch.
`start` after `end` reuses the existing `InvalidTradeRequestException` → 400
`INVALID_REQUEST` (no new exception type); an empty range returns 200 with
just the header row, matching `GET /api/orders`'s own empty-list-is-valid
precedent. No Flyway migration — every CSV column already existed on
`Order`/`IndicatorSnapshot`/`SignalCallEntry`.

Frontend: new `frontend/src/order/OrderExport.tsx` (rendered inside
`OrderHistory.tsx` right after its heading — independent of whatever page
of orders the table has loaded, so no `refreshKey` coupling needed), two
plain `<input type="date">` fields plus a button, with client-side
empty-date and start-after-end checks before any network call (fail fast,
mirroring `TradeForm.tsx`'s own validate-before-submit shape).
`order/api.ts` gained `exportOrdersCsv`, which fetches (not a plain
`<a href>` navigation) so a validation failure renders through this app's
existing typed `MarketDataError` handling instead of landing on raw JSON;
on success it parses the filename back out of `Content-Disposition` (a
plain regex, not a full RFC 6266 parser, since this app only ever generates
its own simple `filename="..."` form), then downloads via
`URL.createObjectURL` + a synthesized hidden `<a download>` click +
`URL.revokeObjectURL`. New `.order-export`/`.order-export__error` CSS rules
follow the existing `light-dark()` pattern. No new Vitest cases — same gap
already accepted for `Watchlist.tsx`/`OrderHistory.tsx` (view-only DOM
components with no pure logic to unit test); `npm run build`/`lint`/`test`
all pass clean (13 tests, unchanged, same pre-existing unrelated
`AuthContext.tsx` lint warning as every prior story).

Tested via 9 new `OrderCsvExporterTest` cases (empty list → header-only,
no-signal-snapshot → blank signal columns, a populated snapshot → matched
rule/call/rule-table-version/hold-term rendered correctly, and both RFC
4180 escaping cases — comma and embedded quote), 3 new `OrderServiceTest`
cases (start-after-end throws, no-mode-param uses the mode-agnostic query
and resolves the signal reference, an explicit mode uses the mode-filtered
query), and 3 new `OrderQueryControllerTest` cases (success with the
attachment header and CSV body, mode passed through, start-after-end →
400) — 15 new tests, 315 backend tests total, `./mvnw verify` green.

Verified live via the `run` skill against the real running stack (Docker
Oracle XE + real public Binance API, no mocking; logged in through E1-F3-S2's
session-cookie auth) — stale `java`/`node` processes from an earlier
session were again holding 8080/5173 (this file's own recurring gotcha),
killed and both restarted clean before trusting `/health`. Exercised the
endpoint via `curl` against real Oracle first: start-after-end correctly
400'd `INVALID_REQUEST`; a wide empty-range export correctly returned 200
with just the header row (`Content-Length: 343`, matching a bare header);
a `mode=PAPER` export returned the identical empty result, confirming the
mode-filtered query path runs without error. Then clicked through the real
UI: the "Order history" section now shows the new Start date/End date/
"Export to CSV" row; clicking Export with both dates empty rendered "Choose
both a start and end date." with no network call; setting start after end
and clicking Export rendered "Start date must not be after end date.",
also with no network call; a valid wide range (2020-01-01 to 2026-07-29)
fired exactly one `GET /api/orders/export?...` (confirmed via
`read_network_requests`, status 200) and produced a real file download —
confirmed on disk at `~/Downloads/trade-history-2020-01-01-to-2026-07-29.csv`,
343 bytes, correct filename parsed from `Content-Disposition`, header row
matching the API response exactly with real CRLF line endings (verified via
`cat -A`). A populated CSV row with a real signal-snapshot reference (matched
rule, call, hold-term) was **not** observed live in this session — same
structural gap as every E4-F2/F3/E5-F2/F3 story before it, since no real
order has ever been created on this dev machine (no real
`BINANCE_TRADING_API_KEY`/`BINANCE_TRADING_API_SECRET` exist here); flagged,
not silently assumed, and covered instead by `OrderCsvExporterTest`'s
populated-snapshot case above. Browser console showed no errors throughout.

This closes out F5.3 (order status & history) and E5 (Auto-Trade Execution)
in full. E5-F4-S1 (notifications) is next — the one story flagged in
`docs/agile-plan.md` as softly depending on the E3-F3-S1 watchlist stretch
feature, which is already done.

## E5-F4-S1 — notifications

E5-F4-S1 (notifications) is done, closing out F5.4 and E5 (Auto-Trade
Execution) in full. A `Plan`-agent design gate resolved both open questions
before implementation. **Delivery channel**: in-app only, not email — no
email/SMTP infrastructure exists anywhere in this codebase, and standing one
up would need real provider credentials this dev machine doesn't have (the
same gap already flagged for `ALPACA_TRADING_API_KEY`/`BINANCE_TRADING_API_KEY`
throughout E4). Confirmed directly with the user before implementation, per
this epic's established "confirm the channel/credential-source decision"
precedent (E4-F2-S1, E4-F3-S1); email is a flagged, explicitly-deferred
fast-follow, not built here. **Watchlist signal-change detection**: the
first background/scheduled job in this codebase — every prior story
deliberately avoided one (E5-F3-S1's own design gate rejected background
order-status polling in favor of a manual refresh button) because nothing
needed to run without a user request until now. A new `notification`
package: `WatchlistSignalPoller` (`@Scheduled(fixedDelayString =
"${notification.watchlist-poll.fixed-delay-ms}")`, `fixedDelay` not
`fixedRate` so a slow cycle never overlaps the next one) polls every
watchlisted ticker sequentially with a 750ms inter-ticker pause — this is a
single-user tool with an expected-small watchlist, so no adaptive
rate-limit framework was needed. "Changed" means the `SignalCall`
(BUY/SELL/HOLD) itself transitions, not a rule-detail change while the call
stays the same. The "previous known call" baseline reuses the existing
append-only `signal_calls` audit table (`SignalCallEntryRepository`'s new
`findTopByTickerOrderByCreatedAtDescIdDesc`) rather than a new "last known
state" table — restart-safe for free, since it's a real persisted row. A
ticker's first-ever poll establishes the baseline silently (no notification)
to avoid a notification burst the moment a ticker is first watchlisted or
right after an app restart. Every per-ticker failure (market closed,
insufficient history, rate-limited, etc. — every exception in this codebase
is an unchecked `RuntimeException`) is caught, logged, and skipped so one
ticker's failure never aborts the batch — a stock ticker polled outside
market hours will routinely throw on every off-hours cycle, expected, not an
error. `WatchlistSignalPoller` is gated by `@ConditionalOnProperty(name =
"notification.watchlist-poll.enabled", matchIfMissing = true)`, forced
`false` in `backend/src/test/resources/application.properties` — this
codebase's repeated "no live network calls in CI" discipline, since an
`@SpringBootTest` would otherwise trigger a real Alpaca/Binance call via
this bean. New `common.SchedulingConfig` (`@EnableScheduling`) is the app's
first scheduling config, alongside the existing `common.ClockConfig`.

New `notification.Notification` (JPA entity, append-only, same audit-log
pattern as `IndicatorSnapshot`/`SignalCallEntry` — `readAt` is the one
intentionally-mutable field, for unread/read UI state) is associated with
*either* an `Order` (an order-outcome notification) *or* a `SignalCallEntry`
(a watchlist signal-change notification), never both — DB-enforced via
`ck_notifications_association` in `V9__add_notifications.sql`, the same
defense-in-depth CHECK-constraint style as `V1`'s stock/leverage check and
`V5`'s hold-term all-or-nothing check. `NotificationType` mirrors every
status `OrderService.applyOutcome` can persist (`ORDER_FILLED`,
`ORDER_PARTIALLY_FILLED`, `ORDER_REJECTED`, `ORDER_FAILED`,
`ORDER_CANCELLED`, `ORDER_PARTIALLY_PROTECTED`, `ORDER_SUBMISSION_UNKNOWN`)
plus `SIGNAL_CHANGED` — not just the AC's literally-named "fills or is
rejected," since `PARTIALLY_PROTECTED`/`SUBMISSION_UNKNOWN` are exactly the
states E4/E5's own design gates flagged as needing manual attention.
`NotificationService.recordOrderOutcome`/`recordSignalChange` both swallow
and log their own failures — a notification-recording bug must never fail
an order response or abort the poller's batch. The order-outcome hook is a
single choke point: `OrderService.applyOutcome` now captures the order's
`previousStatus` before overwriting it, and only calls
`notificationService.recordOrderOutcome` on a genuine transition — this is
what stops a manual `refreshOrder` re-poll of an already-terminal order
(e.g. re-fetching an already-`FILLED` order) from re-notifying every time.
New `GET /api/notifications`/`GET /api/notifications/unread-count`/
`POST /api/notifications/{id}/read`/`POST /api/notifications/read-all`
(`NotificationController`) — marking read is idempotent-by-convention (a
no-op on an unknown/already-read id, matching `WatchlistService.remove`'s
DELETE-style precedent), so no dedicated not-found error code was needed;
a new `InvalidNotificationRequestException`/`NotificationExceptionHandler`
pair handles only the `limit`-bounds case, mirroring every other domain's
own small per-domain exception-handler precedent.

Frontend: new `frontend/src/notification/` package (`api.ts`,
`NotificationPanel.tsx` — a list with an unread-count badge, a manual
"Refresh" button, and per-item/mark-all "Mark read" actions; **no
`setInterval`/automatic polling**, same "manual refresh only" bias as
`OrderHistory`'s E5-F3-S1 precedent, since the watchlist-signal half is
produced by a backend scheduled job on its own interval, not by any
frontend action). Rendered at the top of `DashboardPage`, above the
watchlist. New `.notification-panel__*` CSS rules follow the existing
`light-dark()` pattern.

A real bug was found and fixed during live verification (not caught by any
unit test, since Mockito-based tests use plain POJOs with no real Hibernate
proxies, and the one `@SpringBootTest` watchlist test runs its whole method
inside one transaction so a lazy load never crosses a transaction boundary
the way it does in production): `WatchlistSignalPoller` iterating
`WatchlistEntry.getTicker()` after `WatchlistService.list()`'s
`@Transactional(readOnly = true)` method had already returned threw
`org.hibernate.LazyInitializationException: could not initialize proxy - no
session`, since `WatchlistEntry.ticker` is a lazy `@ManyToOne` and the
Hibernate session was already closed by the time the poller (running with
no open transaction, by design — it must not hold a DB connection across its
market-data/signal HTTP calls, the same discipline `OrderService` already
follows around its broker calls) touched it. Fixed with a new real-join
query, `WatchlistEntryRepository.findAllWatchlistedTickersOrderByCreatedAtDesc`
(`select w.ticker from WatchlistEntry w order by w.createdAt desc`), which
returns fully-loaded `Ticker` entities rather than the lazy association, and
a new `WatchlistService.listTickers()` wrapping it; `WatchlistSignalPoller`
now calls that instead of `list()`. A new `WatchlistServiceTest` case
(`listTickers_ordersByMostRecentlyAddedFirst`) covers the query's ordering,
though — as this bug's own root cause implies — no test in this codebase's
current H2/`@Transactional`-test-method style can actually reproduce the
lazy-loading failure itself; the live `run`-skill verification is what
caught it.

Tested via `NotificationServiceTest` (12 tests: every `OrderStatus`→
`NotificationType` mapping, rejection-reason message formatting, the
repository-throws-swallows-exception case, signal-change message
formatting, list/count/mark-read/mark-all-read), `WatchlistSignalPollerTest`
(4 tests: first-ever-poll-establishes-baseline-silently, call-unchanged-
doesn't-notify, call-changed-notifies-with-both-calls, and one-ticker-
failing-doesn't-block-the-others — this last one also caught a real test
bug of its own: giving two distinct `Ticker` fixtures the same hardcoded id
made them `equals()`-collide under Mockito's argument matching, since
`Ticker.equals` is id-based; fixed with a per-call incrementing id),
`NotificationControllerTest` (6 tests: status codes and error bodies), and
3 new `OrderServiceTest` cases (a filled order notifies with the correct
previous status, a `SUBMISSION_UNKNOWN`→`FILLED` refresh notifies, and a
same-status refresh does *not* re-notify) — 27 new tests, 334 backend tests
total, `./mvnw verify` green. No new frontend Vitest cases — same gap
already accepted for `Watchlist.tsx`/`OrderHistory.tsx` (view-only DOM
components with no pure logic to unit test); `npm run build`/`lint`/`test`
all pass clean (13 tests, unchanged, same pre-existing unrelated
`AuthContext.tsx` lint warning as every prior story). `simplify` skill run
clean: the `OrderStatus`→`NotificationType` map stayed a plain documented
mapping (no generic event-dispatch framework), the poller stayed scoped to
exactly the polling/comparison logic it needs (no reusable "scheduled job"
abstraction, since there's only one job), and the double
`findTopByTickerOrderByCreatedAtDescIdDesc` query per ticker per poll cycle
(once for the "previous" baseline, once to re-fetch the "current" entry
`SignalService.computeSignalWithProvenance` doesn't return directly) was
deliberately left as-is rather than widening `SignalComputation`'s return
shape — that record is already constructed directly across a dozen-plus
`OrderServiceTest`/`TickerSignalOrderE2ETest` call sites, so a shape change
for a single low-frequency caller's minor query-count savings would be a
disproportionate blast radius.

Verified live via the `run` skill against the real running stack — Docker
Desktop wasn't running at the start of this session and had to be launched
first, then Oracle XE brought up fresh and polled healthy before the
backend would connect. `V9` applied cleanly against real Oracle (schema
version 8 → 9). After hitting and fixing the `LazyInitializationException`
above and restarting the backend clean, exercised the API via `curl` first:
watchlisted `SOLUSDT` (a live `SELL`) and `BTCUSDT`; `GET /api/notifications`
and `GET /api/notifications/unread-count` both correctly returned empty/zero
on a fresh container; `limit=0` correctly 400'd `INVALID_REQUEST`; a full
`POST /api/tickers/SOLUSDT/orders` attempt correctly 503'd
`BROKER_CREDENTIAL_NOT_CONFIGURED` — the same reachable terminus every prior
E4/E5 story has hit on this dev machine (no real
`BINANCE_TRADING_API_KEY`/`BINANCE_TRADING_API_SECRET` exist here) — and a
direct `sqlplus` query confirmed **zero** rows were ever written to either
`orders` or `notifications`, proving the pre-flight failure correctly never
reaches the notification hook. A direct `sqlplus` query against
`signal_calls`, cross-referenced with wall-clock timestamps, confirmed
`WatchlistSignalPoller` fired automatically on its own ~2-minute `local`-
profile schedule (not from any manual request) across multiple cycles,
correctly polling both watchlisted tickers each time with zero errors on
the scheduling thread. Then clicked through the identical flow in a real
browser: the new "Notifications" section rendered "No notifications yet."
with working Refresh/disabled-at-zero "Mark all read" controls; looking up
`SOLUSDT` rendered its live `SELL` badge, stat tiles, and chart; filling the
trade form and confirming through the E5-F2-S2 dialog correctly rendered
"No broker credentials are configured for this asset type yet — the order
was not submitted." with the Notifications section still correctly reading
"No notifications yet." afterward. Browser console showed no errors at any
point.

A real live-observed order-fill/reject notification and a real
live-observed watchlist signal-change notification were **not** obtained in
this session — the former needs real broker credentials this dev machine
doesn't have (same gap as every E4-F2/F3/E5-F2/F3 story before it); the
latter would need an actual market call to flip during the session, not
practically reproducible on demand. An attempt to seed a realistic "previous
call" row directly via SQL (to let a real scheduled poll cycle detect a
transition and prove the path end-to-end) was correctly blocked by the
auto-mode permission classifier as a direct, invasive database mutation
outside the app's own write paths — not worked around. Both paths are
covered instead by `NotificationServiceTest`/`WatchlistSignalPollerTest`'s
mocked-adapter cases above; flagged, not silently assumed, same disclosure
style as this file's other structural-rather-than-live-observed gaps (e.g.
E3-F1-S2's un-observed BUY badge, E3-F2-S1's un-observed `autoSize` resize).

This closes out F5.4 (notifications) and E5 (Auto-Trade Execution) in full.

## E6-F1-S1 — global paper/live mode switch

E6-F1-S1 (global paper/live mode switch) is done, starting E6 (Risk & Safety
Controls). A `Plan`-agent design gate resolved every open question before
implementation, since this is the first story to make `TradingMode.LIVE`
reachable anywhere in the app. **Where the mode lives**: a new append-only
`trading_mode_events` table (`V10__add_trading_mode_events.sql`) — "current"
mode is always the latest row by id, the same "latest row = current state"
pattern `signal_calls` already established, rather than an in-memory
singleton (this codebase's own bias: every meaningful piece of state is a DB
row) or a Spring profile (fixed at JVM startup, can't be user-toggled at
runtime). **The safety question this story couldn't dodge just because
E6-F1-S2/S3 aren't built yet**: relying on "no `LIVE` broker credentials are
seeded anywhere yet" as the only backstop against a premature live switch
would be safety-by-accident, not a designed control — confirmed directly with
the user, `TradingModeService.switchTo` now unconditionally throws a new
`LiveModeNotYetAvailableException` (403 `LIVE_MODE_NOT_YET_AVAILABLE`) for
any attempt to switch to `LIVE`, deliberate temporary scaffolding for
E6-F1-S2 (paper-trade threshold)/E6-F1-S3 (risk consent) to *replace*, not
just delete. Switching to `PAPER` always succeeds; switching to the mode
already active is an idempotent no-op (no new history row). Reads
(`TradingModeService.current()`) are never cached — a fresh DB read every
call, correctness over performance for money-moving code.

New `com.autotrade.dashboard.tradingmode` package: `TradingModeEvent`
(entity)/`TradingModeEventRepository`/`TradingModeService`/
`TradingModeResponse`/`TradingModeChangeRequest`/
`LiveModeNotYetAvailableException`/`TradingModeController`
(`GET`/`POST /api/trading-mode`)/`TradingModeExceptionHandler` — a standalone
top-level resource mirroring `/api/watchlist`/`/api/orders`/
`/api/notifications`'s precedent, normal session auth only (no re-auth/
confirmation step — that's E6-F1-S3's job, not this one's).
`OrderService.submitOrder` now reads `TradingMode` from
`TradingModeService.current()` instead of a hardcoded `TradingMode.PAPER`
literal; `refreshOrder` deliberately still uses the order's own persisted
`orderMode`, never the current global switch — an order placed under `PAPER`
must always be re-polled against `PAPER` even if the switch has since moved
on, or a refresh would poll the wrong broker environment for an existing
order. In practice, behavior is unchanged from before this story: the switch
can only ever be `PAPER` today, since `switchTo(LIVE)` always throws and
neither credential bootstrap (E4-F2-S1/E4-F3-S1) seeds a `LIVE` credential
yet either.

Frontend: new `frontend/src/tradingmode/` package (`api.ts`,
`TradingModeBanner.tsx`) — a persistent banner mounted at the very top of
`DashboardPage`, above even the page title, showing the current mode, when
it last changed (or "Default — never changed"), and a single toggle button
labeled with the *other* mode. A 403 from attempting `LIVE` renders the
backend's own message inline via the existing generic
`MarketDataError`-based error handling — no client-side special-casing of
"LIVE is blocked," so this component needs no changes once E6-F1-S2/S3
replace the guard with a real threshold/consent check. New
`.trading-mode-banner` CSS rules: PAPER uses a calm blue, LIVE a strong red —
a deliberate, singular exception to this app's avoid-raw-red convention,
since LIVE isn't part of a red/green comparison pair the way BUY/SELL is, and
the word "LIVE" is always shown alongside the color.

A real-Oracle-only bug surfaced during live verification, not caught by
`./mvnw verify` (H2's Oracle-compatibility mode doesn't reserve this word):
`MODE` is an Oracle SQL reserved keyword (used in `LOCK TABLE ... MODE`/
`ALTER SESSION ... MODE` syntax) — the migration's original `mode
VARCHAR2(10)` column failed real Oracle with `ORA-00904` ("invalid
identifier") the moment `CREATE TABLE` ran. Fixed by renaming the column to
`trading_mode` in both the migration and `TradingModeEvent`'s `@Column`
mapping (the Java field itself stays named `mode`). Recovering the local Oracle container from the failed migration attempt
needed the same `flyway_schema_history` cleanup this file's E1-F3-S1 entry
already flagged for a failed migration, plus one further step: deleting the
`success=0` row for version 10 from `flyway_schema_history` (Oracle persists
this across restarts, unlike H2), and dropping the `trading_mode_events_seq`
sequence a failed `CREATE TABLE` had already left behind — Oracle DDL
auto-commits per statement regardless of the surrounding transaction, so the
sequence survived even though the table creation that came after it rolled
back.

`simplify`, `security-review`, and `guardrail-check` skills all run clean:
`TradingModeService.current()`/`currentState()` collapsed to avoid two
separate query implementations of the same "latest row, default PAPER"
logic; the `LiveModeNotYetAvailableException` guard is exactly the kind of
check that looks removable but is a deliberate, documented E6 safety net
(per `simplify.md`'s own guidance) and was kept; the guard is enforced
server-side in `TradingModeService` itself, not just hidden by the
frontend — proven via `TradingModeControllerTest`'s direct `POST` against
the raw endpoint, bypassing any UI.

Tested via `TradingModeServiceTest` (7 tests, H2/real repository: default
`PAPER` on empty history, `switchTo(LIVE)` always throws with no row
persisted from both a default and an explicitly-seeded-`PAPER` starting
state, `switchTo(PAPER)` no-op when already `PAPER`, `switchTo(PAPER)`
succeeding from a directly-seeded `LIVE` row — the one path `switchTo`
itself can't produce today, seeded via the repository directly to exercise
it ahead of E6-F1-S2/S3 making it reachable for real — and reading the
latest of several seeded rows), `TradingModeControllerTest` (4 tests: status
codes and error bodies), and two new `OrderServiceTest` cases (`submitOrder`
threads a non-`PAPER` `TradingModeService.current()` value through to the
credential lookup/adapter call/persisted `orderMode`, proving no hardcoding
remains; `refreshOrder` ignores `TradingModeService.current()` entirely and
uses the order's own persisted mode) — 12 new tests, 346 backend tests
total, `./mvnw verify` green. No new frontend Vitest cases — same gap already
accepted for `Watchlist.tsx`/`OrderHistory.tsx`/`NotificationPanel.tsx`
(view-only DOM components with no pure logic to unit test); `npm run
build`/`lint`/`test` all pass clean (13 tests, unchanged, same pre-existing
unrelated `AuthContext.tsx` lint warning as every prior story).

Verified live via the `run` skill against the real running stack (Docker
Oracle XE + real public Binance API, no mocking) — stale `java`/`node`
processes from an earlier session were again holding 8080/5173 (this file's
own recurring gotcha), killed and both restarted clean; the Oracle-reserved-
word bug above was caught and fixed during this restart, then the container
cleanup steps applied before a second restart succeeded, `V10` applying
cleanly (schema version 9 → 10). Logged in through E1-F3-S2's session-cookie
auth (a repeat of the CSRF-cookie-rotates-after-login behavior E1-F3-S2's
own `SpaCsrfTokenRequestHandler` already documents — a request immediately
after login needs `GET /api/auth/me` first to re-prime the cookie, same as
the frontend's own login flow already does) and exercised the API via `curl`
first: `GET /api/trading-mode` returned `{mode: "PAPER", changedAt: null}` on
a fresh container; `POST {mode: "LIVE"}` correctly 403'd
`LIVE_MODE_NOT_YET_AVAILABLE`; `POST {mode: "PAPER"}` correctly 200'd as a
no-op with `changedAt` still `null`. A full `SOLUSDT` SELL trade-form
submission still correctly 503'd `BROKER_CREDENTIAL_NOT_CONFIGURED` — the
same reachable terminus every prior E4/E5 story has hit on this dev machine
(no real `BINANCE_TRADING_API_KEY`/`BINANCE_TRADING_API_SECRET` exist here) —
confirming the `OrderService` refactor from hardcoded `PAPER` to
`TradingModeService.current()` didn't change observable behavior; a direct
`sqlplus` query confirmed zero `orders` rows and zero `trading_mode_events`
rows after all of the above, exactly as designed. Then clicked through the
identical flow in a real browser: the banner rendered `PAPER mode · Default
— never changed` at the very top of the dashboard; clicking "Switch to LIVE"
rendered the backend's own message inline
("LIVE mode isn't available yet — it unlocks once the paper-trade threshold
(E6-F1-S2) and risk-consent step (E6-F1-S3) are in place.") with the banner
staying in `PAPER` state throughout. Browser console showed no errors.

E6-F1-S2 (paper-trade threshold before live unlocks) is next — it can
replace `LiveModeNotYetAvailableException`'s unconditional throw with a real
count check, since `TradingModeService.switchTo` is now the single place
that decision needs to change.

The original agile delivery plan for the project (drafted before any of E1-E3 above
was implemented) lives at `docs/agile-plan.md` — an auto-trade signal dashboard
(React frontend, Java/Spring Boot backend, Oracle Database via local Oracle XE,
broker adapters starting with Alpaca for stocks and Binance for crypto). It covers
epics/features/user stories (INVEST format) and the recommended subagent/skill usage
for solo-driven implementation. The plan was expanded with stories closing gaps found
in a review: CI pipeline, DB migrations, app auth, a testing strategy, market-hours
handling, backtesting, adapter rate-limit/retry/outage handling, trade export,
notifications, a live-mode consent step, a portfolio-level exposure cap,
rule-versioned audit entries, and DB backup/restore. A consistency pass then fixed a
stale story count, extended the Plan-agent/`code-review` gates to cover E6's
guardrail logic (not just E4/E5), and flagged that E5's notification story softly
depends on the stretch watchlist feature. See the per-story entries above for actual
build/lint/test commands and architecture — this paragraph describes the plan
document only, not current repo state.

Project-specific subagent definitions now live in `.claude/agents/` — `Plan.md`,
`Explore.md`, and `general-purpose.md` — customizing the three subagent roles the
agile plan calls for (design gate, research, background implementation) with this
project's domain specifics (Spring Boot/JPA/Oracle conventions, Alpaca/Binance order
semantics, the E2 rule-engine and E6 guardrail requirements). These override the
generic built-in agents of the same name for work done in this repo.

Project-specific skill definitions now live in `.claude/skills/`. Four amend
generic skills with this project's specifics — `run.md` (this stack's launch
sequence: Oracle XE via Docker Compose, Spring Boot backend, React frontend),
`dataviz.md` (Buy/Sell/Hold badge palette, stat tiles, candlestick+indicator
chart for E3), `simplify.md` (where premature abstraction is a real risk here —
adapters, rule engine — versus where a check that looks redundant is a
deliberate E6 safety net), and `security-review.md` (a checklist covering
broker-credential handling, the live-mode gate, and guardrail enforcement).
Three are new, non-generic project skills with no generic equivalent —
`signal-rule-review.md` (E2.3/E2.4 rule-table + backtest checklist),
`adapter-contract-check.md` (E4.1 BrokerAdapter conformance, retry/backoff,
idempotency), and `guardrail-check.md` (E6 risk-control verification before
the live-mode gate closes).

A repo-readiness audit (full directory inventory, cross-checked against
`docs/agile-plan.md`) confirmed the "no source code yet" status above is
accurate and found no gaps owned by an existing story except a few config-
hygiene items, now closed: a `.gitignore` was added (covering Java/Maven/
Gradle, Node, `.env*`, IDE files, and local Oracle data volumes, since E1's
skeleton stories are about to generate exactly what it excludes); the
backend-framework assumption in `docs/agile-plan.md` was confirmed as
**Spring Boot** (moved out of "assumptions" into a new "Confirmed decisions"
section, no objection raised); and F1.3-S1's acceptance criteria now
require a checked-in `.env.example` documenting every required config key
(broker keys, Oracle connection string) with no real values. The indicator-
library choice (`ta4j` vs. hand-rolled) and the ticker asset-type detection
rule remain intentionally open in `docs/agile-plan.md` — both are deferred
by design to when E2-F2 is actually picked up, not pre-build blockers. The
`fewer-permission-prompts` skill is worth running once E1 scaffolding
exists (not before — there's nothing to allowlist yet).

## E6-F1-S2 — paper-trade threshold before live mode unlocks

E6-F1-S2 (paper-trade threshold) is done. A `Plan`-agent design gate (this
story's own precedent from E6-F1-S1) resolved the open questions before
implementation. **What counts as a "successful paper trade"**: an `Order`
row with `orderMode=PAPER` and `status=FILLED`, counted per-order with no
distinct-ticker/distinct-day dedup — this is a solo-user app, not an
adversarial multi-tenant system, and E5-F2-S2's explicit per-order
confirmation step already adds real friction against "spamming" the
threshold. `PARTIALLY_PROTECTED` (entry filled, TP/SL leg failed) does
**not** count, confirmed with the user directly — the gate exists to prove
the user has been through the complete, safely-protected bracket-order flow,
not a degraded one. **Where the threshold lives**: a plain
`@Value`-injected config property (`trading-mode.paper-trade-threshold`,
default 10, overridable via `TRADING_MODE_PAPER_TRADE_THRESHOLD`), not a
DB-editable value — matches this codebase's existing config-property
precedent (`notification.watchlist-poll.*`) and this app's single-operator,
ops-controlled-via-redeploy posture; a DB-backed admin-editable threshold
would be a different, heavier feature this story doesn't need.

`TradingModeService.switchTo` now checks idempotency (`current() ==
requested`) *before* the threshold gate — a no-op `LIVE -> LIVE` shouldn't
re-validate a threshold that's irrelevant to a state that's already true.
The old unconditional-throw scaffolding (`LiveModeNotYetAvailableException`,
per its own javadoc's explicit invitation to be *replaced*, not just
deleted) is gone; a switch to `LIVE` now actually succeeds once
`OrderRepository.countByOrderModeAndStatus(PAPER, FILLED)` meets the
threshold — this is a deliberate incremental-delivery state: `LIVE` is
genuinely reachable with no consent step until E6-F1-S3 lands a second,
independent gate alongside this one (not a replacement of it). New
`PaperTradeThresholdNotMetException` (403 `PAPER_TRADE_THRESHOLD_NOT_MET`,
replacing `LiveModeNotYetAvailableException`'s handler in
`TradingModeExceptionHandler`) carries `completed`/`required` counts.
`TradingModeResponse` (shared by `GET`/`POST /api/trading-mode`) gained
`successfulPaperTrades`/`paperTradeThreshold`/`liveModeUnlocked` fields,
computed fresh on every read — same "correctness over performance for
money-moving code" philosophy as the rest of this service. No Flyway
migration needed: `orders.order_mode`/`orders.status` already exist and were
already queried by mode.

Frontend: `TradingModeState` mirrors the three new response fields.
`TradingModeBanner`'s LIVE toggle is now proactively `disabled` (not just
reactively failing on click) whenever `!liveModeUnlocked`, with inline
explanatory text ("Live mode unlocks after 10 successful paper trades (N
completed).") — the S1 banner's original doc comment claiming "this
component needs no changes" was wrong for this story and has been corrected.
The reactive 403-message path stays as a defense-in-depth fallback for stale
client state, but is no longer the primary UX.

No new automated frontend component test was added for the banner —
`frontend/vitest.config.ts` is deliberately scoped to pure `.ts` logic
(`environment: 'node'`, `include: ['src/**/*.test.ts']`, no jsdom/
testing-library installed anywhere in the repo yet); adding a whole
component-testing stack for one banner is a bigger infrastructure call than
this story warrants. Verified live instead, per this repo's UI Definition of
Done: brought up Oracle XE + backend + frontend, logged into the dashboard,
and confirmed the "Switch to LIVE" button rendered visibly disabled with the
"0 completed" progress text before any paper trades exist.

A real bug surfaced only by running the full `./mvnw verify` suite (not
caught by the two new/edited `tradingmode` test classes in isolation):
`src/test/resources/application.properties` **replaces** the main
`application.properties` for the test classpath rather than merging with
it (Spring Boot loads whichever `application.properties` is highest-priority
on the classpath, not both), so `TradingModeService`'s newly-mandatory
`@Value("${trading-mode.paper-trade-threshold}")` failed to resolve in every
`@SpringBootTest` context that didn't explicitly override it — a
`PlaceholderResolutionException` at bean-creation time, cascading into 33
unrelated test failures/errors across the suite (`ApplicationContext
failure threshold exceeded`) since many contexts share the same
`jdbc:h2:mem:testdb` in-memory instance. Fixed by adding the property to the
test resource file too (default 10, same as prod), leaving
`TradingModeServiceTest`'s own `@TestPropertySource(properties =
"trading-mode.paper-trade-threshold=3")` to override it locally for fast,
deterministic threshold tests. Full suite (`./mvnw verify`) passes: 350
tests, 0 failures, 0 errors. Frontend `npm run build`/`lint`/`test` all
clean.

## E6-F1-S3 — one-time risk-consent acknowledgment before LIVE unlocks

Design gate (`Plan` agent) flagged one real correctness issue before any code was
written: bolting `riskConsentGiven` onto `TradingModeResponse` without touching the
existing `liveModeUnlocked` field would have broken S2's whole point — the LIVE
toggle proactively re-enabling itself once the threshold passed, then reactively
403-ing on the *new* consent check, exactly the UX S2 moved away from. Fix: split
`liveModeUnlocked` into two fields — `paperTradeThresholdMet` (the old
threshold-only computation) and `riskConsentGiven`/`riskConsentGivenAt`, with
`liveModeUnlocked` redefined as `paperTradeThresholdMet && riskConsentGiven` — "would
`switchTo(LIVE)` actually succeed right now." This forced every
`new TradingModeResponse(...)` call site to be reconsidered at compile time (4 sites,
all in `tradingmode/`), which is the point, not incidental churn.

Backend: new append-only `risk_consents` table (migration `V11`, same "latest/only
row = current state" audit pattern as `trading_mode_events`; `DEFAULT ... NOT NULL`
column ordering per this repo's Oracle gotcha) plus `RiskConsentEvent` entity/
repository, mirroring `TradingModeEvent` minus the `mode` column. `TradingModeService`
gained `giveRiskConsent()` — idempotent, a repeat call doesn't insert a second row, so
the audit table reflects one real consent event rather than one row per dialog
confirm — and `switchTo(LIVE)` now checks the consent gate *after* the existing
threshold gate (threshold error takes precedence, matching the AC's own framing "on
top of the paper-trade threshold"). New `RiskConsentNotGivenException` (403
`RISK_CONSENT_REQUIRED`, handled alongside `PaperTradeThresholdNotMetException` in the
existing `TradingModeExceptionHandler`) carries no fields — unlike the threshold
exception there's no progress metric to report, just a boolean. New endpoint
`POST /api/trading-mode/risk-consent` records consent as its own explicit action,
decoupled from `TradingModeChangeRequest` — consent stays an independently auditable
event with its own timestamp regardless of *how* or *when* someone later tries to
switch, and avoids polluting the switch-request DTO with a field that's meaningless
for `PAPER`.

Frontend: `TradingModeBanner`'s toggle now branches three ways instead of two —
blocked by threshold (unchanged: disabled button + progress text), threshold met but
no consent yet (new: clicking "Switch to LIVE" opens a `<dialog>` with the risk
disclaimer instead of switching immediately, mirroring `TradeForm`'s confirm-dialog
idiom — ref + `showModal()`/`close()`, `onCancel` for Esc), and fully unlocked
(unchanged: direct switch). Confirming the dialog calls `giveRiskConsent()` then
`switchTradingMode('LIVE')` as two sequential calls rather than one combined
request — if the switch call fails after consent succeeds (e.g. a stale threshold
read), the user doesn't have to re-consent on retry, only the switch itself needs
retrying. Reused the existing `.trade-confirm-dialog` CSS classes as-is (generic
dialog chrome despite the name) rather than duplicating near-identical rules. No new
component test — same reasoning as S2 (`vitest.config.ts` is deliberately scoped to
pure `.ts` logic, no jsdom/testing-library in the repo).

Verified live end-to-end: brought up Oracle XE + backend + frontend, logged in,
temporarily overrode `TRADING_MODE_PAPER_TRADE_THRESHOLD=0` for this session only (to
exercise the consent gate in isolation rather than seeding ten fake filled orders) and
confirmed all four paths — clicking "Switch to LIVE" opened the disclaimer dialog;
Cancel closed it with no API call and mode stayed PAPER; confirming recorded consent
and switched to LIVE mode (banner flipped red, timestamp shown); switching back to
PAPER and then to LIVE again went straight through with no dialog, confirming consent
persistence is honored on repeat switches. No console errors. Backend and Docker
processes stopped afterward. Full `./mvnw verify` (20 new/updated tests in
`tradingmode`, full suite green) and frontend `npm run build`/`lint`/`test` all clean.

## E6-F2-S1 — hard server-side cap on leverage and position size

Design gate (`Plan` agent) settled the shape before any code: a new `risk` package
rather than folding into `OrderService.validate()`'s existing shape/bounds checks —
this is a separately-configured *risk policy*, not adapter-technical validation, and
gives E6-F2-S2 (kill switch) and E6-F2-S3 (aggregate exposure cap) an obvious shared
home instead of `OrderService` accreting ad hoc private checks. Two product decisions
the plan flagged rather than guessed, both confirmed with the user before writing
code: "position size" means notional exposure (`amountUsd * leverage`, the actual
market exposure taken for a leveraged instrument — not the raw dollar amount typed),
and the starter cap defaults are conservative (stock $5,000, crypto 5x/$2,000),
matching a small paper/testnet-scale account, env-overridable to raise later.

Backend: new `risk` package — `RiskLimitsProperties` (`@ConfigurationProperties`
record, `risk-limits.*` keys, mirroring `BinanceFuturesTradingProperties`'s pattern),
`RiskLimitService.enforcePerOrderCaps(assetType, amountUsd, leverage)` (throws
`RiskLimitExceededException` on breach), `RiskExceptionHandler` (403
`RISK_LIMIT_EXCEEDED`, same per-domain-advice-class convention as
`TradingModeExceptionHandler`), `RiskLimitConfig` (`@EnableConfigurationProperties`).
`RiskLimitService`'s constructor fails fast (`IllegalStateException`) if
`crypto-max-leverage` is ever configured above `OrderService.MAX_CRYPTO_LEVERAGE`
(20x) — a cap set above the adapter's own technical ceiling would silently never
bind, exactly the kind of bug this story exists to prevent. Wired into
`OrderService.submitOrder()` immediately after the existing `validate(...)` call,
before quantity computation, `Order` persistence, or any broker call — a breach gets
the same pre-flight, no-row treatment as `InvalidTradeRequestException`, not the
broker-rejection path (no reconciliation/idempotency concern, since nothing was ever
submitted).

Bug found running the full suite: `backend/src/test/resources/application.properties`
completely shadows (not merges with) main's `application.properties` for every
`@SpringBootTest` — same reason `trading-mode.paper-trade-threshold` is duplicated
there as a literal `10` instead of the main file's placeholder syntax. Missed this
initially, so `RiskLimitsProperties` bound with all-null fields in any full-context
test, and `RiskLimitService`'s fail-fast constructor check threw a `NullPointerException`
on `cryptoMaxLeverage().compareTo(...)` — a confusing failure mode (deep inside bean
instantiation, `UnsatisfiedDependencyException` → `BeanCreationException` →
`BeanInstantiationException` → NPE) for what was really just a missing test property.
Fixed by adding the same three `risk-limits.*` keys (literal values, no placeholder)
to the test-resources file. Documented as a new recurring gotcha in CLAUDE.md since
it'll bite the next new config key too.

Tests: `RiskLimitServiceTest` (7 cases — exact-boundary-inclusive caps, leverage
breach, position-size breach independent of leverage, within-caps allowed, and the
fail-fast startup check) plus three `OrderServiceTest` additions that construct a
*real* `RiskLimitService` (not the class's mocked field) so they exercise genuine
end-to-end enforcement, not just that `OrderService` calls its collaborator: a forged
`PlaceOrderRequest` with leverage above the configured cap but within the adapter's
20x technical ceiling, one with position size over cap at low leverage, and one
exactly at the cap boundary — the first two assert `RiskLimitExceededException`, zero
`orderRepository.save()` calls, and zero adapter interactions, the literal proof of
"backend rejects regardless of what the frontend sent" since neither test goes
anywhere near `frontend/src/trade/validation.ts`. Plus one `OrderControllerTest`
addition proving the HTTP-level 403/`RISK_LIMIT_EXCEEDED` mapping. Full `./mvnw verify`
green (39 tests in `OrderServiceTest` alone, no regressions elsewhere).

Not verified live in a running browser: Docker wasn't running in this session, so the
Oracle-backed full stack (backend + frontend) wasn't brought up for a click-through.
Verification here rests on the unit-test suite (real `RiskLimitService` wired into
`OrderService` in the new tests above, not just mocks) rather than an actual HTTP
round-trip through a live server. Worth a follow-up manual check once Docker's
available: type a crypto leverage of, say, 10 into the actual `TradeForm` (frontend
validation currently only bounds leverage to the adapter's 1–20x range, not this
story's stricter 5x default) and confirm the UI surfaces the 403 sensibly rather than
a generic failure.

## E6-F2-S2 — kill switch that cancels all open orders and blocks new submissions

Design gate (`Plan` agent) settled the shape before any code. Key finding that
changed the plan: `BrokerAdapter.cancelOrder(symbol, clientOrderId, mode)` already
exists and is implemented (idempotently — a no-op on an already-terminal order) by
both `AlpacaTradingAdapter` and `BinanceFuturesTradingAdapter`, so "cancel open
orders on both adapters" needed zero changes to the `BrokerAdapter` interface or
either concrete adapter — just iterating this app's own tracked `Order` rows and
calling the existing per-order `cancelOrder` through `BrokerAdapterRouter`, the same
posture `OrderService.refreshOrder`/`listOrders` already take (this app's `Order`
table as the authoritative record of what it submitted, not a live broker query).

Two product decisions the plan flagged, both confirmed with the user before writing
code (mirroring E6-F2-S1's pattern):

1. **Scope of "cancel all open orders"**: this app's own non-terminal `Order` rows
   only, via the existing per-order `cancelOrder` call — explicitly *not* flattening
   already-filled positions or closing resting TP/SL legs. Binance's `cancelOrder`
   javadoc already documents this as deliberate ("cancel the order, not the
   position") from E4-F3-S2's own anti-auto-flatten decision; extending the kill
   switch to flatten positions would have reversed that and required a genuinely new
   adapter capability. Confirmed: stay scoped to tracked orders.
2. **Race tolerance**: best-effort, no locking — an order already past the
   kill-switch pre-flight check when `engage()` commits completes normally and isn't
   swept by that pass; pressing engage again (idempotent) catches it on a second
   pass. This matches `TradingModeService.switchTo`'s existing read-then-write
   pattern with no transaction spanning the check and the write. Confirmed: accepted
   as consistent with the rest of the codebase rather than adding new
   `SELECT...FOR UPDATE`-style locking found nowhere else in this app.

Backend: new sibling classes in the `risk` package (not folded into
`RiskLimitService` — this is stateful/event-sourced, `RiskLimitService` is
stateless/config-only) —
- `KillSwitchState` (`ENGAGED`/`CLEARED`), `KillSwitchEvent` + `KillSwitchEventRepository`
  (append-only, `findTopByOrderByIdDesc()`, identical pattern to `TradingModeEvent`),
  `KillSwitchEngagedException`, `KillSwitchResponse`/`KillSwitchCancelSummary`/
  `EngageKillSwitchResponse` DTOs.
- `KillSwitchService` — `currentState()`/`isEngaged()`/`assertNotEngaged()`/
  `engage(changedBy)`/`clear(changedBy)`, defaults to `CLEARED` on empty history
  (same "no history = safe default" convention as `TradingModeService`), idempotent
  switch (no duplicate row on a same-state call).
- `KillSwitchController` — `GET /api/kill-switch`, `POST /api/kill-switch/engage`
  (flips state to `ENGAGED` via `KillSwitchService` *then* calls
  `OrderService.cancelAllOpenOrders()`, in that order — so "block new submissions"
  never depends on cancellation succeeding), `POST /api/kill-switch/clear`. Reads the
  current username from `SecurityContextHolder.getContext().getAuthentication()`
  directly rather than an `Authentication` method parameter — both are equivalent
  against the real Spring Security filter chain, but only the former is populated by
  `@WithMockUser` in this codebase's established `@WebMvcTest`
  `@AutoConfigureMockMvc(addFilters = false)` slice-test convention (a plain
  `Authentication` parameter resolves via `request.getUserPrincipal()`, which only
  the disabled filter chain populates — found by a failing `KillSwitchControllerTest`
  during this story, not anticipated by the design gate).
- `RiskExceptionHandler` gains a `KillSwitchEngagedException` → 403
  `KILL_SWITCH_ENGAGED` mapping, same advice class as `RiskLimitExceededException`.
- `OrderService`: `killSwitchService.assertNotEngaged()` is the very first line of
  `submitOrder()` — before signal recomputation, cheaper than `RiskLimitService`'s
  placement and avoids wasted work when blocked. New `cancelAllOpenOrders()` queries
  `orderRepository.findByStatusNotIn(FILLED, CANCELLED, REJECTED, FAILED)` —
  deliberately broader than a literal "open" filter, so `PARTIALLY_PROTECTED`/
  `SUBMISSION_UNKNOWN` rows are attempted too, relying on each adapter's own
  idempotent terminal-order no-op rather than re-deriving "cancellable" per status.
  Each order's cancel is caught independently (`BrokerAdapterException`) so one
  broker's outage doesn't stop the sweep for the rest; a failed cancel leaves that
  `Order` row untouched (same "don't overwrite a known status with a failed-read
  guess" rule `refreshOrder` already follows) rather than guessing at its new state.
  Successful cancels reuse the existing private `applyResult`, so notifications fire
  on genuine transitions same as every other order-outcome path.
- `OrderRepository` gains `findByStatusNotIn(Collection<OrderStatus>)`.
- Migration `V12__add_kill_switch_events.sql` — `kill_switch_events` table
  (`kill_switch_state`, `changed_at`, `changed_by`), copying V10's pattern exactly
  including the "don't name it bare `state`, avoid the reserved-word risk given the
  documented `MODE` gotcha" discipline (not asserting `STATE` is reserved, just not
  worth risking it).

Frontend: new `frontend/src/killswitch/` — `api.ts` mirrors `tradingmode/api.ts`'s
shape exactly. `KillSwitchControl.tsx`, mounted in `DashboardPage` above
`TradingModeBanner`. Deliberately *asymmetric* interaction: engaging is one click
with no confirmation dialog (a confirm step would defeat "stop everything
instantly"), while clearing (which re-opens live risk) uses the `<dialog>`
confirm-dialog idiom mirroring `TradeForm`/`TradingModeBanner`'s existing pattern —
this is the opposite of `TradingModeBanner`'s own LIVE-mode consent flow, and is a
deliberate choice for this control, not an oversight. `TradeForm` also fetches kill
switch state independently on mount (same self-contained-fetch pattern
`TradingModeBanner` already uses, not new global-state plumbing — the form remounts
per looked-up ticker anyway, so this naturally reflects state at time of lookup) and
proactively disables its submit button plus shows an inline message when engaged;
the backend's 403 remains the actual enforcement boundary, this is UX only.

Tests: `KillSwitchServiceTest` (8 cases — default-`CLEARED` on empty history,
`assertNotEngaged` throws only when engaged, idempotent engage/clear with no
duplicate row). Found and fixed a stubbing bug while writing these: a redundant
`when(repository.findTopByOrderByIdDesc()).thenReturn(Optional.empty())` placed
*after* a dynamic `thenAnswer` stub permanently overrode it for the rest of the
test (Mockito's last `when()` wins), silently making `currentState()` always see
"no history" even after a save — three tests failed with confusing
assertion/verification mismatches until this was spotted and the redundant lines
removed. `KillSwitchControllerTest` (3 cases, `@WebMvcTest` + `@WithMockUser`) plus
one `OrderControllerTest` addition for the `KILL_SWITCH_ENGAGED` 403 mapping. Two
`OrderServiceTest` additions for `cancelAllOpenOrders` — one proving both brokers get
called (a crypto order routed through the mocked Binance-side adapter, a stock order
through a separate mocked Alpaca-side adapter, both cancelled), one proving per-order
fault isolation (one adapter throws, the other still succeeds, the failed order's
status is left untouched rather than overwritten) — plus one proving
`submitOrder` blocks with zero side effects (`verifyNoInteractions` on
`signalService`/`orderRepository`/`adapter`) when the kill switch is engaged. Full
`./mvnw verify` green (54 test classes, no regressions). Frontend `npm run build`
(typecheck+build), `npm run lint` (pre-existing unrelated warning in
`AuthContext.tsx` only), and `npm test` (13 tests, 2 files) all clean.

Not verified live in a running browser: Docker wasn't running in this session
either, so the Oracle-backed full stack wasn't brought up for a click-through of
the actual `KillSwitchControl` UI or a real HTTP round-trip through
`/api/kill-switch/*`. Verification rests on the unit/contract-test suite above.
Worth a follow-up manual check once Docker's available: engage the kill switch with
at least one open crypto and one open stock order outstanding, confirm both get
cancelled and the summary message renders sensibly, confirm `TradeForm`'s submit
button disables and a fresh order attempt is blocked, then clear it via the
confirm dialog and confirm trading resumes.

## E6-F2-S3 — portfolio-level aggregate exposure cap on top of per-order limits

No separate `Plan` design gate this time — the shape was already settled by E6-F2-S1's
own forward-looking comment ("gives E6-F2-S2/E6-F2-S3 an obvious shared home") and
E6-F2-S1's existing `risk` package/`RiskLimitService`/`RiskLimitExceededException`/
`RiskExceptionHandler` scaffolding, so this story is additive to that shape rather than
a new design. A few judgment calls made directly (not put to the user, since each had
an unambiguous existing precedent to follow) worth recording:

1. **Mode-scoped, not global.** Aggregate exposure sums only orders in the *same*
   `TradingMode` as the new order, not paper+live combined. Paper and live are separate
   broker accounts/capital pools (paper is fake money), so combining them would
   misrepresent real risk — matches `countByOrderModeAndStatus`'s existing per-mode
   precedent (E6-F1-S2's paper-trade threshold) rather than `cancelAllOpenOrders`'s
   deliberately-global kill-switch precedent, which is a different kind of control
   (a panic button, not a risk budget).
2. **Portfolio-wide, not per-asset-type.** One aggregate cap across stocks and crypto
   combined, matching the story's own "portfolio-level" framing and acceptance
   criterion ("many individually-small orders can't add up") — a per-asset-type split
   would just be two more per-order-style caps, not a portfolio cap.
3. **Fail-fast config validation**, extending E6-F2-S1's existing constructor check
   (`crypto-max-leverage` vs. the adapter's technical ceiling): the aggregate cap must
   be >= the larger of the two per-order position-size caps, or a single
   maximally-sized order would always breach the aggregate cap on its own, even against
   an empty portfolio — the same "a cap that can never bind is a config bug, not a
   valid conservative setting" reasoning.
4. **Starter default of $8,000**, picked the same way E6-F2-S1's defaults were:
   conservative for a personal paper/testnet-scale account, above the $5,000 largest
   per-order cap (per point 3) but not by a huge margin, env-overridable via
   `RISK_LIMITS_MAX_AGGREGATE_EXPOSURE_USD` to raise later.

Backend: `RiskLimitsProperties` gains a fourth field, `maxAggregateExposureUsd`
(`risk-limits.max-aggregate-exposure-usd`). `RiskLimitService` gains
`enforceAggregateExposureCap(currentOpenNotionalUsd, newOrderNotionalUsd)` — a pure
function like `enforcePerOrderCaps`, deliberately *not* given an `OrderRepository`
dependency itself (this service stays config-only/stateless, matching E6-F2-S1's own
`RiskLimitService`-is-stateless-vs-`KillSwitchService`-is-stateful split); the caller
computes `currentOpenNotionalUsd` and hands it in. `OrderRepository` gains
`sumOpenNotionalUsd(orderMode, excludedStatuses)`, a `@Query` JPQL aggregate
(`SELECT COALESCE(SUM(o.requestedAmountUsd * o.leverage), 0) ...`) — `COALESCE` so an
empty portfolio returns a real zero, never `null`, so `OrderService` never needs a null
check. Reuses the same `TERMINAL_STATUSES` list `cancelAllOpenOrders` already excludes
(so "open" means the identical thing in both the kill switch and this cap), rather than
inventing a second "open" definition. Wired into `OrderService.submitOrder()`
immediately after `enforcePerOrderCaps`, still before quantity computation or `Order`
persistence — same pre-flight, no-row, `RiskLimitExceededException`/403
`RISK_LIMIT_EXCEEDED` treatment as E6-F2-S1, so `RiskExceptionHandler` and the
frontend's existing generic `MarketDataError` rendering in `TradeForm` needed zero
changes to surface it. `tradingModeService.current()` was moved a few lines earlier in
`submitOrder()` (it was previously read just before adapter routing) since the
aggregate-cap query now also needs the current mode — no behavior change, just reusing
one read instead of two.

Frontend: no changes. The 403 `RISK_LIMIT_EXCEEDED` error code and its message are
already rendered generically by `TradeForm`'s existing `MarketDataError` handling from
E6-F2-S1 — a new failure reason on an already-wired error path, not a new path.

Tests: `RiskLimitServiceTest` gains 4 cases (exact-boundary-inclusive aggregate cap,
over-cap even with a small in-isolation-compliant new order, empty-portfolio allowed,
plus the new fail-fast constructor check for an aggregate cap configured below the
largest per-order cap) — 11 total, up from 7. `OrderServiceTest` gains 2 cases using
`serviceWithRealRiskLimits()` (E6-F2-S1's real-not-mocked-`RiskLimitService` pattern),
stubbing `orderRepository.sumOpenNotionalUsd` to simulate pre-existing open exposure:
one proving many-small-orders-adding-up rejection (6500 existing + a 2000 new order,
each individually within every per-order cap) with zero `orderRepository.save()` calls
and zero adapter interactions, one proving the exact-boundary-inclusive allowed case
(6000 existing + 2000 new = exactly the 8000 cap). The `@BeforeEach` setup also gained
a lenient default (`sumOpenNotionalUsd` → `BigDecimal.ZERO`) so every pre-existing test
in the file — none of which know about this new repository call — keeps working
unchanged. 30 `OrderServiceTest` cases total (up from 28), 388 backend tests overall,
full `./mvnw test` green, no regressions.

Not verified live in a running browser: Docker wasn't running in this session, so (same
as E6-F2-S1/E6-F2-S2) no actual HTTP round-trip through a live server. Verification
rests on the unit-test suite, including the two tests above that exercise a real
`RiskLimitService` end to end. Worth a follow-up manual check once Docker's available:
place several small paper crypto orders in a row until aggregate exposure approaches
the $8,000 default, confirm the next order gets rejected with a sensible message even
though its own amount/leverage are well within the per-order caps, and confirm a
similarly-sized order still succeeds if submitted against the *other* trading mode
(proving the mode-scoping actually holds against a live backend, not just mocks).

## E6-F3-S1 — immutable audit log of every order and the signal that triggered it

Design gate (`Plan` agent) settled the shape before any code, because the obvious-looking
answer was wrong: `Order` already carries `ticker`, `indicator_snapshot_id`,
`requestedAmountUsd`, `leverage`, and a `status`/`rejectionReason`/`brokerOrderId`
outcome — but `applyOutcome` mutates that same row in place as an order resolves
(`PENDING` → `FILLED`/`REJECTED`/etc, and later again via `refreshOrder`/
`cancelAllOpenOrders`). So `orders` itself is not append-only; the actual gap this story
closes is a row that's genuinely written once and never touched again. The plan
considered and rejected mirroring `trading_mode_events`/`kill_switch_events`'s
"append-only events, latest row = current state" pattern — that pattern tracks *current
state of one global switch*, not *a decision made about one order*, and the acceptance
criteria's "result" is singular, not a transition history. Settled on: one new
`order_audit_entries` row per `Order`, written by `OrderService.submitOrder` at that
order's first resolved outcome only (not from `refreshOrder`/`cancelAllOpenOrders`,
which keep mutating `orders` afterward) — a deliberate scope cut flagged explicitly
rather than silently dropped, since it means an audit row can go stale relative to an
order's later-resolved real status. Also settled: reuse the `SignalCallEntry` already
persisted by `SignalService.computeSignalWithProvenance` for every signal computation
(FK it, don't duplicate its columns) via a new single-snapshot repository lookup, rather
than widening `computeSignalWithProvenance`'s return record — the latter would have
forced mechanical edits to `WatchlistSignalPoller` and ~15 direct
`SignalComputation` constructions in `OrderServiceTest`, for a change three unrelated
stories already depend on. And: no new read endpoint in this story — `listOrders` and
the existing CSV export already serve as review surfaces, and a dedicated audit-trail
viewer is better built once E6-F3-S2 lands the rule-table-version column right next to
it, rather than twice.

Backend: `V13__add_order_audit_entries.sql` — `order_audit_entries` table with FKs to
`orders` (`UNIQUE`, enforcing the 1:1 write-once intent at the DB level, not just in
application code), `tickers`, and `signal_calls`, plus `result_status` with a `CHECK`
constraint whose value list is copied verbatim from `orders.ck_orders_status` (V8) — a
comment in the migration flags that widening one without the other is a bug. New
`OrderAuditEntry` entity: no setters, no `@PreUpdate`, a single constructor, matching
`SignalCallEntry`'s immutable-entity style rather than `Order`'s mutable one. New
`OrderAuditEntryRepository` (plain `JpaRepository`, no custom finders needed yet — a
future audit-viewer story adds `findByOrderId`/whatever it needs then).
`SignalCallEntryRepository` gains
`findTopByIndicatorSnapshot_IdOrderByIdDesc(indicatorSnapshotId)` — the single-snapshot
version of the existing `findByIndicatorSnapshot_IdIn` batched lookup `OrderCsvExporter`
already uses, same "not DB-enforced 1:1, tie-break to the highest id" tolerance.
`OrderService.submitOrder` looks up the `SignalCallEntry` once, right after resolving
the broker credential (i.e. after every pre-flight reject path — `HOLD`, invalid
request, risk-cap breach, kill switch engaged, no credential — has already had its
chance to throw, so none of them ever touch the new repository), then a new private
`recordAuditEntry(Order, SignalCallEntry)` helper wraps each of the four
`applyResult`/`applyOutcome` call sites inside `submitOrder`'s try/catch, saving one
`OrderAuditEntry` populated from the just-resolved `Order`'s outcome fields before
returning the `TradeOrderResponse`. Added a javadoc note on both `applyOutcome` (which
`refreshOrder`/`cancelAllOpenOrders` also call, but never followed by an audit write)
and `recordAuditEntry` itself cross-referencing the scope boundary, so a future reader
doesn't assume the audit log stays current after submission.

Tests: `OrderServiceTest` gains a mocked `OrderAuditEntryRepository` collaborator (now
threaded through both `OrderService` constructors in the file) with a lenient default
stub for the new `SignalCallEntryRepository` lookup, so every pre-existing test — none
of which know about this new repository — keeps working unchanged. New/updated cases:
`filledOrder_recordsAuditEntryLinkedToOrderAndSignalCall` (captures the saved
`OrderAuditEntry` and asserts it's FK'd to the actual persisted `Order` and the actual
stubbed `SignalCallEntry`, with `resultStatus`/`brokerOrderId`/`entryPrice` matching the
broker result), `rejectedOrder_persistsAsRejectedWithReason` extended to also assert the
audit row's `resultStatus`/`rejectionReason`, `holdSignal_throwsSignalNotActionable_...`
and `overCapLeverage_forgedPlaceOrderRequest_...` extended with
`verifyNoInteractions(orderAuditEntryRepository)` to prove pre-flight rejects never
write an audit row, and `refreshOrder_brokerConfirmsFilled_persistsUpdatedStatus`
extended with the same `verifyNoInteractions` to prove the deliberate scope boundary —
a later status resolution through `refreshOrder` does not touch the audit log. Full
`./mvnw verify` green, including Flyway schema validation of V13 against H2 in
Oracle-compatibility mode, no regressions.

Not verified live in a running browser: Docker wasn't running in this session, so (same
as every E6 story so far) no actual HTTP round-trip through a live server or a real
Oracle instance. Verification rests on the unit-test suite and `./mvnw verify`'s
H2-backed Flyway validation of the new migration. Worth a follow-up manual check once
Docker's available: place a paper order through the real UI, confirm exactly one
`order_audit_entries` row appears for it with the right `signal_call_id`/`ticker_id`
FKs, then `refreshOrder` or cancel it and confirm the audit row is unchanged while the
`orders` row itself updates — the concrete behavior this story's scope cut predicts.

## E6-F3-S2 — record the rule-table version alongside the signal snapshot in the audit log

Design gate (`Plan` agent) confirmed there was no real decision left to make on shape —
E6-F3-S1's own changelog entry (above) had already forecast this exact story: "a
dedicated audit-trail viewer is better built once E6-F3-S2 lands the rule-table-version
column right next to it [the `signal_call_id` FK], rather than twice." The only question
was where the value comes from at write time. Two candidates: re-read
`SignalRuleEngine.RULE_TABLE_VERSION` (the engine's current static constant) or reuse
`signalCallEntry.getRuleTableVersion()` (already persisted on the `SignalCallEntry` row
looked up for this specific submission, since E2-F3-S1 — ahead of need, per that story's
own changelog note). Went with the latter: the story's entire purpose is that a *later*
rule-table version bump must not retroactively change what a *past* audit row says
produced it, so the audit write must copy the version actually recorded for that
signal, not re-derive "whatever the constant says right now." The two happen to agree
today (nothing changes `RULE_TABLE_VERSION` mid-request), but only the `SignalCallEntry`
read is correct by construction rather than by coincidence, and doesn't depend on the
signal lookup and the audit write staying in the same request/deploy forever.

Backend: `V14__add_rule_table_version_to_order_audit_entries.sql` — plain
`ALTER TABLE order_audit_entries ADD rule_table_version VARCHAR2(20) NOT NULL`, no
default. Safe because `order_audit_entries` has zero rows in every real environment
(E6-F3-S1 was never live-verified — Docker wasn't up in that session either, see above),
so there's no backfill problem a default would be papering over; mirrors
`signal_calls.rule_table_version`'s own definition exactly (same type/length, no
default, no `CHECK` — free-form version string, not an enum). `OrderAuditEntry` gains a
`ruleTableVersion` field/getter, placed directly after `signalCallEntry` in both the
entity and its constructor to stay visually adjacent to the FK it's copied from.
`OrderService.recordAuditEntry`'s one `new OrderAuditEntry(...)` call site now passes
`signalCallEntry.getRuleTableVersion()` as the new argument — no other call sites exist,
and no other file constructs `OrderAuditEntry` directly.

Tests: `OrderServiceTest.filledOrder_recordsAuditEntryLinkedToOrderAndSignalCall` stubs
the mocked `SignalCallEntry.getRuleTableVersion()` to return
`SignalRuleEngine.RULE_TABLE_VERSION` and asserts the captured `OrderAuditEntry` carries
it through unchanged. No other test needed updating —
`rejectedOrder_persistsAsRejectedWithReason` and the `verifyNoInteractions`-based
pre-flight-reject/scope-boundary tests don't assert on this field, and no standalone
`OrderAuditEntryTest` exists (the entity is exercised only through `OrderServiceTest`,
same as `SignalCallEntry`). Full `./mvnw verify` green (389 tests, 0 failures/errors),
including Hibernate schema validation of the new column against H2's Oracle-compatibility
mode. No config, no new endpoint, no frontend change — CLAUDE.md's E6-F3-S1 note said a
dedicated audit-trail viewer is left for a future story once this column landed; it has
now landed, but the viewer itself is still that future story, not this one.

Not verified live: Docker wasn't running in this session, so no real Oracle round-trip
for the new `ALTER TABLE`. Verification rests on `./mvnw verify`'s H2-backed Flyway
validation, same posture as E6-F3-S1. Worth checking alongside that story's own
follow-up once Docker's available: confirm a placed paper order's audit row carries
`rule_table_version = "v1"` (or whatever `SignalRuleEngine.RULE_TABLE_VERSION` is by
then) directly, with no join needed.

With this story done, E6 (Risk & Safety Controls) is complete; E7 (Observability &
Hardening) is next up.

## E7-F1-S1 — structured logging across backend services

Design gate (own analysis, entered plan mode given the story's breadth — it touches
roughly 15 files across every backend package): read every catch block, `@RestControllerAdvice`,
and retry/backoff loop before writing anything. Confirmed SLF4J/Logback was already on
the classpath (transitively via `spring-boot-starter-webmvc`) and that four classes
already used the `private static final Logger log = LoggerFactory.getLogger(...)`
convention (`WatchlistSignalPoller`, `BrokerCredentialService`,
`CredentialEncryptionService`, `NotificationService`) — extended that rather than
introducing a JSON log encoder or MDC/correlation-id framework, matching this repo's
established bias against a library where a small, consistent, hand-applied convention is
enough (same reasoning as E2-F2-S1's hand-rolled indicators). No new Maven dependency.

Found, by reading rather than guessing, that the two places that mattered most for the
AC ("broker errors logged with context, never silently swallowed") were almost entirely
silent: `RetryingBrokerAdapter` (the single retry/backoff chokepoint shared by both
brokers) logged nothing at any retry, backoff, or terminal-failure point, and
`OrderService.submitOrder`'s four broker-exception catch blocks turned a real order
failure into a `FAILED`/`SUBMISSION_UNKNOWN` `Order` row with zero log line — the most
operationally important code path in the app (money-moving) was completely silent.
`BinanceFuturesTradingAdapter.ensureExitLeg` also swallowed every leg-placement failure
with no log at all, meaning a missing stop-loss/take-profit on a filled leveraged entry
(the exact "unprotected position" scenario that story's own Javadoc calls out as the
crux of that design) had no trace anywhere.

Backend: new `backend/src/main/resources/logback-spring.xml` — one console appender,
one pattern (`%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %-5level [%thread] %logger{36} - %msg%n`),
applied to every profile (local/paper/prod) rather than a per-profile variant, since the
AC asks for a *consistent* format. Added `Logger`s and log calls at: `RetryingBrokerAdapter`
(WARN per retry/rate-limit backoff, ERROR when a transient failure exhausts retries or
`reconcile()` produces `BrokerAdapterAmbiguousOrderException`); `OrderService.submitOrder`'s
four catch blocks (ERROR for ambiguous/unavailable/generic, WARN for rate-limited, all
with broker/symbol/clientOrderId/orderId) and `cancelAllOpenOrders`'s per-order cancel
failure (WARN); `BinanceFuturesTradingAdapter.ensureExitLeg`'s swallowed leg failures
(WARN — the unprotected-position case) and its/`AlpacaTradingAdapter.cancelOrder`'s
"already terminal, idempotent no-op" swallows (DEBUG); `RetryHelper.withOneRetry`
(WARN on the first-attempt failure that triggers its one retry — previously invisible
even when the retry succeeded); all 5 `@RestControllerAdvice` classes
(`MarketDataExceptionHandler`, `OrderExceptionHandler`, `RiskExceptionHandler`,
`TradingModeExceptionHandler`, `NotificationExceptionHandler`), one log line per handler
method; `KillSwitchService.switchTo` (WARN on a real engage/clear transition) and
`RiskLimitService`'s two `enforce*` methods (WARN right before throwing
`RiskLimitExceededException`, with the breached numbers already in the message).

Log-level policy, applied consistently rather than per-file judgment calls: INFO for
ordinary client-driven 4xx that reflect normal user interaction, not an operational
problem (bad/unregistered ticker, validation, signal-not-actionable, order-not-found,
market-closed, paper-trade-threshold/risk-consent gates); WARN for infra/operational
statuses (429/503 — rate-limited, market-data/broker-credential/order-refresh
unavailable) and safety-gate trips (kill switch engaged, risk-limit breach) since those
are working-as-designed but still worth seeing; ERROR reserved for genuine
order-submission failures in `OrderService`/`RetryingBrokerAdapter`. Deliberately did
*not* add a second log line in the concrete adapters (`AlpacaTradingAdapter`,
`BinanceFuturesTradingAdapter`) or market-data clients for failures that rethrow —
every exception in this app terminates at exactly one of three sinks
(`RetryingBrokerAdapter`'s retry handling, `OrderService.submitOrder`'s catch blocks, or
a `@RestControllerAdvice` handler), confirmed by checking that `getAccountStatus`/
`getPosition` aren't even wired to a controller yet, so logging at every intermediate
rethrow site would just duplicate the same event under a different logger name — the
AC's "never silently swallowed" is about failures having *a* trace, not every layer
adding its own. Noted this "log once, at the sink" rule in CLAUDE.md's architecture
section so it isn't relitigated per file next time.

Out of scope, on purpose: no MDC/correlation-id request tracing, no log file
rotation/shipping, no new logging library — none of those are in the AC, and adding them
would be exactly the kind of scope creep the `simplify` skill exists to catch.

Tests: no new test files — this is a logging-only change with no behavior change, so
the existing suite (`RetryingBrokerAdapterTest`, `RetryingBrokerAdapterOutageTest`,
`OrderServiceTest`, `AlpacaTradingAdapterTest`, `BinanceFuturesTradingAdapterTest`, the
five `*ControllerTest` classes covering each exception handler, `RiskLimitServiceTest`)
already exercises every one of the new log call sites, since they're all on pre-existing
exception paths. `./mvnw verify` green with zero regressions, and the captured console
output during that run is itself a live confirmation the new logging works exactly as
designed: real `RetryingBrokerAdapter` ERROR lines with `broker=ALPACA attempt=3/3`,
`OrderService` ERROR lines with real `clientOrderId`/`orderId` values for a forced
Binance outage scenario, `RetryHelper` WARN lines on a forced 500, and INFO/WARN lines
from every exception handler under test — all in the new consistent
timestamp/level/thread/logger/message format.

Not verified live in a running browser: Docker wasn't running in this session, so the
real Oracle-backed stack wasn't brought up for a manual click-through. Given this story
changes zero behavior (only adds logging to already-tested code paths) and the test run
above already produced real console output at every new log site with correct context,
verification rests on that rather than a live HTTP round-trip. Worth a follow-up once
Docker's available: tail the console while placing a real paper order and confirm the
format reads cleanly end-to-end outside a test harness.

## E7-F2-S1 — security review of credential-storage and order-submission code

Ran the `security-review` skill's generic pass plus this repo's own project-specific
checklist against every file the checklist calls out, reading source directly rather than
trusting docstrings: `broker.CredentialEncryptionService`/`BrokerCredentialService` and
the two credential bootstraps (E1-F3-S1); `AlpacaTradingAdapter`/
`BinanceFuturesTradingAdapter` and the HTTP requests they build from ticker/amount/leverage
input (E4.2/E4.3); `security.SecurityConfig`/`AuthController` (F1.3-S2); `TradingModeService`/
`TradingModeController` (E6.1's live-mode gate); `RiskLimitService`/`KillSwitchService`
(E6.2's guardrails); `OrderAuditEntry`/`OrderAuditEntryRepository` (E6.3's audit log).

One confirmed finding. `CredentialEncryptionService` and `SecurityConfig` each have an
insecure, hardcoded, checked-into-source-control dev-only fallback — a fallback encryption
key and a fallback dashboard password respectively — that activates whenever their real
env var (`CREDENTIAL_ENC_KEY_<ID>`, `DASHBOARD_PASSWORD`/`DASHBOARD_PASSWORD_HASH`) is
unset. Both already logged a WARN when this happened, and both are explicitly documented
as "never for paper/prod" in their own Javadoc/CLAUDE.md — but neither actually *refused*
to start on that condition. That's a real gap against this app's own stated posture:
`application-prod.properties`'s header comment promises "No defaults on purpose: every
value must come from the environment, so live mode can never start on placeholder
config," and that promise is genuinely enforced for `spring.datasource.url`/`DB_USERNAME`/
`DB_PASSWORD` (unguarded `${VAR}` placeholders with no default, so Spring's own property
resolution fails startup) but not for these two secrets, which read through a
`:`-defaulted `@Value` (`SecurityConfig`) or directly via `System.getenv()`
(`CredentialEncryptionService`), bypassing that mechanism entirely. Concretely: a paper/prod
deploy that forgets to set `CREDENTIAL_ENC_KEY_V1` would start up looking completely
healthy and would encrypt every real broker API secret at rest with a key that's public in
this git repository — silent, not loud, exactly the kind of gap this story exists to catch
before a live account is ever connected.

Fixed both, narrowly, rather than just flagging them: added an `activeProfile` parameter to
`CredentialEncryptionService`'s constructor (`@Value("${spring.profiles.active:local}")`,
now needs an explicit `@Autowired` since a second/third constructor made Spring's implicit
single-constructor autowiring inapplicable — first attempt at `./mvnw test` failed context
startup with "No default constructor found" until that was added) and a matching
`activeProfile` field on `SecurityConfig`; under the `paper`/`prod` profile specifically,
both now throw `IllegalStateException` at startup instead of silently falling back — same
fail-fast posture as `RiskLimitService`'s constructor checks from E6-F2-S1/E6-F2-S3, and
now genuinely mirroring `${DB_URL}`'s behavior rather than just being documented to.
`local`/test behavior is unchanged (still falls back, still just warns) since the whole
point of the dev-only fallback is to make local dev/tests work without any secrets
configured. `SecurityConfig.userDetailsService`'s hash-resolution branch was extracted into
a plain static method, `resolvePasswordHash(password, passwordHash, activeProfile,
passwordEncoder)`, specifically so this fail-fast branch is unit-testable without a Spring
context — the same reason `CredentialEncryptionService`'s constructor was already
structured as plain Java taking an injectable `Map<String, String>` env.

Everything else reviewed clean, no changes needed:
- **Injection into adapter HTTP calls**: `BinanceFuturesTradingAdapter` already had a
  documented, implemented `SYMBOL_PATTERN` (`^[A-Z0-9-]{1,20}$`) guard, with its own
  Javadoc explicitly calling out that `TickerController`'s validation
  (`@NotBlank @Size(max = 20)`) has no character-class restriction and that the adapter is
  the actual injection boundary before a signed, literal (non-re-encoded) query string is
  built — confirmed this fires fatally, pre-HTTP, on every entry point
  (`placeOrder`/`getOrderStatus`/`getPosition`; `cancelOrder` reuses an already-validated
  symbol via `getOrderStatus`). `AlpacaTradingAdapter` never string-builds a request at
  all — Jackson-serialized JSON bodies and `RestClient` URI templating/`queryParam` for
  every path/query value, both of which encode automatically. `BinanceMarketDataClient`'s
  unauthenticated read-only klines endpoint also uses `queryParam`, not string
  concatenation.
- **Credential handling**: `BrokerCredentialService.DecryptedCredential` overrides
  `toString()` to redact itself, so an accidental `log.info("{}", credential)` can't leak
  plaintext; no `BrokerCredentialController` or any other HTTP-reachable path exposes a
  decrypted credential; exception messages surfaced to the frontend
  (`BrokerCredentialNotConfiguredException`, adapter rejection reasons) are built from
  broker/mode enums and broker-returned error bodies, never from the credential itself.
- **Auth/CSRF**: `SecurityConfig.securityFilterChain` is `.anyRequest().authenticated()`
  with an explicit allowlist of only `/health`, `/actuator/health`, `/api/auth/csrf`,
  `/api/auth/login` — enforced server-side by Spring Security, not just hidden by the
  frontend router. CSRF stays enabled (cookie-based double-submit via
  `CookieCsrfTokenRepository` + a `SpaCsrfTokenRequestHandler` that reads the raw
  `X-XSRF-TOKEN` header), matching Spring Security's own documented pattern for a
  cookie-reading SPA.
- **Live-mode gate (E6.1)**: `TradingModeService.switchTo` checks the paper-trade
  threshold and risk-consent gates itself before writing a new `TradingModeEvent`row;
  `TradingModeController` has no path that sets mode/threshold/consent directly, so there's
  no request-body field that could skip the gates via a direct API call.
- **Guardrails (E6.2)**: `RiskLimitService.enforcePerOrderCaps`/`enforceAggregateExposureCap`
  and `KillSwitchService.assertNotEngaged` are both called from
  `OrderService.submitOrder` pre-flight, reading only server-side config/DB state — nothing
  in `PlaceOrderRequest` can override them. `RiskLimitService`'s constructor already
  fail-fast checks (from E6-F2-S1/E6-F2-S3) that no cap can be configured above the
  ceiling it's supposed to enforce.
- **Audit log (E6.3)**: `OrderAuditEntry` has no setters at all (only a package-visible
  no-arg JPA constructor and the one all-args constructor used at write time);
  `OrderAuditEntryRepository` is a bare `JpaRepository` with no custom update/delete query,
  and no controller anywhere exposes it — `OrderService.recordAuditEntry` is the only write
  path in the app, called exactly once per order from `submitOrder`.

Tests: added `CredentialEncryptionServiceTest.noKeysConfigured_underPaperProfile_failsFastInsteadOfUsingDevKey`/
`..._underProdProfile_...`/`..._underLocalProfile_fallsBackToDevOnlyKey`, and a new
`SecurityConfigPasswordFallbackTest` (4 cases: paper/prod fail fast, local falls back,
an explicit password-hash is used as-is regardless of profile) exercising
`SecurityConfig.resolvePasswordHash` directly with no Spring context. `./mvnw verify` green
end-to-end — full suite, not just the touched tests — confirming the new `@Autowired`
constructor and the extracted static method didn't regress anything else wiring through
`CredentialEncryptionService`/`SecurityConfig` (adapter tests, `OrderServiceTest`, the full
`SecurityConfigTest` login/logout flow, etc.).

Not verified live in a running browser: same as E7-F1-S1, Docker wasn't running in this
session. This story is a review-plus-narrow-fix, not a UI change, so there's nothing new to
click through — the fail-fast behavior itself was verified by unit test (constructing each
service under the `paper`/`prod` profile with no secret configured and asserting it throws)
rather than by an actual failed `spring-boot:run` startup, which would need an env swap this
session didn't have Docker/Oracle up for. Worth a follow-up once Docker's available: start
the app under `SPRING_PROFILES_ACTIVE=paper` with `CREDENTIAL_ENC_KEY_V1`/
`DASHBOARD_PASSWORD_HASH` deliberately unset and confirm it refuses to boot instead of
silently succeeding.

## E7-F3-S1 — tested backup/restore procedure for the Oracle instance

Design gate first (this is infra/tooling, not application code, but still non-trivial
enough to warrant one): compared Data Pump (`expdp`/`impdp`) against a raw volume-level
copy (stop the container, `tar` `./oracle-data`). Went with Data Pump. The story's AC
specifically asks for a restore *tested against a fresh Oracle XE instance*, and a volume
tar can't really prove that — it just proves bytes moved, tied to one exact datafile
layout/version, and the only "restore" it supports is swapping a whole container's
`/opt/oracle/oradata`, not a fresh instance receiving data. Data Pump is schema-scoped, is
verifiable at the row level, and runs online against the live dev container with no
downtime — confirmed `gvenzl/oracle-xe:21-slim` ships no built-in backup/restore helper
scripts (unlike some other DB images), so there was no shortcut to skip past this
comparison.

Added:
- `scripts/db-backup.sh` — schema-scoped `expdp` export of the `autotrade` schema
  (`ORACLE_APP_USER` from `.env`), `docker cp`'d out to a gitignored `./backups/`
  (override via `BACKUP_DIR`), plus a `.manifest.txt` sidecar recording one row-count line
  per table so a later restore has something concrete to diff against.
- `scripts/db-restore.sh` — takes a dump path and a target container name (defaults to
  the restore-test container below), `impdp`s it in, and prints the same row-count query
  for comparison against the backup's manifest.
- `docker-compose.restore-test.yml` — a second, disposable Oracle XE service
  (`autotrade-oracle-xe-restore-test`, its own host port/data volume
  `./restore-test-data`, and its own compose project name `autotrade-restore-test` to
  avoid Compose treating it as an "orphan" of the main `docker-compose.yml` project) used
  only to prove a restore against a genuinely fresh instance, never against the live dev
  database.
- `docs/runbooks/oracle-backup-restore.md` — the documented procedure, matching
  `credential-key-rotation.md`'s format (numbered procedure + a Notes section for
  gotchas).
- `.gitattributes` (new file) — forces `scripts/*.sh` to keep LF line endings on
  checkout. Windows' `core.autocrlf` would otherwise silently convert them to CRLF,
  which breaks bash heredocs/shebangs; this repo already has a related gotcha for
  `mvnw`'s executable bit, so getting ahead of the line-ending version of the same class
  of bug for the new scripts seemed worth the one-line file.
- `.gitignore`: added `restore-test-data/` and `backups/` (the latter with a comment
  pointing at the runbook's off-disk-copy step; `*.dmp` was already ignored, but the
  `.log`/`.manifest.txt` siblings weren't).

Bugs found and fixed while actually running this, not just writing it:
- **First attempt at a Data Pump directory reliably failed.** Creating a fresh
  `CREATE OR REPLACE DIRECTORY` object under `/tmp/autotrade-backup` (mkdir'd
  immediately beforehand, owned by `oracle:oinstall`, world-readable) still made
  `expdp` fail every time with `ORA-39002`/`ORA-39070`/`ORA-29283` ("unable to open the
  log file... nonexistent file or path"), even though the directory demonstrably existed
  and was writable (`touch` through a separate `docker exec` worked fine against the same
  path outside of an `expdp` run). Rather than keep chasing that, switched both scripts to
  use Oracle's own pre-existing `DATA_PUMP_DIR` directory object (under
  `/opt/oracle/admin/XE/dpdump/<guid>/`, looked up dynamically via
  `SELECT directory_path FROM dba_directories`) — which worked immediately, no
  investigation into the exact root cause needed once the working alternative was
  confirmed.
- **`impdp` needs `exclude=user`.** The restore target's `APP_USER` already exists
  (created by the container's own init from `APP_USER`/`APP_USER_PASSWORD`), so a plain
  schema-scoped import tries to (re)create that user and fails with
  `ORA-31684: Object type USER:"AUTOTRADE" already exists` — Data Pump treats this as a
  logged, non-fatal warning but still exits non-zero, which `db-restore.sh`'s `set -e`
  turned into a hard failure despite every table having actually imported correctly (row
  counts already matched on that first, "failed" run). Added `exclude=user` to the
  `impdp` invocation so the import is clean end-to-end with exit 0, since the target
  schema/user is expected to pre-exist in this restore-test setup.
- **Operational fumble, not a script bug**: mid-setup, force-removed
  (`docker rm -f`) a restore-test container that was still mid-`uncompressing database
  data files`, then reused the same `./restore-test-data` volume for a fresh container —
  which left it crash-looping ("Break signaled" 3x, then exited 255) since the volume had
  partial init state. Fixed by wiping `./restore-test-data` entirely and starting the
  disposable instance fresh exactly once. Not a code change, but worth recording: the
  runbook's Notes section calls out `restore-test-data/` needing a clean wipe if the
  restore-test instance isn't torn down with `down -v` before a re-run.

Live-tested end-to-end, against the real local dev Oracle container (not H2, not mocked):
1. `docker compose up -d`, waited for `autotrade-oracle-xe` healthy (already had a
   populated `./oracle-data` volume from prior sessions — 10 tables, real data, not an
   empty schema).
2. `bash scripts/db-backup.sh` — succeeded, `expdp` exported all 10 tables:
   `INDICATOR_SNAPSHOTS` (152 rows), `SIGNAL_CALLS` (150), `flyway_schema_history` (11),
   `TICKERS` (7), `WATCHLIST_ENTRIES` (3), `TRADING_MODE_EVENTS` (3), `NOTIFICATIONS` (1),
   `RISK_CONSENTS` (1), `BROKER_CREDENTIALS` (0), `ORDERS` (0) — into
   `backups/autotrade_20260803_202235.dmp` (~940 KB) plus its `.manifest.txt` recording
   exactly those counts.
3. `docker compose -f docker-compose.restore-test.yml up -d`, waited for
   `autotrade-oracle-xe-restore-test` healthy (brand-new, empty `./restore-test-data`
   volume).
4. `bash scripts/db-restore.sh backups/autotrade_20260803_202235.dmp` — `impdp` completed
   with `Job "SYSTEM"."SYS_IMPORT_SCHEMA_01" successfully completed`, exit 0. The
   script's own row-count query against the restored instance printed the identical 10
   rows/counts as the backup manifest — `INDICATOR_SNAPSHOTS` 152, `SIGNAL_CALLS` 150,
   `flyway_schema_history` 11, `TICKERS` 7, `WATCHLIST_ENTRIES` 3, `TRADING_MODE_EVENTS`
   3, `NOTIFICATIONS` 1, `RISK_CONSENTS` 1, `BROKER_CREDENTIALS` 0, `ORDERS` 0 — an exact
   match, table for table, row for row.
5. `docker compose -f docker-compose.restore-test.yml down -v` + removed
   `./restore-test-data` to tear the disposable instance down completely. Confirmed the
   real dev container (`autotrade-oracle-xe`) stayed healthy and untouched throughout.

That row-count match against a genuinely fresh, empty Oracle XE instance is the "tested"
evidence this story's AC asks for.

Deliberately out of scope, per the story: no scheduled/cron backup (on-demand script
only); backups land same-disk in `./backups/` by default rather than the script itself
pushing to an external location, since only the operator knows what off-disk destination
they actually have available — the runbook documents the manual off-disk-copy step
instead of guessing one. This was E7's last remaining story; the epic is now complete.

## E3-F1-S1 follow-up — asset-type selector on the ticker lookup form

Discovered during manual live-verification (all epics were already "done"): the
dashboard's "Ticker lookup" form had no way to register a new ticker at all, let alone
pick its asset type. `TickerController.register` (`POST /api/tickers`, `AssetType`
required, no symbol-shape inference per E1-F1-S1's convention) existed only as an
API-only path with zero frontend caller — the lookup form just called
`GET /api/tickers/{symbol}/price-history` and surfaced `TICKER_NOT_REGISTERED` for
anything not already registered via curl. Every ticker in the running dev instance had
been registered that way in earlier sessions, which is how this gap stayed unnoticed
until someone tried the form on a genuinely new symbol.

Fix: the form now has a Stock/Crypto radio selector (`TickerMetrics.tsx`, defaults to
Stock) and, on submit, calls a new `registerTicker` (`marketdata/api.ts`, mirrors
`watchlist/api.ts`'s `addToWatchlist` pattern) before the existing signal/chart fetch —
`POST /api/tickers` is idempotent for a symbol already registered under the same type
(200, not 201), so this is a no-op for tickers that already exist. Requesting a
*different* type than what's already registered surfaces
`TickerAssetTypeConflictException`'s `ASSET_TYPE_CONFLICT` message (added to
`TickerMetrics.tsx`'s `ERROR_MESSAGES` map, passed straight through since the backend
message is already specific) instead of silently relabeling the ticker. The watchlist's
"revisit" path (`Watchlist.tsx` → `lookupRequest`) deliberately does *not* register —
that ticker's type is already fixed by its existing registration, and threading the
form's currently-selected radio value through would incorrectly attempt to
re-register a saved ticker under whatever type the form happens to be showing.

Live-verified via the running dev stack (Docker Desktop was down; started it, brought up
Oracle XE/backend/frontend): registered a never-before-seen symbol (`DOGEUSDT`) as
CRYPTO through the form and got back its signal in one submit (no separate
registration step needed), then re-submitted the same symbol as Stock and confirmed the
exact `ASSET_TYPE_CONFLICT` message rendered inline rather than silently switching it.
No backend change — `POST /api/tickers` and its conflict handling already existed;
this was purely a missing frontend caller. No new tests added (this frontend has no
`@testing-library/react` dependency; component-level behavior here was verified live
in-browser rather than with a unit test, consistent with how `TradeForm`/`Watchlist`
etc. are also untested at the component level — only pure logic modules like
`validation.ts` get Vitest coverage).

## Frontend visual pass — design tokens, layout, and a missing-CSS bug fix

Requested as a general "make the UI look more professional" pass, not tied to a
specific story. Audited every component file first rather than guessing: most of the
per-feature color/badge work from E3/E5/E6 (signal badges, trade-form result tones,
order-status colors, trading-mode banner) was already solid and left alone. What was
actually missing was app-shell-level: no design tokens, no consistent card/section
treatment, unstyled `<button>`/`<input>`/`<fieldset>` elements (browser defaults), a
non-sticky plain-text header, and a completely unstyled login page.

Added to `index.css`:
- A `:root` design-token layer (`--color-bg/surface/surface-alt/border/text/
  text-muted/accent/danger`, `--radius-sm/md`, `--shadow-sm/md`) built from colors
  already in use elsewhere in the file (the existing signal-badge/trading-mode-banner
  teal/orange/slate/blue palette), rather than inventing a new one.
- Global base styles for headings (h2 restyled as a small-caps "eyebrow" label —
  reusing `.stat-tile__label`'s existing convention — so every section heading reads
  consistently), buttons (base + `form button[type="submit"]` / dialog-autofocus
  primary variant), and form controls (`input`/`select`/`fieldset`/`legend`, with
  `form > label` stacked vertically but `fieldset label` kept inline so radio/checkbox
  groups don't also get stacked).
- `.app-header` (sticky top bar) / `.app-main` (centered 1000px column) / `.app-toolbar`
  — new layout classes requiring small JSX restructuring in `DashboardPage.tsx` (the
  page title moved from a standalone `<h1>` into the header as its brand element; kill
  switch + trading-mode banner grouped into one toolbar row).
- `.app-main > section` — a single structural selector that gives every top-level
  dashboard block (Notifications/Watchlist/Ticker lookup/Order history — all bare
  `<section>` elements already) uniform card styling with zero per-component
  className changes needed.
- `.login-card` / `.login-page` for `LoginPage.tsx` (previously fully unstyled,
  top-left plain browser form) and `.ticker-lookup-form` for the ticker-lookup row
  (symbol input + asset-type pills + submit button now sit inline instead of stacking).

Real bug found and fixed along the way, not just cosmetic gap-filling:
`KillSwitchControl.tsx` has referenced `.kill-switch`/`.kill-switch__*` classes since
E6-F2-S2 shipped, but `index.css` never defined *any* of them — this app's most
safety-critical single control (the one-click "stop everything" kill switch) has been
rendering as completely unstyled text/buttons this whole time. Added the missing
rules, including a deliberate one-off exception to this app's "avoid raw red for
buttons" convention for `.kill-switch__engage` (danger red, matching
`.trading-mode-banner--live`), since this is the one action in the app that needs to
read as more urgent than everything else on the page.

That fix immediately surfaced a second bug via live-verification (caught by actually
looking at the rendered page, not just by the CSS parsing/building cleanly): the
"Kill switch" button rendered as an empty box with a visible red border but no visible
label. Root cause was a CSS specificity collision — `.kill-switch button { background:
var(--color-surface) }` (added so buttons keep contrast against the tinted
`.trading-mode-banner`-style container backgrounds) is class+element specificity
(0,1,1), which outranks single-class `.kill-switch__engage`'s (0,1,0) regardless of
source order, silently overriding its red `background` while leaving its `border-color`
and explicit `color: #ffffff` untouched — white text on a white background. Fixed by
bumping the engage-button selector to `button.kill-switch__engage` (element+class,
same specificity as the conflicting rule, so source order — which already put it
later in the file — now correctly decides the tie). Confirmed via
`getComputedStyle()` in-browser before and after: `background`/`color` both read
`rgb(255, 255, 255)` beforehand (invisible), `background` reads the danger red and
text is legible after.

Live-verified the whole page in both color schemes (light default, and dark via
`document.documentElement.style.colorScheme = 'dark'` — this app has never had a
dark-mode-specific test before now) across every section: header, kill switch
(cleared and — implicitly, same CSS path — engaged), trading-mode banner, notifications,
watchlist, ticker lookup (form row, stat tiles, signal badge, price chart, trade
form), order history, and the login page. No backend changes. No new tests (same
"no component-level test harness in this frontend" situation as the asset-type-selector
change above — verified live in-browser instead).

## Stock price chart no longer blanks out entirely when the market is closed

Follow-up to E2-F1-S3/E3-F2-S1, prompted by the user noticing that a stock's price
chart simply disappears outside regular trading hours. Investigated first (via the
`Explore` agent) rather than assuming a bug: `MarketDataService.getPriceHistory`
hard-blocks *any* stock fetch when `MarketHoursService.isRegularMarketOpen()` is
false, throwing `MarketClosedException` → 409 `MARKET_CLOSED` before Alpaca is even
called — by design, per E2-F1-S3's own rationale ("Alpaca is never called overnight/
weekends — a free reliability win"). `IndicatorService.getChartData` inherits this
from `MarketDataService` with no fallback, so `TickerMetrics.tsx`'s independent
`Promise.allSettled` chart fetch fails and renders nothing but the error text —
confirmed this is the actual, intended behavior of E2-F1-S3, not a regression.

Key fact that made a fix worthwhile: `AlpacaMarketDataClient.fetchRecentCandles` requests
`timeframe=1Day` — daily bars, not intraday. A daily candle fetched while the market was
open earlier that day (or on a prior trading day) doesn't become *stale* the moment the
market closes; it's the same historical data Alpaca would still return if asked. So the
409 gate isn't protecting data quality for the chart, specifically — it's a blanket
policy applied to every stock endpoint alike.

Rather than loosen `MarketHoursService`'s gate itself (which would also loosen
`/signal` and `/indicators` — the paths that actually feed trading decisions and that
E2-F1-S3/E2-F2-S1 deliberately hardened), scoped the fix to the chart only, leaning on
`IndicatorService.getChartData`'s own pre-existing Javadoc framing: "a read-only
diagnostic view, not the audited signal-computation path." Added an in-memory
`Map<String, ChartDataResponse> lastGoodChartData` (`ConcurrentHashMap`, keyed by the
same `trim().toUpperCase()` normalization `TickerService` already uses, so case doesn't
cause a cache miss) inside `IndicatorService`. On a successful fetch, the response
(now carrying a new `stale` boolean, `false`) is cached before being returned; on
`MarketClosedException`, the cache is checked first — a hit is returned with `stale=true`
instead of propagating, a miss still 409s exactly as before (so a symbol never
successfully fetched during market hours, e.g. right after a backend restart, still
shows the existing `MARKET_CLOSED` error — there's nothing to fall back to yet).
`/signal` and `/indicators` (`computeIndicators`/`computeForSignal`) were not touched —
they still hard-block on a closed market, unchanged.

`ChartDataResponse` gained the `stale` field (5th record component); `IndicatorController`
needed no changes, it just returns whatever `IndicatorService.getChartData` gives it.
Frontend: `ChartDataResponse` in `chart/api.ts` gained the matching `stale: boolean`;
`TickerMetrics.tsx` renders a `.chart-note` (reusing the existing style, no new CSS)
above the chart reading "The market is closed — showing the last available data as of
&lt;last candle's timestamp, localized&gt;." when `stale` is true. Since the chart fetch
now succeeds (200, not an error) when a cache entry exists, `chartError` no longer fires
for this case — only the independent `/signal` fetch still surfaces the `MARKET_CLOSED`
message for the stat tiles/signal badge, which remain correctly blocked.

Tested: `IndicatorServiceTest` gained
`chartData_marketClosed_withPriorSuccessfulFetch_returnsStaleCachedResponse` (populate
cache via a successful call, then assert a subsequent `MarketClosedException` returns
the cached candles/indicators with `stale=true`) and
`chartData_marketClosed_cacheKeyedCaseInsensitively`; the pre-existing
`chartData_marketClosed_propagates` was renamed
`chartData_marketClosed_noPriorFetch_propagates` to make clear it's specifically the
no-cache-yet case that still 409s. `IndicatorControllerTest` gained
`chartData_marketClosedWithCachedFallback_returns200Stale` and the existing green-path
test now asserts `stale=false`. All 399 backend tests pass (`./mvnw verify`); frontend
`npm run build`/`lint`/`test` all clean. No schema change (in-memory cache only, not
persisted — a backend restart during a closed market means the very next stock chart
request still 409s once, same as today, until the market reopens and repopulates the
cache). Not live-verified against the real running stack this time — doing so
meaningfully would require the dev backend to already hold a pre-close cache entry for
a real symbol, which isn't reliably reproducible from a fresh session; relied on the
unit test coverage above (cache hit/miss/case-insensitivity at the service layer, JSON
shape at the controller layer) instead.

## E4-F3-S2 follow-up — Binance order quantity/price precision truncation

Found live, during a first real end-to-end paper-mode run against Binance Futures
Testnet (user asked to exercise the golden path — ticker lookup → signal → paper
order — for real, not just against unit-test fixtures): a BTCUSDT SELL for $100 at
2x leverage was rejected outright by Binance with `-1111 "Precision is over the
maximum defined for this asset"`. Root cause: `OrderService.submitOrder` computes
`quantity = amountUsd / price` at scale 8 (`BigDecimal.divide(price, 8, DOWN)`), and
take-profit/stop-loss prices come straight from user input — both routinely carry
more decimal places than Binance's per-symbol `LOT_SIZE`/`PRICE_FILTER` allow (e.g.
BTCUSDT futures: 3-decimal quantity, 1-decimal price), and
`BinanceFuturesTradingAdapter.placeEntryOrder`/`ensureExitLeg` sent
`request.quantity()`/`stopPrice` straight through via `toPlainString()` with no
truncation. Never caught by `BinanceFuturesTradingAdapterTest` because those tests
construct already-precision-safe request fixtures directly (e.g. `new
BigDecimal("0.01")`) rather than routing through `OrderService`'s real computation —
this only surfaces against the real exchange with a realistic amount/price.

Fixed by adding a hardcoded per-symbol `QUANTITY_PRECISION`/`PRICE_PRECISION` map
(`BinanceFuturesTradingAdapter`, values for `BTCUSDT`/`ETHUSDT`/`DOGEUSDT`/`SOLUSDT` —
the symbols this app's watchlist/backtest fixtures actually use, with a conservative
3-decimal/2-decimal fallback for anything else) and a `roundToPrecision` helper
applied to the entry order's `quantity` and each exit leg's `stopPrice` before they're
sent. Hardcoded rather than fetched live from `/fapi/v1/exchangeInfo` — same bias as
the existing `MAX_LEVERAGE` constant's javadoc ("an extra live-data dependency this
codebase avoids for this kind of check"). Truncates (`RoundingMode.DOWN`, never up —
a caller-approved notional must never be silently exceeded) only when the value's
scale actually exceeds the symbol's precision, so an already-coarser value (like a
test fixture's `"0.01"`) is left untouched rather than zero-padded by an unconditional
`setScale`. A quantity that truncates all the way to zero (e.g. a too-small order
against `DOGEUSDT`'s whole-coin-only precision) now fails with a clear
`RISK`-adjacent-style rejection message instead of Binance's opaque precision error.

Tested: `BinanceFuturesTradingAdapterTest` gained
`placeOrder_highPrecisionQuantityAndPrices_truncatedToSymbolPrecisionBeforeSubmission`
(a scale-8 quantity and two-decimal-heavy TP/SL prices, asserting the exact truncated
query-param values Binance receives) and
`placeOrder_quantityRoundsToZeroAtSymbolPrecision_returnsRejectedWithoutPlacingEntry`.
All 401 backend tests pass (`./mvnw verify`). Live-verified end-to-end against the
real Binance Futures Testnet API (not mocked): the same BTCUSDT SELL that previously
hard-rejected on precision now fills successfully (`orderId=28266206621`).

Second, separate issue surfaced by the same live run, left open: Binance now rejects
`STOP_MARKET`/`TAKE_PROFIT_MARKET` exit legs sent to `/fapi/v1/order` with `"Order
type not supported for this endpoint. Please use the Algo Order API endpoints
instead"` — an apparent breaking change on Binance's side to the Futures Testnet API
surface since E4-F3-S2 was originally built/verified. Effect: every crypto bracket
order's entry now fills, but both protective legs currently fail after retry,
correctly surfacing as `PARTIALLY_PROTECTED` (E4-F3-S2's designed-for partial-failure
path working as intended, not silently hiding the gap) rather than a false success.
Confirmed independently via a direct signed call to Binance's `/fapi/v3/positionRisk`
(bypassing this app entirely) that the resulting entry position is real and open on
the testnet account. Not fixed in this pass — migrating exit-leg placement to
Binance's Algo Order API is a separate, real scope of work (new endpoint, likely a
different request/response shape) tracked as a follow-up, not a quick truncation fix.


## E4-F3-S3 — Binance Algo Order API migration for exit legs

Backlog story added by E4-F3-S2's follow-up entry above: Binance rejects
`STOP_MARKET`/`TAKE_PROFIT_MARKET` exit legs on `/fapi/v1/order` ("use the Algo
Order API endpoints instead"), so every crypto bracket order's protective legs
were failing after retry — correctly surfaced as `PARTIALLY_PROTECTED`, not
hidden, but not fixed either. This story migrates exit-leg placement/lookup to
the Algo Order API so both legs actually place again.

**Design gate.** Given this is live money-handling broker code (the same weight
E4-F3-S2's bracket construction, MARKET-only entries, and partial-failure design
were built under), ran a Plan-agent design pass before touching code. It read the
full `BinanceFuturesTradingAdapter`, `BinanceFuturesTradingAdapterTest`,
`BinanceFuturesTradingAdapterContractTest`, `BrokerAdapterContractTest`, and the
E4-F3-S2 follow-up's root-cause entry, then proposed a concrete migration: move
only `ensureExitLeg`'s placement and its "check-first" lookup to `POST`/`GET
/fapi/v1/algoOrder` (`algoType=CONDITIONAL`), leave the entry leg and
`cancelOrder` untouched, and flagged the whole endpoint shape as unverified since
this project's Binance integration targets a fictional/project-internal API
surface with no real docs to check against — the backlog AC itself (`docs/agile-plan.md`
line 147) specifies the target endpoint shape (`POST/GET/DELETE /fapi/v1/algoOrder`,
`algoType=CONDITIONAL`), which the plan treated as authoritative rather than
something to fact-check.

One real scope question came out of the design pass and was put to the user
directly rather than guessed: should the kill switch's cancel-all-open-orders
sweep also cancel exit legs via the new API, or stay scoped to the entry leg
like today? Answered: entry leg only — by the time exit legs exist, the entry
has already filled and is no longer cancelable, so `cancelOrder`'s job was
always about a still-resting entry, not position-flattening (matches E6-F2-S2's
already-documented "not broker-side position-flattening" scope). No new
cancellation path was built.

**Implementation.** `BinanceFuturesTradingAdapter`:
- New constants `ALGO_TYPE_CONDITIONAL` and `ALGO_ORDER_DOES_NOT_EXIST_CODE`
  (the latter assumed to match the entry endpoint's `-2013`, explicitly flagged
  as unverified — see below).
- New `findAlgoOrder` (the Algo Order API's `GET` equivalent of `findOrder`),
  and a new `BinanceAlgoOrderResponse` record (`algoId`/`algoStatus` in place of
  `orderId`/`status`; no `avgPrice` field since `compositeResult` never reads a
  fill price off an exit leg).
- `ensureExitLeg`'s check-first lookup and its `POST` both retargeted to
  `/fapi/v1/algoOrder`, with `stopPrice`→`triggerPrice` and
  `newClientOrderId`→`clientAlgoId` param renames, plus a new `algoType`
  param; `newOrderRespType` dropped (order-endpoint-specific, not carried over).
- `compositeResult`'s stop-loss/take-profit lookups switched from `findOrder`
  to `findAlgoOrder`; `isTriggered`/`isMissingProtection` retyped to
  `BinanceAlgoOrderResponse` and now read `algoStatus` against an invented
  vocabulary (`WORKING`/`FINISHED`/`CANCELLED`/`EXPIRED`/`REJECTED`) chosen to
  keep the surrounding FILLED/PARTIALLY_PROTECTED/CANCELLED branching
  structurally identical to before.
- Entry-leg placement/lookup (`placeEntryOrder`, the top-of-`placeOrder` check,
  `findOrder`), `cancelOrder`/`deleteOrder`, and `legIds`'s SHA-256 id scheme are
  all unchanged — confirmed via grep that nothing outside this one class
  (`OrderService`, `RetryingBrokerAdapter`, `KillSwitchService`) knows about
  Binance's endpoint shapes, so the migration's blast radius really is this one
  adapter.

**Tests.** `FakeBinanceFuturesTradingServer` (drives the shared
`BrokerAdapterContractTest` suite via `BinanceFuturesTradingAdapterContractTest`)
gained `POST`/`GET /fapi/v1/algoOrder` handlers backed by a new
`algoOrdersByClientAlgoId` map/`StoredAlgoOrder` record, alongside the existing
`/fapi/v1/order` handlers (now entry-only — `handlePlaceOrder` no longer branches
on order type, since only MARKET entries reach it anymore).
`BinanceFuturesTradingAdapterTest`: every exit-leg expectation across
`placeOrder_fullSuccess_setsLeveragePlacesEntryAndBothExitLegs`,
`placeOrder_highPrecisionQuantityAndPrices_truncatedToSymbolPrecisionBeforeSubmission`,
`placeOrder_takeProfitLegFailsAfterRetry_returnsPartiallyProtected`,
`placeOrder_repeatedClientOrderId_replaysExistingLegsWithoutReposting`,
`getOrderStatus_entryFilledBothLegsResting_returnsFilled`,
`getOrderStatus_missingExitLeg_returnsPartiallyProtected`,
`getOrderStatus_exitLegTriggered_returnsCancelled`, and
`cancelOrder_alreadyFilledAndProtected_isIdempotentNoOpWithoutDeleting` moved from
`/fapi/v1/order`/`stopPrice`/old-status-vocabulary to
`/fapi/v1/algoOrder`/`triggerPrice`/the new `algoStatus` vocabulary, via new
`algoOrderJson`/`expectAlgoOrderCheckNotFound` helpers mirroring the existing
`orderJson`/`expectOrderCheckNotFound`. Added one new test,
`getOrderStatus_exitLegNeverPlaced_returnsPartiallyProtected`, distinct from the
existing "missing exit leg" test (a leg that exists but shows a terminal
non-fill status) — this one exercises `findAlgoOrder` returning `null` via
`ALGO_ORDER_DOES_NOT_EXIST_CODE` for a leg that was never placed at all. All 402
backend tests pass (`./mvnw verify`), up from 401.

**Open gap, deliberately not closed in this pass.** The AC's "against the real
Binance Futures Testnet, not just mocks" requirement is not yet satisfied — no
live Binance account was available in this session, so `FakeBinanceFuturesTradingServer`
and the unit tests were built against the same assumed param names, response
shape, and status vocabulary the Plan agent proposed (all internally consistent,
none independently verified). The single highest-risk unverified value is
`ALGO_ORDER_DOES_NOT_EXIST_CODE`: if the real Algo Order API's "not found" error
code differs from `-2013`, `ensureExitLeg`'s check-first idempotency silently
breaks (a genuine "not yet placed" response gets treated as a fatal error, or a
real error gets swallowed as "safe to place"). Before this is trusted with real
paper-mode capital, a live testnet pass is still needed: submit one real bracket
order end-to-end, confirm both legs show protected (not just this app's own
status reporting — cross-check via a direct signed call to
`/fapi/v3/positionRisk`, same technique used in E4-F3-S2's follow-up), and
specifically GET with a bogus `clientAlgoId` first to capture the real
not-found error code before relying on the current placeholder.

## E4-F3-S3 follow-up — live verification against the real Binance Futures Testnet

The prior entry above shipped the Algo Order API migration with every param name,
response field name, status vocabulary value, and the not-found error code marked as
best-effort design, not verified facts, since this project's Binance integration
targets a fictional/project-internal API surface with no real docs to check against.
The user asked to close that gap directly against the real testnet rather than leave
it as a documented risk.

**Method.** Real, signed HTTP calls to `https://testnet.binancefuture.com` using the
`BINANCE_TRADING_API_KEY`/`BINANCE_TRADING_API_SECRET` already in `.env` (the same
credentials `BinanceFuturesTradingAdapter` uses), driven from a standalone bash+openssl
script rather than the full app — bypasses Oracle/Spring entirely, mirrors the "direct
signed call" cross-check technique already used in E4-F3-S2's follow-up. Proceeded in
stages, confirming with the user before each escalation in blast radius (read-only
account/lookup calls first, then one real order placement against an existing test
position, then one real trigger-and-fire):

1. `GET /fapi/v3/account` — confirmed the credentials are live and found a pre-existing
   open BTCUSDT short (`positionAmt: -0.0010`), left over from an earlier story's live
   verification run.
2. `GET /fapi/v1/algoOrder` with a bogus `clientAlgoId` — confirmed the endpoint exists
   at all, and returned `{"code":-2013,"msg":"Order does not exist."}`, identical to
   `/fapi/v1/order`'s own not-found response. `ALGO_ORDER_DOES_NOT_EXIST_CODE`'s
   placeholder value was correct on the first guess.
3. `POST /fapi/v1/algoOrder` with `symbol`/`side`/`algoType=CONDITIONAL`/`type=
   STOP_MARKET`/`triggerPrice`/`closePosition=true`/`clientAlgoId` (a real protective
   order against the existing short, trigger price set safely far from market so it
   wouldn't fire) — accepted. Response: `{"algoId":...,"clientAlgoId":...,
   "algoType":"CONDITIONAL","orderType":"STOP_MARKET",...,"algoStatus":"NEW",
   "triggerPrice":"96301.40",...}`. Every param name was accepted as-is; the response
   field names (`algoId`/`algoStatus`) matched the guess, but the resting status is
   `NEW`, not the guessed `WORKING`.
4. `GET`/`DELETE /fapi/v1/algoOrder` against that same order — GET matched the POST
   response shape; DELETE returned `{"algoId":...,"clientAlgoId":...,"code":"200",
   "msg":"success"}` and a follow-up GET showed `"algoStatus":"CANCELED"` (one L) —
   not the guessed `CANCELLED` (two L's).
5. A second real order, this time with a trigger price close enough to current market
   price to fire from natural price drift (confirmed first that an already-satisfied
   trigger is rejected outright with `-2021 "Order would immediately trigger."`,
   matching real Binance's standard STOP_MARKET safety check) — polled every 3s for
   ~2.5 minutes. It fired: `"algoStatus":"FINISHED"`, `"actualOrderId":"28280966309"`,
   `"actualPrice":"64155.600000"`, and the account's BTCUSDT position went flat
   (confirmed via a follow-up `GET /fapi/v3/positionRisk` returning `[]`) — closing out
   the leftover test position as a side effect, not a problem. `FINISHED` was the one
   status value guessed correctly on the first try.

**Fixed from these findings.** `BinanceFuturesTradingAdapter.isMissingProtection`'s
`"CANCELLED"` check corrected to `"CANCELED"`; `FakeBinanceFuturesTradingServer`'s
`handlePlaceAlgoOrder` now stores/returns `"NEW"` instead of `"WORKING"`;
`BinanceFuturesTradingAdapterTest`'s exit-leg fixtures across all affected tests moved
from `"WORKING"`/`"CANCELLED"` to `"NEW"`/`"CANCELED"`. `isTriggered`'s `"FINISHED"`
check, every request param name, both response field names, and
`ALGO_ORDER_DOES_NOT_EXIST_CODE`'s `-2013` value needed no changes — confirmed correct
as originally coded. The class Javadoc's E4-F3-S3 paragraph, and CLAUDE.md's E4-F3-S3
status entry, were updated to state these as live-confirmed facts rather than
unverified assumptions. All 402 backend tests still pass (`./mvnw verify`) after the
fix — same count as before, since this was a values-only correction, not a new test.

**Scope note.** This verification exercised the exit-leg placement/lookup/cancel path
directly (steps 2-5 above), not a full `OrderService.submitOrder` round trip through
the running Spring Boot app — the app itself wasn't started for this pass (no Oracle
container was brought up). The adapter-level behavior this story is actually about
(the Algo Order API's real shape) is now proven against the live testnet; a
full-stack, entry-plus-both-legs bracket order through the running dashboard remains
untested end-to-end, matching the scope of what was asked.

## E4-F3-S3 second follow-up — full-stack verification through the running app

The previous follow-up entry above verified the Algo Order API migration directly
against Binance (bypassing Oracle/Spring), and flagged one remaining scope gap: nobody
had driven a full `OrderService.submitOrder` round trip through the actual running
dashboard for this migration. The user asked to close that gap too.

**Method.** Brought up the real stack: Oracle XE was already running; found a stale
backend process on :8080 that predated the vocabulary fix from the prior follow-up
(started before the `NEW`/`CANCELED` correction), killed it, and started a fresh
`./mvnw spring-boot:run` with `.env`'s vars exported (`ORACLE_APP_USER_PASSWORD`,
`BINANCE_TRADING_API_KEY`/`SECRET`, etc. — local profile, no `spring-dotenv`
dependency, so vars must be exported into the shell before launch). Confirmed via the
process start time and `Started BackendApplication` log line that this was a genuinely
fresh process, not the stale-port situation this repo's known gotchas warn about.
Logged into the already-running frontend (`localhost:5173`) as `admin` through the
browser (not a direct API call, to exercise the real login/CSRF/session path too),
opened the existing BTCUSDT watchlist entry (signal was SELL at the time), and
submitted a real bracket order through the actual `TradeForm` UI: $100, 1x leverage,
take-profit 60000, stop-loss 68000 (correct SELL-side placement — profit below entry,
stop above). First attempt at $50 was correctly rejected client-side before any broker
call (`quantity rounds to zero at BTCUSDT's precision` — the same guard from E4-F3-S2's
original follow-up), confirming that pre-flight check still works; $100 cleared it.

**Result.** The order came back `ORDER_FILLED` (confirmed via the dashboard's
notification panel), not `PARTIALLY_PROTECTED` — entry filled at $64,062, broker order
id `28285677397`. Cross-checked directly against Binance (bypassing the app) via
`GET /fapi/v1/openAlgoOrders?symbol=BTCUSDT`: both protective legs were genuinely
resting — `TAKE_PROFIT_MARKET` at `triggerPrice=60000`, `STOP_MARKET` at
`triggerPrice=68000`, both with `algoStatus=NEW` and `clientAlgoId`s correctly derived
from the entry's `clientOrderId` (`...-T`/`...-S` suffixes, matching `legIds`' scheme).
This is the real proof the story's AC asked for: `OrderService.submitOrder` genuinely
drives `BinanceFuturesTradingAdapter`'s new Algo Order API path correctly end-to-end,
not just the adapter in isolation against direct signed calls.

**Cleanup.** Cancelled both algo legs (`DELETE /fapi/v1/algoOrder` for each
`clientAlgoId`) and flattened the resulting position with a reduce-only market BUY,
confirmed flat via a final `GET /fapi/v3/positionRisk` returning `[]` — no dangling
test position or orders left on the account. No code changes this pass — this was
verification only, closing the scope note flagged in the prior follow-up entry.

## Tabbed high-fidelity dashboard redesign

Requested as "the UI looks too simple, make it a high-fidelity trading dashboard,
split into tabs" — not tied to a specific story. Second UI pass on top of the earlier
"Frontend visual pass" (design tokens, card treatment); that pass fixed the app-shell
basics, this one restructures layout and adds trading-terminal-style visual detail.

**Layout restructuring** (`DashboardPage.tsx`, new `layout/Tabs.tsx`): the previous
single scrolling column (toolbar, notifications, watchlist, ticker lookup, order
history all stacked) is now a persistent header, a persistent safety-critical status
strip (`TradingModeBanner` + `KillSwitchControl` — deliberately kept outside the tabs
so they stay visible no matter which tab is active, since they're global state that
affects every trade), a sticky sidebar (`Watchlist`, always visible — a real trading
terminal keeps the watchlist reachable regardless of what else is on screen), and a
tabbed main content area (Trade / Orders / Notifications) holding `TickerMetrics`,
`OrderHistory`, and `NotificationPanel` respectively — none of those three components'
internals changed, they're rendered exactly as before, just relocated into tab panels.
Clicking a watchlist symbol now also switches to the Trade tab
(`handleWatchlistSelect`) so the lookup it triggers is immediately visible from any
tab. Tab panels use `hidden`, not conditional rendering, so switching tabs never loses
in-progress state (a partially-typed symbol, a pending order refresh) in the panel
left behind — and all three panels' components mount immediately on page load same as
before (no fetch-on-tab-activation change), so there's no new loading state to handle.

**Bug found via live-verification, not just build-passing**: the first version of
`.app-tab-panel { display: flex }` silently defeated the `hidden` attribute — `[hidden]`
and `.app-tab-panel` are equal specificity (0,1,0 each), and the class rule came later
in the stylesheet, so all three tab panels rendered simultaneously regardless of which
tab was "active" (confirmed by screenshot: Order history table rendering directly
under the Trade tab's ticker lookup). Fixed with an explicit
`.app-tab-panel[hidden] { display: none }` override placed after the base rule.
Live-verified after the fix: Trade/Orders/Notifications each show only their own
content, watchlist-click correctly jumps to and populates the Trade tab.

**Visual detail** (`index.css`, plus small `TickerMetrics.tsx` additions): stat tiles
gained a `tone` prop (price/momentum/volatility/volume) driving a left-border accent
color, so the metrics grid groups by kind at a glance without a legend; the signal
badge gained a directional glyph (▲ BUY / ▼ SELL / ■ HOLD) ahead of the call text; the
order-history table gained a sticky header, zebra-striped rows, a row hover tint, and
a colored status dot (`::before`, `background: currentColor`, reusing the existing
success/warning/error/neutral tone classes — no new color values); the watchlist
sidebar items get an accent border on hover. New tokens (`--color-surface-sunken`,
`--color-accent-soft`, `--color-buy/sell/momentum/volume`, `--sidebar-width`,
`--radius-lg`) were derived from colors already used elsewhere in the file (the
existing signal-badge/chart-palette hues), not invented fresh. Sidebar collapses above
the tab content under a 900px breakpoint.

No backend changes. `npm run build` (typecheck), `npm run lint`, and `npm test` all
pass unchanged. Live-verified in the running app (existing dev stack, not a fresh
boot): Trade tab (ticker lookup → tone-coded stat tiles → SELL glyph badge → trade
form → price chart, all inside one card), Orders tab (zebra table, status dots), and
Notifications tab, plus the watchlist-click-jumps-to-Trade interaction. Did not
re-verify the sub-900px stacked layout live (window-resize automation didn't visibly
change the captured viewport in this session) — the breakpoint itself is a standard,
low-risk `grid-template-columns` collapse.

## Dark-first premium visual pass

The user's reaction to the tabbed redesign above was blunt: "UI still looks very
amateur." Rather than iterate on the same light pastel-blue admin-panel look again,
asked the user to pick a direction up front (dark trading-terminal vs. polished light
SaaS vs. a dark-first hybrid with a light toggle) before touching any code, since
redoing a full visual pass twice on a subjective judgment call would be wasteful. User
picked the dark-first hybrid.

**Theme system.** New `theme/ThemeContext.tsx`: `Theme = 'dark' | 'light'`, default
`'dark'`, persisted to `localStorage` (`autotrade-theme`), sets `data-theme` on
`documentElement`. This replaces the previous approach entirely — the old `index.css`
used `light-dark()` CSS values keyed off `prefers-color-scheme`, which meant the theme
was inferred from OS setting with no in-app override. A theme now has to be an
explicit, user-settable choice for a toggle to make sense, so every token moved to a
plain `:root` (dark defaults) block plus a `:root[data-theme="light"]` override block.
To avoid a flash of the wrong theme between HTML parse and React mount, `index.html`
got a small inline `<script>` that reads `localStorage` and sets `data-theme` before
first paint — `ThemeProvider` takes over from there. New `layout/ThemeToggle.tsx`
(inline sun/moon SVGs, no new icon dependency) sits in the dashboard header and on the
login page (the only two places with anywhere to put it).

**Visual language.** `index.css` rewritten: deep navy/charcoal surfaces
(`--color-surface: #12151d` etc.) over a subtle radial-gradient body background, a
brighter electric-blue accent (`--color-accent: #4c8dff`) with gradient-filled primary
buttons and glow-tinted shadows, a `--font-mono` stack (`ui-monospace`/Cascadia
Mono/Consolas) applied to every numeric display — stat-tile values, chart-adjacent
prices, the order-history table's numeric columns, the trade-confirm dialog's summary
— which is the single detail that reads most as "trading terminal" rather than "CRUD
form" at a glance. Buy/sell/momentum/volume hues carried over unchanged from the prior
palette (still colorblind-safe teal/orange, not red/green) since that constraint was
already correct, just re-expressed as explicit tokens instead of `light-dark()` pairs.
Light theme is a full parallel token set, not an afterthought bolted on — verified
side-by-side, not just "does it still render."

**Two real bugs fixed along the way, not just restyled:**

1. `.app-status-strip`'s cleared-kill-switch and paper-mode-banner states rendered as
   a single bare button floating in a mostly-empty card — `flex: 1 1 16rem` gave both
   cards roughly half the header's width, but neither had enough content to fill it.
   Screenshotting the running app before touching any CSS is what surfaced this (the
   prior redesign's own live-verification screenshots technically showed it too, just
   not flagged). Fixed by restructuring both `KillSwitchControl.tsx` and
   `TradingModeBanner.tsx` to a shared icon + label + state-pill + meta-text +
   right-aligned-action layout, so the idle/cleared state now reads as a deliberate
   compact status card instead of an empty box with a button in the corner.
2. `TradeForm.tsx` ran `validateTradeForm` unconditionally on every render and showed
   every resulting error immediately — a freshly opened, completely untouched form
   displayed "Enter an amount greater than 0" / "Enter a take-profit price greater
   than 0" etc. before the user had typed a single character. Fixed with per-field
   `touched` state (set via a new `onBlur` handler) plus a `submitAttempted` flag; an
   error now only renders once its field has been blurred or a submit was attempted.
   `validation.ts`'s actual validation logic is untouched — this was purely a display-
   timing bug in `TradeForm.tsx`.

**Chart theme-awareness.** `chart/palette.ts`'s `isDarkMode()` used to read
`window.matchMedia('(prefers-color-scheme: dark)')` — now reads
`document.documentElement.dataset.theme !== 'light'` to match the app's own explicit
theme instead of OS preference (consistent with the `light-dark()` removal above).
`PriceChart.tsx` now takes `theme` from `useTheme()` as an explicit `useEffect`
dependency alongside `candles`/`indicators` — without it, toggling the theme mid-view
wouldn't rebuild the chart until the next ticker lookup, since `currentPalette()` is
read once inside the effect, not reactively.

**Verification.** `npm run build` (typecheck + vite build), `npm run lint` (oxlint —
same one pre-existing `only-export-components` warning as `AuthContext.tsx`, no new
warnings), and `npm test` (13/13 passing) all clean. Live-verified in the actual
running dev stack (both frontend and backend already up, not a fresh boot) via
browser automation: full login → dashboard flow in both themes, all three tabs
(Trade with a populated BTCUSDT lookup — stat tiles, SELL signal badge, trade form
with no eager validation errors, price chart re-themed correctly; Orders with its
status pills; Notifications), the theme toggle in both the header and login page,
and theme persistence surviving a real logout/login round trip (confirmed against
the actual `DASHBOARD_PASSWORD` from `.env`, not a mocked session). No backend
changes.

## E2-F4-S2: backtest per-branch expectancy, not just win rate

**Why this story exists.** A signal-quality assessment run this turn (rereading
E2-F4-S1's printed backtest report) found the MAJORITY-branch win rates clustering
right around a coin flip — 42.8-57.9% across BTCUSDT/DOGEUSDT's four directional
branches. A win rate that close to 50/50 tells you almost nothing about whether the
branch is actually worth trusting with capital: a 45% win rate can still be
profitable if wins run bigger than losses on average, and a 55% win rate can still
lose money if losses run bigger than wins. E2-F4-S1 never measured payoff size at
all, only the WIN/LOSS/WASH classification — so there was no way to answer that
question from the existing report. Scoped as its own backlog story rather than
folded into a rule-table redesign, since the AC is purely "measure and report",
not "change the rule table based on what's measured" — that decision (confidence-
weighted voting, a regime filter, re-tuning the deadband) is explicitly left for
later, once there's real expectancy evidence to decide from.

**What changed, mechanically.** `BacktestHarness`'s private `score()` used to
classify a forward move into `DirectionalOutcome.WIN`/`LOSS`/`WASH` and discard the
actual percent move once classified — the number was computed, compared against
`BacktestConfig.WIN_LOSS_DEADBAND_PCT`, then thrown away. Introduced
`DirectionalScoreResult(DirectionalOutcome outcome, BigDecimal signedReturnPct)` so
the signed return survives past classification. `DirectionalAccumulator` (already
tracking win/loss/wash/notScored counts per `Checkpoint`) gained two more
per-checkpoint `BigDecimal` running sums — `winReturnSums`/`lossReturnSums` — fed
only when the outcome is WIN or LOSS respectively (WASH contributes to neither sum,
matching the deadband's own "too small to call a real win or loss" semantics).
`CheckpointStats` gained `avgWinReturnPct`/`avgLossReturnPct` (simple mean of each
sum divided by its count, 0.0 when that count is 0) and a derived `expectancyPct()`
— `(avgWinReturnPct * win + avgLossReturnPct * loss) / scored()`, i.e. the
win-rate-weighted expected return per call with WASH scored as a flat zero, which
is the actual "is this branch worth trusting with capital" number a win rate alone
can't produce.

**The one non-mechanical decision: how to roll up UNANIMOUS+MAJORITY.**
`BacktestHarness.combineCheckpoint()` already existed to merge each directional
rule's `CheckpointStats` into the report's "Overall BUY"/"Overall SELL" rows
(E2-F4-S1). For the plain counts (win/loss/wash/notScored) a simple sum was always
correct and still is. But `avgWinReturnPct`/`avgLossReturnPct` are themselves
averages — summing two averages is wrong, and naively averaging the two branches'
`avgWinReturnPct` values equally would silently misweight a 5-call branch the same
as a 150-call branch. Changed to a call-count-weighted average:
`(a.avgWinReturnPct * a.win + b.avgWinReturnPct * b.win) / (a.win + b.win)`, and
symmetrically for losses — this is the correct combination since each branch's
`avgWinReturnPct` is already itself an arithmetic mean over `a.win`/`b.win`
observations, so weighting by those same counts reconstructs the true combined
mean rather than an average-of-averages.

**Report formatting.** The old single-line-per-rule win-rate row (`min X% mid Y%
max Z%`) couldn't fit three more numbers per checkpoint without becoming
unreadable, so `BacktestReport.printDirectional` now prints one line per
checkpoint under each rule's name (`avg win`/`avg loss`/`expectancy`, signed and
percent-formatted) instead of cramming all three checkpoints onto one line. Purely
a `printTo` presentation change — `BacktestReport`'s record shape (fields, not
methods) is untouched, so nothing that consumes the record itself needed to
change.

**Test coverage.** `BacktestHarnessTest` already asserted structural invariants
only (decision-point counts reconcile across buckets) per E2-F4-S1's own
precedent — the win rate itself is evidence under review, not a regression target,
and the same reasoning extends to expectancy. Added
`assertExpectancySignsAreSane`: for every directional rule (plus the Overall
BUY/SELL roll-ups) and every checkpoint, asserts `avgWinReturnPct > 0` whenever
`win > 0` and `avgLossReturnPct < 0` whenever `loss > 0`. This is a true structural
invariant, not a disguised value assertion — a WIN classification requires
`signedReturnPct` to exceed the positive deadband, and a LOSS requires it to be
below the negative deadband, so an average built only from strictly-positive (or
strictly-negative) inputs mathematically cannot land on the wrong side of zero;
the test would only fail if the sign-tracking logic itself broke (e.g. a WIN
return accidentally summed into `lossReturnSums`), which is exactly the kind of
bug this class of test exists to catch.

**The actual finding, read off the real fixture data** (`./mvnw test
-Dtest=BacktestHarnessTest`, printed report, not asserted — per this story's own
scope, the numbers are evidence to review, not a target to enforce):

- BTCUSDT: both `BULLISH_MAJORITY` and `BEARISH_MAJORITY` are expectancy-positive
  at all three checkpoints (e.g. BULLISH_MAJORITY: +0.59%/+2.01%/+3.51% at
  min/mid/max; BEARISH_MAJORITY: +0.39%/+0.98%/+0.88%) — win rates in the
  48-58% range, but wins consistently outrun losses in size, so the branches
  are worth trusting on this evidence.
- DOGEUSDT: `BULLISH_MAJORITY` is expectancy-positive throughout (win rate
  46.3-49.6%, expectancy +0.03%/+0.30%/+0.22%), but `BEARISH_MAJORITY` is
  expectancy-**negative** at the min and mid checkpoints (-0.087%, -0.305%)
  despite a win rate (42.8%, 40.7%) that looks merely mediocre rather than
  alarming — only recovering to slightly positive (+0.029%) at the max
  checkpoint. This is exactly the failure mode E2-F4-S1's win-rate-only report
  couldn't surface: a coin-flip-adjacent win rate hiding a branch where losses
  run bigger than wins.

No `SignalRuleId`/`SignalRuleEngine`/`HoldTermCalculator` changes — this story is
diagnostic-only, per its own AC. `./mvnw test` (full backend suite) passes clean.
No frontend changes. The DOGEUSDT `BEARISH_MAJORITY` finding is left as an open
question for a future story (confidence-weighted voting, a regime filter, or a
deadband/threshold re-tune) rather than acted on here.
