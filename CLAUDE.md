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
shipped no threshold change (see its entry below for why). E8-F1-S3, a
second follow-up added after E8-F1-S2 traced the BUY-side gain to
`rsiOverbought` instead of `rsiOversold`, is also done and also shipped no
threshold change — see its entry below for why, and for how it closes out
the E8-F4-S1 BUY-side mismatch as a flagged, understood, not-fixable-via-
either-RSI-bound-alone finding, by flagging per-asset thresholds as the one
untried mechanism. E6-F3-S3, a third followup backlog story — this one
added to E6's F6.3 (Audit log) rather than E8 — is also done: it builds the
audit-trail viewer that E6-F3-S1/S2 repeatedly deferred, found during a
general sweep for overdue flagged findings rather than tied to any specific
prior story's own follow-up note. E8-F1-S4, a fourth follow-up (back on
F8.1), implements the per-asset-thresholds mechanism E8-F1-S3 flagged as
untried — a per-symbol `rsiOverbought` override — and is also done; unlike
S2/S3 it did ship a change (`RULE_TABLE_VERSION` v2→v3, one symbol-specific
override), see its entry below for the per-symbol sweep and why only one of
the three calibrated symbols confirmed. E8-F1-S5, a fifth follow-up (also
F8.1), tries the one non-RSI axis E8-F1-S2/S3 named as untried — a MACD
histogram-magnitude vote gate — and is also done; like S2/S3 (and unlike
S4) it shipped no threshold change, see its entry below for the per-symbol
sweep and why no candidate cleared the ship bar on all three symbols at
once. E8-F4-S2, a sixth follow-up (back on F8.4, alongside E8-F4-S1 rather
than F8.1), closes the one gap E8-F4-S1 explicitly left open — out-of-sample
validation of `RegimeGatedRuleEngine` (E8-F3-S2), the one E8-F3 mechanism
E8-F4-S1's own AC never named — and is also done; the engine stays unwired,
see its entry below for why (SELL side confirms out-of-sample, BUY side
doesn't, so the uniform-across-all-three-symbols bar isn't met). E2-F1-S4, a
backlog story added to E2's F2.1 after E2-F1-S3 explicitly scoped out
holiday/early-close handling ("out of scope for v1"), is also done — see
its entry below for the hardcoded 2024-2027 calendar and early-close cutoff.
E8-F3-S3, a seventh E8 follow-up (back on F8.3, alongside E8-F3-S1/S2),
wires `RegimeGatedRuleEngine`'s regime filter into production for SELL
calls only — found during a review of open flagged findings across E8,
picking up the one gap E8-F4-S2 computed but didn't act on: its SELL-side
out-of-sample evidence was clean and uniform across all three symbols even
though the combined BUY+SELL engine stayed unwired. Also done; unlike
E8-F1-S4/S5 it did ship a real behavior change (`RULE_TABLE_VERSION`
v3→v4) — see its entry below for the crypto-only scoping decision and the
recomputed `LiveDriftBaseline` SELL figures. E8-F1-S6, an eighth E8
follow-up (back on F8.1, alongside E8-F1-S2 through S5), tries the last
axis those stories left untried — MA-crossover magnitude thresholding —
and is also done; like S2/S3/S5 it shipped no threshold change, see its
entry below for the per-symbol sweep and why SOLUSDT's own held-out tail
preferred no filter at all while BTCUSDT/DOGEUSDT each wanted a nonzero
one, the same asset-dependent conflict every prior E8-F1 axis hit. E2-F1-S5,
a follow-up to E2-F1-S4 back on E2's F2.1, extends the hardcoded
holiday/early-close calendar from 2024-2027 to 2024-2029 per that story's
own "data-only addition when it's next needed" flag, and is also done —
see its entry below for the newly-added years and the 2027-12-31 observed-
holiday gap the extension surfaced and fixed. E8-F3-S4, a ninth E8
follow-up (back on F8.3, alongside E8-F3-S1/S2/S3), calibrates
`ADX_TRENDING_THRESHOLD` per ticker symbol on the BUY side — the
per-symbol mechanism E8-F4-S2 named after finding the BUY-side regime
effect is fixture-dependent, mirroring how E8-F1-S4 resolved the same
kind of asset-divergent conflict for `rsiOverbought`. Also done, and also
shipped no threshold change: unlike E8-F1-S4's RSI sweep, no symbol here
even produces a confirmable tuning-window winner — see its entry below for
why BTCUSDT's tuning-window winners reverse on its own held-out tail while
DOGEUSDT/SOLUSDT's tuning windows never produce a winner to begin with.
E8-F3-S5, a tenth E8 follow-up (back on F8.3, alongside E8-F3-S1/S2/S3/S4),
re-attempts `WeightedVoteRuleEngine.IndicatorWeights.DEFAULT`'s calibration
at longer horizons than E8-F3-S1's original fixed 5-day one, per that
record's own Javadoc note naming a longer horizon as the one untried lever
behind its all-zero result. Also done, and this time it did ship a value
change (unlike E8-F1-S2/S3/S5/S6/E8-F3-S4's own no-ship precedent): MACD's
weight moved from 0.000 to 0.714, confirmed positive on both the tuning
set and all three held-out/untouched surfaces independently at a 15-day
horizon — see its entry below for why MA-crossover's own similarly-
positive-looking tuning result didn't survive the same held-out check.
`WeightedVoteRuleEngine` itself stays unwired regardless — this only
changes the constant, not `SignalService`/`OrderService`'s call path.
E8-F1-S7, an eleventh E8 follow-up (back on F8.1), evaluates the per-symbol
RSI override (E8-F1-S4) and the SELL-side regime gate (E8-F3-S3) against
AAPL — this repo's first stock fixture, added specifically because both
mechanisms were crypto-only for lack of any stock evidence. Also done, and
also no-ship on both axes: AAPL's RSI-overbought tuning-window winner (76)
reverses sharply on its own held-out tail (candidate 68 — the *worst*
tuning-window candidate — wins there instead), and AAPL's SELL-side regime
split actively contradicts the crypto-wide pattern the SELL gate already
relies on (ranging beats trending on AAPL, not trending beats ranging) —
see its entry below for the full figures. Neither `PerSymbolRuleThresholds`
nor `RegimeGatedRuleEngine`'s crypto-only scoping changes; no
`RULE_TABLE_VERSION` bump. E8-F2-S3, the twelfth and last E8 follow-up
(back on F8.2, alongside E8-F2-S1/S2), closes out the one gap E8-F2-S2
itself flagged as out of scope — Binance Futures perpetual funding-rate
carry cost, which (unlike that story's flat `TRANSACTION_COST_BPS`) scales
with how long a position is actually held. Also done: `BacktestConfig`
gained `FUNDING_RATE_BPS_PER_PERIOD`/`FUNDING_PERIOD_HOURS`, and
`CheckpointStats.expectancyPctAfterCostsAndFunding()` scales that cost by
each checkpoint's own average holding duration (tracked end-to-end from
`WalkForwardScorer.score`'s crossing/horizon-expired paths through
`DirectionalScoreResult`/`DirectionalAccumulator`) rather than applying it
once flat — purely additive to the backtest report, no
`RULE_TABLE_VERSION` bump, no production wiring change, no change to
`LiveSignalDriftService`/`LiveDriftBaseline`'s live comparison (confirmed
scope boundary, same backtest-report-only precedent E8-F2-S1/S2 set). This
closed out all 6 backlog stories filed in the same batch as E8-F1-S6/S7 and
E8-F3-S4/S5, at which point E8's backlog was fully complete — until a
second batch of four more follow-ups (E8-F1-S8 through S11) was filed,
found during a sweep for flagged-but-never-converted findings: E8-F1-S5/S6
each left a SELL-side secondary finding unactioned (chartered for the
BUY-side mismatch instead), and E8-F1-S6's own closing note named
per-symbol MACD/MA thresholds as the one BUY-side mechanism still untried.
E8-F1-S8, the first of that new batch, extends E8-F1-S4's per-symbol
mechanism to `macdMinHistogramMagnitudePct` and is now done — SOLUSDT ships
`macdMinHistogramMagnitudePct = 0.10` (composed alongside its existing
`rsiOverbought = 70` override), BTCUSDT/DOGEUSDT ship no override,
`RULE_TABLE_VERSION` bumps v4→v5 — see its entry below for the full
per-symbol figures and the SOLUSDT SELL-side effect it documents.
E8-F1-S9, the second story of that batch, tried to wire the MACD
histogram-magnitude filter in for SELL calls specifically — the SELL-side
finding E8-F1-S5/S8 each flagged but left unactioned — and is now done,
shipping no change: unlike E8-F1-S8's per-symbol mechanism, this story's
AC calls for one global value wired uniformly across all three symbols,
and BTCUSDT's own SELL-side tuning window never produces a single
candidate that beats its own baseline at every checkpoint, so the bar
fails before DOGEUSDT/SOLUSDT's own (real, but non-overlapping) winners
are even relevant — see its entry below for the full per-symbol figures.
E8-F1-S10, the third story of that batch, extends E8-F1-S4/S8's
per-symbol mechanism to a third axis, `maMinSeparationPctOfPrice` — the
MA-crossover axis E8-F1-S6's own global sweep found asset-divergent — and
is now done, shipping no override for any symbol: under the stricter
tune-then-confirm bar E8-F1-S4/S8 established, BTCUSDT's only
tuning-window winners either reverse sharply on a degenerate held-out
sample or produce zero held-out BUY calls to confirm against, while
DOGEUSDT/SOLUSDT's own tuning windows never produce a winner to begin
with — see its entry below for the full per-symbol figures.
`RULE_TABLE_VERSION` stays v5, the same no-production-change treatment
E8-F1-S6/S9 established, since no symbol's override actually ships.
E8-F1-S11, the fourth and last story of this batch, wires the
MA-crossover separation filter in for SELL calls specifically — the same
SELL-only wiring attempt E8-F1-S9 made on the MACD axis, but this time it
ships: `ma>=2.00%` clears the uniform-across-all-three-symbols
tuning-window bar E8-F1-S9 failed to clear, and confirms on all three
symbols' own held-out tails too. New `MaCrossoverSellGate` is wired into
`SignalService.computeSignalWithProvenance` for crypto tickers only,
`RULE_TABLE_VERSION` bumps v5→v6, and `LiveDriftBaseline`'s SELL
constants are recomputed against both wired SELL-only gates' combined
behavior — see its entry below for the full figures. This closes out the
second E8-F1 follow-up batch (E8-F1-S8 through S11); E8's backlog is
fully complete again, until a future sweep finds more flagged,
never-converted findings. E8-F3-S6, found in exactly that kind of sweep,
calibrates `WeightedVoteRuleEngine.WEIGHTED_MAJORITY_FRACTION` — a
constant flagged as sweep-worthy since E8-F3-S1 but structurally inert
until E8-F3-S5 gave `IndicatorWeights.DEFAULT` a real nonzero weight to
act on. Also done, and also no-ship (stays 0.5): the constant's entire
real-valued range collapses to exactly three behavioral regimes given the
current weights, and the two regimes that differ from the shipped default
are a tie (0.00, provably byte-identical on the real fixture data) or
strictly worse (anything above 1, which produces zero calls since
BULLISH_UNANIMOUS/BEARISH_UNANIMOUS never fire in this data) — see its
entry below for the full mathematical-and-empirical breakdown. E8-F1-S12,
found in the same kind of sweep, evaluates AAPL — this repo's only stock
fixture — against the two axes that shipped real production changes since
E8-F1-S7 first gathered stock evidence (the per-symbol
`macdMinHistogramMagnitudePct` mechanism, E8-F1-S8, and the already-wired
`MaCrossoverSellGate`, E8-F1-S11), neither of which had ever been checked
against a stock before. Also done, also no-ship on the fresh per-symbol
sweep of both axes (AAPL's tuning window genuinely produces winners this
time, unlike DOGEUSDT/SOLUSDT's own "no tuning-window winner to begin
with" shape, but none confirm on AAPL's own held-out tail) — and a
distinct, narrower check found the already-shipped `MaCrossoverSellGate`
value (`ma>=2.00%`) actively makes AAPL's own SELL-side expectancy worse,
not better, at every one of six checkpoints checked, directly
contradicting the crypto-wide finding that gate's scoping already relies
on. `MaCrossoverSellGate.sellGateAppliesTo` stays crypto-only, same as
before, but now backed by real contradicting stock evidence rather than
merely absent evidence — see its entry below for the full figures.

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
- E2-F1-S4: Holiday/early-close calendar. `MarketHoursService` gained a
  hardcoded NYSE/NASDAQ full-holiday and 1:00pm-ET-early-close date set for
  2024-2027 (a bounded near-term range, not a computed Easter/nth-weekday
  calendar — same "hardcoded, no library" precedent as the rest of this
  class), checked in `isRegularMarketOpen()` right alongside the existing
  weekend/09:30-16:00 check: a full holiday returns the existing
  `MARKET_CLOSED` state unchanged since E2-F1-S3, and an early-close day
  swaps the 16:00 close for 13:00 rather than presenting the afternoon as
  still-live. Dates outside 2024-2027 fall back to the plain calendar with
  no holiday awareness — a known, flagged limit, not a silent gap; extending
  the range is a data-only addition when it's next needed. Backend-only
  (`MarketHoursServiceTest` gained 5 new cases: a fixed federal holiday, the
  day immediately before it, and the early-close open/at-cutoff/after-cutoff
  boundary), no frontend change — the frontend only ever reacted to the
  generic `MARKET_CLOSED` 409, never to calendar specifics itself.
- E2-F1-S5: extended the hardcoded holiday/early-close calendar from
  2024-2027 to 2024-2029, following E2-F1-S4's own explicit "extending the
  range is a data-only addition when it's next needed" flag — added before
  the old range actually ran out, not after. New entries: 2028's full
  holiday set, 2029's full holiday set, both years' Thanksgiving-Friday and
  (where July 4th itself is a weekday holiday, not weekend-shifted)
  day-before-July-4th early closes, computed the same way as the original
  2024-2027 set (nth-weekday federal holidays computed directly, Good
  Friday via the Anonymous Gregorian Easter algorithm, cross-checked
  against the existing 2024-2027 entries' Good Friday dates before trusting
  it for the new years). One real gap the extension surfaced and fixed: New
  Year's Day 2028 falls on a Saturday, so NYSE observes it on the preceding
  Friday, **2027-12-31** — a holiday physically inside the already-shipped
  2027 calendar that E2-F1-S4 missed since it never had a reason to look
  past Jan 1 of the following year. `MarketHoursServiceTest` gained 5 new
  cases: a previously-out-of-range 2028 holiday, the newly-added 2027-12-31
  observed holiday specifically, the 2029 early-close open/at-cutoff
  boundary, and a control proving 2030 (still beyond the new 2024-2029
  range) correctly falls back to the plain no-holiday-awareness calendar
  rather than silently extending forever. Backend-only, same as E2-F1-S4 —
  no frontend change, no `RULE_TABLE_VERSION` bump (this calendar has no
  relationship to the signal rule table). `./mvnw verify`: 511 tests, 0
  failures, up from 506.
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
  story's premise on the real fixture data: BTCUSDT's BEARISH_MAJORITY
  branch has negative expectancy at the min/mid checkpoints (-0.09%/-0.31%)
  despite a near-coin-flip win rate (42.8%/40.7%), while every DOGEUSDT
  branch is expectancy-positive at every checkpoint — exactly the "coin-flip
  win rate can still be unprofitable" gap this story set out to measure.
  Findings feed a future rule-table decision, not acted on here. (Ticker
  labels here and in the original E2-F4-S2 CHANGELOG entry were swapped;
  corrected per the E8-F1-S1 CHANGELOG entry's verified fix.)

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
- E6-F3-S3: The audit-trail viewer itself — a new `GET /api/orders/audit-entries`
  endpoint (newest-first, true page-number pagination via a new
  `OrderAuditEntryRepository.findAllByOrderByLoggedAtDesc(Pageable)` — this
  codebase's first genuinely paginated query; every earlier "list" endpoint is
  limit-only, always page 0) and a new "Audit Trail" dashboard tab
  (`frontend/src/auditentry/AuditTrail.tsx`) rendering it. Folded into the
  existing `OrderQueryController`/`OrderService` rather than new classes —
  `OrderService.listOrders`/`exportOrdersCsv` already establish "read-side
  order-history methods live here, bounds validation lives in the
  controller," so `listAuditEntries` follows the same split rather than
  introducing a parallel service. New `AuditEntryResponse` DTO (ticker, side,
  signal call/matched rule + its rationale, rule-table version, hold-term
  range, result status, rejection reason, entry price, logged-at — enough for
  a row to be self-explanatory without a second lookup, deliberately leaving
  out `IndicatorSnapshot`'s raw RSI/MACD values as backtest/calibration-tool
  territory) and a new generic `common.PagedResponse&lt;T&gt;` wrapper (not
  Spring's own `Page&lt;T&gt;` serialized directly — an unstable wire
  contract). The repository query `JOIN FETCH`es `order`/`ticker`/
  `signalCallEntry` with an explicit `countQuery`, same lazy-init-avoidance
  precedent as E8-F5-S1's own audit-entry query. Design gate (`Plan` agent)
  scoped this before implementation; one deviation from its recommendation,
  made after reading the actual code: it proposed a new `OrderAuditEntryService`
  and a new top-level `/api/audit-entries` resource, but `OrderService`
  already held `listOrders` as a read-side method despite being nominally
  "submission-focused," so extending it matched the codebase's real existing
  precedent better than adding a parallel service class would have found.
  While implementing, also fixed a real, unrelated, long-flagged bug found
  during the same "any overdue findings?" sweep that scoped this story: the
  E2-F4-S2 CHANGELOG entry (and CLAUDE.md's own matching Status line) had
  BTCUSDT's and DOGEUSDT's expectancy findings swapped — E8-F1-S1 had already
  caught this via a CSV price-magnitude check but left it uncorrected to stay
  scoped to its own AC; corrected in both files ahead of this story.
  Frontend: `frontend/src/order/statusTone.ts` extracted out of
  `OrderHistory.tsx` (small dedup — `AuditTrail` needed the same status→tone
  mapping) with no behavior change; new `.audit-trail-table*` CSS (a
  dedicated class, not reusing `.order-history-table` directly, since that
  class's `td:nth-child` font-family overrides are tuned to `OrderHistory`'s
  own column order and would have applied to the wrong columns here).
  Test coverage across all three new layers: `OrderAuditEntryRepositoryTest`
  (new, real H2-in-Oracle-mode round trip — proves the `JOIN FETCH`/explicit
  `countQuery` JPQL and pagination math actually work against a real
  datasource, not just a mock), `OrderServiceTest`/`OrderQueryControllerTest`
  (mapping and validation, mocked collaborators, same pattern as `listOrders`'
  own tests), and a new `OrderAuditControllerIntegrationTest` (real HTTP +
  real session-cookie/CSRF login flow, same shape as E8-F5-S1's
  `SignalDriftControllerIntegrationTest`). Docker wasn't available in this
  session either (same blocker E8-F5-S1 hit, re-confirmed rather than assumed:
  the `local` Spring profile requires real Oracle, and H2 is test-scope only,
  not on the `spring-boot:run` runtime classpath), so that integration test
  stands in for the `run` skill's normal live-browser verification — it
  exercises the real repository query, real service wiring, and real
  session auth end to end, just not a real browser render. `npm run build`/
  `lint`/`test` and `./mvnw verify` both green.

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
- E8-F1-S3: `rsiOverbought` recalibration, following up on E8-F1-S2's own
  finding that traced E8-F1-S1's original BUY-side gain to the
  `rsiOverbought` move rather than `rsiOversold`.
  `RsiOverboughtRecalibrationTest` swept `rsiOverbought` candidates 68-76
  (holding `rsiOversold` fixed at the current 25 — E8-F1-S2 already showed
  it has no BUY-side effect) against the same tuning window, then all
  three of E8-F4-S1's out-of-sample surfaces. Result: no ship, but for a
  different reason than E8-F1-S2. Unlike `rsiOversold`, `rsiOverbought`
  *does* measurably affect the BUY side (confirming E8-F1-S2's hypothesis)
  and, mirroring that finding in reverse, has zero measurable effect on
  the SELL side anywhere in the swept range (exactly 5 distinct SELL-side
  result lines across all 8 candidates x 3 fixtures) — each RSI bound only
  ever moves vote counts on its own opposing side's dissent. But checked
  against the actual pre-tuning 30/70 baseline's out-of-sample BUY-side
  expectancy, the effect is asset-dependent in a way no single value
  resolves: BTCUSDT's BUY side improves as `rsiOverbought` is lowered
  toward 68, DOGEUSDT's improves as it's raised toward 75/76, and
  SOLUSDT's is best near the pre-tuning value of 70 and degrades at both
  swept extremes. No candidate in 68-76 beats the pre-tuning baseline on
  all three surfaces at once — a genuine three-way conflict, not a noisy
  single fixture. `RULE_TABLE_VERSION`/thresholds stay at v2, 25/75,
  unchanged; `SignalRuleEngine`'s class Javadoc documents this closed
  finding. This closes out the E8-F4-S1 BUY-side mismatch as understood
  but not fixable via either RSI bound adjusted alone — a fix, if pursued,
  would need a mechanism neither S2 nor S3 tested (e.g. per-asset
  thresholds, or accepting the fixture-dependence as inherent to RSI at a
  daily-candle horizon).
- E8-F1-S4: per-symbol `rsiOverbought` calibration, implementing the one
  mechanism E8-F1-S3 flagged as untried (per-asset thresholds). New
  `signal.PerSymbolRuleThresholds` resolves `RuleThresholds` per normalized
  ticker symbol (falling back to the global `RuleThresholds.DEFAULT` for
  any symbol without its own evidence, including every stock ticker
  unconditionally), wired into `SignalService.computeSignalWithProvenance`
  right after `IndicatorService.computeForSignal` returns, off the
  persisted ticker symbol — no `SignalRuleEngine`/`OrderService`/
  `PlaceOrderRequest` signature changes, reusing the existing 6-arg
  `evaluate` overload. New `backtest.PerSymbolRsiOverboughtCalibrationTest`
  swept BTCUSDT/DOGEUSDT/SOLUSDT **independently** (unlike every earlier
  E8-F1 test, which pooled BTCUSDT+DOGEUSDT and used SOLUSDT only as a
  held-out check) — each symbol's own 68-76 grid tuned against its own
  first 700 candles, then validated against that *same* symbol's own
  held-out tail (candles 700-1000), never another symbol's data. Result:
  BTCUSDT and DOGEUSDT's own tuning-window winners (both 76) failed
  confirmation — not contradicted, but candidates 71-76 produce
  byte-identical classification on both symbols' own held-out tails, so
  there's no held-out evidence the gain generalizes; both ship no override.
  SOLUSDT's own winner (70, the pre-tuning global value) is a genuine
  confirmation — beats the current default (75) at every checkpoint on
  SOLUSDT's own held-out tail, comparable n (67 vs. 69) — so SOLUSDT alone
  ships `rsiOverbought = 70`. `RULE_TABLE_VERSION` bumps v2 → v3 for the
  resolution mechanism itself, per the confirmed design-gate scope,
  independent of the 1-of-3 override count. New `backtest/FixtureSplits.java`
  test helper extracts the `BTCUSDT`/`DOGEUSDT`/`SPLIT_INDEX = 700` fields
  three other calibration tests had each independently redeclared (a fourth
  redeclaration would have made it quadruplicated), refactoring all three
  to use it, zero behavior change. Knock-on fix caught by `./mvnw verify`:
  `monitoring.LiveDriftBaseline.RULE_TABLE_VERSION` was still the literal
  `"v2"`, which would have silently and permanently dropped the baseline
  comparison for every new (v3) audit entry — fixed by moving the literal
  to `"v3"` (label only; the underlying BUY/SELL figures are unaffected,
  since they're derived only from BTCUSDT/DOGEUSDT, neither of which has an
  override, confirmed by `LiveDriftBaselineTest` passing unmodified).
  `LiveSignalDriftServiceTest`/`OrderCsvExporterTest` needed matching
  literal updates, the same category of version-bump fixture fallout
  E8-F1-S1's CLAUDE.md entry already documents. Docker wasn't available in
  this session (same recurring blocker prior E8/E6 stories hit), so in
  place of a live-browser run, two new `SignalServiceTest` cases exercise
  the real (unmocked) `SignalService` → `PerSymbolRuleThresholds` →
  `SignalRuleEngine.evaluate` path end to end: an RSI=72 boundary case that
  is `NO_STRONG_SIGNAL` under the global default but `BEARISH_MAJORITY` for
  ticker `SOLUSDT` specifically (its own 70 override), plus a control case
  on a non-overridden ticker proving the difference is really the per-symbol
  threshold. `./mvnw verify`: 483 tests, 0 failures. Fixture-exhaustion
  caveat, confirmed with the user before implementation: SOLUSDT was the
  last fixture genuinely untouched by any tuning; after this story none of
  BTCUSDT/DOGEUSDT/SOLUSDT remains clean for a future recalibration story
  to validate against.
- E8-F1-S5: MACD histogram-magnitude calibration axis, the one non-RSI
  mechanism E8-F1-S2/S3 named as untried short of E8-F1-S4's per-symbol RSI
  override. `SignalRuleEngine.RuleThresholds` gains a new
  `macdMinHistogramMagnitudePct` field (default 0, reproducing today's
  any-nonzero-crossover MACD vote exactly) — a MACD crossover only counts
  as bullish/bearish once `|histogram| / price` clears this threshold.
  Normalized as a percentage of price rather than a raw histogram value:
  `MacdCalculator`'s histogram is in raw price units (dollars for BTCUSDT,
  fractions of a cent for DOGEUSDT), so a single global raw-magnitude
  threshold would be meaningless across symbols of very different price
  scales — confirmed with the user before implementation, resolved the
  same way `VolatilityCalculator` already normalizes ATR to a percentage
  of price for the same cross-symbol-comparability reason. Computed inside
  `MacdCalculator.calculate` itself as a new 4th `MacdResult` field
  (`histogramPctOfPrice`) rather than threading a price parameter through
  every `SignalRuleEngine`/`WeightedVoteRuleEngine` call site — far less
  invasive than a signature change rippling through `evaluate`/
  `computeVotes`/`RuleEvaluator` and every caller. New
  `backtest.MacdHistogramMagnitudeCalibrationTest` swept BTCUSDT/DOGEUSDT/
  SOLUSDT independently, same per-symbol tune/held-out design E8-F1-S4
  established (each symbol's own 700-candle tuning window, validated
  against that same symbol's own 300-candle held-out tail), with a grid
  (0%, 0.10%-2.00%) sized from a probe run of real `histogramPctOfPrice`
  values across all three fixtures (medians 0.54%/1.03%/1.09%, maxima up to
  ~2.6%-6.0%). Result: no ship. BTCUSDT's BUY side improves at the MID/MAX
  checkpoints under a magnitude filter but not at MIN; SOLUSDT's BUY side
  improves fairly uniformly around 0.50%-1.00%; but DOGEUSDT's BUY side is
  best with no filter at all (magnitude 0) on both its tuning window and
  its own held-out tail — the same asset-dependent, no-single-value-wins-
  everywhere conflict E8-F1-S3 found for `rsiOverbought`, on a different
  axis. `RULE_TABLE_VERSION` stays v3, `macdMinHistogramMagnitudePct`
  stays 0 (unwired at the default, matching S2/S3's precedent of shipping
  the investigation infrastructure without shipping a value change).
  Secondary finding, flagged but not acted on since this story was
  chartered to fix the BUY-side mismatch specifically: the magnitude
  filter improved SELL-side after-cost expectancy fairly consistently
  across all three symbols at nonzero candidates, unlike either RSI bound
  (which each affected only one side). `SignalRuleEngine`'s class Javadoc
  documents this closed finding. MA-crossover thresholding — E8-F1-S5's
  own named fallback — remains the one axis still untried. Every direct
  `MacdResult`/`RuleThresholds` construction site across the codebase
  (production and test fixtures) updated for the new field; existing test
  fixtures pass `0` for the new `histogramPctOfPrice`/
  `macdMinHistogramMagnitudePct` values, which is always behavior-preserving
  since the default threshold comparison passes for any non-negative
  magnitude. `./mvnw verify`: all green.
- E8-F4-S2: out-of-sample validation of `RegimeGatedRuleEngine` (E8-F3-S2),
  the one E8-F3 mechanism `OutOfSampleValidationTest` (E8-F4-S1) explicitly
  left out of scope ("not named in this story's AC... its calibration was
  already fixture-mixed rather than a clean value to validate"). New
  `backtest.RegimeOutOfSampleValidationTest` reuses `FixtureSplits`'
  existing chronological tune/held-out split verbatim (no new fixture, no
  new split) — confirmed as genuine held-out evidence for this mechanism
  specifically, since `RegimeClassifier.ADX_TRENDING_THRESHOLD` (25) was
  fixed a priori as an industry rule-of-thumb and was never tuned against
  any of the three fixtures, unlike the RSI thresholds or indicator
  weights E8-F4-S1 validated. Replayed each symbol's held-out tail
  (BTCUSDT/DOGEUSDT/SOLUSDT) through `BacktestHarness`'s existing
  `buyByRegime`/`sellByRegime` split, same as `RegimeCalibrationTest` does
  for the full fixtures. Result: mixed, same conclusion as E8-F3-S2's
  original in-sample finding, now confirmed rather than just suspected —
  engine stays unwired. The SELL side holds up cleanly: trending beats
  ranging after-cost expectancy on all three symbols at every checkpoint
  (e.g. max: BTCUSDT +1.003% vs. +0.990%, DOGEUSDT +1.780% vs. -0.973%,
  SOLUSDT +1.233% vs. +0.469%). The BUY side doesn't: ranging actually
  *beats* trending on BTCUSDT (max -0.795% vs. -0.077%) and DOGEUSDT (max
  +1.164% vs. +1.280%), and only SOLUSDT favors trending (max +0.400% vs.
  -0.898%) — the same fixture-dependent pattern E8-F1-S3/E8-F1-S5 each
  found on their own axes. Per this story's AC, wiring `applyGate` into
  `SignalService`/`OrderService` requires ranging to be uniformly and
  materially worse than trending *across all three symbols* — met for
  SELL, not for BUY — and since `applyGate` gates both directions
  identically with no BUY/SELL split, the mechanism as specified doesn't
  clear that bar. `RegimeGatedRuleEngine` and `RegimeCalibrationTest`'s
  class Javadocs updated to record this closed finding (previously said
  "unvalidated out-of-sample, pending a future story"); no `RULE_TABLE_VERSION`
  bump, no `SignalService`/`OrderService` change. Assertions are
  structural only (partition invariants), same as every other E8
  calibration test — the printed report is the evidence under review.
  `./mvnw verify`: all green.
- E8-F3-S3: wires the regime filter into production for SELL calls only,
  picking up the one gap E8-F4-S2 computed but didn't act on: its SELL-side
  out-of-sample evidence held cleanly and uniformly across all three
  symbols even though the combined BUY+SELL mechanism stayed mixed and
  unwired. `RegimeGatedRuleEngine` gains `applySellGate` (SELL-only —
  BUY calls and every HOLD-cause rule pass through unchanged regardless of
  regime) and `sellGateAppliesTo(AssetType)`, scoped to `AssetType.CRYPTO`
  only — confirmed with the user before implementation, following the same
  reasoning `PerSymbolRuleThresholds`'s own Javadoc already established for
  its per-symbol RSI override: zero stock evidence exists anywhere in this
  backlog, so extrapolating onto stock tickers would repeat that exact
  mistake. Unlike a per-symbol RSI *value*, ADX/regime is treated as a
  general trend-persistence mechanism the evidence found works uniformly
  across every crypto symbol tested, so it's scoped to the whole asset type
  rather than a fixed per-symbol allow-list. `IndicatorService.
  IndicatorComputation` gained a third field (`adx`, computed but never
  persisted to `IndicatorSnapshot` or exposed on `IndicatorResponse` — a
  `RegimeGatedRuleEngine` concern only, matching how the class already
  stayed out of `NO_STRONG_SIGNAL`'s rationale string); `SignalService.
  computeSignalWithProvenance` resolves the ticker's asset type and, for
  crypto, classifies that `adx` into a `Regime` and calls `applySellGate`
  on the rule-table match before `HoldTermCalculator`/persistence — the
  same "resolve something extra, apply it after `evaluate` returns" seam
  E8-F1-S4's `PerSymbolRuleThresholds` wiring already established.
  `RULE_TABLE_VERSION` bumps v3→v4: unlike E8-F1-S4/S5's own no-value-change
  version bumps, this one changes a real resolved `SignalRuleId` for a real
  input class (a crypto SELL call in a ranging regime). `BacktestHarness.run`
  gained a 5th-arg `applySellRegimeGate` overload (default `false`,
  zero behavior change for every existing caller) so `LiveDriftBaselineTest`
  could replay the gated behavior against the real BTCUSDT/DOGEUSDT
  fixtures — `LiveDriftBaseline`'s SELL constants were genuinely
  *recomputed* against that gated run, not just relabeled like E8-F1-S4's
  v2→v3 bump was: a live v4 SELL audit entry can only ever be a
  trending-regime call now, so the old ungated-pooled SELL figures no
  longer describe what a v4 SELL call looks like. Every checkpoint moved up
  from its v3 value (MIN -0.019962→0.033652, MID 0.159881→0.180769, MAX
  0.153708→0.222951 after-cost expectancy %) — expected, since dropping the
  ranging-regime SELL calls removes exactly the calls E8-F4-S2 found
  perform worse. BUY constants are unaffected and confirmed byte-identical
  by `LiveDriftBaselineTest`'s own unchanged (ungated) BUY assertions. Found
  a real, previously-unnoticed hardcoded-version-literal fixture the same
  way E8-F1-S4's own CHANGELOG entry flagged as a recurring risk:
  `OrderCsvExporterTest` asserted a literal `"...,v3,..."` CSV row (the
  version came from the real, dynamically-read `SignalRuleEngine.
  RULE_TABLE_VERSION`, not a mock) — fixed to `"...,v4,..."`. New tests:
  `RegimeGatedRuleEngineTest` (pure enum-in/enum-out coverage of
  `applySellGate`/`sellGateAppliesTo`, mirroring `applyGate`'s existing
  style), four new `SignalServiceTest` cases exercising the real
  (unmocked) `SignalService`→`RegimeGatedRuleEngine` path end to end
  (ranging-regime crypto SELL suppressed, trending-regime crypto SELL
  control, ranging-regime crypto BUY unaffected, ranging-regime stock SELL
  unaffected — proving the regime-driven suppression, the BUY passthrough,
  and the crypto-only scoping are each independently real, not assumed),
  and a new `BacktestHarnessTest` case pinning the gated overload
  structurally (gated SELL total = ungated SELL total minus its
  ranging-regime calls; BUY totals identical either way) on both
  BTCUSDT/DOGEUSDT fixtures. Docker wasn't available in this session (same
  recurring blocker prior E8/E6 stories hit), so the four real-`SignalService`
  `SignalServiceTest` cases above stood in for the `run` skill's normal
  live-browser verification, same fallback E8-F1-S4 used. `./mvnw verify`:
  504 tests, 0 failures, up from 493 — no frontend changes (`SignalResponse`'s
  shape is unchanged — only which `SignalRuleId` a crypto SELL call can
  resolve to, for inputs that already existed).
- E8-F1-S6: MA-crossover-magnitude calibration axis, the last mechanism
  E8-F1-S5 named as untried after RSI bounds (E8-F1-S2/S3) and MACD
  histogram magnitude (E8-F1-S5) each failed to fix the BUY-side
  out-of-sample mismatch uniformly across all three symbols.
  `MovingAverageResult` gains a new `separationPctOfPrice` field
  (`|shortMa - longMa| / lastClose * 100`, scale-4 HALF_UP), computed in
  `MovingAverageCrossoverCalculator.calculate` — normalized against the
  candle series' last close, the same normalization basis E8-F1-S5's
  `MacdResult.histogramPctOfPrice` established, for direct cross-symbol
  comparability with that precedent. `SignalRuleEngine.RuleThresholds`
  gains a matching trailing `maMinSeparationPctOfPrice` field (default 0
  via a new `MA_MIN_SEPARATION_PCT_OF_PRICE` constant, reproducing today's
  any-crossover-counts behavior exactly); `computeVotes`'s `maBullish`/
  `maBearish` reads are gated by it the same way `macdMinHistogramMagnitudePct`
  already gates the MACD vote. Every existing `MovingAverageResult`/
  `RuleThresholds` construction site across the codebase (23 call sites,
  15 files, including a fully-qualified `new SignalRuleEngine.RuleThresholds(...)`
  site in `PerSymbolRuleThresholds` that a plain `new RuleThresholds(`
  grep missed on the first pass and had to be caught by a compile error)
  updated for the new trailing argument — `BigDecimal.ZERO` for test
  fixtures, always behavior-preserving under the default `>= 0` gate. A
  throwaway probe (written, run once, deleted before committing, same
  precedent as E8-F1-S5's own MACD probe) found real `separationPctOfPrice`
  values across each fixture's own tuning window ranging up to roughly
  13.66% (BTCUSDT), 36.19% (DOGEUSDT), and 23.94% (SOLUSDT), with medians
  of 3.20%/6.97%/6.54% respectively — considerably coarser than E8-F1-S5's
  MACD-histogram medians (0.54%/1.03%/1.09%), sizing a `{0.00, 1.00, 2.00,
  3.00, 4.00, 5.00, 7.00, 10.00}` percent candidate grid for the new
  `backtest.MaCrossoverSeparationCalibrationTest`, which swept
  BTCUSDT/DOGEUSDT/SOLUSDT independently against their own
  `FixtureSplits` 70/30 tune/held-out windows, the same per-symbol design
  E8-F1-S4/S5 established. Result: no ship. On their own held-out tails,
  BTCUSDT's BUY side is best around 1.00% separation (beats the no-filter
  baseline's after-cost expectancy at all three checkpoints, n=64 vs. 80)
  and DOGEUSDT's is best around 2.00% (same all-three-checkpoints
  improvement, n=33 vs. 47) — but SOLUSDT's BUY side is best with *no*
  filter at all: every nonzero candidate in the swept range makes it worse
  at every checkpoint, directly conflicting with what BTCUSDT/DOGEUSDT each
  want. No single value clears the all-three-symbols-simultaneously ship
  bar, the same asset-dependent, no-single-value-wins-everywhere pattern
  every prior E8-F1 axis hit. `MA_MIN_SEPARATION_PCT_OF_PRICE` stays 0,
  `RULE_TABLE_VERSION` stays v4 — `SignalRuleEngine`'s class Javadoc gained
  a new paragraph documenting this closed finding, following the same
  E8-F1-S2/S3/S5-style no-ship treatment (ship the investigation
  infrastructure, not a value). Secondary finding, flagged but not acted
  on (same pattern as E8-F1-S5's own secondary MACD finding): a ~2.00%
  separation threshold improved SELL-side after-cost expectancy uniformly
  across all three symbols at every checkpoint on their own held-out
  tails, but this story was chartered for the BUY-side mismatch
  specifically. This closes out the list of axes E8-F1-S2/S3/S5 named as
  untried; a future fix would need a mechanism none of E8-F1-S2 through S6
  tried (e.g. per-symbol MA/MACD thresholds, mirroring E8-F1-S4's
  per-symbol RSI approach). Docker wasn't available in this session (same
  recurring blocker prior E8/E6 stories hit); since this story shipped no
  production behavior change (the new field stays at its inert default),
  no live-browser/`SignalServiceTest` end-to-end verification was needed
  beyond the calibration test's own `./mvnw test` run — the same
  no-production-change precedent E8-F1-S2/S3/S5 already established (only
  E8-F1-S4/E8-F3-S3, which shipped real behavior changes, needed that
  fallback). `./mvnw verify`: 506 tests, 0 failures, up from 504.
- E8-F3-S4: per-symbol `ADX_TRENDING_THRESHOLD` calibration, on the BUY
  side only — the mechanism E8-F4-S2 named after finding the BUY-side
  regime effect is fixture-dependent (ranging beats trending on
  BTCUSDT/DOGEUSDT, trending beats ranging on SOLUSDT), the same
  asset-divergent shape E8-F1-S4 resolved for `rsiOverbought` with a
  per-symbol override. New `RegimeClassifier.classify(BigDecimal,
  BigDecimal)` overload takes an explicit threshold; the existing
  no-arg `classify(BigDecimal)` stays wired unconditionally to the
  global default and is the only one the shipped SELL gate
  (`applySellGate`, E8-F3-S3) ever calls — deliberately never a single
  shared `Regime` value reclassified once per symbol, so a per-symbol
  BUY threshold can never leak into the SELL gate's already-validated
  behavior for the same symbol. New `signal.PerSymbolAdxThresholds`
  (keyed by normalized symbol, falls back to the global default)
  mirrors `PerSymbolRuleThresholds`'s shape; new
  `RegimeGatedRuleEngine.applyBuyGate`/`buyGateAppliesTo(String)` mirror
  `applySellGate`/`sellGateAppliesTo`, but keyed by a fixed per-symbol
  allow-list rather than `AssetType` (the BUY-side effect is
  fixture-dependent, unlike the SELL side's uniform crypto-wide result,
  so wiring can't generalize to the whole asset class the way
  E8-F3-S3's did). `BacktestHarness.run` gained a `regimeThreshold`-
  accepting overload (defaults to the global constant for every
  existing caller) so a new `backtest.PerSymbolAdxTrendingThresholdCalibrationTest`
  could sweep a `{15, 18, 20, 22, 25, 28, 30, 35, 40}` percent-of-price-
  probe-sized grid (probe found real per-fixture ADX ranging roughly
  9-68 across all three fixtures, medians 22-26) through the existing
  `buyByRegime`/`sellByRegime` split, per symbol, against that symbol's
  own `FixtureSplits` 70/30 tune/held-out windows — the same
  per-symbol tune-then-validate design E8-F1-S4 established. Result:
  **no ship, for all three symbols independently.** BTCUSDT's tuning
  window does produce candidates (ADX≥25/28/30) where trending
  uniformly beats ranging's after-cost expectancy at every checkpoint
  with non-degenerate n (e.g. ADX≥25: trending n=55 max +0.975% vs.
  ranging n=120 max -0.155%) — but every one of those reverses on
  BTCUSDT's own held-out tail (same threshold: trending n=38 max
  -0.795% vs. ranging n=42 max -0.077%); the only held-out-tail
  candidate where trending wins (ADX≥35) has a degenerate trending
  bucket (n=3). DOGEUSDT and SOLUSDT both fail earlier than that:
  ranging beats trending at every one of the 9 swept candidates on
  each symbol's own tuning window, so neither ever produces a
  tuning-window winner to validate in the first place — for SOLUSDT
  specifically, this means its tuning window (ranging favored) and its
  own held-out tail (trending favored at the default, per E8-F4-S2)
  actively disagree with each other before any candidate is even
  tested. `PerSymbolAdxThresholds.OVERRIDES` and
  `RegimeGatedRuleEngine.BUY_GATE_CONFIRMED_SYMBOLS` both ship empty;
  `RULE_TABLE_VERSION` stays v4 (no bump — the same "ships only inert,
  unwired investigation infrastructure" precedent E8-F3-S1/S2 and
  E8-F1-S2/S3/S5/S6 established, this time for a per-symbol mechanism
  rather than a single global one). New test coverage:
  `RegimeClassifierTest` (the new 2-arg overload plus a pinned
  byte-identical-to-1-arg-overload check), `PerSymbolAdxThresholdsTest`,
  `RegimeGatedRuleEngineTest` (mirrors the existing `applySellGate`
  block for `applyBuyGate`/`buyGateAppliesTo`), `BacktestHarnessTest`
  (structural pin of the new threshold-accepting overload: an
  ADX≥0 threshold must classify every decision point trending, an
  unreachable threshold must classify every one ranging, and the
  existing 5-arg overload must be byte-identical to the new one called
  with the global default explicitly). Since this story shipped no
  production behavior change, no live-browser/`SignalServiceTest`
  end-to-end verification was needed beyond the calibration test's own
  run — the same no-production-change precedent E8-F1-S2/S3/S5/S6
  established. `./mvnw verify`: 527 tests, 0 failures, up from 506.
- E8-F3-S5: re-attempts `WeightedVoteRuleEngine.IndicatorWeights.DEFAULT`'s
  calibration at a longer horizon, the one untried lever
  `IndicatorWeights.DEFAULT`'s own Javadoc named after E8-F3-S1's original
  fixed-5-day/TP5%/SL3% calibration came back all-zero (confirmed
  out-of-sample by E8-F4-S1). New `WalkForwardScorer.findFirstCrossing`
  overload takes an explicit `takeProfitPct`/`stopLossPct` instead of
  always reading `BacktestConfig`'s fixed constants; new
  `BacktestHarness.runIndicatorExpectancy(candles, horizonDays,
  takeProfitPct, stopLossPct)` replays per-indicator scoring at that
  explicit horizon, reusing `SignalRuleEngine.computeVotes` as the same
  single source of truth for "what counts as a bullish/bearish read" every
  other per-indicator scoring path already uses — deliberately narrower
  than `run`'s full walk-forward loop, since the combined rule table's
  matched-rule/hold-gate/regime bookkeeping isn't horizon-dependent for
  this purpose. New `backtest.IndicatorExpectancyAlternateHorizonCalibrationTest`
  swept two candidates against the same full BTCUSDT/DOGEUSDT tuning
  fixtures the original E8-F3-S1 calibration used: 10 days (2x baseline,
  TP10%/SL6%) and 15 days (TP15%/SL9%, anchored to `HoldTermRule
  .STRONG_LOW`'s own `maxDays` rather than picked arbitrarily) — TP/SL
  scaled proportionally with the horizon rather than held at the 5-day
  baseline's 5%/3%, since a fixed short TP/SL at a longer horizon would
  just mean more decision points fall back to horizon-expiry scoring
  instead of resolving via a genuine crossing. Both candidates found MACD
  positive (10d: +0.289%, 15d: +0.714% combined after-cost expectancy);
  15d also found MA-crossover positive (+0.162%); RSI stayed negative at
  every horizon, never a shipping candidate here. Per the confirmed ship
  bar, the 15-day winners were checked against the same held-out
  BTCUSDT/DOGEUSDT tails plus the untouched SOLUSDT fixture E8-F4-S1 used:
  **MACD held up cleanly** — combined +0.845% after costs, and positive on
  all three individual surfaces independently (BTCUSDT +1.930%, DOGEUSDT
  +0.132%, SOLUSDT +0.746%), not just in aggregate. **MA-crossover did
  not** — its barely-positive combined figure (+0.038%) is carried
  entirely by DOGEUSDT (+1.533%); BTCUSDT (-0.274%) and SOLUSDT (-0.275%)
  are both negative, the same "one fixture masks a two-of-three-
  disagreeing result" pattern every other E8-F1 per-symbol axis already
  found doesn't generalize. Shipped: `IndicatorWeights.DEFAULT.macdWeight`
  0.000 → 0.714 (the tuning-set combined figure, same "ship the tuning-set
  value, confirm it holds out-of-sample" methodology E8-F3-S1/E8-F4-S1
  established); `rsiWeight`/`maCrossoverWeight` stay 0.000.
  `WeightedVoteRuleEngine` stays unwired — `SignalService`/`OrderService`
  still call `SignalRuleEngine.evaluate` directly — but `evaluate`'s own
  behavior changes for future callers/tests: with MACD now the only
  nonzero weight, a lone-or-majority vote that includes a bullish/bearish
  MACD read always clears the weighted-majority bar (MACD's weight already
  equals all of a nonzero `totalWeight`), newly resolving
  BULLISH_MAJORITY/BEARISH_MAJORITY where the unweighted table would call
  NO_STRONG_SIGNAL; a lone RSI-only or MA-only vote still resolves
  NO_STRONG_SIGNAL (their own weight is still zero); UNANIMOUS is
  unaffected (decided off the raw 3-of-3 count, not weight). No
  `RULE_TABLE_VERSION` bump — this constant lives entirely outside
  `SignalRuleEngine`'s table. New test coverage:
  `defaultEvaluate_loneMacdVote_promotesToBullishMajority` (proves the new
  DEFAULT-weight behavior against the unweighted table's own
  NO_STRONG_SIGNAL on the same input) plus an updated
  `defaultEvaluate_usesDefaultWeights` docstring (its own case only
  exercises a lone RSI vote, so it's unaffected by the MACD-weight
  change — the stale "DEFAULT floors every weight to zero" comment it and
  `zeroTotalWeight_loneIndicator_staysNoStrongSignal` carried was
  corrected). Since `WeightedVoteRuleEngine` is never called by
  `SignalService`/`OrderService`, no live-browser/`SignalServiceTest`
  end-to-end verification was needed, the same no-production-change
  precedent E8-F1-S2/S3/S5/S6/E8-F3-S4 established for their own no-ship
  findings — this story ships a real constant change but zero production
  call-path change. `./mvnw verify`: 530 tests, 0 failures, up from 527.
- E8-F1-S7: evaluates the per-symbol `rsiOverbought` override (E8-F1-S4)
  and the SELL-side regime gate (E8-F3-S3) against AAPL — this repo's
  first **stock** fixture, added because both mechanisms were scoped to
  crypto only for lack of any stock evidence anywhere in the backlog. New
  `backend/src/test/resources/backtest/aapl-daily-history.csv` (1000 real
  daily NASDAQ sessions, 2022-08-15 to 2026-08-10, fetched once from Yahoo
  Finance's public `v8/finance/chart` JSON endpoint — Stooq's CSV endpoint,
  the first choice, turned out to have gone behind an anti-bot challenge
  and was ruled out live during design-gate scoping) sized to **row count**
  (1000, matching the crypto fixtures' n and therefore their 70/30
  tune/held-out statistical power) rather than calendar date range, since a
  stock only trades ~252 days/year and matching the crypto fixtures'
  ~2.75-year span would have yielded only ~690 candles — too close to a
  700-candle tuning window to leave a meaningful held-out tail; AAPL's own
  date range runs longer (~4 years) as a result. `FixtureSplits` gained
  `AAPL`/`AAPL_TUNING`/`AAPL_HELD_OUT` plus its own `AAPL_SPLIT_INDEX`
  constant (computed as 70% of AAPL's actual row count rather than reusing
  the crypto fixtures' literal `SPLIT_INDEX`, so a future differently-sized
  stock fixture can't silently inherit the wrong split point). Two new
  tests: `StockPerSymbolRsiOverboughtCalibrationTest` (a fresh 68-76
  tune-then-validate sweep against AAPL's own tuning/held-out split, the
  same methodology `PerSymbolRsiOverboughtCalibrationTest` (E8-F1-S4) used
  per crypto symbol — fresh because AAPL, unlike BTCUSDT/DOGEUSDT, had
  never been swept before, so there was no existing candidate to merely
  replay) and `StockRegimeOutOfSampleValidationTest` (a held-out-only
  replay of the existing global `ADX_TRENDING_THRESHOLD`, mirroring
  `RegimeOutOfSampleValidationTest` (E8-F4-S2)'s own validation-only shape,
  since the threshold itself was never tuned to any fixture). Both come
  back **no-ship**, and for reasons distinct from any prior E8-F1
  no-ship: AAPL's RSI-overbought tuning-window winner (76 — beats the 75
  default at every checkpoint on the tuning window, larger n too, 208 vs.
  202) fails held-out confirmation not by a near-miss but by a sharp
  reversal — held out, candidate 68 (the *worst* tuning-window candidate)
  is the clear winner (max checkpoint +1.009% after-cost expectancy vs.
  76's own held-out +0.304% and the 75-default's +0.279%), the sharpest
  tuning/held-out disagreement anywhere in this backlog. The regime-gate
  check is more pointed still: AAPL's held-out SELL-side split shows
  ranging beating trending at every checkpoint (max: ranging +1.518%
  after-cost vs. trending +0.800%, on a larger n, 41 vs. 8) — the *opposite*
  of the uniform trending-beats-ranging pattern all three crypto symbols
  showed and that `applySellGate`'s crypto-only wiring already relies on.
  Neither `PerSymbolRuleThresholds.OVERRIDES` nor
  `RegimeGatedRuleEngine.sellGateAppliesTo` changes — AAPL keeps falling
  back to `RuleThresholds.DEFAULT` and stays outside the SELL gate's scope,
  same as every stock ticker today — but both classes' Javadocs are
  updated to record that the "zero stock evidence exists" gap they
  previously described is now closed with *negative* evidence (a real
  sweep that didn't confirm, and for the regime gate, one that actively
  contradicts the crypto-wide finding) rather than an absent one. No
  `RULE_TABLE_VERSION` bump, no production wiring change, so no live-
  browser/`SignalServiceTest` end-to-end verification was needed beyond
  the calibration tests' own run — the same no-production-change
  precedent E8-F1-S2/S3/S5/S6/E8-F3-S4 established. `./mvnw verify`: 532
  tests, 0 failures, up from 530.
- E8-F2-S3: funding-rate carry cost, closing the gap E8-F2-S2 itself
  flagged as out of scope (funding is paid periodically and scales with
  hold duration, unlike that story's flat `TRANSACTION_COST_BPS`). New
  `BacktestConfig.FUNDING_RATE_BPS_PER_PERIOD` (3, i.e. 0.03%/8h — roughly
  3x Binance's documented 0.01%/period floor rate, the same "overstate
  cost is the safer failure mode" bias `TRANSACTION_COST_BPS` already
  uses, confirmed with the user before implementation) and
  `FUNDING_PERIOD_HOURS` (8, Binance's real funding settlement cadence).
  `WalkForwardScorer.score`'s two return paths (a TP/SL crossing vs. the
  horizon-expired fallback) now both record how many days forward they
  actually resolved at, threaded through a new `DirectionalScoreResult
  .daysHeld` field into a new per-checkpoint `CheckpointStats
  .avgHoldingDays` (via `DirectionalAccumulator`, shared by
  `BacktestHarness` and `monitoring.LiveSignalDriftService`) that feeds a
  new derived method, `expectancyPctAfterCostsAndFunding()` — exact, not
  an approximation, since funding cost is linear in days held so
  `rate * avg(daysHeld)` over every scored call (win/loss/**wash**, since
  a wash still means a position was held and paid funding) is
  algebraically identical to netting each call's own cost before
  re-averaging, the same identity the existing flat-cost method already
  relies on. `BacktestReport.printCheckpoint` now shows the funding-
  inclusive figure alongside the existing after-costs one, per the AC's
  "with and without funding cost side by side" requirement — purely
  additive at the `CheckpointStats` layer, so it applies everywhere that
  method is already reused (per-rule, overall BUY/SELL, per-indicator,
  regime-split rows) with no per-caller changes. Confirmed with the user
  before implementation, matching E8-F2-S1/S2's own backtest-report-only
  precedent: `LiveSignalDriftService`/`LiveDriftBaseline`'s live-
  monitoring comparison keeps comparing on `expectancyPctAfterCosts()`
  unchanged — wiring the funding-adjusted figure into live drift
  monitoring is a real, separate future story, not folded in here.
  Illustrative, not a ship/no-ship finding: on the real fixtures, funding
  materially widens already-negative branches (BTCUSDT Overall BUY max,
  3.7-day avg hold: after-costs -0.079% → after-costs+funding -0.412%)
  and can erode a positive-after-costs branch toward breakeven (DOGEUSDT
  Overall BUY max, 1.7-day avg hold: +0.197% → +0.042%) — exactly the
  duration-sensitivity a flat per-trade cost couldn't show. No
  `RULE_TABLE_VERSION` bump, no `SignalService`/`OrderService`/
  `PlaceOrderRequest` changes, no schema migration, no frontend changes.
  `./mvnw verify`: 538 tests, 0 failures, up from 532. This was the last
  of the 6 backlog stories filed alongside E8-F1-S6/S7 and E8-F3-S4/S5 —
  E8's backlog was fully complete at that point, before a second batch of
  four more follow-ups (E8-F1-S8 through S11) was filed.
- E8-F1-S8: per-symbol `macdMinHistogramMagnitudePct` calibration on the BUY
  side, the first story of the second E8-F1-S8-through-S11 follow-up batch
  (filed after E8-F2-S3 closed out the first batch), extending E8-F1-S4's
  per-symbol mechanism to this axis — E8-F1-S6's own closing note had named
  per-symbol MACD/MA thresholds as the one still-untried lever after every
  prior E8-F1 axis hit the same asset-divergent, no-single-value-wins-
  everywhere conflict. `PerSymbolMacdHistogramMagnitudeCalibrationTest` swept
  BTCUSDT/DOGEUSDT/SOLUSDT independently against E8-F1-S4's own tune/held-out
  design (each symbol's own 700-candle tuning window, validated against that
  same symbol's own 300-candle held-out tail), reusing E8-F1-S5's original
  0.00%-2.00% candidate grid. Result: BTCUSDT and DOGEUSDT both ship no
  override. BTCUSDT's tuning-window winners (macd&gt;=0.75%/1.00%, both
  beating the magnitude-0 baseline's after-cost expectancy at every
  checkpoint, n=64/39 vs. baseline n=175) each fail held-out confirmation
  specifically at the MIN checkpoint (0.75%: held-out min -0.488% vs.
  baseline -0.425%; 1.00%: held-out min -0.429% vs. baseline -0.425%) even
  though both confirm at MID/MAX. DOGEUSDT's only tuning-window winner
  (macd&gt;=0.10%, n=127 vs. baseline n=132) fails held-out confirmation
  completely — worse than baseline at every one of MIN/MID/MAX on its own
  held-out tail. SOLUSDT ships `macdMinHistogramMagnitudePct = 0.10`,
  composed into `PerSymbolRuleThresholds.OVERRIDES`'s existing SOLUSDT entry
  alongside its E8-F1-S4 `rsiOverbought = 70` override (the first symbol in
  this map with two independently-calibrated non-default fields at once) — a
  genuine, non-degenerate confirmation, beating the magnitude-0 baseline's
  BUY-side after-cost expectancy at every checkpoint on both its tuning
  window (n=186 vs. 188) and its own held-out tail (n=67 vs. 69). Unlike
  either RSI bound (E8-F1-S3 found `rsiOverbought` has zero measurable
  SELL-side effect), this axis gates `computeVotes`'s `macdBullish`/
  `macdBearish` reads symmetrically off the same threshold, so there is no
  way to make it BUY-only at the vote-computation layer without an
  out-of-scope `evaluate` signature change — SOLUSDT's SELL-side
  classification changes too. Checked and pinned down as a real test
  assertion (`shippedSolusdtCandidateAlsoImprovesSellSide`), not just
  printed: the effect is positive at every checkpoint on both windows
  (tuning baseline -0.528%/-0.216%/-0.216%, n=87 → shipped
  -0.399%/-0.073%/-0.072%, n=83; held-out baseline +0.357%/+0.647%/+0.844%,
  n=49 → shipped +0.462%/+0.805%/+0.995%, n=45) — matching the direction
  E8-F1-S5 had already flagged as a consistent-but-unactioned secondary
  finding across all three symbols. Acting on that SELL-side effect beyond
  SOLUSDT (e.g. wiring a SELL-only gate the way E8-F3-S3 did for the regime
  filter) is E8-F1-S9's separate, chartered story, not this one.
  `RULE_TABLE_VERSION` bumps v4 → v5 for the resolution mechanism itself,
  per this story's confirmed scope, regardless of the 1-of-3 override count
  — the same treatment E8-F1-S4's own v2→v3 bump got. Fixture fallout
  caught by `./mvnw verify`: `LiveDriftBaseline.RULE_TABLE_VERSION` needed
  only a label update (BTCUSDT/DOGEUSDT — the only two fixtures that
  baseline is computed from — still resolve to `RuleThresholds.DEFAULT`
  under v5 exactly as under v4, confirmed by `LiveDriftBaselineTest` passing
  unmodified); `OrderCsvExporterTest`'s hardcoded `v4` CSV-row literal and
  `StockPerSymbolRsiOverboughtCalibrationTest`'s `(v4/current default)`
  print-label literal both moved to v5; and a real, more substantive gap
  surfaced by the SOLUSDT MACD override going live — the pre-existing
  E8-F1-S4 `SignalServiceTest` case
  (`solusdtOverride_rsi72BearishOnlyUnderPerSymbolOverride_producesBearishMajority`)
  used a MACD fixture with `histogramPctOfPrice=0`, which SOLUSDT's own new
  0.10% override now gates out entirely (0 &lt; 0.10), dropping that test's
  MACD vote and collapsing its scenario from `BEARISH_MAJORITY` to `HOLD`;
  fixed by bumping that fixture's histogram magnitude to 1.0 (comfortably
  clear of 0.10) in both the SOLUSDT case and its AAPL control, restoring
  the original test's isolation of `rsiOverbought`'s own effect. New
  `signal.PerSymbolRuleThresholdsTest` (this class had no dedicated test
  file before this story) covers unlisted-crypto/stock/unrecognized-symbol
  fallback plus a composition case reading the real SOLUSDT entry, proving
  both its `rsiOverbought`/`macdMinHistogramMagnitudePct` overrides survive
  composition into one record without clobbering each other or leaking into
  the other four fields (still at `DEFAULT`'s values). Two new
  `SignalServiceTest` cases (real, unmocked
  `SignalService` → `PerSymbolRuleThresholds` → `SignalRuleEngine.evaluate`,
  same pattern E8-F1-S4 used) exercise the shipped override end to end: a
  borderline (0.05%-of-price) MACD histogram magnitude that counts as a
  bullish vote under the global default (giving `BULLISH_MAJORITY` with
  RSI's own lone bullish vote) but gets gated out under SOLUSDT's 0.10%
  override (dropping back to a lone RSI vote and `NO_STRONG_SIGNAL`) — this
  story's mechanism *removes* a vote rather than adding a dissenting one,
  the opposite direction from E8-F1-S4's own RSI-widening example. Docker
  wasn't available in this session (same recurring blocker prior E8/E6
  stories hit), so those two `SignalServiceTest` cases stood in for the
  `run` skill's normal live-browser verification, the same fallback
  E8-F1-S4/E8-F3-S3 used for their own shipped-value changes. No frontend
  changes (backend/test-only, same precedent as every other E8-F1 story).
  `./mvnw verify`: 547 tests, 0 failures, up from 538. E8-F1-S9 through S11
  remain open in the second follow-up batch.
- E8-F1-S9: tried to wire `macdMinHistogramMagnitudePct` in for SELL calls
  specifically, mirroring how E8-F3-S3 wired the regime filter for SELL
  only — the SELL-side improvement E8-F1-S5's original global sweep and
  E8-F1-S8's per-symbol BUY-side sweep (SOLUSDT specifically) each flagged
  but left unactioned, chartered here as its own story per those stories'
  own scope notes. Unlike E8-F1-S8's per-symbol mechanism, this story's AC
  calls for one **global** value wired uniformly across every symbol
  (mirroring `RegimeGatedRuleEngine#applySellGate`'s crypto-wide, not
  per-symbol, scope), so the ship bar is stricter: a candidate must beat
  the magnitude=0 SELL-side after-cost-expectancy baseline at every
  checkpoint on **all three** of BTCUSDT/DOGEUSDT/SOLUSDT's own tuning
  windows simultaneously, before even reaching held-out validation. New
  `backtest.SellMacdHistogramMagnitudeCalibrationTest` swept the same
  0.00%-2.00% grid E8-F1-S5/S8 used, per symbol, on the SELL side only.
  Result: no ship, and for the sharpest reason yet on this axis —
  BTCUSDT's own SELL-side tuning window never produces a single candidate
  that beats its own baseline (min/mid/max -0.342%/-0.475%/-0.497%, n=172)
  at all three checkpoints simultaneously: the closest candidate
  (macd&gt;=0.10%) improves mid/max but is slightly worse at min
  (-0.347%), and every candidate at or above 0.25% is worse than baseline
  at every checkpoint. So the global bar fails at the first symbol
  checked, before DOGEUSDT's own real tuning-window winner (macd&gt;=0.75%,
  confirmed on its own held-out tail) or SOLUSDT's own wide winning range
  (macd&gt;=0.10% through 1.50%, also confirmed on held-out) are even
  relevant to the ship decision — neither overlaps a value that also works
  for BTCUSDT, the same asset-dependent, no-single-value-wins-everywhere
  conflict every other E8-F1 axis has hit, now confirmed for the SELL side
  too. `noCandidateClearsTuningWindowBarOnAllThreeSymbolsAtOnce` pins the
  BTCUSDT no-winner finding down as a real assertion, not just a printed
  observation. Net: nothing ships — no new `RuleThresholds` field, no new
  gate class, no `RULE_TABLE_VERSION` bump, no `SignalService` change;
  this story's only artifact is the calibration test itself, the same
  "ship only the investigation, not a value" precedent E8-F1-S2/S3 set
  (rather than E8-F1-S5/S6/S8's precedent of also shipping an inert new
  field, since `macdMinHistogramMagnitudePct` already exists from
  E8-F1-S5 and a SELL-only gate mechanism would be genuinely new code
  this no-ship finding doesn't justify writing). `SignalRuleEngine`'s
  class Javadoc gained a new closing paragraph documenting this finding.
  Since this story shipped no production behavior change, no live-browser/
  `SignalServiceTest` end-to-end verification was needed beyond the
  calibration test's own run — the same no-production-change precedent
  E8-F1-S2/S3/S5/S6/E8-F3-S4 established. `./mvnw verify`: 550 tests, 0
  failures, up from 547. E8-F1-S10/S11 remain open in the second
  follow-up batch.
- E8-F1-S10: per-symbol `maMinSeparationPctOfPrice` calibration, the third
  story of the second follow-up batch, extending E8-F1-S4/S8's per-symbol
  mechanism to this axis — the MA-crossover asset-divergent conflict
  E8-F1-S6's own global sweep found (BTCUSDT prefers ~1.00% separation,
  DOGEUSDT ~2.00%, SOLUSDT no filter at all, on held-out tails checked
  directly). New `backtest.PerSymbolMaCrossoverSeparationCalibrationTest`
  swept the same 0.00%-10.00% grid E8-F1-S6 used, per symbol, against
  E8-F1-S4/S8's stricter tune-then-confirm bar (a candidate must first
  beat the `separation=0` baseline at every checkpoint on a symbol's own
  tuning window before its held-out tail is even checked) — a stricter
  bar than E8-F1-S6's own held-out-only one. Result: no ship, for all
  three symbols independently. BTCUSDT's tuning window does produce
  winners (ma>=5.00%, n=64, beating the baseline's aft-cost expectancy
  +0.075%/+0.165%/+0.200% with +0.107%/+0.380%/+0.605%; also ma>=7.00%/
  10.00% at much smaller n) but every one fails held-out confirmation:
  5.00% reverses sharply on a degenerate held-out sample (min -1.284% vs.
  baseline -0.425%, n=6), and 7.00%/10.00% produce zero held-out BUY calls
  to confirm against at all. Notably, BTCUSDT's own true held-out optimum
  (ma>=1.00%, the same value E8-F1-S6's global sweep found) never reaches
  held-out evaluation here at all, since it fails the tuning-window
  pre-selection step (tuning min -0.050% vs. baseline +0.075%) — the
  tune-then-confirm design that worked for E8-F1-S4/S8's RSI/MACD axes
  filters out a real held-out winner on this axis before it's even seen.
  DOGEUSDT and SOLUSDT fail earlier still: neither symbol's tuning window
  ever produces a candidate beating the `separation=0` baseline at every
  checkpoint (every nonzero candidate is worse at some checkpoint on
  both), so neither reaches held-out confirmation at all — the same
  "no tuning-window winner to begin with" shape E8-F3-S4 found for
  DOGEUSDT/SOLUSDT on the ADX axis. `PerSymbolRuleThresholds.OVERRIDES`
  is unchanged (still SOLUSDT-only, from E8-F1-S8); `RULE_TABLE_VERSION`
  stays v5 — since no symbol's override actually ships, no new resolution
  logic is added to `PerSymbolRuleThresholds`, so unlike E8-F1-S4/S8's own
  version bumps this gets the same no-production-change treatment
  E8-F1-S6/S9 established. A secondary, out-of-scope finding, consistent
  with E8-F1-S6's own: a ~2.00% separation threshold improves SELL-side
  after-cost expectancy at every checkpoint on all three symbols' own
  held-out tails in this per-symbol split too (e.g. BTCUSDT max +1.213%
  vs. baseline +0.999%; DOGEUSDT max +1.612% vs. +1.206%; SOLUSDT max
  +1.506% vs. +0.844%) — acting on it is E8-F1-S11's separate, chartered
  story, not this one. `SignalRuleEngine`'s and `PerSymbolRuleThresholds`'s
  class Javadocs both gained a new closing paragraph documenting this
  finding. Since this story shipped no production behavior change, no
  live-browser/`SignalServiceTest` end-to-end verification was needed
  beyond the calibration test's own run — the same no-production-change
  precedent E8-F1-S2/S3/S5/S6/E8-F3-S4/E8-F1-S9 established. `./mvnw
  verify`: 552 tests, 0 failures, up from 550. E8-F1-S11 remains open in
  the second follow-up batch.
- E8-F1-S11: wires the MA-crossover separation filter in for SELL calls
  specifically, the fourth and last story of the second E8-F1 follow-up
  batch — the same SELL-only wiring attempt E8-F1-S9 made on the MACD
  axis, chartered here for the MA-crossover axis after E8-F1-S6/S10 each
  flagged a ~2.00% separation threshold as improving SELL-side after-cost
  expectancy uniformly across all three symbols but left it unactioned.
  New `backtest.SellMaCrossoverSeparationCalibrationTest` swept the same
  0.00%-10.00% grid E8-F1-S6/S10 used against E8-F1-S9's own global,
  uniform-across-all-three-symbols ship bar (a candidate must beat the
  `separation=0` SELL-side after-cost-expectancy baseline at every
  checkpoint on all three of BTCUSDT/DOGEUSDT/SOLUSDT's own tuning
  windows simultaneously, before even reaching held-out confirmation).
  Result, unlike E8-F1-S9's own no-ship finding on the MACD axis: **ship**
  — `ma>=2.00%` clears the tuning-window bar on all three symbols at once
  (BTCUSDT -0.342%/-0.475%/-0.497% → -0.181%/-0.428%/-0.424%, n=115;
  DOGEUSDT +0.175%/+0.616%/+0.521% → +0.207%/+0.844%/+0.787%, n=77;
  SOLUSDT -0.528%/-0.216%/-0.216% → -0.414%/-0.134%/-0.133%, n=69, all
  three beating baseline on every symbol) and then confirms on all three
  symbols' own held-out tails too (BTCUSDT +0.736%/+0.963%/+0.999% →
  +1.046%/+1.191%/+1.213%, n=52; DOGEUSDT +0.230%/+1.042%/+1.206% →
  +0.504%/+1.265%/+1.612%, n=36; SOLUSDT +0.357%/+0.647%/+0.844% →
  +1.002%/+1.246%/+1.506%, n=37). New `signal.MaCrossoverSellGate`
  (mirrors `RegimeGatedRuleEngine.applySellGate`'s shape, but — since
  `maMinSeparationPctOfPrice` gates `computeVotes`'s MA vote directly and
  symmetrically, unlike the regime gate's orthogonal ADX input — actually
  re-runs `SignalRuleEngine.evaluate` under a second, stricter threshold
  and keeps the result only if it's still a SELL call, collapsing to
  `NO_STRONG_SIGNAL` otherwise) is wired into
  `SignalService.computeSignalWithProvenance` for crypto tickers only
  (`MaCrossoverSellGate.sellGateAppliesTo`, same crypto-only scoping
  `RegimeGatedRuleEngine.sellGateAppliesTo` uses, since zero stock
  evidence exists for this axis), composing with the already-wired regime
  gate in either order since both only ever downgrade an already-resolved
  SELL call. `RULE_TABLE_VERSION` bumps v5→v6 — a real behavior change,
  the same treatment E8-F3-S3's v3→v4 bump got.
  `BacktestHarness.run` gained a 7-arg overload
  (`applyMaCrossoverSellGate`, default `false`) so `LiveDriftBaselineTest`
  could replay both wired SELL-only gates' combined behavior against the
  real BTCUSDT/DOGEUSDT fixtures; `LiveDriftBaseline`'s SELL constants
  were genuinely recomputed again (not just relabeled), moving from their
  v5 values (MIN 0.033652, MID 0.180769, MAX 0.222951) to v6 (MIN
  0.067244, MID 0.165253, MAX 0.241016) — MIN/MAX moved up as expected
  from dropping more weak-performing SELL calls, MID moved down slightly
  since the two gates don't remove a strictly nested set of calls. BUY
  constants stay byte-identical (neither gate ever touches BUY),
  confirmed by `LiveDriftBaselineTest`'s own unchanged BUY assertions.
  Fixture fallout caught by `./mvnw verify`: `OrderCsvExporterTest`'s
  hardcoded `v5` CSV-row literal and `StockPerSymbolRsiOverboughtCalibrationTest`'s
  `(v5/current default)` print-label literal both moved to v6; and a
  real, more substantive gap — `SignalServiceTest`'s existing
  `bearishIndicators()` fixture had MA separation at 0, which the newly-
  wired gate would now downgrade from its expected `BEARISH_UNANIMOUS` to
  `BEARISH_MAJORITY` on the one test exercising a TRENDING-regime crypto
  SELL call; fixed by bumping that fixture's separation to 3.00%
  (comfortably past 2.00%), the same "bump the fixture past the new
  gate's threshold" fix E8-F1-S8 applied to its own SOLUSDT MACD-override
  fallout. New test coverage: `MaCrossoverSellGateTest` (mirrors
  `RegimeGatedRuleEngineTest`'s style, but with real `MacdResult`/
  `MovingAverageResult` fixtures since this gate re-derives votes rather
  than taking a pure enum input — covers insufficient-separation
  suppression, sufficient-separation passthrough, a
  BEARISH_UNANIMOUS→BEARISH_MAJORITY downgrade-not-suppression case, BUY
  passthrough, HOLD-cause passthrough, and the crypto/stock scoping), plus
  four new real (unmocked) `SignalServiceTest` cases exercising the actual
  `SignalService`→`MaCrossoverSellGate`→`SignalRuleEngine.evaluate` path
  end to end (insufficient/sufficient separation, BUY unaffected, stock
  ticker unaffected). Docker wasn't available in this session (same
  recurring blocker prior E8/E6 stories hit), so those `SignalServiceTest`
  cases stood in for the `run` skill's normal live-browser verification,
  the same fallback E8-F1-S4/E8-F3-S3/E8-F1-S8 used for their own
  shipped-value changes. `./mvnw verify`: 566 tests, 0 failures, up from
  552. This closes out the second E8-F1 follow-up batch (E8-F1-S8 through
  S11); E8's backlog is fully complete again.
- E8-F3-S6: calibrates `WeightedVoteRuleEngine.WEIGHTED_MAJORITY_FRACTION`,
  found in a sweep for flagged-but-unactioned findings — its own Javadoc
  had flagged it as sweep-worthy since E8-F3-S1, but the constant was
  structurally inert (an explicit `totalWeight.signum() <= 0` guard made
  it unreachable) until E8-F3-S5 gave `IndicatorWeights.DEFAULT` a real
  nonzero weight (`macdWeight = 0.714`). Worked out from `evaluate`'s own
  code before running anything: with only MACD nonzero-weighted, a
  lone-or-2-of-3 vote's weighted sum can only ever be 0.714 (MACD voted)
  or 0.000 (it didn't), so the constant's entire real-valued range
  collapses to exactly three behavioral regimes, not a continuum —
  `fraction == 0` (most permissive, every lone/2-of-3 vote promotes),
  `0 < fraction <= 1` (only MACD-inclusive votes promote — every value
  here, including the shipped 0.5, is provably byte-identical), and
  `fraction > 1` (least permissive, only UNANIMOUS ever resolves a call).
  New 8-arg `WeightedVoteRuleEngine.evaluate` overload takes an explicit
  `majorityFraction` (the 7-arg overload delegates to it with the static
  constant, zero behavior change for every existing caller) and new
  `BacktestHarness.runCombinedCallExpectancy` scores the combined BUY/SELL
  rule-table call (not a single indicator's vote) at the same
  15-day/TP15%/SL9% horizon `macdWeight` was itself calibrated at.
  `WeightedMajorityFractionCalibrationTest` swept one candidate per regime
  (0.00, 0.50, 1.50) against the full BTCUSDT+DOGEUSDT tuning fixtures.
  Result: no ship, stays 0.5. `0.00` produced a report byte-identical to
  `0.50` on both fixtures — not a general mathematical guarantee, but a
  real property of this data (a throwaway probe found zero RSI-only/
  MA-only lone/2-of-3 votes across either fixture; MACD's histogram is
  essentially never exactly zero, so every lone/2-of-3 vote already
  includes it). `1.50` produced zero scored calls on both fixtures —
  BULLISH_UNANIMOUS/BEARISH_UNANIMOUS never fire in this data at all (the
  same finding `WeightedVoteBacktestTest`/E8-F3-S1 already documented),
  so disabling majority resolution disables the engine's output entirely.
  Net: 0.00 ties the default, everything above 1 is strictly worse (an
  always-empty population) — no candidate clears "beats the default," so
  no out-of-sample validation step was needed either. `WEIGHTED_MAJORITY_FRACTION`
  stays 0.5; `WeightedVoteRuleEngine` stays unwired — no
  `SignalService`/`OrderService`/`RULE_TABLE_VERSION` change. `./mvnw
  verify`: 570 tests, 0 failures, up from 566.
- E8-F1-S12: evaluates AAPL — this repo's only stock fixture — against the
  two axes that shipped real production changes since E8-F1-S7 first
  gathered stock evidence: the per-symbol `macdMinHistogramMagnitudePct`
  mechanism (E8-F1-S8, SOLUSDT-only today) and the already-wired,
  crypto-only `MaCrossoverSellGate` (E8-F1-S11). Neither had ever been
  checked against a stock before — both shipped purely "for lack of stock
  evidence," not because stock evidence contradicted them. New
  `StockPerSymbolMacdHistogramMagnitudeCalibrationTest` and
  `StockMaCrossoverSeparationCalibrationTest` mirror
  `StockPerSymbolRsiOverboughtCalibrationTest` (E8-F1-S7)'s own template. A
  throwaway probe (written, run once, deleted before committing) confirmed
  the existing crypto candidate grids (0.00%-2.00% for MACD, 0.00%-10.00%
  for MA separation) remain appropriately sized for AAPL's own
  distributions, so both were reused verbatim. Result: **no ship on either
  axis, but AAPL's tuning window genuinely produces tuning-window winners
  this time** (unlike DOGEUSDT/SOLUSDT's own "no tuning-window winner to
  begin with" shape on these axes) — `macd>=0.50%/0.75%` and
  `ma>=1.00%/2.00%/3.00%/4.00%` each beat their respective baselines at
  every tuning-window checkpoint, but every single one fails held-out
  confirmation, most commonly at the MIN checkpoint specifically. A
  distinct, narrower check found the more notable result: the
  already-shipped `MaCrossoverSellGate` value (`ma>=2.00%`) actively makes
  AAPL's own SELL-side after-cost expectancy uniformly *worse*, not
  better, at all six checkpoints checked (tuning and held-out) — a clean
  contradiction of the crypto-wide finding that gate's scoping already
  relies on, the second time a stock has actively contradicted a
  crypto-wide pattern in this backlog (the first being E8-F1-S7's own
  regime-gate finding). `PerSymbolRuleThresholds.OVERRIDES` is unchanged
  (still SOLUSDT-only); `MaCrossoverSellGate.sellGateAppliesTo` stays
  crypto-only, now backed by active contradicting stock evidence rather
  than merely absent evidence — its own Javadoc updated accordingly, same
  "gap closed with negative evidence" treatment E8-F1-S7 gave
  `PerSymbolRuleThresholds`/`RegimeGatedRuleEngine`. No `RULE_TABLE_VERSION`
  bump — nothing ships. Since this story shipped no production behavior
  change, no live-browser/`SignalServiceTest` end-to-end verification was
  needed beyond the two new calibration tests' own runs, the same
  no-production-change precedent E8-F1-S2/S3/S5/S6/S7/E8-F3-S4/E8-F1-S9/
  S10/E8-F3-S6 established. `./mvnw verify`: 580 tests, 0 failures, up
  from 570.

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
