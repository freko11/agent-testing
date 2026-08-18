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

E1–E7 are done (E6 Risk & Safety Controls, E7 Observability & Hardening). E8 (Signal
Quality & Quant Rigor) is a backlog epic added after E7 shipped; it has gone through
several follow-up batches as calibration findings surfaced new stories, and is currently
**complete** pending the next sweep for flagged-but-unactioned findings. Every story below
is a one-line current-state summary — see `docs/CHANGELOG.md` for the full design-gate
rationale, figures, and live-verification notes per story.

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
- E2-F1-S4: Holiday/early-close calendar — hardcoded NYSE/NASDAQ full-holiday +
  1pm-ET-early-close dates for 2024-2027 in `MarketHoursService`.
- E2-F1-S5: Extended the holiday/early-close calendar to 2024-2029; fixed a missing
  2027-12-31 observed New Year's holiday the extension surfaced.
- E2-F2-S1: RSI, MACD, moving-average crossover
- E2-F2-S2: Volatility (ATR%) / volume-trend metric
- E2-F3-S1: Indicators combined into a Buy/Sell/Hold call (rule engine)
- E2-F3-S2: Suggested hold-term alongside the call
- E2-F4-S1: Backtest harness
- E2-F4-S2: Backtest per-branch expectancy (avg win size vs. avg loss size), not just win
  rate — diagnostic only.

### E3 — Dashboard/Frontend
- Frontend visual pass: design tokens, card-styled sections; fixed the kill switch
  rendering as unstyled text.
- E3-F1-S1: Ticker lookup + metrics display, with a Stock/Crypto asset-type selector.
- E3-F1-S2: Buy/Sell/Hold badge + hold-term, color-coded
- E3-F2-S1: Price chart with MA/RSI overlays (`lightweight-charts`); stale-cache fallback
  for stock charts when the market is closed.
- E3-F3-S1: Watchlist
- Tabbed high-fidelity dashboard redesign: persistent header/safety strip/sidebar,
  Trade/Orders/Notifications tabs.
- Dark-first premium visual pass: persisted theme toggle, redesigned tokens/chart palette,
  fixed status-strip and trade-form layout bugs.

### E4 — Broker Adapter Layer
- E4-F1-S1: `BrokerAdapter` interface + mock implementation + shared contract test suite
- E4-F1-S2: Rate-limit/retry/backoff built into the adapter contract
- E4-F1-S3: Outage handling + duplicate-order prevention
- E4-F2-S1: Alpaca paper account connected; `getAccountStatus`
- E4-F2-S2: Place a market order via Alpaca
- E4-F3-S1: Binance Futures Testnet account connected; `getAccountStatus`
- E4-F3-S2: Place a leveraged order via Binance testnet; follow-up fixed
  quantity/price precision truncation to each symbol's Binance precision.
- E4-F3-S3: Binance Algo Order API migration — exit legs (stop-loss/take-profit) moved to
  `/fapi/v1/algoOrder`, live-verified end-to-end against the real Binance Futures Testnet.

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
- E6-F1-S2: Paper-trade threshold before live mode unlocks (default 10 filled paper
  orders, `trading-mode.paper-trade-threshold`).
- E6-F1-S3: One-time risk-consent acknowledgment, independent of the paper-trade
  threshold — both gates must pass before `switchTo(LIVE)` succeeds.
- E6-F2-S1: Hard server-side cap on leverage and position size (`risk.RiskLimitService`,
  `risk-limits.*` config).
- E6-F2-S2: Kill switch (`risk.KillSwitchService`/`KillSwitchController`) — blocks new
  order submissions and best-effort cancels every open order this app tracks.
- E6-F2-S3: Portfolio-level aggregate exposure cap on top of the per-order caps
  (`risk-limits.max-aggregate-exposure-usd`, default 8000).
- E6-F3-S1: Immutable audit log of every order and the signal that triggered it
  (`OrderAuditEntry`, insert-only, written at submission time).
