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

E1–E7 are done, including E6 (Risk & Safety Controls), E7
(Observability & Hardening), and the E4-F3-S3 backlog story (Binance Algo
Order API migration) added after E4-F3-S2's post-launch verification found
Binance rejecting conditional exit-leg orders on the old endpoint — see
that story's entry below, live-verified end-to-end against the real
Binance Futures Testnet. E8 (Signal Quality & Quant Rigor) is a backlog
epic added after E7 shipped and is now **complete**: E8-F1-S1, E8-F2-S1,
E8-F2-S2, E8-F3-S1, E8-F3-S2, E8-F4-S1, and E8-F5-S1 (E8's last story,
closing out F8.5, its only feature) are all done. E8-F1-S2 is a follow-up
backlog story added to F8.1 after E8-F4-S1 flagged the BUY-side RSI
recalibration as future work — same "found post-launch, added to an
already-listed feature" pattern as E4-F3-S3 — and is also done, though it
shipped no threshold change (see its entry below for why).

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
- E2-F4-S2: Backtest per-branch expectancy (avg win size vs. avg loss size),
  not just win rate. `BacktestHarness`'s directional `score()` now returns a
  `DirectionalScoreResult` (outcome + the signed forward return it was
  classified from, not just the WIN/LOSS/WASH enum) so `CheckpointStats` can
  carry `avgWinReturnPct`/`avgLossReturnPct`/`expectancyPct()` alongside its
  existing win rate, at the same min/mid/max checkpoints E2-F4-S1 already
  reports; the UNANIMOUS+MAJORITY roll-up (`combineCheckpoint`) now does a
  call-count-weighted average of the two branches' avg win/loss sizes rather
  than a plain sum. Diagnostic-only, same as E2-F4-S1: no `SignalRuleEngine`
  changes, reuses the existing checked-in BTCUSDT/DOGEUSDT fixtures.
  `BacktestHarnessTest` gained a structural invariant check (avg win size is
  always positive when win &gt; 0, avg loss size always negative when loss &gt;
  0 — a sign guarantee of the deadband classification itself, not a value
  under review) rather than asserting on the actual numbers. Confirmed the
  story's premise on the real fixture data: DOGEUSDT's BEARISH_MAJORITY
  branch has negative expectancy at the min/mid checkpoints (-0.09%/-0.31%)
  despite a near-coin-flip win rate (42.8%/40.7%), while every BTCUSDT
  branch is expectancy-positive at every checkpoint — exactly the "coin-flip
  win rate can still be unprofitable" gap this story set out to measure.
  Findings feed a future rule-table decision, not acted on here.

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
- Tabbed high-fidelity dashboard redesign: `DashboardPage.tsx` restructured
  from one scrolling column into a persistent header, a persistent safety
  status strip (`TradingModeBanner`/`KillSwitchControl`, visible on every
  tab since they're global state), a sticky sidebar (`Watchlist`, always
  visible), and a tabbed main area (Trade/Orders/Notifications, new
  `layout/Tabs.tsx`) holding `TickerMetrics`/`OrderHistory`/
  `NotificationPanel` unchanged internally, just relocated. Watchlist-click
  now jumps to the Trade tab. Tab panels use `hidden` (not conditional
  rendering) so switching tabs never loses in-progress state; needed an
  explicit `.app-tab-panel[hidden] { display: none }` override after a
  live-verification catch — the base `display: flex` rule was silently
  defeating `hidden` via equal CSS specificity, rendering all three panels
  at once. Added tone-coded stat-tile accents, a directional glyph on the
  signal badge, and a sticky-header/zebra-striped/status-dot order-history
  table for a more "high-fidelity trading terminal" feel.
