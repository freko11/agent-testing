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

All stories below are done, including E6 (Risk & Safety Controls) and E7
(Observability & Hardening), except E4-F3-S3 (Binance Algo Order API
migration), added to `docs/agile-plan.md` as backlog after E4-F3-S2's
post-launch verification found it — see that story's entry below.

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
- Frontend visual pass: design tokens (`index.css` `:root` custom
  properties — surface/border/text/accent/danger colors, radius, shadow,
  reusing the existing light-dark() palette), a sticky `.app-header` +
  centered `.app-main` content column, every top-level dashboard `<section>`
  styled uniformly as a card via the `.app-main > section` structural
  selector (no per-component className needed), consistent button/input/
  fieldset styling, and a centered `.login-card` for the login page. Found
  and fixed a real gap while at it: `KillSwitchControl.tsx` referenced
  `.kill-switch*` classes that had zero CSS at all, so the kill switch — one
  of the most safety-critical controls in the app — rendered as unstyled
  text. See `docs/CHANGELOG.md` for the specificity bug hit while fixing
  that (`.kill-switch button`'s contrast-background rule outranked
  `.kill-switch__engage`'s red one, producing invisible white-on-white
  text) and how it was fixed.
- E3-F1-S1: Ticker lookup + metrics display. The lookup form includes a
  Stock/Crypto asset-type radio selector — since a symbol's asset type can't
  be inferred from its shape (per E1-F1-S1's ticker-registration
  convention), the form now registers/resolves the ticker under the
  selected type (`POST /api/tickers`, idempotent for a matching type) before
  fetching its signal, rather than requiring pre-registration via the API
  with no frontend path. A watchlist revisit skips this — its asset type is
  already fixed by the existing registration — and looks up directly as
  before. Selecting the wrong type for an already-registered symbol surfaces
  `ASSET_TYPE_CONFLICT`'s backend message rather than silently relabeling
  it.
- E3-F1-S2: Buy/Sell/Hold badge + hold-term, color-coded
- E3-F2-S1: Price chart with MA/RSI overlays (`lightweight-charts`). Stock
  charts no longer hard-block when the market is closed: `IndicatorService`
  caches the last successful `getChartData` response per ticker in-memory
  and, on `MarketClosedException`, serves it back with `stale=true` instead
  of propagating the 409 — Alpaca's candles are daily bars, so a fetch made
  while the market was open is still legitimate data once it closes, just
  not fresh. Only the first-ever request for a ticker after a closed-market
  start (no prior cache entry) still 409s `MARKET_CLOSED`, unchanged from
  before. Deliberately scoped to this read-only chart endpoint only —
  `/signal` and `/indicators` (which feed trading decisions) keep the
  original hard market-hours gate untouched. Frontend: `ChartDataResponse`
  gained a `stale` field; `TickerMetrics.tsx` shows a "market is closed,
  showing last available data as of &lt;timestamp&gt;" note above the chart
  when set, reusing the existing `.chart-note` style.
- E3-F3-S1: Watchlist

### E4 — Broker Adapter Layer
- E4-F1-S1: `BrokerAdapter` interface + mock implementation + shared contract test suite
- E4-F1-S2: Rate-limit/retry/backoff built into the adapter contract
- E4-F1-S3: Outage handling + duplicate-order prevention
- E4-F2-S1: Alpaca paper account connected; `getAccountStatus`
- E4-F2-S2: Place a market order via Alpaca
- E4-F3-S1: Binance Futures Testnet account connected; `getAccountStatus`
- E4-F3-S2: Place a leveraged order via Binance testnet. Follow-up: order
  quantity/take-profit/stop-loss prices are now truncated to each symbol's
  known Binance precision before submission (`BinanceFuturesTradingAdapter`'s
  `QUANTITY_PRECISION`/`PRICE_PRECISION` maps) — found live when a real paper
  order was rejected with Binance's `-1111 "Precision is over the maximum
  defined for this asset"`. Known open gap found by the same live run, not
  yet fixed: Binance now rejects `STOP_MARKET`/`TAKE_PROFIT_MARKET` exit legs
  with "use the Algo Order API endpoints instead" — an apparent breaking API
  change since this story was built, so every crypto bracket order's
  protective legs currently fail after retry (correctly surfaced as
  `PARTIALLY_PROTECTED`, not hidden). See `docs/CHANGELOG.md` for detail.
  Tracked as backlog story E4-F3-S3 in `docs/agile-plan.md` (not yet
  implemented — scoped, not built).

