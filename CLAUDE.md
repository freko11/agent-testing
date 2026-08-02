# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this
repository. It's kept intentionally thin — current status, commands, architecture, and
recurring gotchas only. For the full build history (design-gate rationale, bugs found and
fixed, live-verification notes) per story, see **`docs/CHANGELOG.md`**. For the original
epic/feature/story plan, see **`docs/agile-plan.md`**.

## What this is

An auto-trade signal dashboard: a React frontend and Java/Spring Boot backend, backed by
Oracle Database (local Oracle XE via Docker Compose), that pulls market data (Alpaca for
stocks, Binance for crypto), computes technical-indicator-based Buy/Sell/Hold signals, and
submits bracket orders through broker adapters (Alpaca paper trading, Binance Futures
Testnet) with risk/safety guardrails (E6).

## Status

All stories below are done unless noted. Current next-up story: **E6-F1-S2** (paper-trade
threshold before live mode unlocks).

### E1 — Platform Foundation
- E1-F1-S1: Local Oracle XE via Docker Compose
- E1-F1-S2: Spring Boot backend skeleton
- E1-F1-S3: React app skeleton
- E1-F1-S4: CI pipeline (GitHub Actions, branch protection)
- E1-F1-S5: Env/config profiles (local/paper/prod)
- E1-F2: Core data model (schema, JPA entities/repos, Flyway as schema source of truth)
- E1-F3-S1: Broker-credential key rotation (keyring)
- E1-F3-S2: Dashboard login (session-cookie auth, CSRF)
- E1-F4-S1: Mock-broker E2E test (ticker → signal → order)

### E2 — Signal Engine
- E2-F1-S1: Ticker price-history ingestion (Alpaca/Binance)
- E2-F1-S2: Clear error for an unregistered ticker
- E2-F1-S3: Market-hours handling
- E2-F2-S1: RSI, MACD, moving-average crossover
- E2-F2-S2: Volatility (ATR%) / volume-trend metric
- E2-F3-S1: Indicators combined into a Buy/Sell/Hold call (rule engine)
- E2-F3-S2: Suggested hold-term alongside the call
- E2-F4-S1: Backtest harness

### E3 — Dashboard/Frontend
- E3-F1-S1: Ticker lookup + metrics display
- E3-F1-S2: Buy/Sell/Hold badge + hold-term, color-coded
- E3-F2-S1: Price chart with MA/RSI overlays (`lightweight-charts`)
- E3-F3-S1: Watchlist

### E4 — Broker Adapter Layer
- E4-F1-S1: `BrokerAdapter` interface + mock implementation + shared contract test suite
- E4-F1-S2: Rate-limit/retry/backoff built into the adapter contract
- E4-F1-S3: Outage handling + duplicate-order prevention
- E4-F2-S1: Alpaca paper account connected; `getAccountStatus`
- E4-F2-S2: Place a market order via Alpaca
- E4-F3-S1: Binance Futures Testnet account connected; `getAccountStatus`
- E4-F3-S2: Place a leveraged order via Binance testnet

### E5 — Auto-Trade Execution
- E5-F1-S1: Trade input form (amount, leverage, take-profit, stop-loss)
- E5-F1-S2: Hide/default fields by asset type
- E5-F2-S1: Bracket-order construction/submission through the `BrokerAdapter` layer
- E5-F2-S2: Explicit confirmation step before the order fires
- E5-F3-S1: Order status/history (manual refresh)
- E5-F3-S2: CSV export of trade history
- E5-F4-S1: In-app notifications (order outcomes, watchlist signal changes)

### E6 — Risk & Safety Controls (in progress)
- E6-F1-S1: Global paper/live trading-mode switch (live mode still gated —
  `switchTo(LIVE)` unconditionally throws until E6-F1-S2/S3 land)

## Build / lint / test

- **DB**: `docker compose up -d` (Oracle XE; first boot ~60-90s, poll with
  `docker compose ps`). Credentials/ports from `.env` (see `.env.example`).