- Dark-first premium visual pass, prompted by continued "looks amateur"
  feedback on the tabbed redesign above. New `theme/ThemeContext.tsx`
  (dark-first, persisted to `localStorage`, `data-theme` set on
  `documentElement` before first paint via an inline `index.html` script to
  avoid a flash of the wrong theme) replaced the previous OS-inferred
  `light-dark()` token approach — the active theme is now an explicit,
  persisted user choice via a new `layout/ThemeToggle.tsx` in the header
  (and the login page). `index.css` tokens rewritten as explicit
  `:root` (dark default) plus a `:root[data-theme="light"]` override block:
  deep navy/charcoal surfaces with a subtle radial-gradient backdrop, a
  brighter electric-blue accent, glow-tinted shadows, a monospace numeric
  font stack (`--font-mono`) applied to prices/stat-tile values/table
  numeric columns, and refined status pills for order/kill-switch/trading-
  mode state. `chart/palette.ts`'s `isDarkMode()` now reads `data-theme`
  instead of `prefers-color-scheme`, and `PriceChart.tsx` takes `theme` as
  an effect dependency (via `useTheme()`) so the chart actually rebuilds on
  a mid-session toggle instead of only picking up theme on next lookup.
  Restructured `KillSwitchControl.tsx`/`TradingModeBanner.tsx` markup
  (icon + label + state pill + meta text + right-aligned action, mirrored
  between both) to fix a real layout bug caught in live-verification: the
  cleared/paper-mode idle states rendered as a single bare button floating
  in a mostly-empty card, since `.app-status-strip`'s equal `flex: 1 1 16rem`
  stretch gave both cards half the header width with nothing to fill it.
  Also fixed `TradeForm.tsx` showing every field's validation error
  immediately on a freshly-opened, untouched form (e.g. "Enter an amount
  greater than 0" before the user had typed anything) — added per-field
  `touched` state (set `onBlur`) plus a `submitAttempted` flag, so an
  error only surfaces once its field has been interacted with or a submit
  was attempted, without changing `validation.ts`'s actual validation
  logic. Live-verified in both themes: full login → dashboard flow, all
  three tabs, a populated ticker lookup (stat tiles, signal badge, trade
  form, price chart), and the theme toggle persisting across a logout/
  login round trip. `npm run build`, `npm run lint`, and `npm test` all
  pass unchanged.

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
  defined for this asset"`. Its other follow-up gap (Binance rejecting
  `STOP_MARKET`/`TAKE_PROFIT_MARKET` exit legs on `/fapi/v1/order`) is now
  fixed by E4-F3-S3 below.
- E4-F3-S3: Binance Algo Order API migration. `ensureExitLeg`/a new
  `findAlgoOrder` now place and look up the two exit legs (stop-loss,
  take-profit) against `POST`/`GET /fapi/v1/algoOrder`
  (`algoType=CONDITIONAL`) instead of the now-rejecting `/fapi/v1/order`,
  using `clientAlgoId`/`triggerPrice`/`algoId`/`algoStatus` in place of that
  endpoint's `newClientOrderId`/`stopPrice`/`orderId`/`status`. The entry
  leg is untouched (still a plain MARKET order on `/fapi/v1/order`), and so
  is `cancelOrder`'s scope (still entry-leg-only — confirmed with the user
  before implementation: by the time exit legs exist the entry has already
  filled and is no longer cancelable, so there was never a gap here to
  close; the kill switch's cancel-all sweep stays entry-only too, the same
  scope choice). `compositeResult`'s `isTriggered`/`isMissingProtection`
  read `algoStatus` — live-verified against the real Binance Futures
  Testnet (a real signed `POST`/`GET`/`DELETE /fapi/v1/algoOrder` round
  trip, including watching a conditional order actually fire and close a
  position), not left as guesses. Two initial guesses were wrong and
  corrected from that live run: the resting status is `NEW`, not `WORKING`;
  the cancelled status is `CANCELED` (one L), not `CANCELLED`. Everything
  else (every param/response field name, `ALGO_ORDER_DOES_NOT_EXIST_CODE`
  matching the entry endpoint's `-2013`, and the `FINISHED`-on-trigger
  status) was confirmed correct on the first guess. `EXPIRED`/`REJECTED`
  weren't directly observed (low-risk, unambiguous spellings). Full-stack
  follow-up: a real bracket order (BTCUSDT SELL, $100, 1x) submitted through
  the actual running dashboard (logged in via the browser, not a direct API
  call) came back `OrderStatus.FILLED` — not `PARTIALLY_PROTECTED` — and a
  direct Binance cross-check via `GET /fapi/v1/openAlgoOrders` confirmed
  both legs genuinely resting (`TAKE_PROFIT_MARKET`/`STOP_MARKET`, correct
  trigger prices, correctly-derived `clientAlgoId`s) before being cancelled
  and the position flattened as cleanup. Proves `OrderService.submitOrder`
  actually drives this migration correctly end-to-end, not just the
  adapter in isolation. See `docs/CHANGELOG.md`'s E4-F3-S3 entries for the
  raw request/response bodies and the full design-gate rationale.

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