### E5 — Auto-Trade Execution
- E5-F1-S1: Trade input form (amount, leverage, take-profit, stop-loss)
- E5-F1-S2: Hide/default fields by asset type
- E5-F2-S1: Bracket-order construction/submission through the `BrokerAdapter` layer
- E5-F2-S2: Explicit confirmation step before the order fires
- E5-F3-S1: Order status/history (manual refresh)
- E5-F3-S2: CSV export of trade history
- E5-F4-S1: In-app notifications (order outcomes, watchlist signal changes)

### E6 — Risk & Safety Controls (in progress)
- E6-F1-S1: Global paper/live trading-mode switch
- E6-F1-S2: Paper-trade threshold before live mode unlocks (configurable via
  `trading-mode.paper-trade-threshold`, default 10 successful filled paper
  orders)
- E6-F1-S3: One-time risk-consent acknowledgment, independent of the
  paper-trade threshold — both gates must pass before `switchTo(LIVE)`
  succeeds. `TradingModeResponse.liveModeUnlocked` is true only when both
  `paperTradeThresholdMet` and `riskConsentGiven` are true; the frontend
  banner opens a disclaimer dialog (mirroring `TradeForm`'s confirm-dialog
  pattern) the first time a user tries to switch to LIVE once the threshold
  is met, and never asks again once consent is recorded.
- E6-F2-S1: Hard server-side cap on leverage and position size, enforced by
  the new `risk.RiskLimitService` regardless of what the frontend sent.
  Configured via `risk-limits.*` (`stock-max-position-size-usd` default 5000,
  `crypto-max-leverage` default 5, `crypto-max-position-size-usd` default
  2000). "Position size" is notional exposure (`amountUsd * leverage`); stock
  leverage is always 1x, so its notional equals the entered amount. Called
  from `OrderService.submitOrder` right after the existing shape/bounds
  `validate()` call, before any `Order` row is created or the broker is
  called — a breach throws `RiskLimitExceededException` (403
  `RISK_LIMIT_EXCEEDED`), the same pre-flight, no-row treatment as
  `InvalidTradeRequestException`. `RiskLimitService`'s constructor fails
  fast if `crypto-max-leverage` is ever configured above
  `OrderService.MAX_CRYPTO_LEVERAGE` (20x) — a cap above the adapter's own
  ceiling would never actually bind.