- **Backend** (`backend/`, Java 21, Spring Boot 4.1): `./mvnw spring-boot:run` to run
  (needs `ORACLE_APP_USER_PASSWORD` env var matching `.env`); `./mvnw verify` to
  build+test (runs against in-memory H2 in Oracle-compatibility mode, no Docker
  needed). Single test: `./mvnw test -Dtest=ClassName#methodName`.
- **Frontend** (`frontend/`, Vite + React 19 + TypeScript): `npm install && npm run dev`
  to run; `npm run build` (typecheck + build), `npm run lint` (oxlint), `npm test`
  (Vitest) to verify.
- Switch backend profile: `SPRING_PROFILES_ACTIVE=paper` env var or
  `-Dspring-boot.run.profiles=paper` (mvnw). Profiles: `local` (default), `paper`,
  `prod` (no defaults — every value must come from the environment).

## Architecture

**Backend** packages under `com.autotrade.dashboard`:
- `ticker` — ticker registration (`POST /api/tickers`, asset type is explicit, no
  symbol-shape heuristics).
- `marketdata` — `MarketDataClient` (Alpaca/Binance read-only candles),
  `MarketDataService` (routes by asset type), `MarketHoursService` (hardcoded
  NYSE/NASDAQ calendar), `RetryHelper` (one bounded retry).
- `indicator` — hand-rolled `RsiCalculator`/`MacdCalculator`/
  `MovingAverageCrossoverCalculator`/`VolatilityCalculator`/`VolumeTrendCalculator`
  (pure static, no library — see `docs/CHANGELOG.md` E2-F2-S1 for why), plus
  `IndicatorService`/`IndicatorSnapshot` persistence.
- `signal` — `SignalRuleEngine` (versioned rule table, safety gates + 2-of-3
  directional vote → BUY/SELL/HOLD), `HoldTermCalculator` (versioned day-range
  table), `SignalCallEntry` audit log.
- `broker` — `BrokerCredentialService`/`CredentialEncryptionService` (keyring-based
  rotation), per-broker credential bootstraps from env vars.
- `brokeradapter` — `BrokerAdapter` interface, `RetryingBrokerAdapter` decorator
  (retry/backoff/outage-reconciliation), `AlpacaTradingAdapter`,
  `BinanceFuturesTradingAdapter`, `BrokerAdapterRouter` (routes by asset type).
- `order` — `OrderService` (bracket-order submission, re-derives signal
  server-side, never trusts client-cached data), status polling, CSV export.
- `notification` — `WatchlistSignalPoller` (scheduled job, the app's first
  background task), `NotificationService`.
- `tradingmode` — `TradingModeService` (append-only `trading_mode_events`,
  latest row = current mode; `LIVE` gated until E6-F1-S2/S3).
- `watchlist`, `security` (session auth), `common` (`Clock`/`SchedulingConfig`).
- Schema: Flyway `V1`–`V10` under `backend/src/main/resources/db/migration/` is the
  single source of truth; `spring.jpa.hibernate.ddl-auto=validate` everywhere.

**Frontend** (`frontend/src/`) mirrors the backend split: `marketdata`, `signal`,
`chart`, `trade`, `order`, `watchlist`, `notification`, `tradingmode`, `auth`. Each
domain has its own `api.ts` (typed fetch wrapper, shared `MarketDataError` parsing)
and one or two components wired into `DashboardPage`.

Project-specific subagents live in `.claude/agents/` (`Plan`, `Explore`,
`general-purpose`); project-specific skills in `.claude/skills/` (`run`, `dataviz`,
`simplify`, `security-review`, `signal-rule-review`, `adapter-contract-check`,
`guardrail-check`).

## Known recurring gotchas

Real bugs hit more than once across stories — check these before assuming something
new is broken. Full detail/original story for each is in `docs/CHANGELOG.md`.