### E8 — Signal Quality & Quant Rigor (backlog, complete)
- E8-F1-S1: Threshold calibration pass. `SignalRuleEngine` gained a nested
  `RuleThresholds` record (`rsiOversold`/`rsiOverbought`/`volatilityExtreme`/
  `volumeDriedUp`) plus a `RuleThresholds`-accepting `evaluate` overload — the
  existing 5-arg `evaluate` now delegates to it with `RuleThresholds.DEFAULT`,
  so no production caller changed — and a matching overload on
  `BacktestHarness.run`, so a new `ThresholdCalibrationTest`
  (`backend/src/test/java/.../backtest/`) could sweep threshold candidates
  through the real backtest harness without reflectively mutating production
  constants. Swept RSI (oversold/overbought moved together), volatility-
  extreme, and volume-dried-up one dimension at a time (others held at
  baseline) against the same checked-in BTCUSDT/DOGEUSDT fixtures E2-F4-S1/S2
  already use. Finding: widening RSI from 30/70 to 25/75 raised win rate and
  expectancy on the BUY side across both fixtures at every min/mid/max
  checkpoint, with a *larger* scored sample at each step (fewer RSI-vs-other-
  indicator conflicts resolved to `CONFLICTING_SIGNALS`/HOLD, not a smaller/
  noisier n) — not an overfit to a handful of points. Shipped: RSI 30/70 →
  25/75, `RULE_TABLE_VERSION` bumped v1 → v2. Volatility-extreme (8.0) and
  volume-dried-up (0.20) were left unchanged: volume-dried-up was completely
  flat across the whole 0.10–0.40 sweep on both fixtures (a dead parameter in
  that range, no calibration signal either way), and volatility's only
  better-looking candidate (5.0) only looked better on an n=10 DOGEUSDT
  sample — not enough to trust over the n=133+ baseline. `SignalRuleEngineTest`'s
  RSI boundary tests and its bullish/bearish RSI fixture constants were
  updated to the new 25/75 boundary (previously exactly-at-old-boundary
  values like RSI=25 would otherwise land exactly on the *new* boundary
  instead of clearly past it); `SignalServiceTest` and `OrderCsvExporterTest`
  had similar hardcoded-boundary/hardcoded-version-literal fixtures that
  needed the same update, caught by `./mvnw verify` after the threshold
  change (2 failures, both fixed). Confirmed via grep that every consumer of
  `RULE_TABLE_VERSION` (`SignalCallEntry`, `SignalResponse`,
  `BacktestReport`, `OrderAuditEntry` via `SignalCallEntry`) reads the
  constant dynamically — no hardcoded `"v1"` literal anywhere in production
  code — so the version bump needed no migration; past `order_audit_entries`
  rows keep their frozen `"v1"` string untouched, per E6-F3-S2's write-once
  guarantee. Deliberately conservative and explicitly scoped: this pass is
  provisional pending E8-F4-S1's out-of-sample validation, since the same two
  fixtures were both the tuning set and the only evidence used here — no
  train/held-out split was attempted, since that's E8-F4-S1's own AC to
  close, not preempted here.