- E6-F3-S2: Audit log records the rule-table version alongside the signal snapshot
  (denormalized `rule_table_version` column).
- E6-F3-S3: Audit-trail viewer — `GET /api/orders/audit-entries` (this codebase's first
  paginated endpoint) + a new "Audit Trail" dashboard tab.

### E7 — Observability & Hardening
- E7-F1-S1: Structured logging across every backend service (`logback-spring.xml`);
  closed silent-failure gaps in the broker retry and order-submission paths.
- E7-F2-S1: `security-review` pass on credential storage and order-submission code; fixed
  insecure dev-only password/encryption-key fallbacks silently activating in paper/prod.
- E7-F3-S1: Documented and scripted Oracle backup/restore (`scripts/db-backup.sh`/
  `db-restore.sh`), live-tested against a disposable second instance. E7 complete.

### E8 — Signal Quality & Quant Rigor (backlog, complete)
- E8-F1-S1: RSI thresholds recalibrated 30/70 → 25/75 (`RULE_TABLE_VERSION` v1→v2);
  volatility-extreme/volume-dried-up left unchanged (no calibration signal in the sweep).
- E8-F2-S1: TP/SL-aware walk-forward backtest scoring added to `BacktestHarness`,
  replacing fixed-day-endpoint scoring — diagnostic only.
- E8-F2-S2: Flat transaction-cost (20bps) added to backtest expectancy
  (`expectancyPctAfterCosts()`) — diagnostic only.
- E8-F3-S1: `WeightedVoteRuleEngine` (weighted-vote scoring) added, deliberately unwired;
  `IndicatorWeights.DEFAULT` calibrated all-zero at a 5-day horizon.
- E8-F3-S2: `RegimeGatedRuleEngine` (ADX trend/regime filter) added, deliberately unwired —
  evidence came back fixture-dependent (BTCUSDT vs. DOGEUSDT disagree).
- E8-F4-S1: Out-of-sample validation of E8-F1-S1's RSI shift and E8-F3-S1's weights (new
  SOLUSDT fixture + 70/30 splits) — weighted-vote result replicates; RSI's BUY-side gain
  doesn't (flagged for follow-up).
- E8-F5-S1: Live signal-drift monitoring added (`monitoring.LiveSignalDriftService`,
  `GET /api/monitoring/signal-drift`) — closed out E8's original backlog.
- E8-F1-S2: BUY-side RSI recalibration follow-up — no ship; `rsiOversold` has no
  measurable BUY-side effect (the E8-F1-S1 gain traces to `rsiOverbought` instead).
- E8-F1-S3: `rsiOverbought` recalibration follow-up — no ship; asset-dependent BUY-side
  effect, no single value wins on all three surfaces. Closes the BUY-side mismatch as
  understood but not fixable via either RSI bound alone.
- E8-F1-S4: Per-symbol `rsiOverbought` calibration (`PerSymbolRuleThresholds`) — SOLUSDT
  ships override 70; BTCUSDT/DOGEUSDT no ship. `RULE_TABLE_VERSION` v2→v3.
- E8-F1-S5: MACD histogram-magnitude calibration axis — no ship, asset-dependent
  (DOGEUSDT prefers no filter). `macdMinHistogramMagnitudePct` field added, stays 0.
- E8-F4-S2: Out-of-sample validation of `RegimeGatedRuleEngine` — SELL side confirms
  uniformly across all three symbols, BUY side doesn't. Engine stays unwired.
- E8-F3-S3: Wires the regime filter for SELL calls only, crypto-only (`applySellGate`).
  `RULE_TABLE_VERSION` v3→v4.
- E8-F1-S6: MA-crossover-magnitude calibration axis — no ship, asset-dependent (SOLUSDT
  prefers no filter).
- E8-F3-S4: Per-symbol `ADX_TRENDING_THRESHOLD` calibration (BUY side) — no ship for all
  three symbols independently (no confirmable tuning-window winner).