- **Windows loses the executable bit on wrapper scripts** (`mvnw`, etc.) — check
  `git ls-files -s <path>` reads `100755` before committing; fix with
  `git update-index --chmod=+x <path>`.
- **Oracle column-clause ordering**: `DEFAULT x NOT NULL`, never
  `NOT NULL DEFAULT x` (H2's Oracle mode accepts the wrong order; real Oracle
  rejects with `ORA-00907`).
- **Oracle rejects a redundant `MODIFY ... NOT NULL`** on a column that's already
  `NOT NULL` (`ORA-01442`) — H2 silently allows it. Drop the `NOT NULL` clause if
  only the `DEFAULT` is actually changing.
- **Failed migration cleanup**: a failed migration leaves a `success=0` row in
  `flyway_schema_history` on real Oracle (H2 doesn't persist this across
  restarts) — delete it before retrying. Oracle DDL auto-commits per statement,
  so objects created before the failure point (e.g. a sequence) can survive a
  rolled-back `CREATE TABLE` and need manual cleanup too.
- **`Instant` columns need `@JdbcTypeCode(SqlTypes.TIMESTAMP)`** when mapped to a
  plain `TIMESTAMP(6)` (no timezone) column, or real Oracle throws
  `ORA-18716` on read (H2 doesn't catch this).
- **Plain `Integer`/`int` columns need `@JdbcTypeCode(SqlTypes.NUMERIC)`** (with
  matching `precision`/`scale`) to match a `NUMBER(n)` column, or Hibernate schema
  validation fails (`found [numeric], but expecting [integer]`).
- **Widening a CHECK constraint on Oracle** needs drop-then-recreate, not a direct
  `ALTER ... MODIFY`.
- **Oracle reserved words** (e.g. `MODE`) can't be column names — fails with
  `ORA-00904` on real Oracle, not caught by H2.
- **Stale `java`/`node` processes** from earlier sessions routinely hold ports
  8080/5173 across restarts — check `netstat -ano | grep :8080` (or `:5173`)
  before trusting a live verification, especially if `/health` returns 200
  suspiciously fast.
- **CSRF cookie rotates after login** — a request immediately after login needs
  `GET /api/auth/me` first to re-prime the cookie.
- **`@MockitoSpyBean` vs `@MockitoBean`**: if a bean's constructor eagerly calls a
  method on an injected dependency at context-refresh time (e.g.
  `MarketDataService` calling `supportedAssetType()`), a plain `@MockitoBean`
  returns null from that unstubbed call and silently corrupts wiring — use
  `@MockitoSpyBean` and stub only the specific method needed.
- **Lazy JPA associations outside their transaction**
  (`LazyInitializationException`) — a `@ManyToOne` lazy field touched after its
  `@Transactional` method has returned fails in real usage (same-transaction test
  styles won't reproduce it); use a real join-fetch query returning fully-loaded
  entities instead.
- **Binance signed requests**: never rebuild the query string via
  `UriBuilder`/`UriComponentsBuilder` after signing — re-encoding breaks the HMAC
  signature. Send the literal, pre-built query string.

## Mandatory workflow

Every change to the codebase, no matter how small, must:
1. Update the **Status** section of this CLAUDE.md (one line per story/feature) to
   reflect the change, and append a full narrative entry (design-gate rationale,
   bugs found/fixed, live-verification notes) to `docs/CHANGELOG.md` — keep
   CLAUDE.md itself limited to current-state facts (status, commands,
   architecture, recurring gotchas), not per-story prose.
2. Be committed to git immediately after — do not batch multiple unrelated changes into one commit, and do not leave changes uncommitted.
3. Use a meaningful commit message that describes what actually changed and why — no generic messages like "update", "fix", or "changes".
4. Be pushed to the remote (`origin`) immediately after committing — do not leave commits sitting local-only.

This applies to every edit session: if files change, CLAUDE.md and docs/CHANGELOG.md
change, a git commit, and a push to origin all follow in the same turn.
