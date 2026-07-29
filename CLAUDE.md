# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Status

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

E1-F1-S2 (Spring Boot backend skeleton) is done. `backend/` is a Maven project
(Java 21, Spring Boot 4.1, package `com.autotrade.dashboard`) with `web`, `actuator`,
`data-jpa`, `validation` starters plus the `ojdbc11` Oracle driver. Run it with
`./mvnw spring-boot:run` from `backend/` (requires `ORACLE_APP_USER_PASSWORD` in the
environment, matching `.env`'s app-user password — Spring Boot reads env vars via
relaxed binding, no extra wiring needed). `/health` (actuator remapped to web
base-path `/`) returns 200 once the Oracle XE container from S1 is up, since the
datasource is already pointed at it (`jdbc:oracle:thin:@//localhost:${ORACLE_HOST_PORT:1522}/XEPDB1`)
— no JPA entities/repositories yet, that's F1.2. Build/test: `./mvnw verify`.

E1-F1-S3 (React app skeleton) is done. `frontend/` is a Vite + React 19 + TypeScript
app with `react-router-dom`, one placeholder route (`/` → `DashboardPage`). Run with
`npm install && npm run dev` from `frontend/`; build with `npm run build`.

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

E1 (Platform Foundation) is now fully closed out — every story across F1.1
through F1.4 is done.

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
story. E4-F3-S2 (place a leveraged order via Binance testnet) is next, and
can now build `placeOrder`/`getOrderStatus`/`cancelOrder` on top of this
story's signing/error-classification/config plumbing.

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

When code is added to this repository, update this file with:
- Build, lint, and test commands (including how to run a single test)
- High-level architecture and project structure

## Mandatory workflow

Every change to the codebase, no matter how small, must:
1. Update this CLAUDE.md file to reflect the change (new commands, architecture shifts, updated status, etc.).
2. Be committed to git immediately after — do not batch multiple unrelated changes into one commit, and do not leave changes uncommitted.
3. Use a meaningful commit message that describes what actually changed and why — no generic messages like "update", "fix", or "changes".
4. Be pushed to the remote (`origin`) immediately after committing — do not leave commits sitting local-only.

This applies to every edit session: if files change, CLAUDE.md changes, a git commit, and a push to origin all follow in the same turn.