- E8-F3-S5: `WeightedVoteRuleEngine.IndicatorWeights.DEFAULT` recalibrated at a 15-day
  horizon — MACD weight 0.000→0.714, confirmed out-of-sample on all three surfaces;
  RSI/MA-crossover stay 0. Engine still unwired.
- E8-F1-S7: Evaluates the per-symbol RSI override and SELL regime gate against AAPL (first
  stock fixture) — no ship on both axes; AAPL's regime split actively contradicts the
  crypto-wide finding.
- E8-F2-S3: Funding-rate carry cost added to backtest expectancy
  (`expectancyPctAfterCostsAndFunding()`) — diagnostic only, not wired into live
  monitoring.
- E8-F1-S8: Per-symbol `macdMinHistogramMagnitudePct` calibration — SOLUSDT ships
  override 0.10 (composed with its existing `rsiOverbought=70`); BTCUSDT/DOGEUSDT no ship.
  `RULE_TABLE_VERSION` v4→v5.
- E8-F1-S9: SELL-side global MACD magnitude gate — no ship (BTCUSDT's own tuning window
  never clears the bar).
- E8-F1-S10: Per-symbol `maMinSeparationPctOfPrice` calibration — no ship for all three
  symbols.
- E8-F1-S11: MA-crossover SELL gate — shipped, `ma>=2.00%`, wired crypto-only
  (`MaCrossoverSellGate`). `RULE_TABLE_VERSION` v5→v6.
- E8-F3-S6: `WeightedVoteRuleEngine.WEIGHTED_MAJORITY_FRACTION` calibration — no ship,
  stays 0.5 (the constant's real-valued range collapses to only 3 behavioral regimes, and
  neither alternative beats 0.5 on the tuning fixtures).
- E8-F1-S12: Evaluates AAPL against the MACD-magnitude and MA-crossover-separation axes —
  no ship on either; the already-shipped `MaCrossoverSellGate` value actively hurts AAPL's
  own SELL-side expectancy (gate stays crypto-only).
- E8-F5-S2: Wires funding-adjusted expectancy into live signal-drift monitoring —
  `LiveDriftBaseline`/`CheckpointDrift` gain funding-adjusted fields, informational only
  (`possibleDecay` still gates on the cost-only figure alone).
- E8-F6-S1: Calibrates `HoldTermRule`'s 6 branch day-ranges against realized backtest
  expectancy — no ship on any branch; 4 of 6 branches (all `STRONG_*` + `MODERATE_LOW`)
  never fire at all in these crypto fixtures, and the two that do (`MODERATE_MEDIUM`,
  `MODERATE_HIGH`) are asset-divergent/inconclusive. `HOLD_TERM_TABLE_VERSION` stays v1.
- E8-F6-S2: Sweeps `VOLATILITY_LOW_MAX`/`VOLATILITY_MEDIUM_MAX` cutoffs against pooled
  ATR% distribution — no ship; the one candidate with a distinct `MODERATE_LOW` signal on
  tuning (2.0→3.5) flips sign on held-out. Confirms `MODERATE_LOW` is a genuinely dead
  branch for crypto at any reasonable cutoff, not a mis-tuned threshold.

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
  `MovingAverageCrossoverCalculator`/`VolatilityCalculator`/`VolumeTrendCalculator`/
  `AdxCalculator` (E8-F3-S2, Wilder ADX for trend-strength/regime
  classification)
  (pure static, no library — see `docs/CHANGELOG.md` E2-F2-S1 for why), plus
  `IndicatorService`/`IndicatorSnapshot` persistence.