- E8-F2-S1: TP/SL-aware backtest scoring. `BacktestHarness` now runs a
  day-by-day walk-forward scan (`findFirstCrossing`) per BUY/SELL decision
  point, checking each candle's high/low against a take-profit/stop-loss
  price before falling back to the existing fixed-day MIN/MID/MAX endpoint
  scoring — so reported win rate/expectancy reflects what a real bracket
  order would have realized (an early TP/SL exit), not just the forward
  price at an arbitrary day count. New `BacktestConfig.TAKE_PROFIT_PCT`/
  `STOP_LOSS_PCT` (5%/3%, harness-only diagnostic constants, same treatment
  as this file's other backtest-only thresholds) since neither the signal
  path nor `PlaceOrderRequest` (E5-F2-S1's free-form user-entered TP/SL
  prices) had anything to derive a percentage from — confirmed with the
  user before implementation as an explicitly uncalibrated placeholder, not
  data-backed. New `ExitReason` enum (`TP_HIT`/`SL_HIT`/`HORIZON_EXPIRED`)
  threaded through `DirectionalScoreResult`, new `tpHit`/`slHit`/
  `horizonExpired` counts on `CheckpointStats`, and `minResult`/`midResult`/
  `maxResult` on `BacktestDecisionPoint` for the spot-check table.
  Per-checkpoint-bound design (confirmed with the user over the AC's
  ambiguity): the shared crossing scan is bounded by `holdTerm.maxDays()`,
  but each of MIN/MID/MAX only adopts that crossing if it happened at or
  before that checkpoint's own day — so an early TP hit doesn't collapse
  all three checkpoints to an identical result, preserving E2-F4-S1's point
  of comparing the hold-term range itself. Same-day tie-break (a daily OHLC
  bar can't say which of high/low happened first intraday) resolves
  conservatively to stop-loss. New `BacktestHarnessTpSlTest` with
  hand-crafted synthetic candles pins the crossing algorithm down exactly
  (TP hit, SL hit, same-day tie, horizon-expired fallback, per-checkpoint
  bound) — the two real BTCUSDT/DOGEUSDT fixtures can't provide ground
  truth for this, only evidence under review, so `score`/`findFirstCrossing`
  were relaxed from `private` to package-private specifically to make this
  test possible. `BacktestHarnessTest`/`ThresholdCalibrationTest` gained a
  new structural invariant (`tpHit+slHit+horizonExpired` partitions
  `scored()` exactly); `ThresholdCalibrationTest` itself needed no other
  changes since it only calls `CheckpointStats` accessor methods. Backend,
  `src/test/java`-only, same precedent as E2-F4-S1/S2 and E8-F1-S1 — no
  `SignalRuleEngine`/`RULE_TABLE_VERSION`/`OrderService`/`PlaceOrderRequest`
  changes. Live run against the real fixtures found nearly every scored
  BUY/SELL call at the MAX checkpoint now resolves via an early TP/SL
  crossing rather than the fixed horizon (e.g. DOGEUSDT BULLISH_MAJORITY's
  max checkpoint: `avgWinReturnPct`/`avgLossReturnPct` land almost exactly
  on +5.00%/-3.00%, with `horizonExpired=0` of 179 scored) — the old
  fixed-day endpoint scoring was systematically letting winners run past
  (and losers fall past) where a real bracket order would have already
  closed the position, so this is a materially different, more realistic
  measurement than E2-F4-S1/S2's, not just a refinement. E8-F2-S2
  (transaction costs) is a separate follow-up story, not implemented here.
- E8-F2-S2: Transaction-cost-aware backtest expectancy. New
  `BacktestConfig.TRANSACTION_COST_BPS` (20bps, harness-only diagnostic
  placeholder — same treatment as this file's other backtest-only
  thresholds) approximating a flat round-trip spread+slippage+fee cost
  (~10bps Binance Futures taker fees, plus an added ~10bps slippage buffer
  biased toward DOGEUSDT's worse execution quality rather than BTCUSDT's,
  since overstating cost is the safer failure mode for a story about not
  overstating paper profitability); single flat value across both
  fixtures, not asset-differentiated, confirmed with the user before
  implementation, since the harness carries no asset-type parameter through
  its call chain today. New `CheckpointStats.expectancyPctAfterCosts()`
  derived method (`expectancyPct() - costPct`) — no new record fields, no
  changes to `DirectionalScoreResult`/`DirectionalAccumulator`/
  `BacktestDecisionPoint`/`ExitReason`/`combineCheckpoint`, since a flat
  per-trade cost applies identically regardless of exit reason or
  MIN/MID/MAX checkpoint and needs no new per-outcome state to compute; the
  WIN/LOSS/WASH deadband classification itself is untouched, only the
  reported expectancy magnitude changes. `BacktestReport.printCheckpoint`
  now prints both figures side by side (`expectancy +X% (after costs
  +/-Y%)`) per the AC. New `CheckpointStatsTest` (hand-constructed records,
  no need to go through the harness's accumulator/combine machinery since
  the method is a pure function of the record's own fields) pins the
  arithmetic down exactly, including the story's motivating case: a
  win-heavy, thin-margin branch (`win=6 @ +0.5%, loss=4 @ -0.4%`, raw
  expectancy +0.14%) flips negative (-0.06%) once the flat cost is
  subtracted — a branch that looks paper-profitable but wouldn't survive
  real execution costs, now visible on one report line instead of requiring
  separate manual arithmetic. `BacktestHarnessTest`/`ThresholdCalibrationTest`
  gained one new structural invariant (`expectancyPctAfterCosts() <=
  expectancyPct()`, since cost is never negative) alongside their existing
  ones; `ThresholdCalibrationTest`'s own compact sweep printer was
  deliberately left unchanged (that tool's report scope is E8-F1-S1's
  threshold sweep, not this story's target). Deliberately out of scope,
  confirmed with the user: Binance Futures perpetual funding-rate carry
  cost — unlike spread/slippage/fees, funding is paid periodically and
  scales with hold duration rather than being a flat one-time cost, which
  the AC's "spread/slippage/fees" wording doesn't cover. Backend,
  `src/test/java`-only, same precedent as every other E8 story so far — no
  `SignalRuleEngine`/`OrderService`/`PlaceOrderRequest` changes.
- E8-F3-S1: Weighted-vote scoring layer, built as a new, deliberately
  **unwired** `signal.WeightedVoteRuleEngine` — `SignalService`/`OrderService`
  keep calling `SignalRuleEngine.evaluate` exactly as before, no config flag,
  no `RULE_TABLE_VERSION` bump, no `OrderAuditEntry` change, same "add a new
  class, don't touch the production call path" pattern as E8-F1-S1's
  `RuleThresholds`. `SignalRuleEngine`'s three vote booleans-per-indicator
  were extracted out of `evaluate` into a new public `computeVotes`/
  `IndicatorVotes` (public, not package-private, since `BacktestHarness` in
  the separate `backtest` package needs it too) with a pinned-down unit test
  proving zero behavior change; a new `signal.IndicatorId` enum (RSI/MACD/
  MA_CROSSOVER) keys per-indicator data everywhere else. `BacktestHarness`
  gained genuinely new capability — per-indicator scoring, independent of the
  combined rule table's matched rule/hold-term, using the existing E8-F2-S1
  TP/SL-aware `findFirstCrossing`/`score` walk-forward scan bounded by the
  existing `BacktestConfig.HOLD_REFERENCE_HORIZON_DAYS` (5 days, reused
  rather than adding a second fixed-horizon constant, confirmed with the
  user) — accumulated into a new single-checkpoint `IndicatorAccumulator`
  (no MIN/MID/MAX split, since a lone indicator has no rule-derived hold
  term) reusing the existing `CheckpointStats` shape unchanged, surfaced as
  `BacktestReport.indicatorStats` and a new `printIndicatorExpectancy` block.
  `WeightedVoteRuleEngine.IndicatorWeights` is `weight_i = max(0,
  expectancyPctAfterCosts_i)` (confirmed with the user: after-costs, not raw
  expectancy, so a weight already reflects real trading friction) — `DEFAULT`
  is computed, not guessed, from a new `IndicatorExpectancyCalibrationTest`
  run against the real checked-in BTCUSDT/DOGEUSDT fixtures, combined
  call-count-weighted across both (reusing `BacktestHarness.combineCheckpoint`,
  relaxed from `private` to package-private for the test, same precedent as
  E8-F2-S1's `score`/`findFirstCrossing`). The actual computed finding: under
  this fixed 5-day/5%-TP/3%-SL scoring, all three indicators' combined
  after-cost expectancy came back *negative* (RSI -0.728%, MACD -0.039%,
  MA-crossover -0.131% — stop-loss hits substantially outnumber take-profit
  hits for every indicator at this short a horizon), so `IndicatorWeights.
  DEFAULT` is `(0.000, 0.000, 0.000)` — every weight floors to zero. This is
  a real result, not a bug: a new `WeightedVoteBacktestTest` A/B-replays both
  engines through the same walk-forward machinery (a new `BacktestHarness.
  RuleEvaluator` functional interface + a `run(label, candles, evaluator,
  thresholds)` overload, so any 5-arg-shaped rule engine can be swapped in)
  and confirms that with `DEFAULT`, the weighted engine calls `NO_STRONG_
  SIGNAL` on every single decision point in both fixtures (BULLISH_UNANIMOUS/
  BEARISH_UNANIMOUS never occur in this data either — all three indicators
  never agree simultaneously — so the "always-reachable-regardless-of-weight"
  UNANIMOUS branch never fires, and majority is unreachable with zero total
  weight). Rather than let a naive `weightedSum >= totalWeight * fraction`
  comparison vacuously read `0 >= 0` as true and "promote" every lone/
  majority vote once every weight is zero, `evaluate` explicitly guards
  `totalWeight.signum() <= 0` before the majority comparison. The story's
  actual intended mechanism — a dominant lone/2-of-3 indicator promoting a
  call the unweighted table would call `NO_STRONG_SIGNAL`, and a low-weight
  one still failing to — is proven independently in `WeightedVoteRuleEngineTest`
  using non-default injected weights (the constructor-level `evaluate`
  overload, mirroring `RuleThresholds`'s pattern), since `DEFAULT`'s own
  current calibration can't currently exercise it; `evaluateUnweighted` (pure
  delegation to `SignalRuleEngine.evaluate`) is verified to match exactly
  across every `SignalRuleEngineTest` branch — the literal "fallback/
  comparison mode" the AC asks for. All three safety gates and the conflict/
  dissent gate are unchanged from `SignalRuleEngine` (reused via
  `computeVotes`, not duplicated); all-three-agree resolves UNANIMOUS off the
  raw 3-of-3 count rather than a weight comparison, specifically so one
  zero-weighted indicator agreeing alongside the other two can never
  vacuously read as clearing the UNANIMOUS bar with only 2 real votes.
  `WEIGHTED_MAJORITY_FRACTION` (0.5 of total weight) is an explicit,
  documented uncalibrated placeholder, same treatment as `BacktestConfig.
  TAKE_PROFIT_PCT`. Explicitly provisional pending E8-F4-S1's out-of-sample
  validation — both fixtures are this calibration's only tuning data, the
  same caveat `ThresholdCalibrationTest` (E8-F1-S1) already documents.
  Backend, `src/main/java` for `IndicatorId`/`WeightedVoteRuleEngine` (new,
  unwired classes) plus the `computeVotes` extraction inside
  `SignalRuleEngine` (no behavior change), `src/test/java` for everything
  else — no `SignalService`/`OrderService`/`PlaceOrderRequest` changes.
- E8-F3-S2: Trend-strength/regime filter, built as a new, deliberately
  **unwired** `signal.RegimeGatedRuleEngine` — same "add a new class, don't
  touch the production call path" pattern as E8-F3-S1's
  `WeightedVoteRuleEngine`. New `indicator.AdxCalculator.calculate(candles,
  period)` computes Wilder's ADX (per-candle +DM/-DM/TR, Wilder-smoothed
  over `period` using the running-average recursion form
  `VolatilityCalculator`'s ATR already uses, +DI/-DI, DX, then a
  Wilder-smoothed ADX) — chosen over a long/short ATR-ratio alternative
  (confirmed with the user) because ADX measures directional persistence,
  which is what the AC actually asks for, where an ATR ratio would measure
  volatility expansion and could misread a whipsaw as "trending". New
  `signal.Regime` enum (TRENDING/RANGING) and `signal.RegimeClassifier`
  (`classify(BigDecimal adx)`, threshold `ADX_TRENDING_THRESHOLD=25` — the
  common ADX rule-of-thumb, an explicitly uncalibrated placeholder like
  `BacktestConfig.TAKE_PROFIT_PCT`, with the ambiguous 20–25 band resolving
  to RANGING as the more conservative default) mirror `HoldTermCalculator`'s
  classify-a-precomputed-value shape. `RegimeGatedRuleEngine.applyGate
  (SignalRuleId, Regime)` is a pure, engine-agnostic post-filter (confirmed
  with the user: gate, not down-weight, since down-weighting would only
  meaningfully compose with `WeightedVoteRuleEngine`'s already-degenerate
  all-zero default weights) that collapses a directional BUY/SELL call to
  the existing `SignalRuleId.NO_STRONG_SIGNAL` when the regime is RANGING —
  confirmed with the user to reuse the existing enum value rather than add a
  new `SignalRuleId` constant, keeping this story's blast radius to new,
  additive classes only (`NO_STRONG_SIGNAL`'s rationale string is a known,
  accepted minor inaccuracy for a regime-gated call, since the indicators
  did actually agree — the regime override suppressed it, not dissent).
  `BacktestHarness` gained a fourth-decision-point-tag capability alongside
  E8-F3-S1's per-indicator scoring: every BUY/SELL decision point now also
  computes its ADX/regime and is tallied into one of four new
  `DirectionalAccumulator`s (buy/sell x trending/ranging), reusing the
  existing `CheckpointStats`/`DirectionalOutcomeStats` shapes unchanged, via
  a new `RegimeSplitStats(trending, ranging)` record surfaced as
  `BacktestReport.buyByRegime`/`sellByRegime` and a new
  `printRegimeExpectancy` block. New `RegimeCalibrationTest` is this story's
  evidence deliverable; `BacktestHarnessTest`/`RegimeCalibrationTest` both
  assert the regime split partitions the existing unsplit
  `overallBuy`/`overallSell` totals exactly. `AdxCalculatorTest` hand-derives
  two exact reference values at period=2 using exact-fraction arithmetic (a
  clean one-directional uptrend fixture, where -DM is always zero so DX is
  always exactly 100 regardless of magnitude → ADX=100.0000; an alternating
  up/down chop fixture, hand-solved to exactly 80/3 → ADX=26.6667) — real
  pinned reference values, not tolerance checks, the same rigor
  `BacktestHarnessTpSlTest` established for the TP/SL crossing scan, since
  the two real BTCUSDT/DOGEUSDT fixtures can't provide ground truth for the
  algorithm itself, only evidence under review.

  **Wiring decision (evidence-gated, confirmed with the user as the bar
  before implementation):** `RegimeCalibrationTest`'s actual run against
  both fixtures does *not* clear the bar of "ranging expectancy
  consistently and materially worse than trending, on both fixtures" — the
  evidence is mixed, not confirming the story's premise uniformly. On
  BTCUSDT, ranging is worse than trending for SELL at every checkpoint
  (e.g. max: trending +0.100% after-cost expectancy vs. ranging -0.227%)
  and roughly a wash for BUY. On DOGEUSDT the result *inverts*: ranging
  BUY/SELL both score higher after-cost expectancy than trending at every
  checkpoint (e.g. SELL max: trending +0.751% vs. ranging +0.641% — still
  both positive, and BUY max: trending +0.046% vs. ranging +0.291%, ranging
  ahead). So `RegimeGatedRuleEngine` stays unwired — `SignalService`/
  `OrderService` still call `SignalRuleEngine.evaluate` directly, unfiltered
  — the same outcome as E8-F3-S1, but for a different reason: E8-F3-S1's
  calibration came back uniformly negative, this story's regime hypothesis
  came back fixture-dependent, which is itself real evidence that "ranging
  = lower-quality signal" doesn't hold uniformly across assets at
  `ADX_TRENDING_THRESHOLD=25`, not just an unlucky calibration. Explicitly
  provisional/pending E8-F4-S1's out-of-sample validation, same caveat
  every other E8 calibration carries. Backend, `src/main/java` for
  `AdxCalculator`/`Regime`/`RegimeClassifier`/`RegimeGatedRuleEngine` (new,
  unwired classes), `src/test/java` for everything else — no
  `SignalService`/`OrderService`/`PlaceOrderRequest` changes.
- E8-F4-S1: Out-of-sample validation of E8-F1-S1's RSI 25/75 threshold shift
  and E8-F3-S1's `WeightedVoteRuleEngine.IndicatorWeights.DEFAULT` — both
  were previously tuned and evaluated on the same two checked-in
  BTCUSDT/DOGEUSDT fixtures, with no held-out data. New
  `backend/src/test/resources/backtest/solusdt-daily-history.csv` (1000
  daily candles, same Nov 2023–Jul 2026 window as the existing two
  fixtures, a symbol neither calibration has ever seen) plus a
  chronological 70/30 split within BTCUSDT/DOGEUSDT (tune-on-earlier,
  hold-out-on-later) feed a new `backtest.OutOfSampleValidationTest`,
  `src/test/java`-only like every other E8 calibration test. Findings:
  the weighted-vote `IndicatorWeights.DEFAULT` (all-zero) replicates
  cleanly — every indicator's held-out/untouched-fixture combined
  after-cost expectancy stayed negative. The RSI 25/75 shift does **not**
  replicate uniformly: its SELL-side improvement holds across all three
  assets, but its BUY-side improvement — an equally central part of the
  original finding — reverses on two of the three out-of-sample checks,
  including the fully-untouched SOLUSDT fixture. Per the confirmed scope
  boundary (validate, not re-tune), `RULE_TABLE_VERSION` stays at v2 and
  the shipped 25/75 thresholds are unchanged; the BUY-side mismatch is
  reported as a flagged finding for a future recalibration story rather
  than acted on here. E8-F3-S2's regime filter is out of scope (not named
  in this story's AC). See `docs/CHANGELOG.md`'s E8-F4-S1 entry for the
  full per-fixture figures.
- E8-F5-S1: Live signal-drift monitoring, closing out E8 — periodically (or
  on-demand) re-scores the rule table's live performance against production
  `OrderAuditEntry` rows, since neither `Order` nor `OrderAuditEntry` record
  a trade's exit outcome, so scoring one means re-fetching real forward
  market data and running the same TP/SL walk-forward scan `BacktestHarness`
  uses. That scan (`score`/`findFirstCrossing`/`percentChange`, now `public`
  static methods on a new `backtest.WalkForwardScorer`) plus its supporting
  types (`BacktestConfig`, `Checkpoint`, `CheckpointStats`,
  `DirectionalOutcome`, `DirectionalOutcomeStats`, `DirectionalScoreResult`,
  `ExitReason`, and a newly-top-level `DirectionalAccumulator`, promoted out
  of `BacktestHarness`'s private nested class) were promoted from
  `src/test/java` to `src/main/java` — a pure relocation/reshape (confirmed
  via unchanged `BacktestHarnessTpSlTest`/`BacktestHarnessTest`/every other
  E8 calibration test after the move, all still green) except one load-
  bearing signature change: `findFirstCrossing`/`score` now take {@code
  forwardCandles} (candles strictly after the decision day) instead of
  `(candles, decisionIndex)`, since a live decision point has no single
  contiguous fixture series to index into. New `monitoring` package:
  `LiveSignalDriftService` (mirrors `notification.WatchlistSignalPoller`'s
  shape — batches `MarketDataService.getPriceHistory` once per distinct
  ticker, catches/logs/skips per-ticker market-data failures without
  aborting the run) replays `OrderAuditEntry` rows with `resultStatus` in
  `{FILLED, PARTIALLY_PROTECTED}` (both mean the entry leg actually filled —
  real exposure existed) using each entry's frozen `SignalCallEntry`
  call/hold-term/decision price, grouped by `rule_table_version` and
  BUY/SELL into a `DirectionalAccumulator`, and compares it against
  `LiveDriftBaseline`'s pinned current-version (`v2`) BUY/SELL
  `expectancyPctAfterCosts` at MIN/MID/MAX — real figures derived by running
  `BacktestHarness` against the checked-in BTCUSDT/DOGEUSDT fixtures
  (combined call-count-weighted across both, the same combination
  `IndicatorExpectancyCalibrationTest` established for
  `WeightedVoteRuleEngine.IndicatorWeights.DEFAULT`), not fabricated
  placeholders, and re-derived/pinned by a companion `LiveDriftBaselineTest`.
  Flags `possibleDecay` only when a checkpoint's live sample meets a
  configured minimum size AND trails the baseline by more than a configured
  threshold (`monitoring.live-drift.min-sample-size`/`decay-threshold-pct`,
  both explicit uncalibrated placeholders) — never on a small sample alone.
  `GET /api/monitoring/signal-drift` (optional `lookbackDays` override,
  normal session auth, no new `SecurityConfig` carve-out) and an
  `@Scheduled`/`@ConditionalOnProperty`-gated job (`monitoring.live-drift.*`
  config, `enabled` default `true`) both call the same `computeDrift`
  method; ephemeral only, per confirmed scope — no new table, no persisted
  report, recomputed fresh every call. `OrderAuditEntryRepository`'s new
  query `JOIN FETCH`es `ticker`/`signalCallEntry`/`indicatorSnapshot` eagerly
  rather than relying on `@Transactional`, since the `@Scheduled` method
  calls `computeDrift` via same-class self-invocation, which bypasses
  Spring's transactional proxy — a real instance of this file's own
  documented lazy-association gotcha, caught before it could bite. No
  `OrderService`/`SignalService`/`SignalRuleEngine`/`PlaceOrderRequest`/
  `OrderAuditEntry` write-path changes, no frontend changes. Docker wasn't
  available in the sandboxed worktree this story was implemented in, so
  live verification against the real running app (the `run` skill's normal
  path) fell back to a real full-Spring-context `@SpringBootTest`
  (`SignalDriftControllerIntegrationTest`) that re-enables the feature via
  `@TestPropertySource` and drives it through actual HTTP/session auth/JSON
  against H2 in Oracle-compatibility mode — see `docs/CHANGELOG.md`'s
  E8-F5-S1 entry for the full account. E8 is now complete.
- E8-F1-S2: BUY-side RSI recalibration, following up on E8-F4-S1's flagged
  finding. `RsiOversoldRecalibrationTest` swept `rsiOversold` candidates
  24-32 (holding `rsiOverbought` fixed at the already-validated 75) against
  a tuning window, then all three of E8-F4-S1's out-of-sample surfaces.
  Result: no ship. Every candidate produces byte-identical BUY-side
  outcomes — `rsiOversold` has no measurable effect on BUY-side
  classification in this data, so there was never a BUY-side fix available
  on this axis; E8-F1-S1's original BUY-side gain turns out to have been a
  knock-on effect of the `rsiOverbought` move, not the oversold move. Worse,
  `rsiOversold` *does* affect the SELL side (via RSI-bullish votes
  suppressing would-be SELL calls into `CONFLICTING_SIGNALS`), and reverting
  it to the pre-tuning 30 makes SELL-side after-cost expectancy worse on
  BTCUSDT/SOLUSDT at every checkpoint (mixed on DOGEUSDT) versus the
  current 25. So reverting would only cost the already-working SELL side
  for zero BUY-side benefit. `RULE_TABLE_VERSION`/thresholds stay at v2,
  25/75, unchanged — `SignalRuleEngine`'s class Javadoc documents this
  closed finding, same treatment E8-F3-S2 gave its own mixed regime
  evidence. The original E8-F4-S1 BUY-side mismatch remains open, flagged
  as not fixable via `rsiOversold` alone.

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
- `signal` — `SignalRuleEngine` (versioned rule table, safety gates + 2-of-3
  directional vote → BUY/SELL/HOLD), `HoldTermCalculator` (versioned day-range
  table), `SignalCallEntry` audit log. `WeightedVoteRuleEngine` (E8-F3-S1) is
  an alternative, deliberately **unwired** weighted-vote scoring layer —
  reuses `SignalRuleEngine.computeVotes`/`IndicatorVotes`, not called by
  `SignalService`/`OrderService`. `IndicatorId` (RSI/MACD/MA_CROSSOVER) keys
  per-indicator data for both. `RegimeGatedRuleEngine` (E8-F3-S2) is another
  deliberately **unwired** class — a pure `SignalRuleId x Regime ->
  SignalRuleId` post-filter (`applyGate`) that suppresses a directional call
  in a RANGING regime, composable with either `SignalRuleEngine` or
  `WeightedVoteRuleEngine`'s output; `Regime`/`RegimeClassifier` classify an
  `AdxCalculator` reading into TRENDING/RANGING.
- `backtest` (`src/main/java`, E8-F5-S1) — the TP/SL-aware walk-forward scoring
  primitives promoted out of the test-only `BacktestHarness` so live signal
  monitoring can reuse them: `WalkForwardScorer` (`score`/`findFirstCrossing`/
  `percentChange`, taking a `forwardCandles` slice rather than a full candle
  list + index), `DirectionalAccumulator` (top-level now, was a private nested
  class), `BacktestConfig` (TP/SL %, deadband, transaction-cost bps —
  diagnostic placeholders), `Checkpoint`, `CheckpointStats`,
  `DirectionalOutcome`, `DirectionalOutcomeStats`, `DirectionalScoreResult`,
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
- `monitoring` (E8-F5-S1) — `LiveSignalDriftService` (`@Scheduled`/
  `@ConditionalOnProperty`-gated, mirrors `notification.WatchlistSignalPoller`'s
  batch-per-ticker/catch-and-skip shape) re-scores the rule table's live
  performance against `FILLED`/`PARTIALLY_PROTECTED` `OrderAuditEntry` rows
  using `backtest.WalkForwardScorer` against real forward market data, grouped
  by `rule_table_version` and BUY/SELL; `LiveDriftBaseline` pins the current
  version's original-backtest `expectancyPctAfterCosts` figures to diff
  against. `SignalDriftController` (`GET /api/monitoring/signal-drift`) and
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