- E6-F2-S2: Kill switch (`risk.KillSwitchService`/`KillSwitchController`,
  same append-only-event/"latest row = current state" pattern as
  `TradingModeService`) that blocks new order submissions
  (`OrderService.submitOrder`'s very first line,
  `KillSwitchService.assertNotEngaged()`) and best-effort cancels every
  non-terminal `Order` this app has submitted across both brokers
  (`OrderService.cancelAllOpenOrders`, reusing the existing
  `BrokerAdapter.cancelOrder` — no adapter interface change needed).
  Engaging flips state to `ENGAGED` *before* the cancel sweep runs, so
  "block new submissions" never depends on cancellation succeeding; one
  order's broker failure doesn't stop the sweep for the rest. Scoped to
  this app's own tracked `Order` rows, not broker-side position-flattening
  (matches E4-F3-S2's no-auto-flatten precedent). `POST
  /api/kill-switch/engage` and `/clear`, `GET /api/kill-switch`; frontend
  `KillSwitchControl` (one-click engage, confirm-dialog-gated clear) above
  `TradingModeBanner` on the dashboard, plus a proactive disable in
  `TradeForm`.
- E6-F2-S3: Portfolio-level aggregate exposure cap on top of the per-order
  caps, via a new `risk-limits.max-aggregate-exposure-usd` config key
  (default 8000) and `RiskLimitService.enforceAggregateExposureCap`. Catches
  many individually-small orders adding up to outsized risk, which
  E6-F2-S1's per-order caps can't. "Open exposure" is the sum of
  `requestedAmountUsd * leverage` across this app's own non-terminal `Order`
  rows — a new `OrderRepository.sumOpenNotionalUsd` JPQL aggregate query,
  `COALESCE`d to zero so an empty portfolio never needs a null check — scoped
  to the *same trading mode* as the new order (paper and live are separate
  broker accounts/capital pools, matching `countByOrderModeAndStatus`'s
  existing per-mode precedent), not scoped by asset type (a true portfolio
  total, stocks and crypto combined). Called from `OrderService.submitOrder`
  right after the per-order cap check, still before any `Order` row exists —
  same pre-flight, no-row, `RiskLimitExceededException`/403
  `RISK_LIMIT_EXCEEDED` treatment as E6-F2-S1, so the frontend needed no
  changes to surface it. `RiskLimitService`'s constructor fails fast if
  `max-aggregate-exposure-usd` is ever configured below the larger of the two
  per-order position-size caps — otherwise a single maximally-sized order
  would always breach the aggregate cap on its own, even with zero other
  open exposure, the same "a cap that can never bind is a config bug"
  reasoning as E6-F2-S1's leverage-ceiling check.
- E6-F3-S1: Immutable audit log of every order and the signal that triggered
  it, via a new `order_audit_entries` table/`OrderAuditEntry` entity
  (insert-only, no setters, no `@PreUpdate`) written once by
  `OrderService.submitOrder`, right after that order's first resolved
  outcome (`FILLED`/`REJECTED`/`FAILED`/`SUBMISSION_UNKNOWN`/etc), FK'd to
  both the `Order` row and the `SignalCallEntry` already persisted for that
  submission's signal computation (looked up via a new
  `SignalCallEntryRepository.findTopByIndicatorSnapshot_IdOrderByIdDesc`,
  the single-snapshot version of the existing batched CSV-export lookup —
  chosen over widening `SignalService.computeSignalWithProvenance`'s return
  shape, to avoid touching its other two callers).  Deliberately scoped to
  submission time only: unlike `Order` itself (mutated in place by
  `applyOutcome`/`refreshOrder`/`cancelAllOpenOrders` as a real order's
  status resolves further), the audit row is never updated after that first
  write, so it freezes the decision made at order-placement time rather than
  tracking the order's live/current status — `Order`/`OrderResponse`/CSV
  export remain the source of truth for that. No new config, no new
  read endpoint (existing `listOrders`/CSV export already serve as review
  surfaces); a dedicated audit-trail viewer is left for a future story now
  that E6-F3-S2 has landed the rule-table-version column alongside it.
- E6-F3-S2: The audit log now records the rule-table version alongside the
  signal snapshot — a new `rule_table_version` column on
  `order_audit_entries` (`V14__add_rule_table_version_to_order_audit_entries.sql`),
  set on `OrderAuditEntry` construction in `OrderService.recordAuditEntry`
  from `signalCallEntry.getRuleTableVersion()` (the value already frozen on
  the specific `SignalCallEntry` row for that submission), not re-read from
  `SignalRuleEngine.RULE_TABLE_VERSION` — so a later rule-table version bump
  can never retroactively change what a past audit row says produced it.
  Denormalized rather than left as a join through the existing
  `signal_call_id` FK, so the audit row is self-contained for a future
  audit-trail viewer. No config, no new endpoint, no frontend change.

### E7 — Observability & Hardening
- E7-F1-S1: Structured logging across every backend service, via a single
  `backend/src/main/resources/logback-spring.xml` console pattern
  (timestamp/level/thread/logger/message) applied to every profile — no new
  logging dependency, extending the SLF4J/Logback convention a few classes
  already used. Closed the gap where the two places that mattered most for
  "broker errors logged with context, never silently swallowed" were almost
  entirely silent: `RetryingBrokerAdapter` (the shared retry/backoff
  chokepoint for both brokers — now logs each retry/rate-limit backoff at
  WARN and the terminal `BrokerAdapterUnavailableException`/
  `BrokerAdapterAmbiguousOrderException` wrap at ERROR) and
  `OrderService.submitOrder`'s four broker-exception catch blocks (now ERROR
  for genuine submission failures, WARN for rate-limited, all with
  broker/symbol/clientOrderId/orderId context — previously a failed order
  went to a `FAILED`/`SUBMISSION_UNKNOWN` row with zero log line). Also
  closed two genuinely-silent swallows flagged during design:
  `BinanceFuturesTradingAdapter.ensureExitLeg` (a missing stop-loss/
  take-profit leg on a filled entry — an unprotected leveraged position —
  now logs WARN) and its `deleteOrder`/`AlpacaTradingAdapter.cancelOrder`
  "already terminal" no-ops (DEBUG). All 5 `@RestControllerAdvice` classes
  and `KillSwitchService`/`RiskLimitService` now log at the point they
  translate an exception to a response or trip a safety gate — INFO for
  ordinary client-driven 4xx (bad ticker, validation, not-found), WARN for
  infra/operational statuses (429/503) and safety-gate trips (kill switch,
  risk-limit breach), so routine user interaction doesn't drown out real
  operational signal. Deliberately did *not* add a second log line in the
  concrete adapters/market-data clients for failures that already get
  logged once downstream (by `RetryingBrokerAdapter`, `OrderService`, or an
  exception handler) — every exception in this app terminates at one of
  those three sinks, so logging again at the throw site would just
  duplicate the same event. Out of scope (kept tight to the AC): no
  MDC/correlation-id tracing, no log file rotation/shipping, no new logging
  library.
- E7-F2-S1: `security-review` run against credential-storage
  (`broker.CredentialEncryptionService`/`BrokerCredentialService`, the
  credential bootstraps) and order-submission code (`OrderService`,
  `AlpacaTradingAdapter`, `BinanceFuturesTradingAdapter`, `RiskLimitService`,
  `KillSwitchService`, `TradingModeService`, `OrderAuditEntry`, and
  `SecurityConfig`'s auth/CSRF setup). One confirmed finding, fixed: the
  dashboard operator password and the credential-encryption key both had an
  insecure, source-visible dev-only fallback (`SecurityConfig`'s
  `DEV_FALLBACK_PASSWORD`, `CredentialEncryptionService`'s
  `DEV_FALLBACK_KEY`) that silently activated whenever their env vars were
  unset — logged at WARN, but never refused to start, unlike
  `spring.datasource.url`'s own no-default `${DB_URL}` placeholder, which
  already fails paper/prod startup outright on the same kind of gap. Fixed
  by making both fail fast (`IllegalStateException` at startup) under the
  `paper`/`prod` Spring profile specifically, leaving the `local`/test
  fallback behavior unchanged; `CredentialEncryptionService` gained an
  `activeProfile` constructor parameter (`@Value("${spring.profiles.active}")`,
  needed an explicit `@Autowired` once a second constructor made Spring's
  implicit single-constructor autowiring inapplicable) and
  `SecurityConfig.userDetailsService`'s hash-resolution logic was extracted
  into a plain, no-Spring-context-needed static method
  (`resolvePasswordHash`) specifically so this fail-fast branch is
  unit-testable the same way `CredentialEncryptionService`'s constructor
  already was. Everything else reviewed clean: Binance's adapter already had
  a documented, implemented `SYMBOL_PATTERN` guard against query-string
  injection via an unvalidated ticker symbol (`TickerController` only
  enforces `@Size(max=20)`, no character-class check, so the adapter is the
  actual injection boundary); Alpaca's adapter sends structured JSON
  bodies/URI-templated params, no string-built requests; the live-mode gate
  (paper-trade threshold + risk consent) and the kill switch/risk caps are
  enforced entirely server-side with no request-body bypass; the audit log
  (`OrderAuditEntry`) has no setters and its repository exposes no
  update/delete path from any controller. No frontend changes.
- E7-F3-S1: Documented and scripted backup/restore for the Oracle instance —
  `scripts/db-backup.sh` (schema-scoped `expdp` export of the `autotrade`
  schema via Oracle's own pre-existing `DATA_PUMP_DIR`, copied out to a
  gitignored `./backups/` with a per-table row-count `.manifest.txt`
  sidecar) and `scripts/db-restore.sh` (`impdp` into a target
  instance/container, printing the same row-count query to diff against
  that manifest). Chose Data Pump over a raw volume-level copy specifically
  because the AC calls for a *restore tested against a fresh instance* —
  a volume tar only proves bytes moved, not that the data imports cleanly
  elsewhere. Actually exercised, not just written: backed up the live dev
  instance (10 tables, 328 total rows across `INDICATOR_SNAPSHOTS`/
  `SIGNAL_CALLS`/`TICKERS`/etc.), stood up a disposable second Oracle XE
  instance via `docker-compose.restore-test.yml` (separate container/port/
  volume, gitignored `./restore-test-data`, its own compose project name to
  avoid orphan-container ambiguity with the main dev stack), restored into
  it, and confirmed every table's row count matched the backup manifest
  exactly, then tore the disposable instance down. `impdp` needs
  `exclude=user` — the target's `APP_USER` already exists (created by the
  container's own init), so only the schema's objects/data need importing,
  not the user itself. On-demand only, no scheduled/cron backup — out of
  scope per the story. Backups stay same-disk (`./backups/`, gitignored,
  overridable via `BACKUP_DIR`); the runbook documents a manual step to copy
  them off-disk periodically, since that's a location only the operator can
  choose. New `docs/runbooks/oracle-backup-restore.md`; `.gitattributes`
  added to force LF line endings on `scripts/*.sh` so Windows checkouts
  don't silently corrupt them. This was E7's last story — the epic is now
  complete.

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
  server-side, never trusts client-cached data), status polling, CSV export,
  `OrderAuditEntry` (E6-F3-S1's write-once, never-updated audit log of an
  order's submission-time decision).
- `notification` — `WatchlistSignalPoller` (scheduled job, the app's first
  background task), `NotificationService`.
- `tradingmode` — `TradingModeService` (append-only `trading_mode_events`,
  latest row = current mode, gated by both E6-F1-S2's paper-trade threshold and
  E6-F1-S3's `risk_consents` one-time acknowledgment).
- `risk` — `RiskLimitService` (E6-F2-S1's hard per-order leverage/position-size
  caps, config-only via `RiskLimitsProperties`), called from `OrderService`
  pre-flight, before any `Order` row exists; `KillSwitchService`/
  `KillSwitchController` (E6-F2-S2's global kill switch, append-only
  `kill_switch_events`).
- `watchlist`, `security` (session auth), `common` (`Clock`/`SchedulingConfig`).
- Schema: Flyway `V1`–`V14` under `backend/src/main/resources/db/migration/` is the
  single source of truth; `spring.jpa.hibernate.ddl-auto=validate` everywhere.
- Logging (E7-F1-S1): `backend/src/main/resources/logback-spring.xml` — one console
  pattern across every profile. SLF4J `private static final Logger log` per class;
  every exception either terminates in `RetryingBrokerAdapter`/`OrderService`
  (broker/order failures) or a `@RestControllerAdvice` handler (everything surfaced
  over HTTP) — that's the one place each failure gets logged, so don't add a second
  log line further down the same call chain.

**Frontend** (`frontend/src/`) mirrors the backend split: `marketdata`, `signal`,
`chart`, `trade`, `order`, `watchlist`, `notification`, `tradingmode`, `killswitch`,
`auth`. Each domain has its own `api.ts` (typed fetch wrapper, shared `MarketDataError`
parsing) and one or two components wired into `DashboardPage`.

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
- **`backend/src/test/resources/application.properties` shadows, not merges
  with, main's `application.properties`** for every `@SpringBootTest` — a new
  `@ConfigurationProperties`/`@Value` key added to main's file also needs a
  literal (non-placeholder) default added there, or any bean whose
  constructor reads it unconditionally fails context startup with a
  confusing `NullPointerException` deep in bean instantiation, not an
  obviously-missing-property error (bit E6-F2-S1's `RiskLimitService`).

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

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