- `signal` — `SignalRuleEngine` (versioned rule table, currently
  `RULE_TABLE_VERSION` v6, safety gates + 2-of-3 directional vote →
  BUY/SELL/HOLD), `HoldTermCalculator` (versioned day-range table),
  `SignalCallEntry` audit log. `PerSymbolRuleThresholds` (E8-F1-S4, extended
  by E8-F1-S8) resolves `RuleThresholds` per normalized ticker symbol —
  currently only SOLUSDT has non-default overrides (`rsiOverbought=70`,
  `macdMinHistogramMagnitudePct=0.10`, composed into one entry), every other
  symbol falls back to `RuleThresholds.DEFAULT` (25/75/0/0) — called from
  `SignalService.computeSignalWithProvenance`, the one production (i.e. not
  deliberately unwired) consumer of `RuleThresholds` besides the default.
  `WeightedVoteRuleEngine` (E8-F3-S1) is an alternative, deliberately
  **unwired** weighted-vote scoring layer — reuses
  `SignalRuleEngine.computeVotes`/`IndicatorVotes`, not called by
  `SignalService`/`OrderService`. `IndicatorWeights.DEFAULT` (E8-F3-S5)
  gives MACD a nonzero weight (0.714, derived at a 15-day/TP15%/SL9%
  horizon after E8-F3-S1's original 5-day calibration came back all-zero);
  RSI/MA-crossover stay at 0.000 — a real constant change, but the class
  itself is still unwired. `WEIGHTED_MAJORITY_FRACTION` (E8-F3-S6) stays
  at its original 0.5 — with only MACD nonzero-weighted, the constant's
  entire range collapses to three behavioral regimes (0, `(0,1]`, `>1`)
  and neither of the other two regimes beats 0.5 on the tuning fixtures
  (0 ties byte-identically, `>1` produces zero calls since UNANIMOUS never
  fires in this data). `IndicatorId` (RSI/MACD/MA_CROSSOVER) keys
  per-indicator data for both. `RegimeGatedRuleEngine` (E8-F3-S2) has both a
  both-directions `applyGate` (still **unwired** — the combined BUY+SELL
  mechanism never cleared the wiring bar) and a SELL-only `applySellGate`
  (E8-F3-S3, **wired** — `SignalService.computeSignalWithProvenance` calls
  it for crypto tickers, per `sellGateAppliesTo(AssetType)`, after
  `SignalRuleEngine.evaluate` returns) — `Regime`/`RegimeClassifier`
  classify an `AdxCalculator` reading into TRENDING/RANGING for both.
  `RegimeClassifier.classify` has a threshold-accepting overload
  (E8-F3-S4) that the SELL gate never calls (always the global-default
  no-arg form); a BUY-side counterpart, `applyBuyGate`/
  `buyGateAppliesTo(String)`, mirrors `applySellGate` but stays
  **unwired for every symbol** — `PerSymbolAdxThresholds` (E8-F3-S4,
  mirrors `PerSymbolRuleThresholds`'s per-symbol shape) ships with an
  empty override map, since no symbol's tuning-window winner confirmed
  on its own held-out tail. `MaCrossoverSellGate` (E8-F1-S11, **wired** —
  `SignalService.computeSignalWithProvenance` calls it for crypto tickers,
  per `sellGateAppliesTo(AssetType)`, after `RegimeGatedRuleEngine
  .applySellGate` returns) re-runs `SignalRuleEngine.evaluate` under a
  stricter `maMinSeparationPctOfPrice=2.00%` threshold and collapses a
  SELL call to `NO_STRONG_SIGNAL` if it no longer qualifies — unlike the
  regime gate's orthogonal ADX input, `maMinSeparationPctOfPrice` gates
  `computeVotes`'s MA vote directly, so "SELL-only" here means a full
  second rule-table evaluation, not a plain enum-in/enum-out filter. The
  two SELL-only gates compose in either order (both only ever downgrade
  an already-resolved SELL call).
- `backtest` (`src/main/java`, E8-F5-S1) — the TP/SL-aware walk-forward scoring
  primitives promoted out of the test-only `BacktestHarness` so live signal
  monitoring can reuse them: `WalkForwardScorer` (`score`/`findFirstCrossing`/
  `percentChange`, taking a `forwardCandles` slice rather than a full candle
  list + index), `DirectionalAccumulator` (top-level now, was a private nested
  class), `BacktestConfig` (TP/SL %, deadband, transaction-cost bps,
  funding-rate bps/period (E8-F2-S3) — diagnostic placeholders),
  `Checkpoint`, `CheckpointStats` (`expectancyPctAfterCostsAndFunding()`
  scales funding cost by `avgHoldingDays`, unlike the flat
  `expectancyPctAfterCosts()`), `DirectionalOutcome`,
  `DirectionalOutcomeStats`, `DirectionalScoreResult` (carries `daysHeld`),
  `ExitReason`. `BacktestHarness` itself (and `BacktestReport`,
  `BacktestDecisionPoint`, `HoldGateStats`/`HoldGateOutcome`,
  `RegimeSplitStats`, the per-indicator/`HoldGate` accumulators) stays
  `src/test/java`-only — a diagnostic/calibration tool, not something the live
  app calls.
- `broker` — `BrokerCredentialService`/`CredentialEncryptionService` (keyring-based
  rotation), per-broker credential bootstraps from env vars.
- `brokeradapter` — `BrokerAdapter` interface, `RetryingBrokerAdapter` decorator
  (retry/backoff/outage-reconciliation), `AlpacaTradingAdapter`,
  `BinanceFuturesTradingAdapter`, `BrokerAdapterRouter` (routes by asset type).
- `order` — `OrderService` (bracket-order submission, re-derives signal
  server-side, never trusts client-cached data), status polling, CSV export,
  `OrderAuditEntry` (E6-F3-S1's write-once, never-updated audit log of an
  order's submission-time decision). `listAuditEntries`/`GET
  /api/orders/audit-entries` (E6-F3-S3) is this codebase's first genuinely
  paginated endpoint (`common.PagedResponse<T>`, page-number/page-size —
  every earlier list endpoint is limit-only).
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
- `monitoring` (E8-F5-S1) — `LiveSignalDriftService` (`@Scheduled`/
  `@ConditionalOnProperty`-gated, mirrors `notification.WatchlistSignalPoller`'s
  batch-per-ticker/catch-and-skip shape) re-scores the rule table's live
  performance against `FILLED`/`PARTIALLY_PROTECTED` `OrderAuditEntry` rows
  using `backtest.WalkForwardScorer` against real forward market data, grouped
  by `rule_table_version` and BUY/SELL; `LiveDriftBaseline` pins the current
  version's original-backtest `expectancyPctAfterCosts` figures to diff
  against, plus (E8-F5-S2) funding-adjusted `expectancyPctAfterCostsAndFunding`
  counterparts surfaced alongside the cost-only comparison on `CheckpointDrift`
  as an informational-only figure — `possibleDecay` still gates on the
  cost-only `driftPct` alone. `SignalDriftController` (`GET /api/monitoring/signal-drift`) and
  the scheduled job call the same `computeDrift` method; ephemeral only, no
  new table.
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
`chart`, `trade`, `order`, `auditentry` (E6-F3-S3's audit-trail viewer, its own
domain despite reading through the `order` resource — a distinct concept, order
status vs. why an order fired), `watchlist`, `notification`, `tradingmode`,
`killswitch`, `auth`. Each domain has its own `api.ts` (typed fetch wrapper, shared
`MarketDataError` parsing) and one or two components wired into `DashboardPage`.

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
1. Update the **Status** section of this CLAUDE.md (one line per story/feature — a
   single line, no paragraphs; put rationale, figures, and verification notes in the
   `docs/CHANGELOG.md` entry only) to reflect the change, and append a full narrative
   entry (design-gate rationale, bugs found/fixed, live-verification notes) to
   `docs/CHANGELOG.md` — keep CLAUDE.md itself limited to current-state facts (status,
   commands, architecture, recurring gotchas), not per-story prose.
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
